# data-api 部署与运维（Nginx 前置、双实例）

本目录是 M13–M26 的落地物。拓扑：

```
浏览器 / 第三方 / OpenMetadata
          │  :8088 (HTTP) 或 :8443 (TLS)
        nginx  ── 限流(IP/Token) + CORS + 统一入口 + 故障转移
          │
   ┌──────┴──────┐
data-api:12346  data-api:12347      （cluster profile，magic 脚本存共享元库 magic_api_file）
   └──────┬──────┘
          │
   DS 元数据库（H2/MySQL/PG）  +  业务库（只读账号）
```

## 一、M13–M26 落地对照

| 编号 | 内容 | 落地位置 | 上线动作 |
|---|---|---|---|
| M13 | 脚本版本管理/快照/回滚/下线 | `da_api_release` + `/data-api/releases/**`；`deploy/magic-api-seed/` 纳入 git | 发布前 `POST /data-api/releases/snapshot` |
| M14 | magic 资源改共享存储 | `application-cluster.yaml`（`resource.type=database`）+ `magic_api_file` + 首次启动自动迁移 `./data/magic-api` | 两实例都用 `cluster` profile |
| M15 | CORS 白名单 | `cors-allowed-origins` + `WebCorsConfiguration`；magic `support-cross-domain=false`；nginx CORS | 配置允许的来源 |
| M16 | 业务库只读账号 | `deploy/sql/create-readonly-account-{mysql,doris}.sql` | 建账号后把 DS 源中心连接改为该账号 |
| M17 | 拒绝返回 4xx | 拦截器写 `HttpServletResponse.setStatus`（401/403/429/413/500），body 保留 magic code | 无需动作 |
| M18 | app_key / app_secret | `da_app_credential` + `/data-api/apps/**` + `X-App-Id`/`X-App-Secret` | 管理员创建应用凭证 |
| M19 | 限流与配额 | 进程内令牌桶（按 user/app）+ nginx `limit_req` | 调 `rate-limit-*`；多实例严格配额需 Redis |
| M20 | 响应治理 | `ResponseGovernor`：字段脱敏 + 响应体上限（默认 2MB，超限 413） | 配 `mask-fields`、`response-max-bytes` |
| M21 | 数据源探活告警 | `DatasourceHealthService` + `/data-api/datasources/health` + webhook | 配 `datasource-health-webhook` |
| M22 | SQL 取表 → 血缘 | `SqlTableExtractor` + `da_api_table_ref` + `/data-api/catalog/lineage` | OpenMetadata P1 读取该接口 |
| M23 | 控制台 token TTL | `console-token-ttl-minutes` / `console-token-max` | 无需动作 |
| M24 | 审计治理 | `/data-api/audit/stats`、`/data-api/audit/cleanup`、每日 03:30 清理 | 配 `audit-retention-days` |
| M25 | OpenAPI 导出 | `/data-api/openapi.json`（默认需 token） | 给对接方或 OM 导入 |
| M26 | 双实例 + Nginx | `deploy/nginx/data-api.conf`、`deploy/systemd/data-api@.service`、`deploy/windows/start-data-api.cmd` | 按下文部署 |

## 二、上线步骤

### 1. 元库与扩展表

执行 `src/main/resources/sql/da_tables_{mysql|postgresql|h2}.sql`。应用启动时也会补齐
`magic_api_file`、`da_api_release`、`da_api_table_ref`、`da_app_credential`、`da_api_invoke_log`
（`DataApiSchemaBootstrap`），但生产仍建议先跑 DDL 以便审阅。

### 2. 业务库只读账号

按 `deploy/sql/` 中模板创建 `ds_api_ro`，只授 `SELECT`；到 DS 源中心把对应数据源的账号改掉。
验证：用该账号执行一条 `update ... limit 0` 必须被数据库拒绝（`reject-dml` 只是应用层护栏）。

### 3. 启动两个实例（共享 magic 资源）

```bash
# Linux systemd
sudo systemctl enable --now data-api@12346 data-api@12347

# Windows（本机开发）
deploy\windows\start-data-api.cmd 12346
deploy\windows\start-data-api.cmd 12347
```

两实例必须使用 `--spring.profiles.active=<dbprofile>,cluster`。首个实例发现 `magic_api_file`
为空时，会把 `./data/magic-api` 导入数据库；也可把 `deploy/magic-api-seed/` 放到该目录后启动。
之后每 30s 两实例从共享库刷新一次（`magic-resource-refresh-ms`）。

> 注意：magic-api 官方集群通知插件未引入，当前靠"数据库存储 + 定时 refresh"收敛；
> 控制台同时编辑同一脚本时后写覆盖先写，正式环境应限制控制台访问来源（nginx 已限制内网）。

### 4. Nginx

```bash
sudo cp deploy/nginx/data-api.conf /etc/nginx/conf.d/data-api.conf
# 修改 upstream、server_name、cors_origin 白名单；需要 TLS 时打开 ssl 注释
sudo nginx -t && sudo systemctl reload nginx
```

网关暴露：`http://<host>:8088/data-api/**`（`/magic/` 仅内网）。nginx 层已做：
- `limit_req`：IP 100r/s、Token 50r/s（burst 见配置）；
- CORS 白名单回显；
- `proxy_next_upstream` 故障转移；`/data-api/health` 不记日志。

### 5. 应用凭证与授权

```bash
# 创建 app（secret 只返回一次）
curl -s -X POST -H "token: <ADMIN_TOKEN>" -H "Content-Type: application/json" \
  -d '{"appId":"cms","userId":100,"scopes":"/smoke/**","expireTime":"2027-12-31 23:59:59"}' \
  http://127.0.0.1:8088/data-api/apps

# 第三方调用
curl -H "X-App-Id: cms" -H "X-App-Secret: <SECRET>" \
  http://127.0.0.1:8088/data-api/gateway/smoke/select1
```

ACL 中主体类型 `APP` 的 `subject_id` 填 `appId`，与 USER/TOKEN 规则叠加，DENY 优先。

### 6. 发布、回滚、下线

```bash
# 发布后快照
curl -X POST -H "token: <ADMIN_TOKEN>" -H "Content-Type: application/json" \
  -d '{"remark":"2026-11 release"}' http://127.0.0.1:8088/data-api/releases/snapshot
# 历史
curl -H "token: <ADMIN_TOKEN>" "http://127.0.0.1:8088/data-api/releases?apiPath=/smoke/select1"
# 回滚到某个版本（会写一条 ROLLBACK 记录）
curl -X POST -H "token: <ADMIN_TOKEN>" http://127.0.0.1:8088/data-api/releases/12/rollback
# 下线
curl -X POST -H "token: <ADMIN_TOKEN>" http://127.0.0.1:8088/data-api/releases/12/offline
```

## 三、验证清单

| 项 | 命令 | 期望 |
|---|---|---|
| 健康 | `curl http://<host>:8088/data-api/health` | `status=UP`，不返回数据源 key |
| 401 | 无 token 调 gateway | HTTP 401 + body `code=401` |
| app 鉴权 | `X-App-Id`/`X-App-Secret` | HTTP 200 + `code=1` |
| 限流 | `for i in $(seq 1 500); do curl -s -o /dev/null -w "%{http_code}\n" ...; done` | 出现 429 |
| 脱敏 | 脚本返回含 `password` 字段 | 值为 `***` |
| 超限 | 查询大结果集 | HTTP 413 + 提示分页 |
| 探活 | 停业务库后等 1 个周期 | `/datasources/health` 显示 `ok=false`，webhook 收到 DOWN |
| 血缘 | `curl -H "token: ..." .../catalog/lineage` | 返回 `apiPath → tables` |
| OpenAPI | `curl -H "token: ..." .../openapi.json` | 合法 OpenAPI 3.0.1 |
| 审计 | `GET /data-api/audit/stats` | 调用数/错误数/平均延迟 |
| 双实例 | 停 12346 后继续调 8088 | 请求由 12347 承接 |

## 四、已知边界

- 限流与 ACL 缓存是**进程内**的；多实例下总配额是实例数倍。严格配额需 Redis（未引入）。
- 血缘是正则抽取，动态拼接 SQL 不产生边；宁缺勿错。
- `reject-dml`、`readOnly` 不是权限边界，必须配合只读账号（M16）。
- 控制台临时 token 存内存，重启失效；长期使用请走 DS Access Token。
- 血缘刷新是"按 API 全量替换"，多实例同时刷新会有短暂空窗；对账以最后一次为准。
- 数据源探活会占用调度线程（单源最长 10s），因此调度池已调到 4；数据源多时按需增大。
