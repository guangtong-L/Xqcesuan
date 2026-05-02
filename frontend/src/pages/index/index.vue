<script setup lang="ts">
/**
 * 首页：今日日期 + 主 CTA + 4 卡片 + 签到 + Banner
 * 影响平台：H5 / 微信小程序 / App（广告仅小程序生效）
 */
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/store/user'
import { formatDate } from '@/utils/date'
import { track } from '@/utils/tracker'
import AdBanner from '@/components/AdBanner/index.vue'
import { ADS } from '@/config/ads'

const userStore = useUserStore()

const today = ref(formatDate())
const dailyTip = ref('愿你今天像清晨的第一缕阳光，温柔而坚定。')

interface FeatureCard {
  key: 'today' | 'bazi' | 'match' | 'sign'
  title: string
  desc: string
  emoji: string
  enabled: boolean
}

// UI 优化：每张卡片配 emoji 图标 + 配色，强化辨识度
const cards = ref<FeatureCard[]>([
  { key: 'today', title: '今日运势', desc: '3 秒看完今日宜忌', emoji: '✨', enabled: true },
  { key: 'bazi',  title: '八字流年', desc: '看年度走势',       emoji: '🔮', enabled: true },
  { key: 'match', title: '星座匹配', desc: '看 TA 与你合不合', emoji: '💞', enabled: true },
  { key: 'sign',  title: '幸运签',   desc: '每日抽一签',       emoji: '🎋', enabled: true }
])

// 卡片 key → 路由
const ROUTE: Record<FeatureCard['key'], string> = {
  today: '',           // 走 onClickCalc 的 result 流程
  bazi:  '/pages/bazi/bazi',
  match: '/pages/match/match',
  sign:  '/pages/sign/sign'
}

onMounted(() => {
  track('page_view', { page: 'index' })
})

function onClickCalc() {
  track('click_calc', {})
  if (userStore.hasProfile) {
    uni.navigateTo({ url: '/pages/result/result' })
  } else {
    uni.navigateTo({ url: '/pages/calc/calc' })
  }
}

function onClickCard(card: FeatureCard) {
  track('click_card', { key: card.key })
  if (!card.enabled) {
    uni.showToast({ title: '敬请期待', icon: 'none' })
    return
  }
  // 今日运势：复用主流程
  if (card.key === 'today') {
    onClickCalc()
    return
  }
  // 八字流年：必须先有 profile
  if (card.key === 'bazi' && !userStore.hasProfile) {
    uni.showToast({ title: '请先填写信息', icon: 'none' })
    setTimeout(() => uni.navigateTo({ url: '/pages/calc/calc' }), 800)
    return
  }
  uni.navigateTo({ url: ROUTE[card.key] })
}

function onSignIn() {
  const days = userStore.signIn()
  uni.showToast({ title: `已连续签到 ${days} 天`, icon: 'success' })
}
</script>

<template>
  <view class="home">
    <!-- Hero：紫色渐变 + 装饰星点（模拟夜空治愈感） -->
    <view class="home__hero">
      <view class="home__hero-deco home__hero-deco--1">·</view>
      <view class="home__hero-deco home__hero-deco--2">·</view>
      <view class="home__hero-deco home__hero-deco--3">·</view>
      <text class="home__date">{{ today }}</text>
      <text class="home__tip">{{ dailyTip }}</text>
      <button class="home__cta" @click="onClickCalc">开始今日测算</button>
    </view>

    <!-- 4 个功能卡片 -->
    <view class="home__cards">
      <view
        v-for="c in cards"
        :key="c.key"
        class="home__card"
        :class="[`home__card--${c.key}`, { 'home__card--disabled': !c.enabled }]"
        @click="onClickCard(c)"
      >
        <text class="home__card-emoji">{{ c.emoji }}</text>
        <text class="home__card-title">{{ c.title }}</text>
        <text class="home__card-desc">{{ c.desc }}</text>
      </view>
    </view>

    <!-- 签到行 -->
    <view class="home__sign" @click="onSignIn">
      <text class="home__sign-icon">🌟</text>
      <text class="home__sign-text">已连续签到 {{ userStore.signInDays }} 天，点我签到</text>
    </view>

    <AdBanner :unit-id="ADS.HOME_BANNER" />

    <text class="home__disclaimer">本内容仅供娱乐参考，请理性看待。</text>
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.home {
  padding: $space-5;

  /* ========== Hero ========== */
  &__hero {
    @include hero-gradient-purple;
    @include card-hero;
    position: relative;
    text-align: center;
    overflow: hidden;
  }
  /* 装饰星点：增加治愈氛围，弱化原本的"广告 banner 感" */
  &__hero-deco {
    position: absolute;
    color: rgba(255, 255, 255, 0.5);
    font-size: 48rpx;
    line-height: 1;
    pointer-events: none;
    &--1 { top: 20rpx;  left: 40rpx; }
    &--2 { top: 60rpx;  right: 50rpx; font-size: 32rpx; }
    &--3 { bottom: 20rpx; right: 30rpx; font-size: 40rpx; opacity: 0.6; }
  }
  &__date {
    font-size: $fs-xl;
    opacity: 0.9;
    letter-spacing: 2rpx;
  }
  &__tip {
    display: block;
    font-size: $fs-base;
    margin: $space-4 0 $space-5;
    opacity: 0.95;
    line-height: $lh-base;
  }
  &__cta {
    background: #fff;
    color: $color-primary;
    border-radius: $radius-pill;
    font-size: $fs-xl;
    font-weight: $fw-semi;
    padding: 22rpx 0;
    box-shadow: 0 4rpx 16rpx rgba(0, 0, 0, 0.08);
    &:active { opacity: 0.85; }
  }

  /* ========== 4 张功能卡片 ========== */
  &__cards {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: 20rpx;
    margin-top: $space-5;
  }
  &__card {
    background: $color-card;
    border-radius: $radius-lg;
    padding: 36rpx $space-4;
    text-align: center;
    box-shadow: $shadow-sm;
    transition: transform $transition-fast;
    /* 用顶部色条区分功能模块 */
    border-top: 4rpx solid $color-primary-light;
    &:active { transform: scale(0.98); }
    &--today  { border-top-color: $color-primary; }
    &--bazi   { border-top-color: $color-wealth; }
    &--match  { border-top-color: $color-love; }
    &--sign   { border-top-color: $color-health; }
    &--disabled { opacity: 0.5; }
  }
  &__card-emoji {
    display: block;
    font-size: 56rpx;
    line-height: 1;
    margin-bottom: $space-3;
  }
  &__card-title {
    font-size: $fs-lg;
    font-weight: $fw-semi;
    color: $color-text;
    display: block;
  }
  &__card-desc {
    font-size: $fs-sm;
    color: $color-text-sub;
    margin-top: $space-2;
    display: block;
  }

  /* ========== 签到行 ========== */
  &__sign {
    margin: $space-5 0 0;
    background: $color-card;
    border-radius: $radius-lg;
    padding: 28rpx;
    text-align: center;
    font-size: $fs-base;
    color: $color-text-body;
    box-shadow: $shadow-sm;
    display: flex;
    align-items: center;
    justify-content: center;
    gap: $space-2;
    &:active { opacity: 0.85; }
  }
  &__sign-icon { font-size: $fs-xl; }
  &__sign-text { color: $color-text-body; }

  /* ========== 免责声明 ========== */
  &__disclaimer {
    @include disclaimer;
  }
}
</style>
