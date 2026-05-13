/**
 * 埋点封装：本期 console.log + uni.reportAnalytics
 * 上线后可对接 ALS / 自建上报
 */

export type TrackEvent = string

/**
 * 上报埋点
 * @param event 事件名（建议在 TrackEvent 中维护枚举）
 * @param params 自定义参数（小程序 reportAnalytics 仅支持基础类型）
 */
export function track(event: TrackEvent, params: Record<string, string | number | boolean> = {}): void {
  console.log('[track]', event, params)
  try {
    // uni.reportAnalytics 仅在 mp-weixin 且事件已在小程序后台配置时生效
    // #ifdef MP-WEIXIN
    uni.reportAnalytics(event, params)
    // #endif
  } catch (e) {
    console.warn('[track] 上报失败', event, e)
  }
}
