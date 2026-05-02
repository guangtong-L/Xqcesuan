import { createSSRApp } from 'vue'
import { createPinia } from 'pinia'
import App from './App.vue'

// uni-app vue3 标准入口（导出 createApp）
export function createApp() {
  const app = createSSRApp(App)
  app.use(createPinia())
  return { app }
}
