# data-api 升 Spring Boot 3 与业务查询台合并方案

| 项目 | 内容 |
|---|---|
| 文档名称 | data-api 升 Spring Boot 3 与业务查询台（多数据源只读 SQL 控制台）合并方案 |
| 版本 | V0.1（2026-11-06 初稿） |
| 日期 | 2026-11-06 |
| 代码基线 | `data-center-mono`，分支 `chore/upgrade-ds-seatunnel`；DS 3.4.3 / magic-api 2.2.2 / data-api 当前为 Spring Boot 2.7.18（class major 52） |
| 涉及模块 | `dolphinscheduler/dolphinscheduler-data-api`、`dolphinscheduler/dolphinscheduler-ui` 的 data-api 视图、`deploy/local` |
| 编写目的 | 把"data-api 升 Boot 3"与"给业务用户提供多数据源查询台"合并成一条可执行路线，明确范围、存储、护栏、分期与验收 |
| 证据口径 | 代码与依赖为当前工作区实测；框架版本取自本地 `~/.m2` 中实际 jar；服务器资源数据取自 2026-11-05 实测 |

---

## 零、结论摘要

1. **两条线必须一起排**：Boot 3 迁移要替换掉的，正是查询台要复用的那批能力——用户身份、数据源注册表、JDBC URL 拼装、口令解码。先做查询台会在即将拆除的接口上返工，正确顺序是**先迁移、再建台**。
2. **Boot 3 的唯一可行路径是脱离 DS 反应堆**。实测耦合面：**19 个 DS 外部类、120 处引用、2 个 `@Import` 配置**。DS 3.4.3 是 Spring Boot 2.7 / `javax.*` / Spring 5，与 Boot 3 的 Jakarta EE 9 / Spring 6 不能在同一个 fat jar 共存，改版本号解决不了。
3. **目录与包名保持不变，只更换 Maven parent**。此前讨论过把模块搬到仓库根 `data-api/`，本版方案修正为"原路径独立"：既然包名仍然是 `org.apache.dolphinscheduler.dataapi`，搬目录只会带来脚本、文档、IDE 的无意义改动。改动面收敛为 `pom.xml` + 代码替换 + 启动脚本参数。
4. **查询台面向业务用户，只读**。只做单条 `SELECT / WITH / EXPLAIN`、结果网格与导出；不做 DDL、表设计器、数据行编辑、会话/锁诊断。这类能力留给运维侧的桌面工具。
5. **所有自有数据统一落到线上 PG**：实例 `123.57.2.64:5432`，开发环境默认也连线上 PG，不再使用本机 H2 作为默认元库；H2 仅在断网离线时作为兜底 Profile 保留。
6. **总量约 10.5 人日**。其中 P0 的 magic-api 2.2.2 在 Boot 3 + Jetty 11 下的冒烟是 **Go/No-Go 门**——不通过就停在 P0，改动不进主干。

---

## 一、范围

### 1.1 本方案包含

| 编号 | 内容 |
|---|---|
| S1 | data-api 脱离 DS Maven 反应堆，改用 Spring Boot 3.0.13 + JDK 17 |
| S2 | 自建 DS 用户/会话/数据源/授权关系的只读访问层，替换 19 个 DS 外部类 |
| S3 | 数据源注册表 + 类型描述符（新增数据源类型不再改 Java） |
| S4 | 业务查询台后端：语句守卫、只读执行、结果截断、CSV 导出、取消、审计 |
| S5 | 业务查询台前端：数据源选择、SQL 编辑器、结果网格、历史 |
| S6 | 自有数据表统一落线上 PG；开发环境切换连接 |
| S7 | 本地启动脚本的 JDK/参数/健康检查调整 |

### 1.2 本方案不包含

| 不做 | 原因 |
|---|---|
| DML / DDL / 存储过程 | 业务用户只读；写操作留给运维工具 |
| 表设计器、数据行编辑、ER 图 | 属于 DBA 工具能力，自研成本高、风险大 |
| 会话/锁/慢查询/备份恢复面板 | 同上，用 pgAdmin / DBeaver 完成 |
| 列级脱敏 | 任意 SQL 无法可靠做列级脱敏；靠数据库视图与只读账号列权限解决 |
| data-asset 改用共享注册表 | 可做但不在本轮；data-asset 继续用现有配置 |
| 引入 CloudBeaver / Linkis / Metabase | 2C2G 线上放不下；产品内只做受控只读查询台 |

---

## 二、已确认决策

| 编号 | 决策 | 理由 | 影响 |
|---|---|---|---|
| D1 | data-api 升 Spring Boot 3.0.13 / JDK 17 | 与 data-asset 统一构建栈；Boot 2.7 开源支持已于 2023-11 结束 | parent、依赖、注解体系全部调整 |
| D2 | 脱离 DS Maven 反应堆，**不搬目录、不改包名** | 包名变更成本远大于收益；只换 parent 可把 diff 收敛到 pom + 代码替换 | `dolphinscheduler/pom.xml` 移除模块声明 |
| D3 | 查询台面向**平台业务用户**，只读 | 写能力与业务页面的权限模型冲突 | 只做 L1 查询控制台；不做 L2/L3 |
| D4 | 先在**开发环境**部署验证 | 线上 2C2G 余量仅约 142MB，且当前只保留 PG | 上线前不进生产 profile |
| D5 | 自有数据统一存**线上 PG** | 避免 H2/生产两套数据；DS 元数据、扩展表、审计同实例便于治理 | 开发默认连线上；H2 降级为离线兜底 |
| D6 | 不引入外部 DBA 平台 | CloudBeaver 社区文档最低 2GB、Linkis 需 4~6GB | 只保留桌面 DBeaver 作运维手段 |
| D7 | 数据源注册表以 DS 源中心为准，我方只加扩展字段 | DS 已有 CRUD/连接测试/28 种连接器；data-api 已直读该表 | 新增一张 `da_` 扩展表，不改 DS 表结构 |

---

## 三、现状与实测证据

### 3.1 耦合面

`dolphinscheduler-data-api` 当前是 `dolphinscheduler` 反应堆的子模块（`dolphinscheduler/pom.xml` 第 61 行），parent 为 DS 3.4.3。对 DS 的编译期依赖实测如下：

| 类别 | 数量 | 明细 |
|---|---|---|
| DS 外部类 import（去重） | **19** | `common`：`CommonConfiguration`、`Flag`、`UserType`、`ServerLifeCycleManager`、`DefaultUncaughtExceptionHandler`、`EncryptionUtils`；`dao`：`DaoConfiguration`、`DataSource`、`Session`、`User`、`DataSourceMapper`、`SessionDao`、`UserDao`；`plugin.datasource.api`：`DataSourcePluginManager`、`DataSourceUtils`、`PasswordUtils`；`spi`：`BaseConnectionParam`、`ConnectionParam`、`DbType` |
| DS 类引用行 | **120** | 含同包内自身类引用 |
| Spring 配置 `@Import` | **2** | `DaoConfiguration`、`CommonConfiguration`（`DataApiServer`） |
| `javax.*` 注解 | **6** | `@PostConstruct` ×4、`@PreDestroy` ×2；`javax.sql.DataSource` 属 JDBC，保留 |
| MyBatis | 全量 | 仅用于 DS 实体；替换后整体移除 |

**结论**：迁移不是"改 19 个 import"，而是把"用户/会话/数据源/授权"四类读取与"URL 拼装/口令解码"两组工具自建。范围可控，但必须做回归。

### 3.2 框架与依赖实测

| 项 | 现状 | Boot 3 下需要 |
|---|---|---|
| parent | DS 3.4.3（Boot 2.7.18、Spring 5、`java.version=1.8`） | `spring-boot-starter-parent:3.0.13`、`java.version=17` |
| magic-api | 2.2.2，starter 同时依赖 `magic-api-servlet-jakarta` 与 `-javaee` | 排除 `magic-api-servlet-javaee`，保留 jakarta；jar 内已带 `AutoConfiguration.imports` |
| Web 容器 | 排除 Tomcat，使用 Jetty（Boot 2.7 对应 Jetty 9/10） | Boot 3 对应 Jetty 11（Jakarta Servlet） |
| H2 | DS BOM 锁 **2.2.220** | Boot 3.0.13 默认 **2.1.214**，降级打不开现有 `dsdb` 文件 → **显式锁 2.2.220** |
| 口令 | base64 + salt，开关 `DATASOURCE_ENCRYPTION_ENABLE` 默认 false | 复制约 40 行；开关与盐改环境变量，必须与 DS 同源 |
| 登录摘要 | `EncryptionUtils.getMd5` 即 `DigestUtils.md5Hex` | 直接使用 commons-codec |
| 数据源驱动 | `dolphinscheduler-datasource-all` 提供 28 种处理器 | 改为注册表 + 描述符驱动，按需引驱动 |

### 3.3 现有数据表（data-api 自有）

`DataApiSchemaBootstrap` 在元数据库中按需建表，已有 5 张：

| 表 | 用途 |
|---|---|
| `magic_api_file` | magic 脚本文件存储（集群共享模式） |
| `da_api_release` | API 发布版本与回滚 |
| `da_api_table_ref` | API 引用的表（血缘抽取） |
| `da_app_credential` | 调用方 app_id / secret 摘要 |
| `da_api_invoke_log` | 接口调用审计 |

### 3.4 magic-api Boot 3 可行性

本地 `~/.m2` 实测 magic-api 2.2.2 starter：

- jar 内同时存在 `META-INF/spring.factories` 与 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`；
- starter POM 同时依赖 `magic-api-servlet-jakarta` 与 `magic-api-servlet-javaee`；
- `magic-api-servlet-jakarta:2.2.2` 已存在本地仓库。

**判断**：具备 Boot 3 运行条件，但必须排除 javaee 并在 Jetty 11 下实测登录、资源加载、动态数据源与脚本执行。这是 P0 的内容。

---

## 四、目标架构

```
dolphinscheduler-ui (Vite dev :5173)
  ├─ /dataservice/*            现有数据服务菜单
  ├─ data-api/studio           magic 控制台
  ├─ data-api/datasources      数据源视图
  └─ data-api/console          ★ 业务查询台（新增）

data-api (:12346, Spring Boot 3.0.13 / JDK 17)
  ├─ registry    数据源注册表 + 类型描述符（新）
  ├─ magic       magic-api 2.2.2（排除 javaee）
  ├─ console     查询台：守卫 / 执行 / 导出 / 审计（新）
  └─ governance  ACL / 限流 / 调用审计 / 血缘 / 发布（原样迁移）

线上 PostgreSQL 123.57.2.64:5432
  ├─ DS 元数据表 t_ds_*             （只读）
  ├─ data-api 扩展表 da_*、magic_api_file
  └─ 业务库 agri_sat                （通过 DS 数据源 + 只读账号访问）
```

---

## 五、数据存储：统一线上 PG

### 5.1 连接与库/模式

| 项 | 取值 | 说明 |
|---|---|---|
| 实例 | `123.57.2.64:5432` | 线上 2C2G 机器，PostgreSQL 16.15 |
| 元数据库 | `dolphinscheduler`（默认） | 若线上实例无此库，则新建 `data_platform`；两者只差连接串 |
| 我方表所在模式 | `data_api` | 与 DS 的 `public.t_ds_*` 隔离，避免混表 |
| 业务库 | `agri_sat` | 通过 DS 数据源中心登记，查询台用扩展表里的只读账号 |
| 字符集/时区 | UTF8 / UTC | 与现有 data-api Jackson 配置一致 |

连接串示例（不含凭据）：

```
jdbc:postgresql://123.57.2.64:5432/dolphinscheduler?currentSchema=data_api&sslmode=prefer
```

### 5.2 配置口径

| 环境变量 | 用途 | 是否入仓库 |
|---|---|---|
| `DA_META_JDBC_URL` | 元数据库连接串 | 是（默认值写主机与库名） |
| `DA_META_USER` / `DA_META_PASSWORD` | 元数据库账号 | **否**，只由本机环境或启动脚本注入 |
| `DA_META_SCHEMA` | 我方表模式，默认 `data_api` | 是 |
| `DATASOURCE_ENCRYPTION_ENABLE` / `DATASOURCE_ENCRYPTION_SALT` | 与 DS 同源的口令解密开关与盐 | 否 |

**开发环境**：`start-local.ps1` 默认 `--spring.profiles.active=postgresql`，连接线上 PG；`h2` profile 保留，仅在断网时显式指定。**生产 profile 暂不启用**——查询台开关 `console.enabled` 默认关闭。

### 5.3 表清单

沿用现有 5 张表，新增 2 张（均为业务口径描述，DDL 由 `DataApiSchemaBootstrap` 按方言生成）：

| 表 | 用途 | 关键内容 |
|---|---|---|
| `da_datasource_ext` | 数据源治理扩展，键为 DS 数据源 id | 只读账号密文、是否允许查询台、默认行数上限、脱敏列、负责人 |
| `da_console_query_log` | 查询台审计，只追加 | 用户、数据源、SQL 摘要与哈希、行数、耗时、结果码、拒绝原因 |

存储约定：

1. **审计只追加**，不提供修改/删除接口；保留期与 `da_api_invoke_log` 一致，由定时任务清理。
2. **SQL 不落全量**：存前 4KB + SHA-256，既满足追溯又避免把敏感常量写进日志表。
3. **凭据只存密文**：只读账号口令用环境密钥加密，密钥不进仓库、不进数据库。
4. **建表幂等**：沿用 `CREATE TABLE IF NOT EXISTS`，服务启动自动补齐，不引入迁移框架。

### 5.4 连接池预算

| 池 | 上限 | 说明 |
|---|---|---|
| 元数据库池 | 5 | 只做元数据读写与审计落库 |
| 每个业务源池 | 3~5 | 查询台与 magic 共用同一注册表，**各自建池**时合计不超过 20 |
| data-asset 业务池 | 3 | 现有配置不变 |

线上 PG `max_connections` 默认 100；加上 DS、data-asset 与运维工具，建议总连接数控制在 **40 以内**，并在上线前用 `pg_stat_activity` 复核。

### 5.5 环境隔离与风险

| 风险 | 说明 | 处置 |
|---|---|---|
| 开发与生产共用同一实例 | 开发执行发布/回滚/审计会写入真实库 | 我方表按模式隔离（开发可用 `data_api_dev`），并约定开发环境不执行发布类操作 |
| 开发误连生产业务库 | 查询台可能读到真实数据 | 开发环境只登记脱敏/测试数据源；只读账号按库授权 |
| 网络可达性 | 5432 目前对 `0.0.0.0` 开放且为弱口令 | 安全组收口到固定出口 IP；口令轮换另行处理 |
| 与 DS 元数据耦合 | DS 升级可能改表结构 | 把 4 张 DS 表的依赖写入 DS 升级检查单 |

---

## 六、数据源注册表与类型描述符

### 6.1 注册表来源

| 内容 | 来源 |
|---|---|
| 数据源清单、类型、连接参数 | DS `t_ds_datasource`（只读） |
| 用户可见范围 | DS `t_ds_relation_datasource_user`（管理员看全量） |
| 治理字段 | 我方 `da_datasource_ext` |
| 连接池 | data-api 按注册表懒加载，变更检测沿用现有指纹机制 |

### 6.2 类型描述符

新增 `datasource-types.yaml`，一种类型一行；新增数据源 = 描述符 + 驱动 jar + 扩展表一行，不改 Java：

| 字段 | 作用 |
|---|---|
| `driver` / `urlTemplate` | 驱动类与 JDBC URL 拼装 |
| `quote` | 标识符引号（`"` / `` ` `` / `[]`） |
| `limitOffset` | 分页语法 |
| `catalogMode` | MySQL 用 catalog、PG 用 schema、Oracle 用 schema=用户 |
| `readOnlyTx` | `SET TRANSACTION READ ONLY` / `START TRANSACTION READ ONLY` / 空 |
| `metadataStrategy` | 默认 `jdbc` 通用反射；Hive 等用专用实现 |
| `systemSchemas` | 需要隐藏的系统模式 |

### 6.3 口令处理

- 读取 DS `connection_params` 中的口令后，按 `DATASOURCE_ENCRYPTION_ENABLE` / `SALT` 解码；
- 查询台使用 `da_datasource_ext` 中的**只读账号**，不复用 DS 的管理账号；
- 两个开关必须与 DS 运行配置一致，否则解码失败；在部署检查单中列为强制项。

---

## 七、业务查询台

### 7.1 接口

| 接口 | 说明 |
|---|---|
| `POST /data-api/console/execute` | 单条只读 SQL；返回列、行、截断标记、耗时 |
| `POST /data-api/console/cancel` | 取消正在执行的查询 |
| `POST /data-api/console/export` | 流式 CSV，上限 10 万行 |
| `GET /data-api/console/datasources` | 按用户授权过滤后的数据源 |
| `GET /data-api/console/history` | 只返回本人的查询记录 |

鉴权沿用现有 `token` / `sessionId` 请求头；身份解析使用 DS 用户、Access Token 与 UI 会话。

### 7.2 护栏（七层）

| 层 | 措施 |
|---|---|
| 身份 | 未登录 401；禁用用户拒绝 |
| 源授权 | 管理员全量；普通用户按 `t_ds_relation_datasource_user` |
| 语句 | JSqlParser 解析：单条、类型 ∈ `SELECT/WITH/EXPLAIN`；拒绝多语句、`FOR UPDATE`、`INTO`、`SET ROLE`、`COPY`、`pg_sleep`、`dblink` |
| 连接 | 只读账号 + `setReadOnly(true)` + 方言只读事务 |
| 资源 | 展示 1000 行、导出 10 万行、60 秒超时、SQL ≤ 20KB、每用户并发 1 |
| 审计 | 每次尝试（含拒绝）落 `da_console_query_log` |
| 限流 | 复用现有令牌桶，按用户维度追加限制 |

### 7.3 结果与导出

- 结果集流式读取，达到上限立即截断并返回 `truncated=true`；
- 列类型、可空、主键标记来自注册表的元数据接口；
- 导出走独立接口，避免大结果占用内存；
- 取消通过 `Statement.cancel` 实现，并记录取消原因。

### 7.4 前端

- 位置：`dolphinscheduler-ui/src/views/data-api/console/`；
- 组件：数据源选择、CodeMirror 6 + `@codemirror/lang-sql`、运行（Ctrl+Enter）/取消、结果网格、CSV 导出、历史侧栏；
- 主题：跟随 DS UI 主题；错误提示沿用 data-api 现有的两种包体形状处理；
- P3 增量：对象树与表名补全、EXPLAIN 面板。

### 7.5 边界声明

1. **只读**：即使未来开放写能力，也必须是独立的、带审批的运维入口，不能长在业务页面上。
2. **不脱敏**：任意 SQL 无法可靠做列级脱敏；查询台只对"允许整表读取"的数据源开放。
3. **非 JDBC 源不覆盖**：Kafka、ES、HDFS 等没有统一的 `DatabaseMetaData`，需各自的元数据适配器。

---

## 八、实施计划

| 阶段 | 产出 | 验收门槛 | 工作量 |
|---|---|---|---|
| P0 可行性 | Boot 3 骨架 + magic-api 2.2.2（排除 javaee）启动 | `/magic/web` 能登录、加载资源、执行一条查询 | 0.5 人日 |
| P1 拆分 | parent 改 Boot 3.0.13、JDK 17；DS 根 pom 移除模块 | `mvn package` 产出可执行 jar，`java -jar` 正常起 12346 | 1 人日 |
| P2 替换 | 19 个 DS 类 → 自建；`javax` → `jakarta`；移除 MyBatis/DS 依赖 | 迁移回归 8 项全过 | 2 人日 |
| P3 注册表 | 描述符 YAML + 注册表 + `da_datasource_ext` + 授权过滤 | PG 与 MySQL 两源连通；未授权源 403 | 1.5 人日 |
| P4 查询台后端 | 守卫 / 执行 / 审计 / 导出 / 取消 | 17 条用例全绿 | 2 人日 |
| P5 查询台前端 | 编辑器、结果网格、导出、历史 | 浏览器端跑通，切源与错误提示正确 | 2.5 人日 |
| P6 联调 | 开发环境部署 + 全量回归 | 一键启动、状态页全 200 | 1 人日 |

**合计 10.5 人日**，单人约 2~2.5 周；P4/P5 可与 P1/P2 部分并行。

> 说明：此前记录的"0.5~1 人日"只覆盖代码改写，不含全量回归、注册表抽取与查询台；本方案按完整口径重算。

---

## 九、验收清单

### 9.1 迁移回归（P2）

1. DS 账号口令登录、Access Token 登录、UI 会话透传；
2. 数据源加载：PG 业务源 + 一个非 PG 源（MySQL）均能建池；
3. magic 控制台：登录、资源加载、脚本执行、reload；
4. 6 个农业接口经网关返回 200；
5. ACL：允许/拒绝两条路径；
6. 审计：成功与失败均落 `da_api_invoke_log`；
7. 限流：令牌桶触发 429；
8. 发布/回滚：快照生成与回滚各一次。

### 9.2 查询台用例（P4）

- 正向 9 条：普通查询、聚合、大结果截断、导出、切换数据源、中文、NULL、超时边界、取消；
- 拒绝 8 条：`update`/`delete`/`create`、多语句、`for update`、`pg_sleep`、未授权源、超长 SQL、未登录、并发第二条。

### 9.3 存储验收（P6）

- 线上 PG 首次启动自动建出 7 张表（5 张现有 + 2 张新增），重复启动无报错；
- 查询台执行一次后 `da_console_query_log` 有记录，SQL 仅为摘要 + 哈希；
- 连接数：`select count(*) from pg_stat_activity` 不超过预算；
- 开发机不再生成新的 H2 文件（离线 profile 除外）。

---

## 十、风险与回滚

| 风险 | 影响 | 处置 |
|---|---|---|
| magic-api 在 Boot 3 + Jetty 11 下异常 | 整个 API 开发页不可用 | **P0 冒烟**；不通过则停在 P0，改动不进主干 |
| H2 文件版本降级 | 离线开发起不来 | 显式锁 2.2.220；或离线统一走 PG |
| DS 元数据表结构/口令规则变更 | data-api 读取失败 | 4 张表依赖写入 DS 升级检查单；口令开关同源 |
| 开发/生产共用 PG 实例 | 误写生产数据 | 模式隔离 + 约定开发环境不执行发布操作 |
| 查询压垮业务库 | 业务受影响 | 只读事务、60s 超时、行数上限、池上限、单用户并发 1 |
| Boot 3 与 Jakarta 迁移遗漏 | 运行期 `NoSuchMethod` | P0 覆盖 Jetty/Servlet 路径；回归清单包含登录与资源加载 |
| 回滚 | — | 代码回滚到迁移前提交即可；数据库无破坏性变更，新增表可保留 |

---

## 十一、附录：证据索引

| 项 | 位置 |
|---|---|
| 模块 parent 与依赖 | `dolphinscheduler/dolphinscheduler-data-api/pom.xml` |
| DS 类导入清单 | `dolphinscheduler/dolphinscheduler-data-api/src/main/java/**` |
| 扩展表建表逻辑 | `dolphinscheduler/dolphinscheduler-data-api/src/main/java/org/apache/dolphinscheduler/dataapi/config/DataApiSchemaBootstrap.java` |
| 身份解析 | `.../dataapi/auth/DsTokenAuthorizationInterceptor.java` |
| 数据源加载 | `.../dataapi/datasource/DsMagicDataSourceLoader.java` |
| 口令处理 | `dolphinscheduler/dolphinscheduler-datasource-plugin/dolphinscheduler-datasource-api/src/main/java/org/apache/dolphinscheduler/plugin/datasource/api/utils/PasswordUtils.java` |
| DS 数据源授权 SQL | `dolphinscheduler/dolphinscheduler-dao/src/main/resources/org/apache/dolphinscheduler/dao/mapper/DataSourceMapper.xml` |
| DS 用户表字段 | `dolphinscheduler/dolphinscheduler-dao/src/main/resources/org/apache/dolphinscheduler/dao/mapper/UserMapper.xml` |
| 本地方案文档 | `dolphinscheduler/dolphinscheduler-data-api/docs/DATA-API-OPENMETADATA-PLAN.md` |
