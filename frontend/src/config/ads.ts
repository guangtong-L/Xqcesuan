/**
 * 流量主广告位集中配置
 *
 * 维护规则：
 * 1. 所有 unitId 在此文件统一定义，禁止散落到组件里
 * 2. dev / prod 各一套，避免开发污染流量主数据
 * 3. 上线前由运营提供真实 unitId，把 PROD 区的 'adunit-xxxx' 替换掉
 *
 * 申请位置：
 *   小程序后台 → 推广 → 流量主 → 广告位管理 → 新建广告位
 *
 * 类型对照：
 *   Banner 原生模板 → '原生模板'（旧版叫 Banner，已合并）
 *   激励视频 → '激励式'
 *   插屏 → '插屏式'
 */

// 是否生产环境（uni-app 默认 production 即上线包）
const IS_PROD = process.env.NODE_ENV === 'production'

const DEV = {
  HOME_BANNER:         'adunit-dev-home-banner',
  RESULT_NATIVE:       'adunit-dev-result-native',
  RESULT_REWARD:       'adunit-dev-result-reward',
  RESULT_INTERSTITIAL: 'adunit-dev-result-interstitial',
  HISTORY_NATIVE:      'adunit-dev-history-native',
  MINE_BANNER:         'adunit-dev-mine-banner'
}

const PROD = {
  HOME_BANNER:         'adunit-xxxxxxxxxxxx',  // TODO: 上线前替换
  RESULT_NATIVE:       'adunit-xxxxxxxxxxxx',
  RESULT_REWARD:       'adunit-xxxxxxxxxxxx',
  RESULT_INTERSTITIAL: 'adunit-xxxxxxxxxxxx',
  HISTORY_NATIVE:      'adunit-xxxxxxxxxxxx',
  MINE_BANNER:         'adunit-xxxxxxxxxxxx'
}

export const ADS = IS_PROD ? PROD : DEV

/**
 * 频控配置
 */
export const AD_LIMIT = {
  /** 激励视频：单页生命周期内最多次数 */
  REWARD_PER_PAGE: 4,
  /** 插屏：单用户单日最多次数 */
  INTERSTITIAL_PER_DAY: 1,
  /** 启动后多少秒内不允许出插屏（避免冷启动打扰） */
  INTERSTITIAL_COLD_GUARD_SEC: 5
}
