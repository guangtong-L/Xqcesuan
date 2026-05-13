<script setup lang="ts">
/**
 * 运势结果详情页
 * 影响平台：H5 / 微信小程序（激励视频/分享/插屏仅小程序生效）
 * 关键交互：未解锁的维度详情需要看激励视频；离开页面触发插屏（每日 1 次频控）
 */
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { useFortuneStore } from '@/store/fortune'
import { useUserStore } from '@/store/user'
import { calcFortune } from '@/api/fortune'
import { track } from '@/utils/tracker'
import ScoreRing from '@/components/ScoreRing/index.vue'
import DimensionCard from '@/components/DimensionCard/index.vue'
import AdBanner from '@/components/AdBanner/index.vue'
import type { Dimension, FortuneResp } from '@/api/types'
import { ADS, AD_LIMIT } from '@/config/ads'
import { showRewardedVideo, showInterstitial } from '@/utils/ad'

const fortuneStore = useFortuneStore()
const userStore = useUserStore()

const loading = ref(false)
const errorMsg = ref('')
const result = computed<FortuneResp | null>(() => fortuneStore.current)
const deep = computed(() => fortuneStore.deep)

// 未成年（<14 岁）屏蔽感情维度
const isUnderage = computed(() => {
  if (!userStore.profile?.birthday) return false
  const birth = new Date(userStore.profile.birthday)
  const ageMs = Date.now() - birth.getTime()
  const age = Math.floor(ageMs / (365.25 * 24 * 60 * 60 * 1000))
  return age < 14
})

const visibleDimensions = computed<Dimension[]>(() => {
  if (!result.value) return []
  return isUnderage.value
    ? result.value.dimensions.filter((d) => d.key !== 'love')
    : result.value.dimensions
})

const rewardCount = ref(0)
const interstitialShown = ref(false)

onMounted(async () => {
  // 没有结果且有 profile，自动 calc 一次
  if (!result.value && userStore.profile) {
    await doCalc()
  }
  if (result.value) {
    track('result_view', { recordId: result.value.recordId, score: result.value.score, level: result.value.level })
  }
})

async function doCalc() {
  if (!userStore.profile) {
    uni.showToast({ title: '请先填写信息', icon: 'none' })
    uni.redirectTo({ url: '/pages/calc/calc' })
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    await fortuneStore.calc({
      birthday: userStore.profile.birthday,
      birthHour: userStore.profile.birthHour,
      gender: userStore.profile.gender,
      tags: userStore.profile.tags,
      calendarType: 'solar'
    })
  } catch (e: any) {
    errorMsg.value = e?.message || '网络开小差，等会再试试'
  } finally {
    loading.value = false
  }
}

/**
 * 点击维度"查看详情"或"解锁开运建议"
 * 触发激励视频，完整观看回调后调 unlock
 */
async function onDimensionUnlock(dim: Dimension) {
  track('result_dimension_click', { dimension: dim.key })
  if (rewardCount.value >= AD_LIMIT.REWARD_PER_PAGE) {
    uni.showToast({ title: '今天看得有点多啦，明天再来', icon: 'none' })
    return
  }
  const ok = await showRewardedVideo(ADS.RESULT_REWARD)
  // #ifdef MP-WEIXIN
  if (!ok) {
    uni.showToast({ title: '完整观看才能解锁哦', icon: 'none' })
    return
  }
  rewardCount.value++
  await doUnlock(`mock_ad_token_${Date.now()}`)
  // #endif
  // #ifndef MP-WEIXIN
  // 非小程序环境：直接走解锁，方便 H5 联调
  await doUnlock('mock_ad_token_h5')
  // #endif
}

async function doUnlock(adToken: string) {
  try {
    await fortuneStore.unlock(adToken)
    if (result.value) track('result_unlock_success', { recordId: result.value.recordId })
    uni.showToast({ title: '已解锁深度内容', icon: 'success' })
  } catch (e: any) {
    uni.showToast({ title: e?.message || '解锁失败', icon: 'none' })
  }
}

function onRetry() {
  track('result_retry_click')
  doCalc()
}

function onShare() {
  track('result_share_click', { way: 'share' })
  uni.showToast({ title: '请点击右上角分享', icon: 'none' })
}

function onSaveImage() {
  track('result_share_click', { way: 'save_image' })
  uni.showToast({ title: '保存图片功能开发中', icon: 'none' })
}

function goCalc() {
  uni.redirectTo({ url: '/pages/calc/calc' })
}

/**
 * 离开页面触发插屏（每日 N 次频控 + 启动 5s 内静默，逻辑封装在 utils/ad.ts）
 */
onUnmounted(() => {
  if (interstitialShown.value) return
  showInterstitial(ADS.RESULT_INTERSTITIAL, AD_LIMIT.INTERSTITIAL_PER_DAY).then((ok) => {
    if (ok) interstitialShown.value = true
  })
})

// 微信原生分享
// #ifdef MP-WEIXIN
const onShareAppMessage = () => ({
  title: result.value ? `今日运势 ${result.value.score} 分，来看看你的~` : '今日运势',
  path: '/pages/index/index'
})
defineExpose({ onShareAppMessage })
// #endif
</script>

<template>
  <view class="result">
    <view v-if="loading" class="result__loading">运势计算中…</view>
    <view v-else-if="errorMsg" class="result__error">
      <text>{{ errorMsg }}</text>
      <button class="result__retry-btn" @click="doCalc">重试</button>
    </view>
    <template v-else-if="result">
      <!-- 顶部摘要 -->
      <view class="result__summary">
        <ScoreRing :score="result.score" :level="result.level" />
        <view class="result__summary-meta">
          <text class="result__zodiac">{{ result.zodiac }}</text>
          <text class="result__date">{{ result.date }}</text>
        </view>
        <text class="result__summary-text">{{ result.summary }}</text>
      </view>

      <!-- 摘要下原生广告位 -->
      <AdBanner :unit-id="ADS.RESULT_NATIVE" />

      <!-- 宜忌 -->
      <view class="result__yiji">
        <view class="result__yiji-col result__yiji-col--yi">
          <text class="result__yiji-title">宜</text>
          <text v-for="(t, i) in result.yi" :key="'y'+i" class="result__yiji-item">{{ t }}</text>
        </view>
        <view class="result__yiji-col result__yiji-col--ji">
          <text class="result__yiji-title">忌</text>
          <text v-for="(t, i) in result.ji" :key="'j'+i" class="result__yiji-item">{{ t }}</text>
        </view>
      </view>

      <!-- 四大维度 -->
      <view class="result__dims">
        <DimensionCard
          v-for="d in visibleDimensions"
          :key="d.key"
          :dimension="d"
          :unlocked="result.unlocked"
          @unlock="onDimensionUnlock(d)"
        />
      </view>

      <!-- 幸运三件套 -->
      <view class="result__lucky">
        <view class="result__lucky-item">
          <text class="result__lucky-label">幸运色</text>
          <view class="result__lucky-color" :style="{ background: result.lucky.colorHex }"></view>
          <text>{{ result.lucky.color }}</text>
        </view>
        <view class="result__lucky-item">
          <text class="result__lucky-label">幸运数字</text>
          <text class="result__lucky-num">{{ result.lucky.number }}</text>
        </view>
        <view class="result__lucky-item">
          <text class="result__lucky-label">幸运方位</text>
          <text class="result__lucky-num">{{ result.lucky.direction }}</text>
        </view>
      </view>

      <!-- 解锁深度内容 -->
      <view v-if="result.hasDeepContent" class="result__deep">
        <view v-if="!deep" class="result__deep-locked" @click="onDimensionUnlock(result.dimensions[0])">
          <text class="result__deep-icon">🔒</text>
          <text>看 15 秒小视频，解锁本月走势 + 开运建议</text>
        </view>
        <view v-else class="result__deep-unlocked">
          <text class="result__deep-title">本月走势</text>
          <text class="result__deep-text">{{ deep.monthTrend }}</text>
          <text class="result__deep-title">年度关键词</text>
          <view class="result__deep-tags">
            <text v-for="(k, i) in deep.yearKeyword" :key="i" class="result__deep-tag">{{ k }}</text>
          </view>
          <text class="result__deep-title">开运建议</text>
          <text v-for="(a, i) in deep.advice" :key="'a'+i" class="result__deep-text">· {{ a }}</text>
          <text class="result__deep-title">速配星座</text>
          <text class="result__deep-text">{{ deep.compatibleZodiac.join('、') }}</text>
        </view>
      </view>

      <!-- 操作区 -->
      <view class="result__actions">
        <button class="result__btn" @click="onShare">分享好友</button>
        <button class="result__btn" @click="onSaveImage">保存图片</button>
        <button class="result__btn result__btn--primary" @click="onRetry">再测一次</button>
      </view>

      <text class="result__disclaimer">{{ result.tips || '本结果仅供娱乐参考，不构成任何决策建议。' }}</text>
    </template>

    <view v-else class="result__empty">
      <text>还没有运势数据，先去填写信息吧～</text>
      <button class="result__btn result__btn--primary" @click="goCalc">去填写</button>
    </view>
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.result {
  padding: $space-5;

  /* ========== 加载/错误/空 三态 ========== */
  &__loading, &__error, &__empty {
    text-align: center;
    color: $color-text-body;
    padding: 120rpx 0;
    font-size: $fs-lg;
  }
  &__retry-btn {
    @include btn-primary;
    margin-top: $space-5;
    width: 60%;
    font-size: $fs-base;
  }

  /* ========== 顶部摘要 ========== */
  &__summary {
    @include card-base;
    padding: 40rpx $space-5;
    text-align: center;
    border-radius: $radius-xl;
  }
  &__summary-meta {
    display: flex;
    justify-content: center;
    gap: $space-4;
    margin-top: $space-3;
    font-size: $fs-md;
    color: $color-text-strong;
  }
  &__zodiac {
    color: $color-primary;
    font-weight: $fw-medium;
  }
  &__date {
    color: $color-text-sub;
  }
  &__summary-text {
    display: block;
    margin-top: $space-4;
    font-size: $fs-base;
    color: $color-text-strong;
    line-height: $lh-base;
  }

  /* ========== 宜 / 忌 ========== */
  &__yiji {
    display: flex;
    gap: 20rpx;
    margin-top: $space-4;
  }
  &__yiji-col {
    flex: 1;
    @include card-base;
    &--yi { border-top: 6rpx solid $color-health; }
    &--ji { border-top: 6rpx solid $color-danger; }
  }
  &__yiji-title {
    font-size: $fs-lg;
    font-weight: $fw-semi;
    color: $color-text;
    display: block;
    margin-bottom: $space-3;
  }
  &__yiji-item {
    display: block;
    font-size: $fs-md;
    color: $color-text-strong;
    line-height: $lh-loose;
  }

  /* ========== 4 维度 ========== */
  &__dims {
    margin-top: $space-4;
    display: flex;
    flex-direction: column;
    gap: 20rpx;
  }

  /* ========== 幸运三件套 ========== */
  &__lucky {
    margin-top: $space-4;
    @include card-base;
    padding: $space-5;
    display: flex;
    justify-content: space-around;
    text-align: center;
  }
  &__lucky-item {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: $space-2;
    font-size: $fs-md;
  }
  &__lucky-label {
    color: $color-text-sub;
    font-size: $fs-xs;
  }
  &__lucky-color {
    width: 56rpx;
    height: 56rpx;
    border-radius: 50%;
    box-shadow: $shadow-sm;
    border: 2rpx solid #fff;
  }
  &__lucky-num {
    font-size: $fs-2xl;
    font-weight: $fw-semi;
    color: $color-primary;
  }

  /* ========== 深度内容 ========== */
  &__deep {
    margin-top: $space-4;
    @include card-base;
    padding: $space-5;
  }
  &__deep-locked {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: $space-3;
    color: $color-text-strong;
    font-size: $fs-base;
    padding: $space-4 0;
    background: $color-primary-light;
    border-radius: $radius-md;
    &:active { opacity: 0.85; }
  }
  &__deep-icon {
    font-size: $fs-2xl;
  }
  &__deep-unlocked {
    display: flex;
    flex-direction: column;
    gap: $space-3;
  }
  &__deep-title {
    font-size: $fs-base;
    font-weight: $fw-semi;
    color: $color-text;
    margin-top: $space-3;
  }
  &__deep-text {
    font-size: $fs-md;
    color: $color-text-body;
    line-height: $lh-loose;
  }
  &__deep-tags {
    display: flex;
    flex-wrap: wrap;
    gap: $space-2;
  }
  &__deep-tag {
    background: $color-primary-light;
    color: $color-primary;
    border-radius: $radius-pill;
    padding: 8rpx $space-4;
    font-size: $fs-sm;
  }

  /* ========== 操作区 ========== */
  &__actions {
    margin-top: $space-5;
    display: flex;
    gap: $space-3;
  }
  &__btn {
    flex: 1;
    background: $color-card;
    color: $color-text-body;
    border-radius: $radius-pill;
    font-size: $fs-md;
    padding: 18rpx 0;
    box-shadow: $shadow-sm;
    &--primary {
      background: linear-gradient(135deg, $color-primary 0%, $color-primary-soft 100%);
      color: #fff;
      box-shadow: $shadow-md;
      font-weight: $fw-medium;
    }
    &:active { opacity: 0.85; }
  }

  &__disclaimer {
    @include disclaimer;
  }
}
</style>
