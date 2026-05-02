/**
 * 扩展功能：八字流年 / 星座匹配 / 幸运签
 */
import { request } from './request'
import type {
  BaziReq, BaziResp,
  MatchReq, MatchResp,
  SignResp
} from './types'

/** 八字流年（按当年生成） */
export function calcBazi(params: BaziReq): Promise<BaziResp> {
  return request<BaziResp>('/api/fortune/bazi', 'POST', params as unknown as Record<string, unknown>)
}

/** 星座匹配（顺序无关） */
export function matchZodiac(params: MatchReq): Promise<MatchResp> {
  return request<MatchResp>('/api/fortune/match', 'POST', params as unknown as Record<string, unknown>)
}

/** 今日幸运签（同一人当日结果一致） */
export function drawSign(): Promise<SignResp> {
  return request<SignResp>('/api/fortune/sign', 'GET')
}
