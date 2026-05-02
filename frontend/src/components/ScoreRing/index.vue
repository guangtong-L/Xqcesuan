<script setup lang="ts">
/**
 * 环形评分（CSS conic-gradient 实现）
 * 微信小程序、H5、App 三端均支持 conic-gradient（基础库 2.10+）
 *
 * UI 优化（2026-04）：
 * - 渐变色：从浅紫到主紫，更有层次
 * - 入场动画：分数从 0 缓动到目标值（0.6s）
 * - 内圈底色微调，增加圆润感
 */
import { computed, onMounted, ref, watch } from 'vue'

interface Props {
  score: number          // 0-100
  level?: string         // "大吉" / "吉" / "平" 等，可选展示
  size?: number          // rpx
  stroke?: number        // rpx 圆环厚度
  color?: string
  bgColor?: string
}

const props = withDefaults(defineProps<Props>(), {
  size: 240,
  stroke: 18,
  color: '#7c5cff',
  bgColor: '#ede8ff',
  level: ''
})

const safeScore = computed(() => Math.max(0, Math.min(100, props.score || 0)))

// 入场动画：从 0 缓动到目标分数
const animScore = ref(0)
let raf: number | null = null

function animate(target: number) {
  const start = animScore.value
  const startAt = Date.now()
  const dur = 600  // 0.6s 缓动
  const tick = () => {
    const t = Math.min(1, (Date.now() - startAt) / dur)
    // easeOutCubic
    const eased = 1 - Math.pow(1 - t, 3)
    animScore.value = Math.round(start + (target - start) * eased)
    if (t < 1) {
      raf = requestAnimationFrame(tick)
    }
  }
  raf = requestAnimationFrame(tick)
}

onMounted(() => animate(safeScore.value))
watch(safeScore, (v) => {
  if (raf) cancelAnimationFrame(raf)
  animate(v)
})

const angle = computed(() => (animScore.value / 100) * 360)

// 渐变色：浅紫 → 主紫，更有动态感
const ringStyle = computed(() => ({
  width: `${props.size}rpx`,
  height: `${props.size}rpx`,
  background: `conic-gradient(from 0deg, ${props.color} 0deg, #b39dff ${angle.value * 0.6}deg, ${props.color} ${angle.value}deg, ${props.bgColor} ${angle.value}deg 360deg)`
}))

const innerStyle = computed(() => ({
  width: `${props.size - props.stroke * 2}rpx`,
  height: `${props.size - props.stroke * 2}rpx`
}))
</script>

<template>
  <view class="score-ring" :style="ringStyle">
    <view class="score-ring__inner" :style="innerStyle">
      <text class="score-ring__num">{{ animScore }}</text>
      <text class="score-ring__unit">分</text>
      <text v-if="level" class="score-ring__level">{{ level }}</text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.score-ring {
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background $transition-slow;
  box-shadow: $shadow-md;

  &__inner {
    background: $color-card;
    border-radius: 50%;
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
  }

  &__num {
    font-size: 64rpx;
    font-weight: $fw-bold;
    color: $color-text;
    line-height: 1;
  }

  &__unit {
    font-size: $fs-sm;
    color: $color-text-sub;
    margin-top: $space-2;
  }

  &__level {
    font-size: $fs-sm;
    color: $color-primary;
    margin-top: $space-2;
    padding: 4rpx 16rpx;
    background: $color-primary-light;
    border-radius: $radius-pill;
  }
}
</style>
