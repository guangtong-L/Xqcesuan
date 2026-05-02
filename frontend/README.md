# 极简星座/八字流年运势测算 - 前端

> 技术栈：uni-app + Vue3 + TypeScript + uni-ui + Pinia
> 编译目标：微信小程序为主，兼容 H5 调试
> 后端通信：wx.cloud.callContainer（微信云托管 SpringBoot）

---

## 一、本地启动

### 方式 A：命令行（CLI）

```bash
# 1. 安装依赖
cd frontend
npm install

# 2. 启动微信小程序开发模式（产物输出到 dist/dev/mp-weixin）
npm run dev:mp-weixin

# 3. 打开微信开发者工具
#    导入项目 → 项目目录选择：frontend/dist/dev/mp-weixin
#    AppID 填 manifest.json 里配置的（或测试号）
```

### 方式 B：HBuilderX

1. HBuilderX 打开 `frontend/` 整个目录。
2. 顶部菜单 → 运行 → 运行到小程序模拟器 → 微信开发者工具。
3. 首次会要求填写 AppID，与 `src/manifest.json` 一致。

---

## 二、关键开关

### 1. Mock 数据开关（本期默认开启）

`src/api/request.ts` 顶部：

```ts
const USE_MOCK = true  // 本期默认 true，前端独立跑通；后端联调时改 false
```

切到 `false` 之后，会真实调用 `wx.cloud.callContainer`，需要：

- 微信云托管已部署 `fortune-backend` 服务
- `src/api/request.ts` 内 `WX_CLOUD_ENV` 已填正确的云环境 ID
- 小程序后台 → 开发管理 → 服务器域名 → 云托管服务 → 把 `fortune-backend` 加入白名单

### 2. AppID

`src/manifest.json` → `mp-weixin.appid` 占位 `wxXXXXXXXXXXXXXXXX`，上线前必须替换。

### 3. 广告位 ID

页面里所有 `<ad unit-id="adunit-xxxx">` 均为占位，上线前必须替换为微信流量主后台真实 unitId。

---

## 三、目录约定

```
frontend/src/
├── pages/        页面（首页/录入/结果/历史/我的）
├── components/   公共组件（环形评分/维度卡/Banner）
├── api/          接口契约 + 请求封装
├── store/        Pinia
├── utils/        纯工具函数
└── static/       静态资源 + mock JSON
```

每个页面 ≤ 200 行，复杂逻辑下沉到 utils / components。

---

## 四、与后端联调

| 步骤 | 操作 |
|---|---|
| 1 | 确认 `backend` 已 docker 部署到云托管，服务名 `fortune-backend` |
| 2 | `src/api/request.ts` 把 `USE_MOCK` 改为 `false` |
| 3 | `WX_CLOUD_ENV` 填云环境 ID（云托管控制台首页可见） |
| 4 | 微信开发者工具 → 重新编译，调一次 `/api/auth/login` 看 Network |
| 5 | 若 401 / openid 异常，检查云托管"服务路径白名单"是否含 `/api/*` |

---

## 五、合规红线（必读）

- 全站底部固定免责声明："本结果仅供娱乐参考，不构成任何决策建议。"
- 文案严禁包含：命运、注定、煞、化解、破财、血光、必然、一定会
- 4 个功能卡片中除"今日运势"外，其余 MVP 一律"敬请期待"，不要私自接入
- 激励视频解锁：必须 `onClose` 回调 `isEnded === true` 才调 unlock，不允许跳过

---

## 六、上线前 checklist

- [ ] AppID 替换
- [ ] 广告位 unitId 替换
- [ ] USE_MOCK 改 false
- [ ] WX_CLOUD_ENV 填写
- [ ] 文案禁用词扫描（CI 待补）
- [ ] 小程序类目选"工具-实用查询"，不选"星座命理"
