/**
 * 运势相关接口
 */
import { request } from './request'
import type {
  FortuneCalcReq,
  FortuneResp,
  HistoryListReq,
  HistoryListResp,
  DeleteResp,
  UnlockReq,
  UnlockResp,
  StatsTodayResp
} from './types'

/** 4.2 计算今日运势 */
export function calcFortune(params: FortuneCalcReq): Promise<FortuneResp> {
  return request<FortuneResp>('/api/fortune/calc', 'POST', params as unknown as Record<string, unknown>)
}

/** 4.3 历史列表 */
export function getHistoryList(params: HistoryListReq): Promise<HistoryListResp> {
  const qs = `?page=${params.page}&pageSize=${params.pageSize}`
  return request<HistoryListResp>(`/api/fortune/history${qs}`, 'GET')
}

/** 4.4 删除单条 */
export function deleteHistory(recordId: string): Promise<DeleteResp> {
  return request<DeleteResp>(`/api/fortune/history/${recordId}`, 'DELETE')
}

/** 4.5 清空历史 */
export function clearHistory(): Promise<DeleteResp> {
  return request<DeleteResp>('/api/fortune/history', 'DELETE')
}

/** 4.6 解锁深度内容 */
export function unlockFortune(params: UnlockReq): Promise<UnlockResp> {
  return request<UnlockResp>('/api/fortune/unlock', 'POST', params as unknown as Record<string, unknown>)
}

/** 4.7 今日统计（运营接口，前端一般不调） */
export function getStatsToday(): Promise<StatsTodayResp> {
  return request<StatsTodayResp>('/api/stats/today', 'GET')
}
