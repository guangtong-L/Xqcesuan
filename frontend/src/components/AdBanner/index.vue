<script setup lang="ts">
/**
 * Banner 广告封装
 * 仅微信小程序生效；H5/App 自动隐藏
 * 开发者工具不会真的拉广告，加载失败时折叠
 */
import { ref } from 'vue'

interface Props {
  unitId: string
  adIntervals?: number   // 30 ~ 120s
}

withDefaults(defineProps<Props>(), {
  adIntervals: 30
})

const errored = ref(false)

function onError(e: { detail: { errCode: number; errMsg?: string } }) {
  console.warn('[AdBanner] 加载失败', e?.detail)
  errored.value = true
}
function onLoad() {
  errored.value = false
}
function onClose() {
  errored.value = true
}
</script>

<template>
  <!-- #ifdef MP-WEIXIN -->
  <view v-if="!errored" class="ad-banner">
    <ad
      :unit-id="unitId"
      :ad-intervals="adIntervals"
      @load="onLoad"
      @error="onError"
      @close="onClose"
    />
  </view>
  <!-- #endif -->
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

/* 广告容器：与卡片间距保持一致，避免视觉割裂 */
.ad-banner {
  width: 100%;
  margin: $space-3 0;
  border-radius: $radius-md;
  overflow: hidden;
}
</style>
