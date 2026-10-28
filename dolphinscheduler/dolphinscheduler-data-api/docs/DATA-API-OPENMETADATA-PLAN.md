# 数据服务（DolphinScheduler + magic-api）现状与 OpenMetadata 集成待实施文档

| 项目 | 内容 |
|---|---|
| 文档名称 | 数据服务（magic-api）现状梳理与 OpenMetadata 集成待实施说明 |
| 版本 | V0.1 |
| 日期 | 2026-10-27 |
| 代码基线 | `data-center-mono`，分支 `chore/upgrade-ds-seatunnel`；DS 3.4.3 / SeaTunnel 3.0.0 / magic-api 2.2.2 |
| 涉及模块 | `dolphinscheduler/dolphinscheduler-data-api`、`dolphinscheduler-ui` 的 `data-api` 视图 |
| 编写目的 | 说清"现在是什么、跑在哪、怎么和 OpenMetadata 结合、还差什么"；magic-api 结合层的优化项本轮直接落地，OpenMetadata 相关按分期实施 |
| 证据口径 | 代码为当前工作区实测；运行数据来自本机 H2 元库与 2026-10-26 服务日志；未部署 OpenMetadata，相关内容为方案设计，接口路径以目标版本 OpenAPI 为准 |

---

## 零、结论摘要

1. **当前形态**：数据服务是一个**独立 Spring Boot 进程**（默认 `:12346`），复用 DolphinScheduler 元数据库、Access Token 与数据源中心，把 magic-api 的 SQL 脚本发布为 `/data-api/gateway/**` HTTP 接口；DS UI 以 `/dataservice/*` 四个菜单嵌入。它**不在** `dolphinscheduler-dist` 打包链路里，也不由 DS standalone 脚本启动。
2. **magic-api 结合已跑通**：动态数据源加载、Token/SSO 鉴权、路径级 ACL、SQL 转 API、调用审计都已有实现；本机日志显示已加载 `da_test_mysql`、`da_test_doris`、`osint_mysql` 三个数据源。
3. **本轮已修复 12 个结合层问题**（详见第五章），其中影响最大的是：审计日志原先**只记成功且路径缺组前缀**、`query-timeout-seconds` **只注入变量不生效**、ACL 建表 DDL **只兼容 MySQL**、DML 拦截**字符串误杀/写方法漏判**。修复后 19 个单测通过，并用 H2 实库完成 401/403/200 全链路验证。
4. **OpenMetadata 的定位**：把它作为**表级元数据与治理标签的权威源**，数据服务把已发布的 API 作为 `apiEndpoint` 资产登记进去，并建立 `table → apiEndpoint` 血缘；`da_api_asset_bind` 承担"API ↔ 表资产"绑定。**不建议**让 OM 直接管理 magic 脚本。
5. **建议优先级**：先做 P0（OM 单机 + 表目录只读挂接 + API 资产登记），再做 P1（血缘自动解析、标签反向驱动 ACL/脱敏），最后 P2（多实例、app_key、限流网关）。资源是硬约束：OM 需要独立 PostgreSQL 与 Elasticsearch/OpenSearch，现场"4 业务 + 1 NFS"的机器规格需先确认落点。

---

## 零之二、M13–M26 实施结果（2026-10-28）

Nginx 作为统一前置网关；双实例共享 magic 脚本库。全部条目已落地，实测结果如下。

| 编号 | 实现 | 实测/边界 |
|---|---|---|
| M13 | `da_api_release` + `/data-api/releases/**`（snapshot/rollback/offline）；`deploy/magic-api-seed/` 纳入仓库 | 快照 5 个 API；回滚生成新版本；下线后脚本文件被删除 |
| M14 | `cluster` profile：`magic_api_file` 共享存储；首启自动导入文件（含目录标记、去 BOM）；30s 刷新 | 实库迁移 4 文件 + 目录标记，重启后 `/smoke/select1` 返回 `code=1` |
| M15 | `cors-allowed-origins` + `WebCorsConfiguration`；magic `support-cross-domain=false`；nginx CORS | 预检返回 `Access-Control-Allow-Origin: http://localhost:3000` |
| M16 | 只读账号模板 `deploy/sql/*`；连接池 `readOnly` | 账号需在目标库执行（本机无业务库，未实测写拒绝） |
| M17 | 拒绝写 `HttpServletResponse.setStatus`，body 保留 magic code | 无 token 返回 HTTP 401；限流 429；UI axios 已改为读取响应体 |
| M18 | `da_app_credential` + `/data-api/apps/**` + `X-App-Id/X-App-Secret` + APP ACL 主体 | 创建返回 id 与一次性 secret；错误 secret 401；正确凭证 200 |
| M19 | 进程令牌桶 + nginx `limit_req` | `permits=1,bust=1` 时第二次调用 429 |
| M20 | 字段脱敏 + 响应体上限 | `password/phone` 返回 `***`；超限返回 413 |
| M21 | 探活 + `/datasources/health` + webhook | 三个不可达数据源均报 `ok=false`、10s 超时；webhook 需外部地址 |
| M22 | `SqlTableExtractor` + `da_api_table_ref` + `/catalog/lineage` | 抽取 `dwd_orders`、`dim_customer`；系统库表被忽略 |
| M23 | 控制台 token TTL + 容量上限 | 代码生效；未做长时等待验证 |
| M24 | `/audit/stats`、`/audit/cleanup`、每日 03:30 清理 | 统计返回调用数/错误数/平均与最大延迟 |
| M25 | `/data-api/openapi.json` | 输出 OpenAPI 3.0.1，包含网关全路径 |
| M26 | nginx 8088 + 双实例 12346/12347 + systemd/Windows 启动脚本 | `nginx -t` 通过；停掉 12346 后经 8088 连续 5 次仍 200 |

部署与验证步骤见 `deploy/README.md`。仍**未落地**的是 OpenMetadata 本体（P0 起）与 Redis 版严格配额。

## 一、当前代码与运行方式

### 1.1 代码地图

| 关注点 | 位置 | 说明 |
|---|---|---|
| 进程入口 | `src/main/java/.../DataApiServer.java` | `@SpringBootApplication` + `@EnableScheduling`，导入 DS 的 `DaoConfiguration` / `CommonConfiguration`，就绪后加载数据源插件 |
| 配置 | `src/main/resources/application.yaml` | 端口 12346；`magic-api.prefix=/data-api/gateway`；`resource.type=file`；三个 profile（mysql / postgresql / h2） |
| 业务配置类 | `config/DataApiProperties.java` | `data-api.*` 全部开关 |
| 动态数据源 | `datasource/DsMagicDataSourceLoader.java` | 从 `t_ds_datasource` 读取 DORIS/MYSQL，键名优先用源名称（清洗后），注册进 `MagicDynamicDataSource`；60s 定时刷新 |
| 默认数据源 | `config/MagicDataSourceConfiguration.java` | 把元库 `DataSource` 作为 magic 的 `default` 节点（smoke 脚本用） |
| 控制台鉴权 | `auth/DsTokenAuthorizationInterceptor.java` | magic 控制台登录：DS 用户名密码 / Access Token / `sessionId`；用户名密码登录发**内存态**临时 token |
| 调用鉴权 | `auth/DataApiRequestInterceptor.java` | 每次 gateway 调用的 Token 校验、DML 拦截、路径 ACL、数据源授权、审计 |
| ACL | `acl/DataApiAclService.java` / `controller/DataApiAclController.java` | `da_api_acl` 的 CRUD 与求值；按路径通配 + 主体（USER/TOKEN/APP）判定 |
| API 目录 | `service/DataApiCatalogService.java` | 列出已发布 magic API，供 ACL 绑定 UI 使用 |
| 审计 | `service/DataApiInvokeLogService.java` | 写 `da_api_invoke_log` |
| 管理接口 | `controller/DataApiAdminController.java` | `/data-api/health`、`/datasources`、`/datasources/reload`、`/datasources/{key}/ping` |
| 扩展表 DDL | `src/main/resources/sql/da_tables_{mysql,postgresql,h2}.sql` | `da_api_asset_bind`、`da_api_invoke_log`、`da_app_credential`、`da_api_acl` |
| DS UI | `dolphinscheduler-ui/src/views/data-api/{studio,token,datasources,acl}` | `/dataservice/*` 路由；studio 用 iframe + `MAGIC_EDITOR_CONFIG` 做 SSO |
| UI 服务层 | `dolphinscheduler-ui/src/service/modules/data-api/index.ts` | baseURL 取 `VITE_APP_DATA_API_BASE || '/data-api'` |
| magic 脚本 | `./data/magic-api/api/smoke/*.ms` | 文件存储；`smoke/select1` 走默认元库，`osint_tables` 走 `db.osint_mysql` |

### 1.2 运行方式（实测）

```bash
# 构建（本机离线仓库可用）
cd data-center-mono/dolphinscheduler
JAVA_HOME=<jdk> ./mvnw -o -pl dolphinscheduler-data-api -am \
  -DskipTests -Djacoco.skip=true -Dspotless.skip=true package

# 运行（H2 元库；H2 2.3.232 需 JDK 11+）
java -jar dolphinscheduler-data-api/target/dolphinscheduler-data-api-3.4.3.jar \
  --spring.profiles.active=h2 --server.port=12346
```

- **共享元库**：与 api-server 同一 `spring.datasource`；Token 校验 `t_ds_access_token` / `t_ds_user`，UI 登录态用 `t_ds_session`。
- **数据源**：只加载 `data-api.load-db-types` 指定的类型；源中心改了连接参数后按指纹重建 Hikari 池。
- **magic 控制台**：`http://host:12346/magic/web`；业务接口前缀 `/data-api/gateway`。
- **DS UI**：开发态 Vite 代理 `/magic`、`/data-api` 到 `VITE_APP_DATA_API_URL`（默认 `http://127.0.0.1:12346`）；`/dataservice` 是 UI 路由前缀，避免与 HTTP 前缀冲突。
- **重要前置**：首次部署要执行 `src/main/resources/sql/da_tables_*.sql`；`da_api_acl` 现在可自动建表，但审计表与资产绑定表仍依赖 DDL。

### 1.3 部署位置（本机）

| 角色 | 路径 / 端口 | 版本 / 备注 |
|---|---|---|
| 源码 | `D:\HPG\workspace\LHYF\data-center-mono\dolphinscheduler\dolphinscheduler-data-api` | **git 未跟踪**（`git status` 显示 `?? dolphinscheduler-data-api/`），尚未纳入版本管理 |
| 运行 jar | 同目录 `target/dolphinscheduler-data-api-3.4.3.jar` | Spring Boot 可执行 jar |
| 元数据库 | `D:\HPG\workspace\LHYF\data\dsdb`（H2，`MODE=MySQL;AUTO_SERVER=TRUE`） | DS standalone 与 data-api 共用；本机迁移自 `D:\ds\data` |
| DS 运行目录 | `D:\HPG\workspace\LHYF\apache-dolphinscheduler-3.4.3-bin` | 由 `start-standalone-windows.cmd` 启动；该脚本 `JAVA_HOME=D:\HPG\workspace\LHYF\tools\jdk-11\jdk-11.0.2` |
| 日志 | `dolphinscheduler-data-api/logs/dolphinscheduler-data-api*.log` | 最近一次运行记录到 2026-10-26 08:05，之后进程停止（当前 `:12346` 未监听） |
| 打包链路 | `dolphinscheduler/pom.xml` 含该 module；`dolphinscheduler-dist` **不含** | 无 Dockerfile / K8s manifest / systemd 单元；无开机自启 |

**部署结论**：目前是"开发机上的手工单实例"。生产化至少缺四件事：① 纳入 git 与 CI；② 独立启动脚本/服务单元与进程守护；③ magic 资源文件本地存储会与多实例冲突；④ 生产元库（PostgreSQL/MySQL）与 JDBC 驱动、DDL 的配套验证。

### 1.4 当前运行证据

- 启动日志：`Registered magic datasource key=da_test_mysql ... initSql=SET SESSION max_execution_time = 60000`；`key=da_test_doris ... initSql=SET query_timeout = 60`；`key=osint_mysql ...`。
- 健康检查：`{"loadedDatasourceCount":3,"service":"dolphinscheduler-data-api","status":"UP"}`。
- 网关调用：`GET /data-api/gateway/smoke/select1`（admin token）返回 `{"code":1,...,"data":[{"n":1}]}`；`Authorization: Bearer <token>` 同样通过。
- 审计：`da_api_invoke_log` 实测出现 `200 / 401 / 403` 三类记录，`row_count=1`、`trace_id` 32 位。
- 历史故障线索：`data/magic-api/api/smoke/osint_tables.ms` 保存的响应为 `code=-1, executeTime=10010`，与 Hikari `connectionTimeout=10000` 吻合，说明该数据源当时**连不上**（网络/账号/库名），不是脚本语法问题。此类"接口失败但无告警"的问题应纳入 P1 探活。

---

## 二、magic-api 结合层问题清单（现状复盘）

分三档：**已修复（本轮）**、**P0/P1/P2 待实施**。"影响"一列只写可验证的后果。

### 2.1 本轮已修复

| 编号 | 问题（修复前） | 影响 | 修复方式 |
|---|---|---|---|
| M-01 | `postHandle` 固定写 `code=200`，401/403/异常不落库 | 审计无法回答"谁被拒了、拒了多少" | 改用 `afterCompletion`，拒绝时记录 401/403，异常记 500 |
| M-02 | 审计 `api_path` 用 `ApiInfo.getPath()`（组内相对，如 `/select1`） | 与 ACL 的 `/smoke/select1` 对不上，无法按路径统计 | 改为从 requestURI 提取 gateway 相对路径（实测已变为 `/smoke/select1`） |
| M-03 | `query-timeout-seconds` 只注入脚本变量 `daQueryTimeoutSeconds` | 脚本不主动用就完全不生效 | 按方言写入 Hikari `connectionInitSql`：MySQL `max_execution_time`(ms)、Doris `query_timeout`(s)、PG `statement_timeout`(ms) |
| M-04 | 业务池无只读标记、退出不关池 | 误写风险；热部署/重启连接泄漏 | `readOnly=true`（可关）+ `@PreDestroy` 统一关闭 |
| M-05 | DML 拦截用"前后空格关键字"匹配 | `like '%update%'`、`select ... for update` 被误杀；`db.update(...)` 漏判 | 先剥离注释/字符串（含三引号），再按语句首关键字判定，并识别 magic 写方法/`table().insert()` |
| M-06 | 只提取脚本里**第一个** `db.xxx` | 跨库 join 时只校验一个数据源权限 | 提取全部 key 逐个校验 DS 数据源授权 |
| M-07 | 只认 `token` / `magic-token` / `sessionId` | 标准客户端用 `Authorization: Bearer` 调不通 | 增加 Bearer 解析 |
| M-08 | 每次调用都 `SELECT` 全量 ACL | 高 QPS 下元库成为瓶颈 | 规则缓存（默认 10s）+ 增删改即时失效 |
| M-09 | `ensureTable` 用 MySQL 语法，PG 上 `AUTO_INCREMENT` 报错被吞 | PG 环境 ACL 表可能根本建不出来 | 按 `DatabaseMetaData` 生成 MySQL/PG/H2 方言 DDL，并补唯一索引 |
| M-10 | ACL 只能新增/删除，重复提交产生多行 | 同一主体多条规则，求值结果不确定 | `POST` 幂等 upsert；新增 `PUT /data-api/acl/{id}` 改 effect/remark |
| M-11 | `/data-api/health` 未鉴权返回已加载数据源 key | 内部库名/命名泄露 | 只返回数量，明细走需鉴权的 `/datasources` |
| M-12 | JDBC URL 原样写日志并返回给任意登录用户 | URL 若带 `user/password` 参数会泄露 | 统一 `password/pwd/user/username=***` 脱敏 |

### 2.2 待实施（按优先级）

> 说明：下表 M13–M26 已于 2026-10-28 实施，实现与实测见"零之二"；本节保留原始问题描述作为设计依据。

| 编号 | 优先级 | 待实施项 | 现状 / 影响 | 验收标准 |
|---|---|---|---|---|
| M-13 | P0 | **magic 脚本纳入版本管理**：`data/magic-api` 目前是本地文件且整模块未入 git | 换机器/多人协作即丢失；无发布审批与回滚 | 脚本入 git 或改 DB 存储；有草稿/发布/下线/回滚与操作人记录 |
| M-14 | P0 | **magic 资源存储改共享**（DB 或对象存储） | 多实例各存一份，控制台看到的不一致 | 两实例发布同一脚本，任一侧立即可见 |
| M-15 | P0 | **CORS 收敛**：`support-cross-domain: true` | 任意站点可带 token 调网关（配合浏览器场景有 CSRF/盗用风险） | 改为白名单域名；网关侧加 Origin 校验 |
| M-16 | P0 | **凭据硬隔离**：业务连接使用只读 DB 账号 | `reject-dml` 与 `readOnly` 都只是防御，不是权限边界 | 用只读账号启动，人工用 `db.update` 验证被数据库拒绝 |
| M-17 | P1 | **统一错误码**：目前 HTTP 恒 200，业务码在 body（401/403） | 监控/网关无法按 HTTP 状态统计；第三方接入易误判成功 | 拒绝类返回对应 HTTP 4xx，body 保留 magic code；同步更新 UI 与文档 |
| M-18 | P1 | **app_key / app_secret**（`da_app_credential` 已建表未接线） | `APP` 主体在 ACL 中恒不匹配，外部应用只能借个人 Token | 支持 `app_id + 签名/HMAC`；可设 scope、过期时间、吊销 |
| M-19 | P1 | **限流与配额**（单 API / 单主体） | 单个慢查询可拖垮共用的 5 连接池 | 令牌桶/滑动窗口；超限 429；有 QPS/并发指标 |
| M-20 | P1 | **响应治理**：字段级脱敏、最大返回体、分页规范 | `max-rows` 只限行数，不限字段与字节；敏感字段靠脚本自觉 | 可配字段黑/白名单；超限截断并标注 |
| M-21 | P1 | **数据源探活与告警** | `osint_mysql` 曾 10s 超时失败无告警 | 定时 `SELECT 1`；连续失败告警；`/datasources` 展示最近探活结果 |
| M-22 | P1 | **SQL 解析取表 → 血缘** | 现在无法知道 API 读了哪些表，OM 集成的关键前置 | 解析 magic 脚本中的表名（含 `db.table`/CTE），产出 `api_path → table` 映射，供 OM 血缘同步 |
| M-23 | P2 | **控制台临时 token 过期**：`sessionTokens` 无 TTL、不持久化 | 重启即全失效；长期运行内存增长 | 加 TTL 与容量上限，或直接复用 DS session |
| M-24 | P2 | **调用日志治理**：无归档/分区/统计接口 | `da_api_invoke_log` 会无限增长 | 按月分区或归档；提供按 API/主体/时间的聚合查询 |
| M-25 | P2 | **OpenAPI 导出** | 第三方对接只能看 magic 控制台或文档 | `GET /data-api/openapi.json` 输出已发布 API 的 OpenAPI 3.0 |
| M-26 | P2 | **高可用与网关** | 单实例、无 TLS、无统一入口 | 多实例 + 前置网关（APISIX/Nginx）+ 健康检查摘流 |

---

## 三、与 OpenMetadata 集成方案

### 3.1 定位与边界

- **OM 负责**：数据库/表/Schema 元数据、Owner、Domain、Tag（含 PII）、表级血缘、搜索与数据字典。
- **数据服务负责**：SQL 脚本的编写与发布、运行时鉴权/ACL/审计、把 API 暴露为 HTTP。
- **不交给 OM**：magic 脚本正文、Token、连接凭据。OM 只保存"API 资产"的**描述与指向**。
- **收益**：① 发布 API 时不必手工登记读了哪些表；② 调用方能从 OM 搜到"这个 API 对应哪些表、谁负责、是否含敏感字段"；③ 表结构变更可反查受影响 API；④ 与二期清单 C-8（血缘自动化）、C-12（台账自动化）、D-1（供数留痕）对齐。

### 3.2 部署形态与资源

OM 官方部署由三部分组成：**OpenMetadata Server（默认 8585）**、**PostgreSQL（OM 自有元库）**、**Elasticsearch/OpenSearch（搜索与血缘索引）**。可采用 Docker Compose（单机）或 K8s（生产）。

| 方案 | 适用 | 资源（规划值，需实测） | 说明 |
|---|---|---|---|
| A. 单机 Docker Compose | 演示/联调 | 4C8G，磁盘 50G+ | ES 最吃内存；与 DS、data-api 同机需评估 |
| B. 独立服务器 | 生产 | 8C16G 起，ES 单独数据盘 | 推荐；与业务库网络打通、只读账号采集 |
| C. 复用现有 PG | 节省机器 | 仅省一个实例 | **必须独立 database/schema**，避免与 DS 元库耦合；ES 仍不可省 |

现场已知资源紧张（二期清单 B-2：5 台虚拟机缩为 4 业务 + 1 NFS），**部署前必须先确认落点与内存预算**；若短期无法给出 ES 资源，可先做"离线导出 + OM 后补"的 P0 降级，但不能宣称"已集成"。

### 3.3 元模型映射

| 本系统对象 | OpenMetadata 实体 | 建议 FQN / 字段 |
|---|---|---|
| DS 数据源（MySQL/Doris） | `databaseService` | 服务名如 `ds_mysql_osint`、`ds_doris_demo`；OM 官方已有 Doris 连接器（v2.0.x 文档），版本需实测 |
| 源库 / Schema | `database` / `databaseSchema` | 随采集自动生成 |
| 物理表 | `table` | 形如 `<service>.<db>.<schema>.<table>`；以 OM 采集结果为准 |
| magic 分组 `smoke` | `apiCollection` | `data_api_gateway.smoke` |
| 已发布 API `/smoke/select1` | `apiEndpoint` | `data_api_gateway.smoke.select1`；字段含 `endpointURL`（gateway 全路径）、`httpMethod`、`description`、请求/响应 Schema |
| API 读取的表 | `lineage` 边 | 方向：`table`（上游）→ `apiEndpoint`（下游） |
| 数据源 key ↔ DS 源 ID | `da_api_asset_bind.datasource_id` | 复用现有表 |
| API ↔ 表资产 | `da_api_asset_bind.asset_urn` | 存 OM 的 table FQN（现有字段注释写的是 `doris://...`，建议统一为 OM FQN） |
| Owner / 敏感标签 | `owner` / `tag`（PII） | 反向拉取，驱动 ACL 与脱敏（P1） |

> REST 路径按 OM 惯例为 `/api/v1/apiServices`、`/api/v1/apiCollections`、`/api/v1/apiEndpoints`、`/api/v1/lineage`；**实际以目标版本 OpenAPI 文档为准**，接入前用 `GET /api/v1/system/version` 与 Swagger 核对。

### 3.4 数据流与同步策略

```
                ① 周期采集（OM Ingestion）
 MySQL/Doris ───────────────────────────────▶ OM: databaseService/Database/Schema/Table
                                                     ▲
                                                     │ ④ 反向拉取 Tag/Owner/PII
                                                     │
 ② 发布/对账：API 资产                         ③ 血缘：table → apiEndpoint
 data-api ──▶ APIService/APICollection/APIEndpoint ──▶ OM
     │                                                   │
     └── da_api_asset_bind（api_path ↔ table FQN）◀───────┘
```

- **① 表元数据采集**：用 OM 官方 MySQL/Doris 连接器，只读账号，按小时/天调度；先小范围（osint 库）验证。
- **② API 资产登记**：数据服务在"发布/下线/定时对账"三个时机推送；以确定性 FQN 做幂等 upsert，下线时置 `status` 或删除 Endpoint。
- **③ 血缘**：依赖 M-22 的 SQL 表名解析；解析不确定时**不写边**，宁缺勿错，并在绑定表记录 `status=unresolved`。
- **④ 反向治理**：定时拉取 `PII` 标签与 Owner；PII 表默认收紧 ACL（需显式 ALLOW），Owner 用于审批流（后续）。
- **一致性**：推送失败重试 + 对账表；以 `da_api_asset_bind.version` 递增，冲突以"数据服务为准、OM 只读覆盖"。

建议新增对账表（待实施，DDL 草案）：

```sql
CREATE TABLE da_api_om_sync (
  id            BIGINT PRIMARY KEY AUTO_INCREMENT,
  api_path      VARCHAR(255) NOT NULL,
  endpoint_fqn  VARCHAR(512) NOT NULL,
  checksum      VARCHAR(64)  NULL,      -- 描述/脚本/表清单的摘要，变化才推送
  sync_status   TINYINT      NOT NULL DEFAULT 0, -- 0待同步 1成功 2失败 3已下线
  last_sync_time TIMESTAMP   NULL,
  error_msg     VARCHAR(512) NULL,
  UNIQUE KEY uk_da_om_api (api_path)
);
```

### 3.5 鉴权与安全

- OM 侧使用 **Bot（JWT）**，权限最小化：仅 API 资产写入 + 表读取；
- 数据服务到 OM 走内网 HTTPS，凭据放环境变量/密钥管理，**不入 git、不进前端**（与二期清单 E-1、F-1 的整改口径一致）；
- OM 控制台按角色开放，不向数据服务调用方暴露；
- 推送内容只含路径、方法、描述、Schema、表 FQN，**不含 SQL 正文与连接串**（避免元数据侧二次泄露）。

### 3.6 分期计划

| 阶段 | 范围 | 交付物 | 验收 |
|---|---|---|---|
| **P0**（1–2 周） | OM 单机部署；采集 1 个 MySQL + 1 个 Doris；`da_api_asset_bind` 落表；发布页可搜索/绑定表资产；API 资产手工/半自动登记 | OM 可用地址与账号；采集报告；绑定页/接口 | OM 能搜到表；绑定记录可在 OM 找到对应 `apiEndpoint`；失败有明确报错 |
| **P1**（2–4 周） | SQL 表名解析（M-22）；`table → apiEndpoint` 血缘自动推送；PII 标签反向拉取并影响 ACL；对账表与重试 | 解析服务、同步任务、对账报表 | 随机抽 10 个 API，血缘准确率 ≥90%，错边为 0；PII 表默认拒绝可验证 |
| **P2**（4 周+） | API 生命周期与版本、OpenAPI 导出、调用统计回流 OM（使用热度）、多实例与网关 | 生命周期接口、统计看板 | 下线 API 在 OM 同步失效；统计可按 API/主体查询 |

### 3.7 风险与待确认

| # | 风险/待确认 | 影响 | 建议 |
|---|---|---|---|
| 1 | 现场无 OM 部署、无 ES 资源 | 方案无法落地 | 先确认机器与内存预算；或采用外部已有 OM |
| 2 | OM 版本与 Doris 连接器兼容性 | 采不到 Doris 表 | 锁版本 + 先做连通性验证；必要时用 MySQL 协议采集 |
| 3 | magic 脚本里的表名可能是动态拼接/变量 | 血缘解析错误 | 解析不确定不写边；人工确认入口 |
| 4 | API 与表是多对多（一个 API 读多表、多 API 读同表） | 血缘膨胀 | 以"API 直接引用表"为准，不做字段级血缘（P2 再评估） |
| 5 | 元数据双写不一致 | 搜索/血缘过期 | 对账表 + 定时重建；OM 侧不手工改 API 资产 |
| 6 | 凭据与数据出境合规 | 审计风险 | OM 内网、只读账号、脱敏后的样本数据 |

---

## 四、待实施任务清单（可直接排期）

| # | 任务 | 关联 | 优先级 | 预估 | 验收 |
|---|---|---|---|---|---|
| 1 | data-api 模块入 git，补 CI（编译 + 单测 + spotless） | M-13 | P0 | 0.5d | 主干可见、流水线绿 |
| 2 | 补启动脚本/服务单元，纳入 DS 启停顺序 | 部署 | P0 | 0.5d | 重启机器后自动拉起 |
| 3 | CORS 白名单化 | M-15 | P0 | 0.5d | 非白名单 Origin 预检失败 |
| 4 | 业务连接改只读账号并验证写被拒 | M-16 | P0 | 1d | `db.update` 返回数据库权限错误 |
| 5 | OM 单机部署 + 1 库采集 | OM P0 | P0 | 2d | OM 搜到表 |
| 6 | 资产绑定接口与页面（读写 `da_api_asset_bind`） | OM P0 | P0 | 2d | API 页面可绑定/解绑 |
| 7 | API 资产登记任务（apiEndpoint upsert） | OM P0 | P0 | 4d | OM 可见 endpoint |
| 8 | 错误码 HTTP 化（4xx） | M-17 | P1 | 1d | 401/403 返回对应 HTTP 状态 |
| 9 | app_key/app_secret + 签名校验 | M-18 | P1 | 3d | 第三方用 app_key 调通并可吊销 |
| 10 | 限流与配额 | M-19 | P1 | 2d | 超限 429，指标可见 |
| 11 | 数据源探活告警 | M-21 | P1 | 1d | 断网后 1 个周期内告警 |
| 12 | SQL 表名解析 + 血缘推送 | M-22/OM P1 | P1 | 5d | 抽样准确率 ≥90% |
| 13 | PII 标签反向驱动 ACL | OM P1 | P1 | 3d | PII 表默认拒绝 |
| 14 | OpenAPI 导出 | M-25 | P2 | 2d | swagger 可导入第三方工具 |
| 15 | 调用日志归档与统计 | M-24 | P2 | 2d | 可按 API/时间聚合 |
| 16 | magic 资源改 DB 存储 + 多实例验证 | M-14 | P2 | 5d | 双实例一致 |
| 17 | 控制台临时 token TTL | M-23 | P2 | 0.5d | 过期后自动失效 |

---

## 五、本轮已直接改动（magic-api 结合层）

### 5.1 改动文件

| 文件 | 改动 |
|---|---|
| `config/DataApiProperties.java` | 新增 `aclCacheMs`、`invokeLogEnabled`、`invokeLogQueueSize`、`readOnlyConnections`、`sessionInitSql` |
| `auth/DataApiRequestInterceptor.java` | Bearer 支持；多数据源授权；DML 守卫重写；`afterCompletion` 审计（含拒绝/异常/行数/traceId）；审计路径改 URI |
| `service/DataApiInvokeLogService.java` | 异步有界队列 + 批量写入 + 丢弃计数 + 优雅停止 |
| `datasource/DsMagicDataSourceLoader.java` | 方言会话超时 SQL；`readOnly`；`@PreDestroy` 关池；JDBC URL 脱敏 |
| `acl/DataApiAclService.java` | 方言建表；规则缓存；`create` 幂等 upsert；新增 `update`；缓存失效 |
| `controller/DataApiAclController.java` | 新增 `PUT /data-api/acl/{id}` |
| `controller/DataApiAdminController.java` | `/health` 不再返回数据源 key |
| `application.yaml` | 新配置项与注释 |
| `README.md` | 审计、超时、ACL、运行环境说明 |
| 测试 | `DataApiRequestInterceptorTest` 扩到 5 个用例；新增 `DataApiAclDdlTest`、`DsMagicDataSourceLoaderPoolTest` |

### 5.2 行为对照

| 场景 | 改动前 | 改动后（实测） |
|---|---|---|
| 无 token 调 API | 401，**无审计** | 401，审计 `code=401`、`user_id=null` |
| 用户被 ACL DENY | 403，**无审计** | 403，审计 `code=403`、`user_id=100` |
| 正常调用 | 审计 `code=200`、路径 `/select1` | 审计 `code=200`、`row_count=1`、路径 `/smoke/select1`、32 位 trace |
| ACL 重复提交 | 新增第二行 | 同一 `id=7` 更新为 DENY，行数不变 |
| ACL 修改 | 只能删了重加 | `PUT` 直接改 effect/remark，立即生效 |
| 会话超时 | 无效 | 启动日志可见 MySQL/Doris 各自的 init SQL |
| `select ... for update` | 被误判为写操作 | 放行 |
| `db.update(...)` 脚本 | 可能漏判 | 拦截 |

### 5.3 兼容性与回滚

- 配置项均有默认值，**老配置文件可直接启动**；`session-init-sql` 默认启用，若某数据库不认对应会话变量，把该类型置空即可。
- 审计表结构**未变**，无需迁移；`code` 语义从"恒 200"变为"200/401/403/500"，已有报表口径需同步（目前无下游）。
- `POST /acl` 从"纯新增"变为"幂等 upsert"，对原 UI 兼容（返回结构不变）。
- `/health` 字段 `loadedDatasources` 改为 `loadedDatasourceCount`，**这是一处不兼容变更**；仓库内无调用方，外部若有监控需一并改。
- 回滚：仅回退 `dolphinscheduler-data-api` 源码与 jar；无数据库结构变更，回滚无需 DDL。

---

## 六、验证方法

### 6.1 构建与单测

```bash
cd data-center-mono/dolphinscheduler
JAVA_HOME=D:/HPG/workspace/LHYF/tools/jdk-11/jdk-11.0.2 \
  ./mvnw -o -pl dolphinscheduler-data-api test -Dspotless.skip=true -Djacoco.skip=true
# 期望：Tests run: 19, Failures: 0, Errors: 0
```

> 注意：`mvn test` 的 JaCoCo instrument 会改写 `target/classes`，之后再 `package` 会报
> `Cannot process instrumented class`。打包前先删 `target/classes` 与旧 jar，或全程加 `-Djacoco.skip=true`；离线仓库缺少 clean 插件，无法用 `mvn clean`。

### 6.2 启动与接口

```bash
java -jar dolphinscheduler-data-api/target/dolphinscheduler-data-api-3.4.3.jar \
  --spring.profiles.active=h2
curl -s http://127.0.0.1:12346/data-api/health
curl -s -H "token: <ACCESS_TOKEN>" http://127.0.0.1:12346/data-api/gateway/smoke/select1
curl -s -H "Authorization: Bearer <ACCESS_TOKEN>" ...同上
```

### 6.3 审计与 ACL

```sql
-- H2/MySQL/PG 通用
SELECT id, api_path, user_id, latency_ms, row_count, code, trace_id
FROM da_api_invoke_log ORDER BY id DESC LIMIT 10;
```

| 操作 | 期望 |
|---|---|
| 不带 token 调用 | body `code=401`；审计一行 `code=401` |
| 添加 DENY 规则后调用 | body `code=403`；审计 `code=403` |
| 用 `PUT` 改 ALLOW 后调用 | body `code=1`；审计 `code=200, row_count=1` |
| 重复 `POST` 同一规则 | 返回同一 `id`，表中仍一行 |
| 日志中查看数据源注册 | 出现 `initSql=SET SESSION max_execution_time = ...` / `SET query_timeout = ...` |

### 6.4 OpenMetadata 联调（P0 后）

1. `GET http://<om>:8585/api/v1/system/version` 确认版本与连通；
2. 用 Bot Token 建 `apiService` → `apiCollection` → `apiEndpoint`，再 `GET` 回读确认 FQN；
3. 用 `PUT /api/v1/lineage` 建 `table → apiEndpoint` 边，在 OM UI 查看血缘图；
4. 停掉一个数据源，验证探活告警与审计中的失败率（P1）。

---

## 七、附录

### 7.1 配置项全表

| 配置 | 默认 | 说明 |
|---|---|---|
| `server.port` | 12346 | 服务端口 |
| `spring.profiles.active` | postgresql | 元库方言；本机用 h2 |
| `magic-api.prefix` | `/data-api/gateway` | 业务 API 前缀 |
| `magic-api.resource.type/location` | file / `./data/magic-api` | 脚本存储（多实例需改） |
| `magic-api.support-cross-domain` | true | **建议收敛** |
| `data-api.max-rows` | 1000 | JdbcTemplate maxRows |
| `data-api.datasource-refresh-ms` | 60000 | 源中心刷新 |
| `data-api.load-db-types` | DORIS,MYSQL | 加载类型 |
| `data-api.pool-maximum-pool-size` | 5 | 每业务源连接数 |
| `data-api.query-timeout-seconds` | 60 | 会话级超时 |
| `data-api.reject-dml` | true | 写操作拦截 |
| `data-api.acl-mode` | optional | off/optional/enforce |
| `data-api.acl-cache-ms` | 10000 | ACL 缓存 |
| `data-api.invoke-log-enabled` | true | 审计开关 |
| `data-api.invoke-log-queue-size` | 5000 | 审计队列 |
| `data-api.read-only-connections` | true | 连接只读标记 |
| `data-api.session-init-sql.<TYPE>` | 空 | 覆盖会话初始化 SQL |

### 7.2 接口清单（data-api 自有）

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| GET | `/data-api/health` | 无 | 状态 + 数据源数量 |
| GET | `/data-api/datasources` | token/sessionId | 已加载数据源明细（URL 脱敏） |
| POST | `/data-api/datasources/reload` | token/sessionId | 立即刷新 |
| GET | `/data-api/datasources/{key}/ping` | token/sessionId | `SELECT 1` |
| GET | `/data-api/acl` | admin | 规则列表 |
| POST | `/data-api/acl` | admin | 幂等新增/更新 |
| PUT | `/data-api/acl/{id}` | admin | 改 effect/remark |
| DELETE | `/data-api/acl/{id}` | admin | 删除 |
| GET | `/data-api/acl/apis` | admin | 已发布 API 目录 |
| ANY | `/data-api/gateway/**` | token/ACL | 业务 API |

### 7.3 参考资料

- OpenMetadata 元数据标准（API 资产）：<https://docs.open-metadata.org/latest/main-concepts/metadata-standard/apis>
- OpenMetadata Doris 连接器：<https://docs.open-metadata.org/v2.0.x/connectors/database/doris>
- magic-api 2.2.2 源码（本机 `~/.m2/repository/org/ssssssss/magic-api/2.2.2/magic-api-2.2.2-sources.jar`）
- 二期优化项清单：`D:\HPG\workspace\LHYF\021天基数据平台二期优化项清单_V0.1.md`（C-8、C-12、D-1、D-2、E-1 与本方案相关）
- 本机布局：`D:\HPG\workspace\LHYF\DS_ST_LAYOUT.md`
