<script setup lang="ts">
import { onLaunch, onShow, onHide } from '@dcloudio/uni-app'
import { useUserStore } from '@/store/user'
import { markAppLaunch } from '@/utils/ad'

// App 入口生命周期
onLaunch(() => {
  console.log('[App] onLaunch')
  // 记录启动时间，用于插屏 5s 冷启动保护
  markAppLaunch()
  // 启动时尝试静默登录（仅微信小程序）
  // #ifdef MP-WEIXIN
  const userStore = useUserStore()
  userStore.silentLogin().catch((e) => {
    console.warn('[App] 静默登录失败，继续浏览', e)
  })
  // #endif
})

onShow(() => {
  console.log('[App] onShow')
})

onHide(() => {
  console.log('[App] onHide')
})
</script>

<style lang="scss">
/**
 * 全局样式入口
 * - rpx 适配（uni-app 默认）
 * - 重置 button 默认样式（去除微信原生灰色边框）
 * - :root 上挂全套设计 Token 的 CSS 变量副本（便于运行时主题切换）
 *   注：scss 编译期 Token 在 src/styles/tokens.scss，与此处变量同名
 */

page {
  background-color: #f7f7fa;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  font-size: 28rpx;
  color: #1f1f24;
}

view, text, image, button {
  box-sizing: border-box;
}

button {
  margin: 0;
  padding: 0;
  border: none;
  background: transparent;
  &::after {
    border: none;
  }
}

/* ============================================================
 * 全局设计 Token（运行时 CSS 变量副本）
 * 与 src/styles/tokens.scss 中的 SCSS 变量保持同步命名
 * ============================================================ */
:root,
page {
  /* 颜色 - 主色 */
  --color-primary:        #7c5cff;
  --color-primary-light:  #ede8ff;
  --color-primary-soft:   #b39dff;
  --color-primary-deep:   #5a3fd6;

  /* 颜色 - 模块辅助色 */
  --color-love:    #ff5da6;
  --color-health:  #51c878;
  --color-wealth:  #f0ad4e;
  --color-career:  #4a90e2;
  --color-danger:  #ff6b6b;

  /* 颜色 - 季节色（八字流年） */
  --color-spring:  #51c878;
  --color-summer:  #ff8e3c;
  --color-autumn:  #d4a017;
  --color-winter:  #4a90e2;

  /* 颜色 - 文字 */
  --color-text:        #1f1f24;
  --color-text-strong: #333333;
  --color-text-body:   #555555;
  --color-text-sub:    #888888;
  --color-text-mute:   #aaaaaa;

  /* 颜色 - 背景 */
  --color-bg:           #f7f7fa;
  --color-card:         #ffffff;
  --color-divider:      #f0f0f5;
  --color-divider-light:#f5f5f8;

  /* 字号阶梯 */
  --fs-xs: 22rpx;
  --fs-sm: 24rpx;
  --fs-md: 26rpx;
  --fs-base: 28rpx;
  --fs-lg: 30rpx;
  --fs-xl: 32rpx;
  --fs-2xl: 36rpx;
  --fs-3xl: 48rpx;
  --fs-display: 96rpx;

  /* 圆角阶梯 */
  --radius-sm:  12rpx;
  --radius-md:  16rpx;
  --radius-lg:  24rpx;
  --radius-xl:  32rpx;
  --radius-pill: 999rpx;

  /* 间距阶梯 */
  --space-1: 4rpx;
  --space-2: 8rpx;
  --space-3: 16rpx;
  --space-4: 24rpx;
  --space-5: 32rpx;
  --space-6: 48rpx;

  /* 阴影 3 档 */
  --shadow-sm: 0 2rpx 8rpx rgba(31, 31, 36, 0.04);
  --shadow-md: 0 8rpx 24rpx rgba(124, 92, 255, 0.12);
  --shadow-lg: 0 12rpx 40rpx rgba(124, 92, 255, 0.18);

  /* 过渡 */
  --transition-fast: 0.2s ease;
  --transition-base: 0.3s ease;
}
</style>
