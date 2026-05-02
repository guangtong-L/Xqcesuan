/**
 * 用户 store：登录态 + 资料 + 签到
 */
import { defineStore } from 'pinia'
import { login as loginApi } from '@/api/auth'
import { setStorage, getStorage, removeStorage } from '@/utils/storage'
import type { Gender, DimensionKey } from '@/api/types'

export interface UserProfile {
  nickname: string
  gender: Gender
  birthday: string         // yyyy-MM-dd
  birthHour: number        // 0-23 / -1
  tags: DimensionKey[]
  agreed: boolean          // 协议勾选
}

interface UserState {
  openid: string
  token: string
  profile: UserProfile | null
  signInDays: number
  lastSignDate: string     // yyyy-MM-dd
}

const KEY_PROFILE = 'user_profile'
const KEY_TOKEN = 'token'
const KEY_OPENID = 'openid'
const KEY_SIGN = 'sign_in'

export const useUserStore = defineStore('user', {
  state: (): UserState => ({
    openid: getStorage<string>(KEY_OPENID) || '',
    token: getStorage<string>(KEY_TOKEN) || '',
    profile: getStorage<UserProfile>(KEY_PROFILE),
    signInDays: getStorage<{ days: number; date: string }>(KEY_SIGN)?.days || 0,
    lastSignDate: getStorage<{ days: number; date: string }>(KEY_SIGN)?.date || ''
  }),

  getters: {
    isLogged: (s): boolean => !!s.token,
    hasProfile: (s): boolean => !!s.profile && !!s.profile.birthday
  },

  actions: {
    /**
     * 静默登录：仅 mp-weixin
     * 失败不抛错，外层不阻断浏览
     */
    async silentLogin(): Promise<void> {
      // #ifdef MP-WEIXIN
      try {
        const wxRes = await new Promise<{ code: string }>((resolve, reject) => {
          uni.login({
            provider: 'weixin',
            success: (r) => resolve({ code: r.code }),
            fail: (e) => reject(e)
          })
        })
        const data = await loginApi({ code: wxRes.code })
        this.openid = data.openid
        this.token = data.token
        // token 7 天，提前 5min 失效
        const ttl = 7 * 24 * 60 * 60 * 1000 - 5 * 60 * 1000
        setStorage(KEY_OPENID, data.openid, ttl)
        setStorage(KEY_TOKEN, data.token, ttl)
      } catch (e) {
        console.warn('[user] 静默登录失败', e)
        throw e
      }
      // #endif
    },

    saveProfile(p: UserProfile): void {
      this.profile = p
      setStorage(KEY_PROFILE, p)
    },

    /**
     * 签到：每日仅一次，本地累加
     * @returns 当前连续天数（如果今天已签返回原值）
     */
    signIn(): number {
      const today = new Date()
      const y = today.getFullYear()
      const m = String(today.getMonth() + 1).padStart(2, '0')
      const d = String(today.getDate()).padStart(2, '0')
      const todayStr = `${y}-${m}-${d}`
      if (this.lastSignDate === todayStr) return this.signInDays

      // 判断是否连续：昨日签过则 +1，否则重置为 1
      const yesterday = new Date(today)
      yesterday.setDate(today.getDate() - 1)
      const yStr = `${yesterday.getFullYear()}-${String(yesterday.getMonth() + 1).padStart(2, '0')}-${String(yesterday.getDate()).padStart(2, '0')}`
      this.signInDays = this.lastSignDate === yStr ? this.signInDays + 1 : 1
      this.lastSignDate = todayStr
      setStorage(KEY_SIGN, { days: this.signInDays, date: todayStr })
      return this.signInDays
    },

    logout(): void {
      this.openid = ''
      this.token = ''
      this.profile = null
      removeStorage(KEY_OPENID)
      removeStorage(KEY_TOKEN)
      removeStorage(KEY_PROFILE)
    }
  }
})
