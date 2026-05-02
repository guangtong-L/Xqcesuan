/**
 * 广告调用统一封装
 *
 * - 激励视频 showRewardedVideo(unitId)：返回 Promise<boolean>
 *     true  = 完整观看完成（可发放奖励 / 调 unlock 接口）
 *     false = 中途关闭 / 拉取失败 / 用户拒绝
 *
 * - 插屏 showInterstitial(unitId, dailyLimit)：返回 Promise<boolean>
 *     带每日次数本地频控；触发即写 storage
 *
 * - 仅微信小程序生效，其他端 resolve(false) 直接放过，方便 H5 调试
 *
 * - 全部 try/catch 包裹，开发者工具或 SDK 异常不阻塞主流程
 *
 * 线上风险：
 *   1. unlock 接口必须在 onClose(isEnded=true) 后才调用；中途关闭不能解锁
 *   2. 激励视频实例缓存复用（微信文档要求），重复 createRewardedVideoAd 同 unitId 拿到同一实例
 *   3. 插屏频控用本地 storage，清缓存后会重置；线上有需要可改服务端记录
 */

import { track } from './tracker'

const STORAGE_KEY_INTERSTITIAL = 'lastInterstitialDate'
const STORAGE_KEY_LAUNCH_TIME = '__app_launch_ts'

/** 缓存激励视频实例，避免重复创建 */
const rewardCache: Record<string, any> = {}

/**
 * 激励视频
 * @returns 是否完整观看
 */
export function showRewardedVideo(unitId: string): Promise<boolean> {
  return new Promise((resolve) => {
    // #ifndef MP-WEIXIN
    // 非小程序环境直接放过（H5 调试时模拟解锁）
    resolve(false)
    return
    // #endif

    // #ifdef MP-WEIXIN
    try {
      let ad = rewardCache[unitId]
      if (!ad) {
        ad = (wx as any).createRewardedVideoAd({ adUnitId: unitId })
        ad.onError((err: any) => {
          track('ad_reward_fail', { unitId, errMsg: err?.errMsg || '' })
        })
        rewardCache[unitId] = ad
      }

      // 注意：onClose 是事件监听器，重复 add 会重复触发；这里每次都重新挂一次性 close
      const onCloseOnce = (res: any) => {
        ad.offClose(onCloseOnce)
        if (res && res.isEnded) {
          track('ad_reward_complete', { unitId })
          resolve(true)
        } else {
          track('ad_reward_close', { unitId, progress: 'partial' })
          resolve(false)
        }
      }
      ad.onClose(onCloseOnce)

      ad.load().then(() => {
        track('ad_reward_show', { unitId })
        return ad.show()
      }).catch((err: any) => {
        track('ad_reward_fail', { unitId, errMsg: err?.errMsg || '' })
        ad.offClose(onCloseOnce)
        resolve(false)
      })
    } catch (e) {
      console.warn('[ad] reward video error', e)
      resolve(false)
    }
    // #endif
  })
}

/**
 * 插屏广告（带每日次数频控 + 启动 5s 内静默）
 * @param unitId
 * @param dailyLimit 单日上限，默认 1
 */
export function showInterstitial(unitId: string, dailyLimit = 1): Promise<boolean> {
  return new Promise((resolve) => {
    // #ifndef MP-WEIXIN
    resolve(false)
    return
    // #endif

    // #ifdef MP-WEIXIN
    try {
      // 启动 5s 内不弹（避免被判"诱导/打扰"）
      const launch = uni.getStorageSync(STORAGE_KEY_LAUNCH_TIME)
      if (launch && Date.now() - Number(launch) < 5000) {
        resolve(false)
        return
      }

      // 每日频控
      const todayStr = new Date().toISOString().slice(0, 10)
      const last = uni.getStorageSync(STORAGE_KEY_INTERSTITIAL)
      // last 结构：{ date: 'yyyy-MM-dd', count: N }
      const lastObj = (typeof last === 'object' && last) ? last : { date: '', count: 0 }
      const todayCount = lastObj.date === todayStr ? lastObj.count : 0
      if (todayCount >= dailyLimit) {
        resolve(false)
        return
      }

      const ad = (wx as any).createInterstitialAd({ adUnitId: unitId })
      ad.onError((err: any) => {
        track('ad_interstitial_fail', { unitId, errMsg: err?.errMsg || '' })
      })
      ad.show().then(() => {
        track('ad_interstitial_show', { unitId })
        uni.setStorageSync(STORAGE_KEY_INTERSTITIAL, { date: todayStr, count: todayCount + 1 })
        resolve(true)
      }).catch(() => resolve(false))
    } catch (e) {
      console.warn('[ad] interstitial error', e)
      resolve(false)
    }
    // #endif
  })
}

/**
 * App.onLaunch 时调一次，记下启动时间，用于插屏冷启动保护
 */
export function markAppLaunch(): void {
  uni.setStorageSync(STORAGE_KEY_LAUNCH_TIME, Date.now())
}
