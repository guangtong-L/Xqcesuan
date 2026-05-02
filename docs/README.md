# 极简星座/八字流年运势小程序 - 项目交付包

> 日期：2026-04-30 ｜ 阶段：MVP P1（Mock 数据期）

---

## 项目结构

```
D:/code/运势/
├── docs/                       # 全部文档（先看）
│   ├── 01-PRD.md
│   ├── 02-技术方案与接口契约.md
│   ├── 03-联调对接说明.md
│   ├── 04-本地启动手册.md
│   ├── 05-埋点与广告位.md
│   └── 06-测试用例与验收清单.md
├── frontend/                   # uni-app + Vue3 + TS（微信小程序为主）
│   ├── src/
│   │   ├── pages/{index,calc,result,history,mine}/
│   │   ├── components/{ScoreRing,DimensionCard,AdBanner}/
│   │   ├── api/{request,types,auth,fortune}.ts
│   │   ├── store/{user,fortune}.ts
│   │   ├── utils/{date,storage,tracker}.ts
│   │   └── static/mock/   # 本期 USE_MOCK 用的静态 JSON
│   ├── package.json
│   ├── vite.config.ts
│   ├── tsconfig.json
│   └── README.md
└── backend/                    # Spring Boot 2.7 + Lombok + Hutool
    ├── src/main/java/com/fortune/
    │   ├── controller/{Auth,Fortune,Stats}Controller.java
    │   ├── service/{Auth,Fortune,History}Service.java
    │   ├── mock/MockDataService.java
    │   ├── dto/{req,resp}/...
    │   ├── common/{ApiResponse,ErrorCode,BizException,GlobalExceptionHandler,TraceIdFilter,RateLimitInterceptor,OpenIdResolver}.java
    │   ├── config/{WebMvc,Jackson,Cors}Config.java
    │   └── FortuneApplication.java
    ├── src/main/resources/
    │   ├── application(-dev/-prod).yml
    │   └── mock/{fortune_text,lucky_pool,yi_ji_pool,deep_content}.json
    ├── pom.xml
    ├── Dockerfile
    └── README.md
```

---

## 各角色看哪份

| 角色 | 必看 | 选看 |
|---|---|---|
| 产品 | 01-PRD、05-埋点 | 06-测试 |
| 前端 | 02-契约、03-联调、04-启动 | 05-埋点 |
| 后端 | 02-契约、04-启动 | 03-联调（错误码） |
| 测试 | 06-测试、03-联调 | 01-PRD |
| 运维 | 04-启动 | 02-技术方案第 7/10 节 |
| 法务/合规 | 01-PRD 第 6 节、02-契约第 6 节（禁用词） | - |

---

## 本期完成范围

✅ 产品 PRD（含验收 DoD、合规红线、广告策略、不做项）
✅ 技术方案 + 6 个 API 接口契约（统一响应、错误码、限流、TraceId）
✅ 前端 5 个页面（首页/录入/结果/历史/个人中心）+ 3 个组件 + 4 个 API + 2 个 Store
✅ 后端 3 个 Controller + 3 个 Service + 1 个 Mock 数据加载 + 完整 common 层
✅ 文案池 JSON（吉/平/大吉 × 4 维度，宜忌、幸运、深度内容）
✅ 联调说明、本地启动、埋点广告、测试用例 4 份配套文档
✅ Dockerfile + 云托管发布配置

❌ 未做（P2 阶段）：MySQL/Redis、JWT 鉴权、微信广告回调验签、真实八字算法
❌ 未做（P3 阶段）：付费会员、AI 生成、多端适配

---

## 30 分钟跑通本地

```bash
# 1. 后端
cd D:/code/运势/backend
mvn -DskipTests spring-boot:run
# 验证：curl http://localhost:8080/api/stats/today

# 2. 前端（先用 H5 看 UI）
cd D:/code/运势/frontend
pnpm install
pnpm dev:h5
# 浏览器访问 http://localhost:5173

# 3. 切到微信小程序
pnpm dev:mp-weixin
# 微信开发者工具导入 dist/dev/mp-weixin
```

详见 `04-本地启动手册.md`。

---

## 关键风险（线上必读）

1. **数据丢失**：本期内存方案，重启数据归零。请在录入页文案明示。
2. **越权防护**：已做 openid 校验，接 DB 后保持。
3. **激励视频被刷**：本期 unlock 不验签 adToken，必须在 P2 接微信广告回调。
4. **审核类目**：小程序类目选「工具-实用查询」，避开「星座命理」。
5. **文案禁用词**：上线前 CI 扫描，命中阻断。
6. **跨天时区**：Dockerfile 已设 Asia/Shanghai，回归时务必校验。
