<script setup lang="ts">
/**
 * 星座匹配页：选 2 个星座 → 出匹配度 + 描述 + 建议
 * 影响平台：H5 / 微信小程序
 */
import { ref, computed } from 'vue'
import { matchZodiac } from '@/api/extra'
import { track } from '@/utils/tracker'
import AdBanner from '@/components/AdBanner/index.vue'
import { ADS } from '@/config/ads'
import type { MatchResp } from '@/api/types'

const ZODIACS = [
  '白羊座', '金牛座', '双子座', '巨蟹座',
  '狮子座', '处女座', '天秤座', '天蝎座',
  '射手座', '摩羯座', '水瓶座', '双鱼座'
]

const aIdx = ref(4)  // 默认狮子
const bIdx = ref(8)  // 默认射手
const result = ref<MatchResp | null>(null)
const loading = ref(false)

const aName = computed(() => ZODIACS[aIdx.value])
const bName = computed(() => ZODIACS[bIdx.value])

function onAChange(e: any) { aIdx.value = parseInt(e.detail.value, 10) }
function onBChange(e: any) { bIdx.value = parseInt(e.detail.value, 10) }

async function onMatch() {
  if (loading.value) return
  loading.value = true
  result.value = null
  track('match_submit', { a: aName.value, b: bName.value })
  try {
    result.value = await matchZodiac({ zodiacA: aName.value, zodiacB: bName.value })
  } catch (e: any) {
    uni.showToast({ title: e?.message || '匹配失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function levelColor(level: string): string {
  if (level === '绝配') return '#ff5da6'
  if (level === '合拍') return '#7c5cff'
  if (level === '中等') return '#f0ad4e'
  return '#888'
}
</script>

<template>
  <view class="match">
    <view class="match__form">
      <view class="match__col">
        <text class="match__col-label">我的星座</text>
        <picker mode="selector" :range="ZODIACS" :value="aIdx" @change="onAChange">
          <view class="match__col-pick">{{ aName }}</view>
        </picker>
      </view>
      <!-- UI 优化：渐变线条 + 中心爱心，替代单字符突兀的 ❤ -->
      <view class="match__connector">
        <view class="match__connector-line"></view>
        <view class="match__connector-icon">💞</view>
        <view class="match__connector-line"></view>
      </view>
      <view class="match__col">
        <text class="match__col-label">TA 的星座</text>
        <picker mode="selector" :range="ZODIACS" :value="bIdx" @change="onBChange">
          <view class="match__col-pick">{{ bName }}</view>
        </picker>
      </view>
    </view>

    <button class="match__btn" :disabled="loading" @click="onMatch">
      {{ loading ? '匹配中…' : '看看匹配度' }}
    </button>

    <view v-if="result" class="match__result">
      <view class="match__score-box" :style="{ background: levelColor(result.level) }">
        <text class="match__score">{{ result.score }}</text>
        <text class="match__level">{{ result.level }}</text>
      </view>

      <view class="match__card">
        <text class="match__card-title">整体描述</text>
        <text class="match__card-text">{{ result.summary }}</text>
      </view>
      <view class="match__card">
        <text class="match__card-title">爱情建议</text>
        <text class="match__card-text">{{ result.loveText }}</text>
      </view>
      <view class="match__card">
        <text class="match__card-title">相处提示</text>
        <text class="match__card-text">{{ result.tipText }}</text>
      </view>

      <AdBanner :unit-id="ADS.HOME_BANNER" />
      <text class="match__disclaimer">{{ result.tips }}</text>
    </view>
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.match {
  padding: $space-5;

  /* ========== 选星座表单 ========== */
  &__form {
    @include card-base;
    padding: $space-5;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }
  &__col {
    flex: 1;
    text-align: center;
  }
  &__col-label {
    font-size: $fs-sm;
    color: $color-text-sub;
    display: block;
  }
  &__col-pick {
    margin-top: $space-3;
    font-size: $fs-xl;
    font-weight: $fw-semi;
    color: $color-primary;
    padding: $space-3 0;
    background: $color-primary-light;
    border-radius: $radius-md;
  }

  /* 渐变连接器：粉 → 紫 渐变线 + 中心 emoji */
  &__connector {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    width: 80rpx;
    margin: 32rpx 8rpx 0;
  }
  &__connector-line {
    width: 4rpx;
    height: 24rpx;
    background: linear-gradient(180deg, $color-love 0%, $color-primary 100%);
    border-radius: $radius-pill;
  }
  &__connector-icon {
    font-size: 40rpx;
    margin: 4rpx 0;
    line-height: 1;
  }

  /* 主按钮 */
  &__btn {
    @include btn-primary;
    margin-top: $space-5;
  }

  /* ========== 结果区 ========== */
  &__result {
    margin-top: $space-5;
    display: flex;
    flex-direction: column;
    gap: 20rpx;
  }
  &__score-box {
    border-radius: $radius-xl;
    padding: 48rpx 0;
    text-align: center;
    color: #fff;
    box-shadow: $shadow-md;
    /* 内部叠加柔光，增加质感 */
    background-image: linear-gradient(180deg, rgba(255, 255, 255, 0.15) 0%, rgba(255, 255, 255, 0) 60%);
  }
  &__score {
    font-size: $fs-display;
    font-weight: $fw-bold;
    display: block;
    line-height: 1;
  }
  &__level {
    font-size: $fs-xl;
    opacity: 0.95;
    margin-top: $space-3;
    display: block;
    letter-spacing: 4rpx;
  }

  &__card {
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
    line-height: 1.7;
  }

  &__disclaimer {
    @include disclaimer;
    margin-top: $space-3;
  }
}
</style>
