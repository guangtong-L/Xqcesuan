import { defineConfig } from 'vite'
import uni from '@dcloudio/vite-plugin-uni'
import path from 'path'

// uni-app 官方推荐的最简配置
// 注意：mp-weixin 平台不支持 import.meta.env 全部能力，环境变量请走 utils/request.ts 的常量开关
export default defineConfig({
  plugins: [uni()],
  resolve: {
    alias: {
      '@': path.resolve(__dirname, './src')
    }
  },
  // 微信小程序不需要 server 配置；H5 调试时再覆盖
  server: {
    port: 5173,
    host: '0.0.0.0'
  }
})
