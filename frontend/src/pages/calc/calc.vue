<script setup lang="ts">
/**
 * 信息录入页：昵称/性别/生日/时辰/标签/协议
 * 关键交互：必填校验、协议未勾选按钮置灰、未来日期禁选、提交防抖
 */
import { ref, computed } from 'vue'
import { useUserStore } from '@/store/user'
import { useFortuneStore } from '@/store/fortune'
import { formatDate, isMinor } from '@/utils/date'
import { track } from '@/utils/tracker'
import type { Gender, DimensionKey } from '@/api/types'

const userStore = useUserStore()
const fortuneStore = useFortuneStore()

// 默认值（如已有 profile 则回填）
const nickname = ref(userStore.profile?.nickname || '小星友')
const gender = ref<Gender>(userStore.profile?.gender || 'U')
const birthday = ref(userStore.profile?.birthday || '')
const birthHour = ref<number>(userStore.profile?.birthHour ?? -1)
const tags = ref<DimensionKey[]>(userStore.profile?.tags || [])
const agreed = ref(userStore.profile?.agreed ?? false)

const today = formatDate()
const submitting = ref(false)

const TAG_OPTIONS: Array<{ key: DimensionKey; name: string }> = [
  { key: 'love', name: '爱情' },
  { key: 'career', name: '事业' },
  { key: 'wealth', name: '财运' },
  { key: 'health', name: '健康' }
]

const HOUR_OPTIONS = ['不确定', ...Array.from({ length: 24 }, (_, i) => `${i}:00`)]
const hourIndex = ref(birthHour.value === -1 ? 0 : birthHour.value + 1)

const canSubmit = computed(() => {
  return !!birthday.value && agreed.value && !submitting.value
})

function onChangeBirthday(e: { detail: { value: string } }) {
  birthday.value = e.detail.value
}
function onChangeHour(e: { detail: { value: string } }) {
  hourIndex.value = parseInt(e.detail.value, 10)
  birthHour.value = hourIndex.value === 0 ? -1 : hourIndex.value - 1
}
function toggleTag(k: DimensionKey) {
  const i = tags.value.indexOf(k)
  if (i >= 0) tags.value.splice(i, 1)
  else if (tags.value.length < 3) tags.value.push(k)
  else uni.showToast({ title: '最多选 3 个', icon: 'none' })
}

async function onSubmit() {
  if (!canSubmit.value) {
    if (!birthday.value) uni.showToast({ title: '请选择生日', icon: 'none' })
    else if (!agreed.value) uni.showToast({ title: '请先勾选协议', icon: 'none' })
    return
  }
  // 未成年提示
  if (isMinor(birthday.value)) {
    uni.showModal({
      title: '温馨提示',
      content: '检测到你未满 14 岁，将屏蔽部分维度。',
      showCancel: false
    })
  }

  submitting.value = true
  uni.showLoading({ title: '推算中...' })
  try {
    userStore.saveProfile({
      nickname: nickname.value,
      gender: gender.value,
      birthday: birthday.value,
      birthHour: birthHour.value,
      tags: tags.value,
      agreed: agreed.value
    })
    await fortuneStore.calc({
      birthday: birthday.value,
      birthHour: birthHour.value,
      gender: gender.value,
      tags: tags.value,
      calendarType: 'solar'
    })
    track('submit_profile', { gender: gender.value })
    uni.hideLoading()
    uni.redirectTo({ url: '/pages/result/result' })
  } catch (e) {
    uni.hideLoading()
    uni.showToast({ title: (e as Error).message || '推算失败，请重试', icon: 'none' })
  } finally {
    setTimeout(() => { submitting.value = false }, 1500)  // 防抖 1.5s
  }
}
</script>

<template>
  <view class="calc">
    <view class="calc__row">
      <text class="calc__label">昵称</text>
      <input v-model="nickname" class="calc__input" maxlength="8" placeholder="≤ 8 字" />
    </view>

    <view class="calc__row">
      <text class="calc__label">性别</text>
      <view class="calc__radio-group">
        <view class="calc__radio" :class="{ active: gender === 'M' }" @click="gender = 'M'">男</view>
        <view class="calc__radio" :class="{ active: gender === 'F' }" @click="gender = 'F'">女</view>
        <view class="calc__radio" :class="{ active: gender === 'U' }" @click="gender = 'U'">不愿透露</view>
      </view>
    </view>

    <view class="calc__row">
      <text class="calc__label">公历生日</text>
      <picker mode="date" :value="birthday" :end="today" @change="onChangeBirthday">
        <view class="calc__picker">{{ birthday || '请选择' }}</view>
      </picker>
    </view>

    <view class="calc__row">
      <text class="calc__label">出生时辰</text>
      <picker mode="selector" :range="HOUR_OPTIONS" :value="hourIndex" @change="onChangeHour">
        <view class="calc__picker">{{ HOUR_OPTIONS[hourIndex] }}</view>
      </picker>
    </view>

    <view class="calc__row calc__row--col">
      <text class="calc__label">关注维度（最多 3）</text>
      <view class="calc__chips">
        <view
          v-for="t in TAG_OPTIONS"
          :key="t.key"
          class="calc__chip"
          :class="{ active: tags.includes(t.key) }"
          @click="toggleTag(t.key)"
        >{{ t.name }}</view>
      </view>
    </view>

    <view class="calc__agree" @click="agreed = !agreed">
      <view class="calc__checkbox" :class="{ active: agreed }"></view>
      <text class="calc__agree-text">我已阅读并同意《用户协议》《隐私政策》《免责声明》</text>
    </view>

    <button class="calc__submit" :disabled="!canSubmit" :class="{ disabled: !canSubmit }" @click="onSubmit">
      生成今日运势
    </button>

    <text class="calc__disclaimer">本结果仅供娱乐参考，不构成任何决策建议。</text>
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.calc {
  padding: $space-5;

  /* 信息行：白底卡片，每行间距统一 */
  &__row {
    background: $color-card;
    border-radius: $radius-md;
    padding: 28rpx;
    margin-bottom: $space-3;
    @include row-flex;
    box-shadow: $shadow-sm;
    &--col {
      flex-direction: column;
      align-items: flex-start;
    }
  }
  &__label {
    color: $color-text-strong;
    font-size: $fs-base;
    font-weight: $fw-medium;
  }
  &__input {
    flex: 1;
    text-align: right;
    font-size: $fs-base;
    color: $color-text;
  }
  &__picker {
    color: $color-primary;
    font-size: $fs-base;
    font-weight: $fw-medium;
  }

  /* 性别 radio：胶囊样式 */
  &__radio-group {
    display: flex;
    gap: $space-3;
  }
  &__radio {
    padding: 10rpx 28rpx;
    background: $color-divider;
    border-radius: $radius-pill;
    font-size: $fs-md;
    color: $color-text-body;
    transition: background $transition-fast;
    &.active {
      background: $color-primary;
      color: #fff;
    }
  }

  /* 关注维度 chips */
  &__chips {
    display: flex;
    flex-wrap: wrap;
    gap: $space-3;
    margin-top: $space-3;
  }
  &__chip {
    padding: 14rpx 32rpx;
    background: $color-divider;
    border-radius: $radius-pill;
    font-size: $fs-md;
    color: $color-text-body;
    transition: background $transition-fast;
    &.active {
      background: $color-primary-light;
      color: $color-primary;
      font-weight: $fw-medium;
    }
  }

  /* 协议勾选 */
  &__agree {
    display: flex;
    align-items: center;
    padding: $space-4 8rpx;
  }
  &__checkbox {
    width: 36rpx;
    height: 36rpx;
    border: 2rpx solid #ccc;
    border-radius: 6rpx;
    margin-right: $space-3;
    flex-shrink: 0;
    transition: all $transition-fast;
    &.active {
      background: $color-primary;
      border-color: $color-primary;
    }
  }
  &__agree-text {
    font-size: $fs-sm;
    color: $color-text-body;
    flex: 1;
    line-height: $lh-base;
  }

  /* 提交按钮：复用 mixin */
  &__submit {
    @include btn-primary;
  }

  &__disclaimer {
    @include disclaimer;
    margin-top: $space-4;
  }
}
</style>
