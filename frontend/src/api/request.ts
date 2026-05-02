/**
 * 请求封装：根据 USE_MOCK 切换两种实现
 *  - true：从 src/static/mock/*.json 读静态假数据
 *  - false：调用 wx.cloud.callContainer 走云托管
 *
 * 本期默认 USE_MOCK = true（后端没起也能跑）
 * 与后端联调时把下方常量改 false；并配置 WX_CLOUD_ENV
 */

import type { ApiResponse } from './types'
import { getStorage } from '@/utils/storage'

// =============== 关键开关 ===============
export const USE_MOCK = true
// 联调时填云托管环境 ID（云开发控制台首页可见，类似 prod-xxxxxx）
const WX_CLOUD_ENV = ''
// 云托管服务名（与 backend 部署时一致）
const WX_SERVICE_NAME = 'fortune-backend'
// =======================================

export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE'

interface RequestOptions {
  path: string
  method?: HttpMethod
  data?: Record<string, unknown>
}

/**
 * mock 路径映射：path → 静态 JSON 模块
 * 后续新增接口时在这里补一行即可
 */
const MOCK_MAP: Record<string, () => Promise<{ default: ApiResponse<unknown> }>> = {
  'POST /api/auth/login': () => import('@/static/mock/login.json'),
  'POST /api/fortune/calc': () => import('@/static/mock/calc.json'),
  'GET /api/fortune/history': () => import('@/static/mock/history.json'),
  'POST /api/fortune/unlock': () => import('@/static/mock/unlock.json'),
  // 删除接口直接构造响应，无需 JSON
}

function buildMockKey(method: HttpMethod, path: string): string {
  // 去除 query string 与路径参数尾段
  const pure = path.split('?')[0].replace(/\/[^/]+$/, (m) => {
    // 形如 /api/fortune/history/rec_xxx → 命中删除单条
    if (/^\/rec_/.test(m) || /^\/\d+$/.test(m)) return ''
    return m
  })
  return `${method} ${pure}`
}

async function mockRequest<T>(opts: RequestOptions): Promise<T> {
  const method = opts.method || 'GET'
  // 模拟 600ms 网络延迟
  await new Promise((r) => setTimeout(r, 600))

  // 删除类接口本地构造
  if (method === 'DELETE' && opts.path.startsWith('/api/fortune/history')) {
    return { deleted: 1 } as unknown as T
  }

  const key = buildMockKey(method, opts.path)
  const loader = MOCK_MAP[key]
  if (!loader) {
    throw new Error(`[mock] 未配置该接口的假数据：${key}`)
  }
  const mod = await loader()
  const resp = mod.default as ApiResponse<T>
  if (resp.code !== 0) {
    throw new Error(resp.message || '业务异常')
  }
  return resp.data
}

/**
 * 真实请求：wx.cloud.callContainer
 * 仅在微信小程序环境可用；H5 调试时建议保持 USE_MOCK=true
 */
function realRequest<T>(opts: RequestOptions): Promise<T> {
  return new Promise((resolve, reject) => {
    // #ifdef MP-WEIXIN
    const token = getStorage<string>('token') || ''
    const header: Record<string, string> = {
      'X-WX-SERVICE': WX_SERVICE_NAME,
      'content-type': 'application/json'
    }
    if (token) header['Authorization'] = `Bearer ${token}`

    // @ts-ignore wx 全局对象在小程序环境注入
    if (typeof wx === 'undefined' || !wx.cloud || !wx.cloud.callContainer) {
      reject(new Error('当前环境不支持 wx.cloud.callContainer'))
      return
    }

    // @ts-ignore
    wx.cloud.callContainer({
      config: { env: WX_CLOUD_ENV },
      path: opts.path,
      method: opts.method || 'GET',
      data: opts.data || {},
      header,
      success: (res: { data: ApiResponse<T> }) => {
        const body = res.data
        if (!body || typeof body.code !== 'number') {
          reject(new Error('响应格式异常'))
          return
        }
        if (body.code !== 0) {
          reject(new Error(body.message || `业务错误(${body.code})`))
          return
        }
        resolve(body.data)
      },
      fail: (err: { errMsg?: string }) => {
        reject(new Error(err?.errMsg || '网络异常'))
      }
    })
    // #endif

    // #ifndef MP-WEIXIN
    reject(new Error('非微信小程序环境，请将 USE_MOCK 设为 true'))
    // #endif
  })
}

/**
 * 统一请求入口
 */
export function request<T>(
  path: string,
  method: HttpMethod = 'GET',
  data?: Record<string, unknown>
): Promise<T> {
  const opts: RequestOptions = { path, method, data }
  if (USE_MOCK) return mockRequest<T>(opts)
  return realRequest<T>(opts)
}
