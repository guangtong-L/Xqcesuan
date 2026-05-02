/**
 * 接口契约 TS 类型定义
 * 与 docs/02-技术方案与接口契约.md 完全对齐，字段不允许私自改名
 */

// ===== 通用 =====
export interface ApiResponse<T = unknown> {
  code: number
  message: string
  data: T
  traceId?: string
}

// ===== 4.1 登录 =====
export interface LoginReq {
  code: string
}

export interface LoginResp {
  openid: string
  token: string
  expireAt: string
  isNew: boolean
}

// ===== 4.2 运势计算 =====
export type Gender = 'M' | 'F' | 'U'
export type CalendarType = 'solar' | 'lunar'
export type DimensionKey = 'love' | 'career' | 'wealth' | 'health'
export type FortuneLevel = '大吉' | '吉' | '平' | '凶'

export interface FortuneCalcReq {
  birthday: string         // yyyy-MM-dd
  birthHour?: number       // 0-23 / -1 未知
  gender: Gender
  tags?: DimensionKey[]    // 最多 4
  calendarType?: CalendarType
}

export interface Dimension {
  key: DimensionKey
  name: string
  score: number
  text: string
}

export interface LuckyTriple {
  color: string
  colorHex: string
  number: number
  direction: string
}

export interface FortuneResp {
  recordId: string
  date: string             // yyyy-MM-dd
  zodiac: string
  score: number            // 1-100
  level: FortuneLevel
  summary: string
  dimensions: Dimension[]
  yi: string[]
  ji: string[]
  lucky: LuckyTriple
  hasDeepContent: boolean
  unlocked: boolean
  tips: string
}

// ===== 4.3 历史列表 =====
export interface HistoryItem {
  recordId: string
  date: string
  zodiac: string
  score: number
  level: FortuneLevel
  summary: string
  createdAt: string
}

export interface HistoryListReq {
  page: number
  pageSize: number
}

export interface HistoryListResp {
  page: number
  pageSize: number
  total: number
  list: HistoryItem[]
}

// ===== 4.4 / 4.5 删除 =====
export interface DeleteResp {
  deleted: number
}

// ===== 4.6 解锁 =====
export interface UnlockReq {
  recordId: string
  adToken: string
}

export interface DeepContent {
  monthTrend: string
  yearKeyword: string[]
  advice: string[]
  compatibleZodiac: string[]
}

export interface UnlockResp {
  recordId: string
  unlocked: boolean
  deepContent: DeepContent
}

// ===== 4.7 统计 =====
export interface StatsTodayResp {
  date: string
  totalCalc: number
  uniqueUser: number
  unlockCount: number
  adRevenueEstimate: number
}

// ===== 4.8 八字流年 =====
export interface BaziReq {
  birthday: string  // yyyy-MM-dd
  gender?: Gender
}
export interface QuarterVO {
  key: 'spring' | 'summer' | 'autumn' | 'winter'
  name: string
  score: number
  text: string
}
export interface BaziResp {
  year: number
  yearOverview: string
  quarters: QuarterVO[]
  keywords: string[]
  suggestion: string
  tips: string
}

// ===== 4.9 星座匹配 =====
export interface MatchReq {
  zodiacA: string
  zodiacB: string
}
export interface MatchResp {
  zodiacA: string
  zodiacB: string
  score: number
  level: '绝配' | '合拍' | '中等' | '待磨合'
  summary: string
  loveText: string
  tipText: string
  tips: string
}

// ===== 4.10 幸运签 =====
export interface SignResp {
  signNo: number
  signName: string  // 上上签 / 上签 / 中签 / 下签
  poem: string[]    // 4 句
  explain: string
  yi: string[]
  ji: string[]
  date: string
  tips: string
}
