# dolphinscheduler-data-api

DolphinScheduler 体系内的数据服务微服务（Phase 1）：基于 **magic-api** 将 SQL 发布为 HTTP API。

## 与 DS 的关系

| 共享 | 说明 |
|------|------|
| 元数据库 | 与 api-server 同一 `spring.datasource` |
| Token | Header `token`，校验 `t_ds_access_token` / `t_ds_user` |
| 源中心 | 读取 `t_ds_datasource`，Doris/MySQL 注册到 magic 动态数据源 |
| 插件 | 复用 `DorisDataSourceProcessor` 等 |

独立端口默认 **12346**，不参与 Master/Worker 调度。

## 启动前

1. 元库执行 DDL（任选其一）：
   - `src/main/resources/sql/da_tables_mysql.sql`
   - `src/main/resources/sql/da_tables_postgresql.sql`
2. 在 DS 源中心登记 **DORIS**（或 MYSQL）数据源。
3. 在 DS 安全中心为用户创建 **Access Token**。
4. 修改 `application.yaml`（或 `--spring.profiles.active=mysql`）指向真实元库。

## 构建与运行

```bash
cd dolphinscheduler
./mvnw -pl dolphinscheduler-data-api -am -DskipTests -Dspotless.skip=true package
java -jar dolphinscheduler-data-api/target/dolphinscheduler-data-api-3.4.3.jar \
  --spring.profiles.active=mysql
```

IDE 直接运行主类：`org.apache.dolphinscheduler.dataapi.DataApiServer`

## 常用地址

| 路径 | 说明 |
|------|------|
| `http://host:12346/magic/web` | magic-api 控制台（可用 DS 用户名密码或 token） |
| `http://host:12346/data-api/health` | 健康检查 + 已加载数据源数量（不再返回 key，避免未鉴权泄露） |
| `http://host:12346/data-api/datasources` | 需 Header `token` |
| `POST /data-api/datasources/reload` | 手动刷新源中心 |
| `GET /data-api/datasources/{key}/ping` | `SELECT 1` 探活（`key=default` 为元库） |
| `/data-api/gateway/**` | magic 发布的业务 API 前缀 |
| `GET /data-api/openapi.json` | OpenAPI 3.0 导出（默认需 token） |
| `GET /data-api/catalog/lineage` | 已发布 API → 表 的血缘（供 OpenMetadata） |
| `GET /data-api/audit/stats` | 调用量/错误数/延迟聚合 |
| `GET/POST /data-api/releases/**` | 发布快照、历史、回滚、下线 |
| `GET/POST /data-api/apps/**` | 外部应用凭证（app_key/secret） |
| `GET /data-api/datasources/health` | 数据源探活结果 |

数据源 key 规则：优先复用源中心 **名称**（清洗为合法标识符，如 `osint_mysql`）；重名/非法名时追加 `_{id}`；完全无法清洗时回退 `{dbType}_{id}`。  
控制台显示名：`{名称} [{TYPE}#{id} / {database}]`，便于与源中心对照。

```js
return db.osint_mysql.select('select * from demo limit 10');
```

调用业务 API 时请携带与 DS 相同的 Header：`token: <access-token>`。

## 配置项（`data-api.*`）

- `max-rows`：结果行上限（同时作为 magic 数据源 `maxRows` 生效）
- `load-db-types`：加载的源类型，默认 `DORIS,MYSQL`
- `reject-dml`：拦截写操作（见下）
- `datasource-refresh-ms`：定时从源中心刷新
- `query-timeout-seconds`：业务查询超时；通过 Hikari `connectionInitSql` 落到会话级
  （MYSQL `max_execution_time`、DORIS `query_timeout`、POSTGRESQL `statement_timeout`）
- `read-only-connections`：业务连接池 `readOnly`（对 MySQL 协议驱动是提示性约束，不是硬隔离）
- `session-init-sql.<TYPE>`：按类型覆盖会话初始化 SQL；空值表示关闭该类型默认值
- `acl-cache-ms`：ACL 规则缓存（默认 10s，0 关闭）
- `invoke-log-enabled` / `invoke-log-queue-size`：调用审计开关与异步队列容量
- `acl-mode`：`off` / `optional` / `enforce`
- `cors-allowed-origins`：浏览器来源白名单；空=仅同源（magic `support-cross-domain` 已关闭）
- `app-auth-enabled`：允许 `X-App-Id` / `X-App-Secret` 调用（凭证存 `da_app_credential`，只存 SHA-256）
- `rate-limit-permits-per-minute` / `rate-limit-burst`：按用户/应用令牌桶（进程内；nginx 另做一层 IP 限流）
- `response-max-bytes` / `mask-fields`：响应体上限（超限 413）与字段脱敏
- `datasource-health-enabled` / `datasource-health-interval-ms` / `datasource-health-webhook`：探活与告警
- `audit-retention-days`：审计保留天数（每日 03:30 清理）
- `console-token-ttl-minutes` / `console-token-max`：控制台临时 token 生命周期与容量
- `open-api-public` / `open-api-server-url`：OpenAPI 导出开关与 servers 地址
- `lineage-enabled`：是否解析脚本表名
- `magic-resource-refresh-ms`：cluster 模式下从共享库刷新脚本的间隔

## 调用审计（`da_api_invoke_log`）

每次 gateway 调用（含 **401 拒绝、403 拒绝、500 异常**）异步批量写入一行：

| 字段 | 说明 |
|------|------|
| `api_path` | 与 ACL 同口径的 gateway 相对路径，如 `/smoke/select1`（不是组内相对路径） |
| `user_id` | 解析到的 DS 用户 ID；未认证为空 |
| `latency_ms` | 从 preHandle 到结束的耗时 |
| `row_count` | 返回集合的元素数（可判定时） |
| `code` | 200 成功、401/403 拒绝、500 异常 |
| `trace_id` | magic-api `requestId`（32 位），便于与日志关联 |

审计写入失败不影响接口返回；队满时丢弃并计数（日志告警）。首次部署需先执行 `src/main/resources/sql/` 下对应 DDL。

## 只读保护说明

`reject-dml` 现在会先剥离注释与字符串字面量，再按语句首关键字拦截，同时识别 magic 的写方法
（`db.insert/update/delete/batchUpdate/call`、`db.table(...).insert(...)`）。因此
`select ... for update`、`like '%update%'` 不再误杀；写操作仍建议配合**只读数据库账号**做硬隔离——
正则与会话只读都只是纵深防御。

## 运行环境（本机实测）

- 构建：`JAVA_HOME` 可用 JDK 8+；**运行 H2 profile 需 JDK 11+**（H2 2.3.232 的 class 版本为 55）。
  本机用 `D:\HPG\workspace\LHYF\tools\jdk-11\jdk-11.0.2` 启动。
- 本模块未打进 `dolphinscheduler-dist`，也不由 DS standalone 脚本拉起，需单独 `java -jar` 启动。
- `magic-api` 资源为**本地文件存储**（`./data/magic-api`），多实例部署时各自一份，需改为共享存储或数据库存储。

## 集群与网关

- `--spring.profiles.active=<db>,cluster`：magic 脚本改存共享元库 `magic_api_file`，首实例自动导入
  `./data/magic-api`，各实例每 30s 刷新。
- 前置网关、双实例、systemd、只读账号脚本与验证清单见
  [`deploy/README.md`](deploy/README.md)（Nginx 反代 `:8088`，`/magic/` 仅内网）。
- 发布流程：`POST /data-api/releases/snapshot` → 变更脚本 → 需要时
  `POST /data-api/releases/{id}/rollback|offline`。

## 待实施文档

代码结构、部署位置、OpenMetadata 集成方案与后续优化项见
[`docs/DATA-API-OPENMETADATA-PLAN.md`](docs/DATA-API-OPENMETADATA-PLAN.md)。

## DS UI 集成

顶部菜单 **数据服务**：

| 菜单 | 路径 | 说明 |
|------|------|------|
| API 开发 | `/dataservice/studio` | 嵌入 magic-api（DS sessionId SSO） |
| Token 配置 | `/dataservice/token` | 复用安全中心 Access Token，并提示 gateway 用法 |
| 数据源状态 | `/dataservice/datasources` | 查看已加载 key / reload / ping |
| 接口授权 | `/dataservice/acl` | API 路径 × 用户/Token 的 ALLOW/DENY |

（UI 使用 `/dataservice`，避免与微服务 HTTP 前缀 `/data-api` 及 Vite 代理冲突。）

开发态 Vite 已代理 `/magic`、`/data-api` → `VITE_APP_DATA_API_URL`（默认 `http://127.0.0.1:12346`）。

鉴权：`token`（Access Token）或 `sessionId`（UI 登录态）或 magic 控制台 `magic-token`。

### 接口 ACL（`da_api_acl`）

- 配置项 `data-api.acl-mode`：`off` | `optional`（默认）| `enforce`
- 路径：相对 gateway，如 `/smoke/select1` 或 `/smoke/**`
- 主体：`USER`（用户 ID 或用户名）、`TOKEN`（Access Token 原文）、`APP`（预留）
- 管理员始终放行；DENY 优先于 ALLOW；脚本引用多个数据源时逐一对用户做数据源授权校验
- 建表：启动时按 MySQL / PostgreSQL / H2 方言自动创建（含唯一键），无需手工执行
- 规则按 `acl-cache-ms` 缓存，增删改后立即失效
- 管理 API：`GET/POST /data-api/acl`、`PUT /data-api/acl/{id}`（改 effect/remark）、
  `DELETE /data-api/acl/{id}`、`GET /data-api/acl/apis`
- `POST` 对同一 `(api_path, subject_type, subject_id)` 幂等更新，不产生重复行

## Phase 1 范围外

- OpenMetadata 目录绑定（表 `da_api_asset_bind` 已预留）
- 对外独立 app_key（表 `da_app_credential` 已预留）
- APISIX 网关
