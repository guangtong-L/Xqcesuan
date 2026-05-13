<script setup lang="ts">
/**
 * 个人中心：头像 + 资料编辑入口 + 签到天数 + 设置 + 协议入口 + Banner
 * 影响平台：H5 / 微信小程序（getUserProfile 仅小程序）
 */
import { computed, onMounted, ref } from 'vue'
import { useUserStore } from '@/store/user'
import { track } from '@/utils/tracker'
import { removeStorage } from '@/utils/storage'
import { clearSignHistory } from '@/utils/signHistory'
import AdBanner from '@/components/AdBanner/index.vue'
import { ADS } from '@/config/ads'

const userStore = useUserStore()

const subscribeOn = ref<boolean>(false)
const defaultAvatar = '/static/icons/avatar-default.png'
const wxNickname = ref('')
const wxAvatar = ref('')

const showName = computed(() => wxNickname.value || '签友')
const showAvatar = computed(() => wxAvatar.value || defaultAvatar)

onMounted(() => {
  track('mine_view', { hasProfile: userStore.hasProfile })
})

function onLogin() {
  // #ifdef MP-WEIXIN
  ;(uni as any).getUserProfile({
    desc: '用于完善资料展示',
    success: (res: any) => {
      wxNickname.value = res?.userInfo?.nickName || ''
      wxAvatar.value = res?.userInfo?.avatarUrl || ''
    },
    fail: () => uni.showToast({ title: '已取消授权', icon: 'none' })
  })
  // #endif
  // #ifndef MP-WEIXIN
  uni.showToast({ title: '请在微信小程序内授权', icon: 'none' })
  // #endif
}

function onEditProfile() {
  track('mine_draw_click')
  uni.switchTab({ url: '/pages/index/index' })
}

function onSubscribeToggle(e: any) {
  subscribeOn.value = !!e?.detail?.value
  track('mine_subscribe_toggle', { on: subscribeOn.value })
  // #ifdef MP-WEIXIN
  if (subscribeOn.value) {
    ;(uni as any).requestSubscribeMessage({
      tmplIds: ['__REPLACE_WITH_REAL_TMPL_ID__'],
      complete: () => { /* 忽略结果，实际上线请记录授权状态 */ }
    })
  }
  // #endif
}

function onClearCache() {
  track('mine_clear_cache_click')
  uni.showModal({
    title: '清除缓存',
    content: '将清除签到天数与抽签记录，确定吗？',
    success: (r) => {
      if (!r.confirm) return
      removeStorage('user_profile')
      removeStorage('fortune_current')
      removeStorage('sign_in')
      clearSignHistory()
      userStore.logout()
      uni.showToast({ title: '已清除', icon: 'success' })
    }
  })
}

function onAgreementClick(type: 'user' | 'privacy' | 'disclaimer') {
  track('mine_agreement_click', { type })
  const titleMap = {
    user: '《用户协议》',
    privacy: '《隐私政策》',
    disclaimer: '《免责声明》'
  }
  uni.showModal({
    title: titleMap[type],
    content: '本小程序内容仅供娱乐参考，不收集敏感个人信息，不构成任何决策建议。完整文本请通过运营提供的入口查看。',
    showCancel: false
  })
}

function onFeedback() {
  track('mine_feedback_click')
  // #ifdef MP-WEIXIN
  uni.showToast({ title: '请通过客服入口反馈', icon: 'none' })
  // #endif
  // #ifndef MP-WEIXIN
  uni.showToast({ title: '反馈入口仅小程序可用', icon: 'none' })
  // #endif
}
</script>

<template>
  <view class="mine">
    <!-- 头像区 -->
    <view class="mine__header">
      <image class="mine__avatar" :src="showAvatar" mode="aspectFill" />
      <view class="mine__header-info">
        <text class="mine__name">{{ showName }}</text>
        <text v-if="!wxNickname" class="mine__login" @click="onLogin">点击登录</text>
        <text v-else class="mine__login mine__login--logged">已登录</text>
      </view>
    </view>

    <!-- 签到 -->
    <view class="mine__signin">
        <text>已连续签到 {{ userStore.signInDays }} 天</text>
    </view>

    <!-- 设置项 -->
    <view class="mine__group">
      <view class="mine__row" @click="onEditProfile">
        <text>去抽今日签</text>
        <text class="mine__row-arrow">›</text>
      </view>
      <view class="mine__row">
        <text>每日一签提醒</text>
        <switch :checked="subscribeOn" @change="onSubscribeToggle" />
      </view>
      <view class="mine__row" @click="onClearCache">
        <text>清除缓存</text>
        <text class="mine__row-arrow">›</text>
      </view>
    </view>

    <!-- 协议入口 -->
    <view class="mine__group">
      <view class="mine__row" @click="onAgreementClick('user')">
        <text>用户协议</text>
        <text class="mine__row-arrow">›</text>
      </view>
      <view class="mine__row" @click="onAgreementClick('privacy')">
        <text>隐私政策</text>
        <text class="mine__row-arrow">›</text>
      </view>
      <view class="mine__row" @click="onAgreementClick('disclaimer')">
        <text>免责声明</text>
        <text class="mine__row-arrow">›</text>
      </view>
      <view class="mine__row" @click="onFeedback">
        <text>意见反馈</text>
        <text class="mine__row-arrow">›</text>
      </view>
    </view>

    <!-- 关于 -->
    <view class="mine__group">
      <view class="mine__row">
        <text>版本</text>
        <text class="mine__row-value">v1.0.0</text>
      </view>
    </view>

    <!-- Banner -->
    <AdBanner :unit-id="ADS.MINE_BANNER" />

    <text class="mine__disclaimer">本内容仅供娱乐参考，请理性看待。</text>
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.mine {
  padding: $space-5;

  /* ========== 头部：柔和粉色卡片，避免与首页紫色 hero 重复 ========== */
  &__header {
    @include hero-soft-pink;
    border-radius: $radius-xl;
    padding: 40rpx $space-5;
    display: flex;
    align-items: center;
    gap: $space-4;
    box-shadow: $shadow-sm;
    border: 2rpx solid rgba(255, 93, 166, 0.08);
  }
  &__avatar {
    width: 112rpx;
    height: 112rpx;
    border-radius: 50%;
    background: #fff;
    box-shadow: $shadow-md;
    border: 4rpx solid #fff;
  }
  &__header-info {
    display: flex;
    flex-direction: column;
    gap: $space-2;
  }
  &__name {
    font-size: $fs-xl;
    font-weight: $fw-semi;
    color: $color-text;
  }
  &__login {
    font-size: $fs-sm;
    color: $color-love;
    padding: 4rpx 16rpx;
    background: rgba(255, 93, 166, 0.1);
    border-radius: $radius-pill;
    align-self: flex-start;
  }
  &__login--logged {
    color: $color-text-sub;
    background: rgba(0, 0, 0, 0.04);
  }

  /* ========== 签到 ========== */
  &__signin {
    margin-top: $space-4;
    @include card-base;
    padding: 28rpx;
    text-align: center;
    font-size: $fs-base;
    color: $color-text-body;
  }

  /* ========== 设置组 ========== */
  &__group {
    margin-top: $space-4;
    background: $color-card;
    border-radius: $radius-lg;
    overflow: hidden;
    box-shadow: $shadow-sm;
  }
  &__row {
    padding: 28rpx $space-5;
    @include row-flex;
    border-bottom: 1rpx solid $color-divider;
    font-size: $fs-base;
    color: $color-text;
    transition: background $transition-fast;
    &:last-child { border-bottom: none; }
    &:active { background: $color-divider-light; }
  }
  &__row-arrow {
    color: #ccc;
    font-size: $fs-xl;
  }
  &__row-value {
    color: $color-text-sub;
    font-size: $fs-md;
  }

  &__disclaimer {
    @include disclaimer;
  }
}
</style>
