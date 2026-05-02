<script setup lang="ts">
/**
 * 历史记录页：分页 + 删除单条 + 清空 + 信息流广告每 5 条插一个
 * 影响平台：H5 / 微信小程序
 */
import { onMounted, ref, computed } from 'vue'
import { getHistoryList, deleteHistory, clearHistory } from '@/api/fortune'
import type { HistoryItem } from '@/api/types'
import { track } from '@/utils/tracker'
import AdBanner from '@/components/AdBanner/index.vue'
import { ADS } from '@/config/ads'

const list = ref<HistoryItem[]>([])
const total = ref(0)
const page = ref(1)
const pageSize = 10
const loading = ref(false)
const finished = ref(false)

const showEmpty = computed(() => !loading.value && list.value.length === 0)

onMounted(() => {
  loadFirst()
})

async function loadFirst() {
  page.value = 1
  finished.value = false
  list.value = []
  await loadMore()
  track('history_view', { total: total.value })
  if (list.value.length === 0) track('history_empty_view')
}

async function loadMore() {
  if (loading.value || finished.value) return
  loading.value = true
  try {
    const data = await getHistoryList({ page: page.value, pageSize })
    list.value.push(...data.list)
    total.value = data.total
    if (list.value.length >= total.value || data.list.length < pageSize) {
      finished.value = true
    } else {
      page.value++
    }
  } catch (e: any) {
    uni.showToast({ title: e?.message || '加载失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function onItemClick(item: HistoryItem) {
  track('history_item_click', { recordId: item.recordId })
  // 历史记录点击后跳结果页（只读模式由 store 控制）
  uni.navigateTo({ url: '/pages/result/result' })
}

function onDelete(item: HistoryItem) {
  track('history_delete_click', { recordId: item.recordId })
  uni.showModal({
    title: '确认删除',
    content: '删除后不可恢复，确认吗？',
    success: async (r) => {
      if (!r.confirm) return
      try {
        await deleteHistory(item.recordId)
        list.value = list.value.filter((x) => x.recordId !== item.recordId)
        total.value = Math.max(0, total.value - 1)
        uni.showToast({ title: '已删除', icon: 'success' })
      } catch (e: any) {
        uni.showToast({ title: e?.message || '删除失败', icon: 'none' })
      }
    }
  })
}

function onClearAll() {
  if (list.value.length === 0) return
  track('history_clear_click', { total: total.value })
  uni.showModal({
    title: '确认清空',
    content: '所有历史记录将被清空，且不可恢复。',
    confirmColor: '#ff5050',
    success: async (r) => {
      if (!r.confirm) return
      try {
        await clearHistory()
        list.value = []
        total.value = 0
        finished.value = true
        uni.showToast({ title: '已清空', icon: 'success' })
      } catch (e: any) {
        uni.showToast({ title: e?.message || '清空失败', icon: 'none' })
      }
    }
  })
}

// 触底加载（页面级 onReachBottom 在 uni-app 中通过页面生命周期）
defineExpose({
  onReachBottom: loadMore
})

/**
 * 纯展示工具：level 文案 → 卡片样式 modifier
 * 仅供样式使用，不影响接口与逻辑
 */
function levelClass(level: string): 'top' | 'good' | 'plain' {
  if (level === '大吉' || level === '上上签') return 'top'
  if (level === '吉' || level === '上签' || level === '小吉') return 'good'
  return 'plain'
}
</script>

<template>
  <view class="history">
    <view class="history__header">
      <text class="history__title">历史记录（共 {{ total }} 条）</text>
      <text v-if="list.length > 0" class="history__clear" @click="onClearAll">清空</text>
    </view>

    <view v-if="showEmpty" class="history__empty">
      <text class="history__empty-emoji">🌙</text>
      <text class="history__empty-text">还没有记录哦，去测一次吧～</text>
      <button class="history__empty-btn" @click="uni.switchTab({ url: '/pages/index/index' })">去首页</button>
    </view>

    <view v-else class="history__list">
      <template v-for="(item, idx) in list" :key="item.recordId">
        <!-- UI 优化：根据 level 给左侧加色条（大吉粉 / 吉紫 / 平灰） -->
        <view
          class="history__item"
          :class="`history__item--${levelClass(item.level)}`"
          @click="onItemClick(item)"
        >
          <view class="history__item-left">
            <text class="history__item-date">{{ item.date }}</text>
            <text class="history__item-summary">{{ item.summary }}</text>
            <text class="history__item-meta">{{ item.zodiac }} · {{ item.level }}</text>
          </view>
          <view class="history__item-right">
            <text class="history__item-score">{{ item.score }}</text>
            <text class="history__item-del" @click.stop="onDelete(item)">删除</text>
          </view>
        </view>
        <!-- 每 5 条插一个信息流广告 -->
        <view v-if="(idx + 1) % 5 === 0" class="history__ad">
          <AdBanner :unit-id="ADS.HISTORY_NATIVE" />
        </view>
      </template>

      <view v-if="loading" class="history__loading">加载中…</view>
      <view v-else-if="finished && list.length > 0" class="history__finished">没有更多啦</view>
    </view>
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.history {
  padding: $space-5;

  /* ========== 头部 ========== */
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

  /* ========== 空状态 ========== */
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

  /* ========== 列表 ========== */
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

    /* level → 左色条 */
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
    /* 治愈风：长文本截断 */
    overflow: hidden;
    text-overflow: ellipsis;
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
  }
  &__item-meta {
    /* 改用 sub 而非 mute，提升可读性 */
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
  &__loading, &__finished {
    text-align: center;
    color: $color-text-sub;
    font-size: $fs-sm;
    padding: $space-4 0;
  }
}
</style>
