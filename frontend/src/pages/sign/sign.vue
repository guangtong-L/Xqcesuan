<script setup lang="ts">
/**
 * 今日幸运签：抽签动画 + 签号 + 4 句签诗 + 解读 + 宜忌
 * 影响平台：H5 / 微信小程序
 * 同一人当日抽到的签是固定的；跨日刷新
 */
import { onMounted, ref } from 'vue'
import { drawSign } from '@/api/extra'
import { track } from '@/utils/tracker'
import AdBanner from '@/components/AdBanner/index.vue'
import { ADS } from '@/config/ads'
import type { SignResp } from '@/api/types'

const data = ref<SignResp | null>(null)
const loading = ref(false)
const drawn = ref(false)
const errorMsg = ref('')

onMounted(() => {
  track('sign_view')
})

async function onDraw() {
  if (loading.value || drawn.value) return
  loading.value = true
  errorMsg.value = ''
  try {
    // 加点抽签仪式感
    await new Promise((r) => setTimeout(r, 800))
    data.value = await drawSign()
    drawn.value = true
    track('sign_drawn', { signNo: data.value.signNo, signName: data.value.signName })
  } catch (e: any) {
    errorMsg.value = e?.message || '抽签失败，请重试'
  } finally {
    loading.value = false
  }
}

function nameColor(name: string): string {
  if (name === '上上签') return 'var(--color-love)'
  if (name === '上签')   return 'var(--color-primary)'
  if (name === '中签')   return 'var(--color-health)'
  return 'var(--color-text-sub)'
}
</script>

<template>
  <view class="sign">
    <view v-if="!drawn" class="sign__intro">
      <view class="sign__bucket" :class="{ shake: loading }">
        <text class="sign__bucket-icon">🎋</text>
      </view>
      <text class="sign__hint">{{ loading ? '正在抽取你的今日签…' : '静下心，轻摇签筒，抽取你今日的幸运签' }}</text>
      <button class="sign__btn" :disabled="loading" @click="onDraw">{{ loading ? '抽取中…' : '抽签' }}</button>
      <text v-if="errorMsg" class="sign__error">{{ errorMsg }}</text>
    </view>

    <template v-else-if="data">
      <view class="sign__head">
        <text class="sign__no">第 {{ data.signNo }} 签</text>
        <text class="sign__name" :style="{ color: nameColor(data.signName) }">{{ data.signName }}</text>
        <text class="sign__date">{{ data.date }}</text>
      </view>

      <view class="sign__poem">
        <text v-for="(line, i) in data.poem" :key="i" class="sign__poem-line">{{ line }}</text>
      </view>

      <view class="sign__card">
        <text class="sign__card-title">签文释义</text>
        <text class="sign__card-text">{{ data.explain }}</text>
      </view>

      <view class="sign__yiji">
        <view class="sign__col sign__col--yi">
          <text class="sign__col-title">宜</text>
          <text v-for="(t, i) in data.yi" :key="'y'+i" class="sign__col-item">{{ t }}</text>
        </view>
        <view class="sign__col sign__col--ji">
          <text class="sign__col-title">忌</text>
          <text v-for="(t, i) in data.ji" :key="'j'+i" class="sign__col-item">{{ t }}</text>
        </view>
      </view>

      <AdBanner :unit-id="ADS.HOME_BANNER" />
      <text class="sign__disclaimer">{{ data.tips }}</text>
    </template>
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

/* 仿古签页配色：米黄 → 浅暖 → 默认底，强化"庙宇签筒"氛围 */
.sign {
  padding: $space-5;
  min-height: 100vh;
  background: linear-gradient(180deg, #fdf3e7 0%, #fbeed8 30%, $color-bg 70%);

  /* ========== 抽签前：签筒区 ========== */
  &__intro {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: 120rpx;
    gap: $space-5;
  }
  &__bucket {
    width: 200rpx;
    height: 200rpx;
    background: linear-gradient(135deg, #fff5e6 0%, #fce5c4 100%);
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8rpx 24rpx rgba(201, 123, 63, 0.18);
    border: 4rpx solid #f5d9a8;
    transition: transform $transition-fast;
    &.shake {
      animation: shake 0.4s infinite;
    }
    &:active {
      transform: scale(0.95);
    }
  }
  @keyframes shake {
    0%   { transform: rotate(-8deg); }
    50%  { transform: rotate(8deg); }
    100% { transform: rotate(-8deg); }
  }
  /* UI 优化：签筒 emoji 从 120rpx 调小到 88rpx，整体更精致 */
  &__bucket-icon {
    font-size: 88rpx;
    line-height: 1;
  }
  &__hint {
    font-size: $fs-md;
    color: #8a6a3e;
    text-align: center;
    letter-spacing: 1rpx;
  }
  /* 签页主按钮保持暖棕色，与古风一致；不走通用 mixin */
  &__btn {
    background: linear-gradient(135deg, #c97b3f 0%, #a35e2a 100%);
    color: #fff;
    border-radius: $radius-pill;
    font-size: $fs-xl;
    font-weight: $fw-semi;
    padding: 22rpx 80rpx;
    margin-top: $space-3;
    box-shadow: 0 6rpx 18rpx rgba(201, 123, 63, 0.3);
    &[disabled] {
      background: #ccc;
      color: #fff;
      box-shadow: none;
    }
    &:active { opacity: 0.85; }
  }
  &__error {
    color: $color-danger;
    font-size: $fs-sm;
    margin-top: $space-3;
  }

  /* ========== 抽签后：签头 ========== */
  &__head {
    background: linear-gradient(180deg, #fffbf3 0%, #fff 100%);
    border-radius: $radius-xl;
    padding: 40rpx 0;
    text-align: center;
    display: flex;
    flex-direction: column;
    gap: $space-2;
    box-shadow: $shadow-sm;
    border: 2rpx solid #f5e6c8;
  }
  &__no {
    font-size: $fs-base;
    color: #8a6a3e;
    letter-spacing: 2rpx;
  }
  &__name {
    font-size: 56rpx;
    font-weight: $fw-bold;
    letter-spacing: 8rpx;
  }
  &__date {
    font-size: $fs-xs;
    color: $color-text-mute;
  }

  /* ========== 仿古签纸 ========== */
  &__poem {
    margin-top: $space-4;
    background: #fffaf2;
    /* 烫金双层边框，强化仪式感 */
    border: 2rpx solid #d4a017;
    box-shadow: inset 0 0 0 6rpx #fffaf2, inset 0 0 0 8rpx rgba(212, 160, 23, 0.3);
    border-radius: $radius-lg;
    padding: 48rpx 40rpx;
    display: flex;
    flex-direction: column;
    gap: $space-3;
    text-align: center;
  }
  &__poem-line {
    font-size: $fs-xl;
    color: #5a3e1a;
    font-family: 'KaiTi', 'STKaiti', 'STSong', serif;
    letter-spacing: 6rpx;
    line-height: 1.7;
  }

  /* ========== 释义卡 ========== */
  &__card {
    margin-top: $space-4;
    @include card-base;
    display: flex;
    flex-direction: column;
    gap: $space-2;
  }
  &__card-title {
    font-size: $fs-base;
    font-weight: $fw-semi;
    color: $color-text;
  }
  &__card-text {
    font-size: $fs-md;
    color: $color-text-body;
    line-height: $lh-loose;
  }

  /* ========== 宜忌（与 result 页保持一致样式） ========== */
  &__yiji {
    margin-top: $space-4;
    display: flex;
    gap: $space-3;
  }
  &__col {
    flex: 1;
    @include card-base;
    display: flex;
    flex-direction: column;
    gap: $space-2;
    &--yi { border-top: 6rpx solid $color-health; }
    &--ji { border-top: 6rpx solid $color-danger; }
  }
  &__col-title {
    font-size: $fs-lg;
    font-weight: $fw-semi;
    color: $color-text;
  }
  &__col-item {
    font-size: $fs-md;
    color: $color-text-strong;
    line-height: $lh-loose;
  }

  &__disclaimer {
    @include disclaimer;
  }
}
</style>
