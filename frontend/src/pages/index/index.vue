<script setup lang="ts">
/**
 * 首页：每日一签活动入口 + 今日日期 + 签到 + Banner
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
const dailyTip = ref('静下心抽一签，把今天过得轻一点、稳一点。')

onMounted(() => {
  track('home_view', { activity: 'daily_sign' })
})

function onDrawSign() {
  track('home_draw_click')
  uni.navigateTo({ url: '/pages/sign/sign' })
}

function onSignIn() {
  const days = userStore.signIn()
  uni.showToast({ title: `已连续签到 ${days} 天`, icon: 'success' })
}
</script>

<template>
  <view class="home">
    <view class="home__hero">
      <view class="home__hero-deco home__hero-deco--1">·</view>
      <view class="home__hero-deco home__hero-deco--2">·</view>
      <view class="home__hero-deco home__hero-deco--3">·</view>
      <text class="home__date">{{ today }}</text>
      <text class="home__title">每日一签</text>
      <text class="home__tip">{{ dailyTip }}</text>
      <button class="home__cta" @click="onDrawSign">抽取今日签</button>
    </view>

    <view class="home__activity">
      <text class="home__activity-icon">🎋</text>
      <view class="home__activity-content">
        <text class="home__activity-title">今日只做一件事</text>
        <text class="home__activity-desc">抽一支签，看一段签文，带走 3 条宜做与 2 条少做。</text>
      </view>
    </view>

    <view class="home__sign" @click="onSignIn">
      <text class="home__sign-icon">🌟</text>
      <text class="home__sign-text">已连续签到 {{ userStore.signInDays }} 天，点我签到</text>
    </view>

    <AdBanner :unit-id="ADS.HOME_BANNER" />

    <text class="home__disclaimer">本签内容仅供娱乐参考，请理性看待。</text>
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.home {
  padding: $space-5;

  &__hero {
    @include hero-gradient-purple;
    @include card-hero;
    position: relative;
    text-align: center;
    overflow: hidden;
  }
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
  &__title {
    display: block;
    font-size: 64rpx;
    font-weight: $fw-bold;
    margin-top: $space-3;
    letter-spacing: 6rpx;
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

  &__activity {
    margin-top: $space-5;
    @include card-base;
    display: flex;
    gap: $space-4;
    align-items: center;
  }
  &__activity-icon {
    font-size: 64rpx;
    line-height: 1;
  }
  &__activity-content {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: $space-2;
  }
  &__activity-title {
    font-size: $fs-lg;
    font-weight: $fw-semi;
    color: $color-text;
  }
  &__activity-desc {
    font-size: $fs-md;
    color: $color-text-sub;
    line-height: $lh-base;
  }

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

  &__disclaimer {
    @include disclaimer;
  }
}
</style>
