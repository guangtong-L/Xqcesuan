# fortune-backend

极简星座/八字流年运势测算 微信小程序 后端（MVP Mock 期）。

技术栈：Spring Boot 2.7.18 + JDK11 + Lombok + Hutool 5.8.x。

> 本期不连数据库，全部走内存 Mock。接口契约严格按 `docs/02-技术方案与接口契约.md`。

---

## 一、本地启动

### 方式 1：Maven
```bash
cd backend
mvn -B -DskipTests=true spring-boot:run
# 访问：http://localhost:8080/api/stats/today
```

### 方式 2：Docker（验证云托管镜像）
```bash
cd backend
docker build -t fortune-backend:dev .
docker run --rm -p 8080:80 -e SPRING_PROFILES_ACTIVE=prod fortune-backend:dev
```

### 方式 3：IDEA
直接运行 `com.fortune.FortuneApplication`，dev profile 默认端口 8080。

---

## 二、本地接口自测

```bash
# 1. 登录拿假 openid 与 token
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"code":"0c3test_wx_code"}'

# 2. 计算运势（带 X-WX-OPENID 模拟云托管）
curl -X POST http://localhost:8080/api/fortune/calc \
  -H "Content-Type: application/json" \
  -H "X-WX-OPENID: mock_openid_demo" \
  -d '{"birthday":"1995-08-15","birthHour":14,"gender":"M","tags":["career","love"],"calendarType":"solar"}'

# 3. 历史
curl "http://localhost:8080/api/fortune/history?page=1&pageSize=10" \
  -H "X-WX-OPENID: mock_openid_demo"

# 4. 解锁
curl -X POST http://localhost:8080/api/fortune/unlock \
  -H "Content-Type: application/json" \
  -H "X-WX-OPENID: mock_openid_demo" \
  -d '{"recordId":"rec_xxx","adToken":"mock_ad_token"}'
```

---

## 三、目录结构（约定）

```
src/main/java/com/fortune/
├── FortuneApplication.java      启动类
├── controller/                  HTTP 入口，仅做参数校验和编排
├── service/                     业务编排 + 算法
├── mock/                        启动期加载文案池，单例线程安全
├── dto/req|resp/                入参出参
├── common/                      统一响应、异常、TraceId、限流、CORS、OpenIdResolver
└── config/                      WebMvc/Jackson/Cors

src/main/resources/
├── application.yml              通用配置
├── application-dev.yml          开发 profile
├── application-prod.yml         生产 profile
└── mock/*.json                  文案池
```

---

## 四、关键 TODO（接 DB / 上线前必改）

| TODO | 位置 | 上线风险 |
|---|---|---|
| `code2Session` 真实换 openid | `AuthService.login` | 高，否则身份可伪造 |
| 历史持久化 MySQL/Redis | `HistoryService` | 高，重启数据丢 |
| Redis + Lua 分布式限流 | `RateLimitInterceptor` | 中，多实例下限流被绕过 |
| `adToken` 微信广告回调验签 | `FortuneService.unlock` | 高，否则被刷广告导致流量主封号 |
| 文案禁用词 CI 扫描 | 需新增脚本 | 高，违规即下架 |
| `/api/stats/today` 加 IP 白名单或 Basic Auth | `StatsController` | 中，运营数据外泄 |

---

## 五、合规红线（务必通读 `docs/01-PRD.md` 第 6 节）

- 文案禁用词：命运、注定、煞、化解、破财、血光、必然、一定会、改运 等。
- 所有结果页强制带 `tips` 免责声明。
- 不得宣称"准确率""灵验""必应"。
