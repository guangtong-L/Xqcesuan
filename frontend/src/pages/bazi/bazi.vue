<script setup lang="ts">
/**
 * 八字流年页：年度综评 + 4 季度走势 + 关键词 + 建议
 * 影响平台：H5 / 微信小程序
 * 数据：依赖用户已录入生日；未录入跳 calc 页
 */
import { onMounted, ref } from 'vue'
import { useUserStore } from '@/store/user'
import { calcBazi } from '@/api/extra'
import { track } from '@/utils/tracker'
import AdBanner from '@/components/AdBanner/index.vue'
import { ADS } from '@/config/ads'
import type { BaziResp } from '@/api/types'

const userStore = useUserStore()
const data = ref<BaziResp | null>(null)
const loading = ref(false)
const errorMsg = ref('')

onMounted(async () => {
  track('bazi_view')
  if (!userStore.profile?.birthday) {
    uni.showToast({ title: '请先填写生日', icon: 'none' })
    setTimeout(() => uni.redirectTo({ url: '/pages/calc/calc' }), 800)
    return
  }
  await load()
})

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    data.value = await calcBazi({
      birthday: userStore.profile!.birthday,
      gender: userStore.profile!.gender
    })
  } catch (e: any) {
    errorMsg.value = e?.message || '加载失败，请重试'
  } finally {
    loading.value = false
  }
}

/**
 * 分数 → 颜色（保留：仅作分数文字用色）
 */
function quarterColor(score: number): string {
  if (score >= 90) return 'var(--color-primary)'
  if (score >= 75) return 'var(--color-health)'
  if (score >= 60) return 'var(--color-wealth)'
  return 'var(--color-text-mute)'
}

/**
 * 季度 key/name → 季节 modifier（用于卡片色条 + 浅底色）
 * 仅用于样式，不影响接口字段
 */
function seasonOf(q: { key?: string; name?: string }): 'spring' | 'summer' | 'autumn' | 'winter' {
  const s = `${q.key || ''}${q.name || ''}`
  if (s.includes('春') || s.toLowerCase().includes('spring') || s.includes('q1')) return 'spring'
  if (s.includes('夏') || s.toLowerCase().includes('summer') || s.includes('q2')) return 'summer'
  if (s.includes('秋') || s.toLowerCase().includes('autumn') || s.toLowerCase().includes('fall') || s.includes('q3')) return 'autumn'
  return 'winter'
}
</script>

<template>
  <view class="bazi">
    <view v-if="loading" class="bazi__loading">推算中…</view>
    <view v-else-if="errorMsg" class="bazi__error">
      <text>{{ errorMsg }}</text>
      <button class="bazi__retry" @click="load">重试</button>
    </view>
    <template v-else-if="data">
      <view class="bazi__hero">
        <text class="bazi__year">{{ data.year }} 年度运势</text>
        <text class="bazi__overview">{{ data.yearOverview }}</text>
      </view>

      <text class="bazi__title">四季走势</text>
      <view class="bazi__quarters">
        <view
          v-for="q in data.quarters"
          :key="q.key"
          class="bazi__quarter"
          :class="`bazi__quarter--${seasonOf(q)}`"
        >
          <view class="bazi__quarter-head">
            <text class="bazi__quarter-name">{{ q.name }}</text>
            <text class="bazi__quarter-score" :style="{ color: quarterColor(q.score) }">{{ q.score }}</text>
          </view>
          <text class="bazi__quarter-text">{{ q.text }}</text>
        </view>
      </view>

      <text class="bazi__title">年度关键词</text>
      <view class="bazi__keywords">
        <text v-for="(k, i) in data.keywords" :key="i" class="bazi__keyword">{{ k }}</text>
      </view>

      <text class="bazi__title">给你的建议</text>
      <view class="bazi__card">
        <text class="bazi__suggestion">{{ data.suggestion }}</text>
      </view>

      <AdBanner :unit-id="ADS.HOME_BANNER" />
      <text class="bazi__disclaimer">{{ data.tips }}</text>
    </template>
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.bazi {
  padding: $space-5;

  &__loading, &__error {
    text-align: center;
    padding: 120rpx 0;
    color: $color-text-body;
    font-size: $fs-lg;
  }
  &__retry {
    @include btn-primary;
    margin-top: $space-5;
    width: 60%;
    font-size: $fs-base;
  }

  /* ========== Hero ========== */
  &__hero {
    @include hero-gradient-purple;
    @include card-hero;
    text-align: center;
    position: relative;
    overflow: hidden;
  }
  &__year {
    font-size: $fs-2xl;
    font-weight: $fw-bold;
    display: block;
    letter-spacing: 2rpx;
  }
  &__overview {
    display: block;
    margin-top: $space-4;
    font-size: $fs-base;
    line-height: 1.7;
    opacity: 0.95;
  }

  /* ========== 小节标题 ========== */
  &__title {
    @include title-section;
  }

  /* ========== 4 季度卡片 ========== */
  &__quarters {
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: $space-3;
  }
  &__quarter {
    background: $color-card;
    border-radius: $radius-lg;
    padding: $space-4;
    display: flex;
    flex-direction: column;
    gap: $space-2;
    box-shadow: $shadow-sm;
    border-top: 6rpx solid $color-primary-light;
    transition: transform $transition-fast;
    &:active { transform: scale(0.99); }

    /* 四季配色：春绿 / 夏橙 / 秋金 / 冬蓝 */
    &--spring {
      border-top-color: $color-spring;
      background: linear-gradient(180deg, rgba(81, 200, 120, 0.06) 0%, #fff 60%);
    }
    &--summer {
      border-top-color: $color-summer;
      background: linear-gradient(180deg, rgba(255, 142, 60, 0.06) 0%, #fff 60%);
    }
    &--autumn {
      border-top-color: $color-autumn;
      background: linear-gradient(180deg, rgba(212, 160, 23, 0.06) 0%, #fff 60%);
    }
    &--winter {
      border-top-color: $color-winter;
      background: linear-gradient(180deg, rgba(74, 144, 226, 0.06) 0%, #fff 60%);
    }
  }
  &__quarter-head {
    display: flex;
    justify-content: space-between;
    align-items: baseline;
  }
  &__quarter-name {
    font-size: $fs-lg;
    font-weight: $fw-semi;
    color: $color-text;
  }
  &__quarter-score {
    font-size: $fs-2xl;
    font-weight: $fw-bold;
  }
  &__quarter-text {
    font-size: $fs-sm;
    color: $color-text-body;
    line-height: $lh-base;
  }

  /* ========== 关键词 chips ========== */
  &__keywords {
    display: flex;
    flex-wrap: wrap;
    gap: $space-2;
  }
  &__keyword {
    background: $color-primary-light;
    color: $color-primary;
    border-radius: $radius-pill;
    padding: 12rpx 28rpx;
    font-size: $fs-md;
    font-weight: $fw-medium;
  }

  /* ========== 建议卡片 ========== */
  &__card {
    @include card-base;
  }
  &__suggestion {
    font-size: $fs-base;
    color: $color-text;
    line-height: $lh-loose;
  }

  &__disclaimer {
    @include disclaimer;
  }
}
</style>
