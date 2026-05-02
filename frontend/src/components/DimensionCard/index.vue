<script setup lang="ts">
/**
 * 维度卡：爱情/事业/财运/健康
 * 未解锁时显示蒙层 + "看广告解锁"
 *
 * UI 优化（2026-04）：
 * - 4 个维度根据 key 自动着不同辅助色（左侧色条 + 分数色）
 * - 蒙层文案胶囊样式更圆润，强化"可点击"暗示
 */
import { computed } from 'vue'
import type { Dimension } from '@/api/types'

interface Props {
  dimension: Dimension
  unlocked: boolean
}

const props = defineProps<Props>()

const emit = defineEmits<{
  (e: 'unlock', key: Dimension['key']): void
}>()

// 维度 key → 颜色 token（CSS 变量名）
const COLOR_MAP: Record<Dimension['key'], string> = {
  love:   'var(--color-love)',
  career: 'var(--color-career)',
  wealth: 'var(--color-wealth)',
  health: 'var(--color-health)'
}

// 维度 key → emoji（视觉辨识 + 萌系治愈）
const EMOJI_MAP: Record<Dimension['key'], string> = {
  love:   '💗',
  career: '💼',
  wealth: '💰',
  health: '🌿'
}

const accent = computed(() => COLOR_MAP[props.dimension.key] || 'var(--color-primary)')
const emoji = computed(() => EMOJI_MAP[props.dimension.key] || '✨')

function onUnlockClick() {
  emit('unlock', props.dimension.key)
}
</script>

<template>
  <view class="dim-card" :style="{ borderLeftColor: accent }">
    <view class="dim-card__head">
      <view class="dim-card__name-wrap">
        <text class="dim-card__emoji">{{ emoji }}</text>
        <text class="dim-card__name">{{ dimension.name }}</text>
      </view>
      <text class="dim-card__score" :style="{ color: accent }">{{ dimension.score }} 分</text>
    </view>

    <view class="dim-card__body">
      <text class="dim-card__text">{{ dimension.text }}</text>
      <!-- 未解锁蒙层 -->
      <view v-if="!unlocked" class="dim-card__mask" @click="onUnlockClick">
        <text class="dim-card__mask-text">看广告解锁详情</text>
      </view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.dim-card {
  background: $color-card;
  border-radius: $radius-lg;
  border-left: 6rpx solid $color-primary;
  padding: $space-4 $space-4 + 4rpx $space-4;
  box-shadow: $shadow-sm;
  transition: transform $transition-fast;

  &:active {
    transform: scale(0.99);
  }

  &__head {
    @include row-flex;
    margin-bottom: $space-3;
  }

  &__name-wrap {
    display: flex;
    align-items: center;
    gap: $space-2;
  }

  &__emoji {
    font-size: $fs-xl;
    line-height: 1;
  }

  &__name {
    font-size: $fs-xl;
    font-weight: $fw-semi;
    color: $color-text;
  }

  &__score {
    font-size: $fs-base;
    font-weight: $fw-semi;
  }

  &__body {
    position: relative;
    min-height: 80rpx;
  }

  &__text {
    font-size: $fs-base;
    color: $color-text-body;
    line-height: $lh-base;
  }

  &__mask {
    position: absolute;
    inset: -8rpx;
    background: rgba(255, 255, 255, 0.88);
    backdrop-filter: blur(4rpx);
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: $radius-md;
  }

  &__mask-text {
    font-size: $fs-md;
    color: $color-primary;
    background: $color-primary-light;
    padding: 14rpx 36rpx;
    border-radius: $radius-pill;
    font-weight: $fw-medium;
    box-shadow: $shadow-sm;
  }
}
</style>
