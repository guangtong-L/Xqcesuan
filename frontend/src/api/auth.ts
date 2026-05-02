/**
 * 鉴权相关接口
 */
import { request } from './request'
import type { LoginReq, LoginResp } from './types'

/**
 * 微信登录：传 wx.login 拿到的 code，换 openid + token
 */
export function login(params: LoginReq): Promise<LoginResp> {
  return request<LoginResp>('/api/auth/login', 'POST', params as unknown as Record<string, unknown>)
}
