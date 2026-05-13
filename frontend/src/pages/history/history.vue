<script setup lang="ts">
/**
 * 抽签记录页：本地记录 + 删除单条 + 清空
 * 影响平台：H5 / 微信小程序
 */
import { computed, onMounted, ref } from 'vue'
import {
  clearSignHistory,
  deleteSignHistory,
  listSignHistory,
  type SignHistoryItem
} from '@/utils/signHistory'
import { track } from '@/utils/tracker'
import AdBanner from '@/components/AdBanner/index.vue'
import { ADS } from '@/config/ads'

const list = ref<SignHistoryItem[]>([])

const total = computed(() => list.value.length)
const showEmpty = computed(() => list.value.length === 0)

onMounted(() => {
  refresh()
  track('history_view', { total: total.value, activity: 'daily_sign' })
  if (list.value.length === 0) track('history_empty_view')
})

function refresh() {
  list.value = listSignHistory()
}

function onItemClick(item: SignHistoryItem) {
  track('history_item_click', { recordId: item.recordId, signName: item.signName })
  uni.showModal({
    title: `第 ${item.signNo} 签 · ${item.signName}`,
    content: item.summary,
    showCancel: false
  })
}

function onDelete(item: SignHistoryItem) {
  track('history_delete_click', { recordId: item.recordId })
  uni.showModal({
    title: '确认删除',
    content: '删除后不可恢复，确认吗？',
    success: (r) => {
      if (!r.confirm) return
      deleteSignHistory(item.recordId)
      refresh()
      uni.showToast({ title: '已删除', icon: 'success' })
    }
  })
}

function onClearAll() {
  if (list.value.length === 0) return
  track('history_clear_click', { total: total.value })
  uni.showModal({
    title: '确认清空',
    content: '所有抽签记录将被清空，且不可恢复。',
    confirmColor: '#ff5050',
    success: (r) => {
      if (!r.confirm) return
      clearSignHistory()
      refresh()
      uni.showToast({ title: '已清空', icon: 'success' })
    }
  })
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function levelClass(level: string): 'top' | 'good' | 'plain' {
  if (level === '上上签') return 'top'
  if (level === '上签') return 'good'
  return 'plain'
}
</script>

<template>
  <view class="history">
    <view class="history__header">
      <text class="history__title">抽签记录（共 {{ total }} 条）</text>
      <text v-if="list.length > 0" class="history__clear" @click="onClearAll">清空</text>
    </view>

    <view v-if="showEmpty" class="history__empty">
      <text class="history__empty-emoji">🎋</text>
      <text class="history__empty-text">还没有抽签记录，去抽今日签吧～</text>
      <button class="history__empty-btn" @click="goHome">去抽签</button>
    </view>

    <view v-else class="history__list">
      <template v-for="(item, idx) in list" :key="item.recordId">
        <view
          class="history__item"
          :class="`history__item--${levelClass(item.signName)}`"
          @click="onItemClick(item)"
        >
          <view class="history__item-left">
            <text class="history__item-date">{{ item.date }}</text>
            <text class="history__item-summary">{{ item.summary }}</text>
            <text class="history__item-meta">第 {{ item.signNo }} 签 · {{ item.signName }}</text>
          </view>
          <view class="history__item-right">
            <text class="history__item-score">{{ item.signName.slice(0, 1) }}</text>
            <text class="history__item-del" @click.stop="onDelete(item)">删除</text>
          </view>
        </view>
        <view v-if="(idx + 1) % 5 === 0" class="history__ad">
          <AdBanner :unit-id="ADS.HISTORY_NATIVE" />
        </view>
      </template>
    </view>
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.history {
  padding: $space-5;

  &__header {
    @include row-flex;
    margin-bottom: $space-4;
    padding: 0 $space-2;
  }
  &__title {
    font-size: $fs-base;
    color: $color-text-strong;
    font-weight: $fw-medium;
  }
  &__clear {
    font-size: $fs-md;
    color: $color-danger;
    padding: $space-2 $space-3;
  }

  &__empty {
    text-align: center;
    padding: 160rpx 0;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: $space-4;
    color: $color-text-sub;
  }
  &__empty-emoji {
    font-size: 96rpx;
    opacity: 0.7;
  }
  &__empty-text {
    font-size: $fs-base;
    color: $color-text-sub;
  }
  &__empty-btn {
    @include btn-primary;
    padding: 0 64rpx;
    height: 80rpx;
    line-height: 80rpx;
  }

  &__list {
    display: flex;
    flex-direction: column;
    gap: 20rpx;
  }
  &__item {
    background: $color-card;
    border-radius: $radius-lg;
    padding: 28rpx;
    @include row-flex;
    box-shadow: $shadow-sm;
    border-left: 6rpx solid $color-divider;
    transition: transform $transition-fast;
    &:active { transform: scale(0.99); }
    &--top   { border-left-color: $color-love; }
    &--good  { border-left-color: $color-primary; }
    &--plain { border-left-color: $color-text-mute; }
  }
  &__item-left {
    flex: 1;
    display: flex;
    flex-direction: column;
    gap: $space-2;
    min-width: 0;
  }
  &__item-date {
    font-size: $fs-base;
    font-weight: $fw-semi;
    color: $color-text;
  }
  &__item-summary {
    font-size: $fs-md;
    color: $color-text-body;
    line-height: $lh-base;
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
  &__item-meta {
    font-size: $fs-xs;
    color: $color-text-sub;
  }
  &__item-right {
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: $space-2;
    margin-left: $space-3;
  }
  &__item-score {
    font-size: 40rpx;
    font-weight: $fw-bold;
    color: $color-primary;
    line-height: 1;
  }
  &__item-del {
    font-size: $fs-xs;
    color: $color-danger;
    padding: 6rpx $space-3;
    border-radius: $radius-sm;
    background: rgba(255, 107, 107, 0.08);
  }
  &__ad {
    margin: $space-2 0;
  }
}
</style>
