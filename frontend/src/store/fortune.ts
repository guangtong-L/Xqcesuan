/**
 * 运势 store：当前结果 + 解锁状态
 */
import { defineStore } from 'pinia'
import { calcFortune, unlockFortune } from '@/api/fortune'
import type { FortuneResp, FortuneCalcReq, DeepContent } from '@/api/types'
import { setStorage, getStorage } from '@/utils/storage'

interface FortuneState {
  current: FortuneResp | null
  deep: DeepContent | null
}

const KEY_CURRENT = 'fortune_current'

export const useFortuneStore = defineStore('fortune', {
  state: (): FortuneState => ({
    current: getStorage<FortuneResp>(KEY_CURRENT),
    deep: null
  }),

  actions: {
    async calc(params: FortuneCalcReq): Promise<FortuneResp> {
      const data = await calcFortune(params)
      this.current = data
      this.deep = null
      // 缓存当日结果，跨页传递用；TTL 一天
      setStorage(KEY_CURRENT, data, 24 * 60 * 60 * 1000)
      return data
    },

    async unlock(adToken: string): Promise<DeepContent> {
      if (!this.current) throw new Error('当前没有运势记录')
      const data = await unlockFortune({ recordId: this.current.recordId, adToken })
      this.deep = data.deepContent
      // 同步 unlocked 标记
      this.current = { ...this.current, unlocked: true }
      setStorage(KEY_CURRENT, this.current, 24 * 60 * 60 * 1000)
      return data.deepContent
    },

    reset(): void {
      this.current = null
      this.deep = null
    }
  }
})
