<script setup lang="ts">
/**
 * 今日幸运签：抽签动画 + 签号 + 4 句签诗 + 解读 + 宜忌
 * 影响平台：H5 / 微信小程序
 * 同一人当日抽到的签是固定的；跨日刷新
 */
import { computed, getCurrentInstance, nextTick, onMounted, ref } from 'vue'
import { drawSign } from '@/api/extra'
import { track } from '@/utils/tracker'
import { saveSignHistory } from '@/utils/signHistory'
import AdBanner from '@/components/AdBanner/index.vue'
import { ADS } from '@/config/ads'
import type { SignResp } from '@/api/types'

const data = ref<SignResp | null>(null)
const loading = ref(false)
const drawn = ref(false)
const errorMsg = ref('')
const exporting = ref(false)
const selectedMood = ref('reset')
const instance = getCurrentInstance()

const POSTER_CANVAS_ID = 'signPosterCanvas'
const POSTER_WIDTH = 750
const POSTER_HEIGHT = 1200

interface MoodOption {
  key: string
  label: string
  word: string
  line: string
  color: string
}

const moods: MoodOption[] = [
  { key: 'reset', label: '想重启', word: '重启签', line: '给今天留一个重新开始的入口。', color: '#7c5cff' },
  { key: 'brave', label: '想勇敢', word: '破局签', line: '把犹豫先放下，给行动一点位置。', color: '#ff5da6' },
  { key: 'calm', label: '想安静', word: '安定签', line: '慢一点，今天不必急着证明什么。', color: '#51c878' },
  { key: 'luck', label: '想好运', word: '顺风签', line: '把注意力放回能推进的小事上。', color: '#d4a017' }
]

const mood = computed(() => moods.find((item) => item.key === selectedMood.value) || moods[0])

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
    saveSignHistory(data.value)
    drawn.value = true
    track('sign_drawn', { signNo: data.value.signNo, signName: data.value.signName, mood: selectedMood.value })
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

function namePosterColor(name: string): string {
  if (name === '上上签') return '#ff5da6'
  if (name === '上签') return '#7c5cff'
  if (name === '中签') return '#51c878'
  return '#888888'
}

function selectMood(key: string) {
  if (loading.value) return
  selectedMood.value = key
  track('sign_mood_select', { mood: key })
}

function drawRoundRect(ctx: any, x: number, y: number, w: number, h: number, r: number, color: string) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.lineTo(x + w - r, y)
  ctx.quadraticCurveTo(x + w, y, x + w, y + r)
  ctx.lineTo(x + w, y + h - r)
  ctx.quadraticCurveTo(x + w, y + h, x + w - r, y + h)
  ctx.lineTo(x + r, y + h)
  ctx.quadraticCurveTo(x, y + h, x, y + h - r)
  ctx.lineTo(x, y + r)
  ctx.quadraticCurveTo(x, y, x + r, y)
  ctx.closePath()
  ctx.setFillStyle(color)
  ctx.fill()
}

function drawCenteredText(ctx: any, text: string, y: number, size: number, color: string, bold = false) {
  ctx.setFillStyle(color)
  ctx.setFontSize(size)
  ctx.setTextAlign('center')
  ctx.setTextBaseline('middle')
  ctx.font = `${bold ? 'bold ' : ''}${size}px sans-serif`
  ctx.fillText(text, POSTER_WIDTH / 2, y)
}

function drawText(ctx: any, text: string, x: number, y: number, size: number, color: string, bold = false) {
  ctx.setFillStyle(color)
  ctx.setFontSize(size)
  ctx.setTextAlign('left')
  ctx.setTextBaseline('top')
  ctx.font = `${bold ? 'bold ' : ''}${size}px sans-serif`
  ctx.fillText(text, x, y)
}

function drawWrappedText(ctx: any, text: string, x: number, y: number, maxWidth: number, lineHeight: number, maxLines = 3) {
  const chars = text.split('')
  const lines: string[] = []
  let line = ''
  for (const ch of chars) {
    const test = line + ch
    if (ctx.measureText(test).width > maxWidth && line) {
      lines.push(line)
      line = ch
      if (lines.length >= maxLines) break
    } else {
      line = test
    }
  }
  if (line && lines.length < maxLines) lines.push(line)
  lines.forEach((l, idx) => ctx.fillText(l, x, y + idx * lineHeight))
}

function fitText(ctx: any, text: string, maxWidth: number, size: number, minSize = 18): number {
  let current = size
  ctx.setFontSize(current)
  while (current > minSize && ctx.measureText(text).width > maxWidth) {
    current -= 1
    ctx.setFontSize(current)
  }
  return current
}

function drawPoster(sign: SignResp): Promise<void> {
  return new Promise((resolve) => {
    const ctx = uni.createCanvasContext(POSTER_CANVAS_ID, instance?.proxy)
    const nameColorHex = namePosterColor(sign.signName)
    const currentMood = mood.value

    const bg = ctx.createLinearGradient(0, 0, 0, POSTER_HEIGHT)
    bg.addColorStop(0, '#111827')
    bg.addColorStop(0.52, '#261a3d')
    bg.addColorStop(1, '#f3d7a6')
    ctx.setFillStyle(bg)
    ctx.fillRect(0, 0, POSTER_WIDTH, POSTER_HEIGHT)

    ctx.setGlobalAlpha(0.24)
    ctx.setFillStyle('#f8d98b')
    ;[[90, 138, 4], [640, 124, 3], [612, 328, 6], [124, 420, 3], [662, 578, 4], [96, 760, 5]].forEach(([x, y, r]) => {
      ctx.beginPath()
      ctx.arc(x, y, r, 0, Math.PI * 2)
      ctx.fill()
    })
    ctx.setGlobalAlpha(1)

    drawRoundRect(ctx, 48, 56, 654, 1088, 46, 'rgba(255, 248, 232, 0.97)')
    drawRoundRect(ctx, 82, 92, 586, 1016, 28, '#fff7e6')

    ctx.setStrokeStyle('#d7ad58')
    ctx.setLineWidth(3)
    ctx.strokeRect(102, 108, 546, 984)

    drawCenteredText(ctx, 'MOOD SIGN', 140, 20, '#b48a45')
    drawCenteredText(ctx, currentMood.word, 184, 46, '#5a3e1a', true)
    drawCenteredText(ctx, sign.date, 230, 24, '#a88856')

    drawRoundRect(ctx, 250, 268, 250, 58, 29, currentMood.color)
    drawCenteredText(ctx, currentMood.label, 297, 26, '#ffffff', true)
    drawCenteredText(ctx, sign.signName, 386, 76, nameColorHex, true)
    drawCenteredText(ctx, `第 ${sign.signNo} 签`, 444, 24, '#8a6a3e')

    ctx.setStrokeStyle('#d4a017')
    ctx.setLineWidth(2)
    ctx.strokeRect(140, 500, 470, 236)
    drawRoundRect(ctx, 162, 522, 426, 192, 14, 'rgba(255, 255, 255, 0.46)')

    sign.poem.forEach((line, idx) => {
      drawCenteredText(ctx, line, 558 + idx * 40, 30, '#4d3518')
    })

    drawText(ctx, '今日回应', 124, 778, 28, '#5a3e1a', true)
    ctx.setFillStyle('#5f574d')
    ctx.setFontSize(24)
    ctx.setTextAlign('left')
    ctx.setTextBaseline('top')
    drawWrappedText(ctx, `${currentMood.line}${sign.explain}`, 124, 828, 502, 36, 4)

    const yiText = sign.yi.slice(0, 2).join('、')
    const jiText = sign.ji.slice(0, 2).join('、')
    drawText(ctx, '宜', 124, 948, 24, '#51c878', true)
    drawText(ctx, yiText, 174, 946, fitText(ctx, yiText, 430, 24, 18), '#3e5d45')
    drawText(ctx, '忌', 124, 1000, 24, '#ff6b6b', true)
    drawText(ctx, jiText, 174, 998, fitText(ctx, jiText, 430, 24, 18), '#694949')

    ctx.setStrokeStyle('rgba(180, 138, 69, 0.35)')
    ctx.setLineWidth(1)
    ctx.beginPath()
    ctx.moveTo(164, 1054)
    ctx.lineTo(586, 1054)
    ctx.stroke()

    drawCenteredText(ctx, '本签仅供娱乐参考，请理性看待', 1078, 21, '#8d8070')
    drawCenteredText(ctx, '今天不是答案，是一个可执行的暗号', 1110, 21, '#6b4b24')

    ctx.draw(false, () => resolve())
  })
}

function canvasToFile(): Promise<string> {
  return new Promise((resolve, reject) => {
    uni.canvasToTempFilePath({
      canvasId: POSTER_CANVAS_ID,
      width: POSTER_WIDTH,
      height: POSTER_HEIGHT,
      destWidth: POSTER_WIDTH * 2,
      destHeight: POSTER_HEIGHT * 2,
      success: (res: any) => resolve(res.tempFilePath),
      fail: (err: any) => reject(err)
    }, instance?.proxy)
  })
}

function saveImage(filePath: string): Promise<void> {
  return new Promise((resolve, reject) => {
    // #ifdef MP-WEIXIN
    uni.saveImageToPhotosAlbum({
      filePath,
      success: () => resolve(),
      fail: (err: any) => reject(err)
    })
    // #endif

    // #ifndef MP-WEIXIN
    uni.previewImage({ urls: [filePath] })
    resolve()
    // #endif
  })
}

async function onExportPoster() {
  if (!data.value || exporting.value) return
  exporting.value = true
  track('sign_export_click', { signNo: data.value.signNo, signName: data.value.signName, mood: selectedMood.value })
  try {
    await nextTick()
    await drawPoster(data.value)
    const filePath = await canvasToFile()
    await saveImage(filePath)
    track('sign_export_success', { signNo: data.value.signNo, signName: data.value.signName, mood: selectedMood.value })
    uni.showToast({ title: '签图已保存', icon: 'success' })
  } catch (e: any) {
    const msg = e?.errMsg?.includes('auth') ? '请允许保存到相册' : '生成签图失败，请重试'
    track('sign_export_fail', { reason: e?.errMsg || e?.message || 'unknown' })
    uni.showToast({ title: msg, icon: 'none' })
  } finally {
    exporting.value = false
  }
}
</script>

<template>
  <view class="sign">
    <view v-if="!drawn" class="sign__intro">
      <view class="sign__ritual">
        <text class="sign__eyebrow">今日心境签</text>
        <text class="sign__ritual-title">先选此刻的你</text>
        <text class="sign__ritual-sub">同一支签，因为心境不同，会变成不一样的提醒。</text>
      </view>

      <view class="sign__moods">
        <view
          v-for="item in moods"
          :key="item.key"
          class="sign__mood"
          :class="{ 'sign__mood--active': selectedMood === item.key }"
          :style="{ borderColor: selectedMood === item.key ? item.color : 'rgba(90, 62, 26, 0.12)' }"
          @click="selectMood(item.key)"
        >
          <text class="sign__mood-label">{{ item.label }}</text>
          <text class="sign__mood-word">{{ item.word }}</text>
        </view>
      </view>

      <view class="sign__deck" :class="{ 'sign__deck--loading': loading }" @click="onDraw">
        <view class="sign__deck-card sign__deck-card--back"></view>
        <view class="sign__deck-card sign__deck-card--front">
          <text class="sign__deck-mark">{{ mood.word }}</text>
          <text class="sign__deck-line">{{ loading ? '正在翻开今日暗号' : '点我翻签' }}</text>
        </view>
      </view>

      <button class="sign__btn" :disabled="loading" @click="onDraw">{{ loading ? '翻签中…' : '翻开今日签' }}</button>
      <text v-if="errorMsg" class="sign__error">{{ errorMsg }}</text>
    </view>

    <template v-else-if="data">
      <view class="sign__head" :style="{ borderColor: mood.color }">
        <text class="sign__mood-chip" :style="{ background: mood.color }">{{ mood.word }}</text>
        <text class="sign__no">第 {{ data.signNo }} 签</text>
        <text class="sign__name" :style="{ color: nameColor(data.signName) }">{{ data.signName }}</text>
        <text class="sign__reply">{{ mood.line }}</text>
        <text class="sign__date">{{ data.date }}</text>
      </view>

      <view class="sign__poem">
        <text v-for="(line, i) in data.poem" :key="i" class="sign__poem-line">{{ line }}</text>
      </view>

      <view class="sign__card">
        <text class="sign__card-title">今日回应</text>
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

      <view class="sign__actions">
        <button class="sign__poster-btn" :disabled="exporting" @click="onExportPoster">
          {{ exporting ? '生成中…' : '生成今日心境签图' }}
        </button>
      </view>

      <AdBanner :unit-id="ADS.HOME_BANNER" />
      <text class="sign__disclaimer">{{ data.tips }}</text>
    </template>

    <canvas
      class="sign__poster-canvas"
      :canvas-id="POSTER_CANVAS_ID"
      :id="POSTER_CANVAS_ID"
      :style="{ width: `${POSTER_WIDTH}px`, height: `${POSTER_HEIGHT}px` }"
    />
  </view>
</template>

<style lang="scss" scoped>
@use '@/styles/tokens.scss' as *;

.sign {
  padding: $space-5;
  min-height: 100vh;
  background:
    radial-gradient(circle at 20% 8%, rgba(212, 160, 23, 0.18) 0, transparent 28%),
    radial-gradient(circle at 86% 18%, rgba(124, 92, 255, 0.18) 0, transparent 26%),
    linear-gradient(180deg, #191120 0%, #312147 42%, #f7ead5 100%);

  &__intro {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: 42rpx;
    gap: $space-5;
  }

  &__ritual {
    color: #fff8ea;
    text-align: center;
    display: flex;
    flex-direction: column;
    gap: $space-3;
  }
  &__eyebrow {
    font-size: $fs-sm;
    color: rgba(255, 248, 234, 0.72);
    letter-spacing: 8rpx;
  }
  &__ritual-title {
    font-size: 56rpx;
    font-weight: $fw-bold;
    letter-spacing: 2rpx;
  }
  &__ritual-sub {
    font-size: $fs-md;
    color: rgba(255, 248, 234, 0.76);
    line-height: $lh-base;
    padding: 0 $space-5;
  }

  &__moods {
    width: 100%;
    display: grid;
    grid-template-columns: 1fr 1fr;
    gap: $space-3;
  }
  &__mood {
    background: rgba(255, 248, 234, 0.9);
    border: 2rpx solid rgba(90, 62, 26, 0.12);
    border-radius: $radius-lg;
    padding: 24rpx 28rpx;
    display: flex;
    flex-direction: column;
    gap: $space-2;
    box-shadow: 0 10rpx 28rpx rgba(0, 0, 0, 0.12);
    transition: transform $transition-fast, background $transition-fast;
    &:active {
      transform: scale(0.98);
    }
    &--active {
      background: #fffaf0;
      transform: translateY(-4rpx);
    }
  }
  &__mood-label {
    font-size: $fs-sm;
    color: #8a6a3e;
  }
  &__mood-word {
    font-size: $fs-xl;
    font-weight: $fw-bold;
    color: #2a1c12;
  }

  &__deck {
    position: relative;
    width: 430rpx;
    height: 560rpx;
    margin-top: $space-2;
    transform-style: preserve-3d;
    transition: transform $transition-slow;
    &--loading {
      animation: flipPulse 0.8s ease-in-out infinite alternate;
    }
  }
  @keyframes flipPulse {
    from { transform: rotate(-2deg) translateY(0); }
    to { transform: rotate(2deg) translateY(-10rpx); }
  }
  &__deck-card {
    position: absolute;
    inset: 0;
    border-radius: 36rpx;
    box-shadow: 0 24rpx 60rpx rgba(0, 0, 0, 0.28);
    border: 2rpx solid rgba(212, 160, 23, 0.45);
    &--back {
      transform: rotate(-7deg) translate(-22rpx, 22rpx);
      background: linear-gradient(145deg, rgba(255, 248, 234, 0.72), rgba(212, 160, 23, 0.2));
    }
    &--front {
      background:
        linear-gradient(180deg, rgba(255, 250, 240, 0.98), rgba(248, 225, 184, 0.96)),
        repeating-linear-gradient(90deg, rgba(212,160,23,.12) 0, rgba(212,160,23,.12) 2rpx, transparent 2rpx, transparent 28rpx);
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: $space-4;
    }
  }
  &__deck-mark {
    font-size: 54rpx;
    color: #5a3e1a;
    font-weight: $fw-bold;
    letter-spacing: 6rpx;
  }
  &__deck-line {
    font-size: $fs-md;
    color: #8a6a3e;
  }

  &__btn {
    width: 100%;
    background: linear-gradient(135deg, #d4a017 0%, #7c5cff 100%);
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
    color: #fff;
    font-size: $fs-sm;
    margin-top: $space-3;
  }

  &__head {
    background:
      linear-gradient(180deg, rgba(255, 251, 243, 0.98) 0%, rgba(255, 255, 255, 0.98) 100%);
    border-radius: $radius-xl;
    padding: 40rpx 0;
    text-align: center;
    display: flex;
    flex-direction: column;
    gap: $space-2;
    box-shadow: $shadow-sm;
    border: 3rpx solid #f5e6c8;
  }
  &__mood-chip {
    align-self: center;
    color: #fff;
    font-size: $fs-sm;
    font-weight: $fw-semi;
    border-radius: $radius-pill;
    padding: 8rpx 24rpx;
    margin-bottom: $space-2;
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
  &__reply {
    display: block;
    margin: $space-2 $space-5 0;
    font-size: $fs-md;
    color: #8a6a3e;
    line-height: $lh-base;
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

  &__actions {
    margin-top: $space-4;
  }
  &__poster-btn {
    background: linear-gradient(135deg, #1f1535 0%, #7c5cff 48%, #d4a017 100%);
    color: #fff;
    border-radius: $radius-pill;
    font-size: $fs-xl;
    font-weight: $fw-semi;
    padding: 22rpx 0;
    box-shadow: 0 10rpx 28rpx rgba(124, 92, 255, 0.28);
    &[disabled] {
      opacity: 0.65;
      box-shadow: none;
    }
    &:active { opacity: 0.88; }
  }

  &__poster-canvas {
    position: fixed;
    left: -9999px;
    top: -9999px;
    pointer-events: none;
  }

  &__disclaimer {
    @include disclaimer;
  }
}
</style>
