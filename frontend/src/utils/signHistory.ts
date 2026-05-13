import { getStorage, setStorage, removeStorage } from '@/utils/storage'
import type { SignResp } from '@/api/types'

const KEY = 'daily_sign_history'
const MAX_HISTORY = 30

export interface SignHistoryItem {
  recordId: string
  date: string
  signNo: number
  signName: string
  summary: string
  createdAt: string
}

export function buildSignRecord(sign: SignResp): SignHistoryItem {
  return {
    recordId: `sign_${sign.date}_${sign.signNo}`,
    date: sign.date,
    signNo: sign.signNo,
    signName: sign.signName,
    summary: sign.explain,
    createdAt: new Date().toISOString()
  }
}

export function saveSignHistory(sign: SignResp): void {
  const record = buildSignRecord(sign)
  const next = [record, ...listSignHistory().filter((item) => item.recordId !== record.recordId)]
    .slice(0, MAX_HISTORY)
  setStorage(KEY, next)
}

export function listSignHistory(): SignHistoryItem[] {
  return getStorage<SignHistoryItem[]>(KEY) || []
}

export function deleteSignHistory(recordId: string): number {
  const current = listSignHistory()
  const next = current.filter((item) => item.recordId !== recordId)
  setStorage(KEY, next)
  return current.length - next.length
}

export function clearSignHistory(): number {
  const total = listSignHistory().length
  removeStorage(KEY)
  return total
}
