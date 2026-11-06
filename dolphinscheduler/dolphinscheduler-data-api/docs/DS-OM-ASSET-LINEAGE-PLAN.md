# 数据资产管理与数据血缘服务方案（独立服务）

> 版本：v1.19（2026-11-05）
> v0.2 的决策保留：独立服务、白名单浏览、血缘自绘。
> v0.7 保留：**OM 作治理主库**（标签/分级/质量/术语/域/产品/目录/搜索）。
> v0.8 收敛 `data-asset` 职责：只做增量能力与接口转发；表/列血缘读 OM；行级血缘按值反查。
> v0.9 可行性复核：质量路线 A 当前不可用，改路线 B 为主。
> v1.0 轻量化定稿：业务数据零落库，只存配置与审计，缓存仅内存。
> v1.1 决策落地：保留 OM；首批 ODS→DWD；键映射平台维护；历史不做；本地 jar / 最终 K8s。
> v1.2 质量路线定稿：采用路线 B（`data-asset` 执行 + 回写 OM）。
> v1.3 收尾：确认 8 个断言；白名单审批默认开启。
> v1.4 实施：P0 后端与端到端验证完成。
> v1.5 调整：页面改为 DS UI 内嵌路由；`data-asset` 仅提供 API；补齐 8 张表白名单。
> v1.6 治理：DS UI 增加分层/分级/认证筛选与治理编辑、治理配置页；`data-asset` 增加治理代理。
> v1.7 版式：参照 OM 数据产品概览页，资产目录改为卡片化布局。
> v1.8 微调：去掉固定高度；搜索框居中并把检索按钮并入输入框；压缩统计卡高度；头部底色改浅蓝。
> v1.9 精简：移除头部标题区，仅保留居中搜索与紧凑统计。
> v1.10 视觉：图标改扁平线性；头部改纯浅灰底；筛选标签改细边框文本；卡片圆角收至 4px。
> v1.11 配色：按 DS 主题色 `#1890ff` 回填——图标浅蓝/浅青底、徽标用 AntD 语义浅色、筛选选中浅蓝底、标题加蓝色竖线。
> v1.12 血缘：血缘页改版（工具栏 + 资产快选 + 四个页签）；新增手动血缘配置、键映射登记、行级开关与行级按值反查接口，P3 行级血缘落地。
> v1.13 版式统一：资产目录上半部去掉浅蓝底与统计小卡，改为血缘页同款白卡（细边框、圆角 8、分隔线、“标签 + 彩色数字”行内统计）。
> v1.14 行级血缘：节点带出**整行数据**（一条或多条），列与脱敏沿用浏览白名单规则；页面按节点展开/收起。
> v1.15 版式：治理配置页改为同款白卡（顶部统计行 + 蓝色竖线面板标题 + 分隔线表单 + 语义色标签）；资产目录检索框取消居中改左右结构，分层/分级/认证/域/数据产品标签按每组一行显示。
> v1.16 启动：`deploy/local/start-local.ps1` 升级为全栈幂等编排（六个服务 + 体检 + 单服务重启），修掉 OM 60s 误报、`-Ports` 变量名冲突与 `SPRING_DATASOURCE_URL` 漏给 data-asset 的问题。
> v1.17 主题：三个资产页面去掉写死色值，统一走 `useAssetTheme()` 主题令牌（跟随 DS 明暗切换），明暗两态均实测通过。
> v1.19 血缘编辑取数：编辑器两侧可选 OpenMetadata 或数据源（直查 information_schema 的库/模式/表/列），保存时缺失表自动登记进 OM（5.3.6）。
> v1.18 版式加厚：头部改为横幅（标题 + 说明 + 检索/操作 + 4~5 张 KPI 卡），标签区改为分组面板（组名 + 命中数 + 选中主色实心带勾），三页统一。
> 关联文档：`DATA-API-OPENMETADATA-PLAN.md`、`AUTH-INTEGRATION.md`
> 字段级定义与 DDL 另出《数据库设计说明书》。

## 一、结论摘要

1. 新建独立服务 **`data-asset`**：独立进程、端口 12347、独立库 `data_asset`、独立前端控制台。**不改 DolphinScheduler / OpenMetadata / magic-api 源码**，只调公开 REST 接口。
2. **数据浏览必须自建**：OM 不执行查询（实测 `sampleData` 返回 9 列 0 行、`/queries` 总数 0）。本服务内置只读受控查询器。
3. **保留 OM 作为治理主库**：数据标签、分级分类/认证、数据质量（定义/结果/事故）、术语、域、数据产品、资产目录与搜索都由 OM 承担。这三项新需求若自建约 **39–45 人日**，而 OM 已实测可用（现有 18 标签、4 术语、1 域、1 产品、1 个质量用例）。
4. **表级/列级血缘不在本地落库**：`data-asset` 只做接口转发（读 OM `getLineage` / `lineage/table/name`），由独立前端自绘；OM 是唯一血缘存储。
5. **行级血缘不落边表**：以列级血缘为索引，“目标行键 → 上游键列映射 → 上游按值反查”逐层解析；结果只做短 TTL 缓存。为保证与表/列级展示一致，**前端功能单独实现**，不复用 OM 页面。
6. **资产元数据以 OM 为准**：本服务只做进程内短 TTL 缓存，不落元数据表；标签、分级、质量全部代理 OM。
7. 部署为 5 个组件：DS + data-api + data-asset + OM + OpenSearch；检索复用 OM `/search/query`，不再自建索引。
8. **存储最小化**：`data_asset` 只存白名单、键映射、行级开关、用户属性与审计（MB 级），不存任何业务数据或血缘副本；缓存仅在进程内存。

## 二、OpenMetadata 引入必要性评估（v0.7 重评：范围变化后）

### 2.1 OM 与本次需求的匹配度

| 本次需求 | 权重 | OM 能否满足 | 若自建成本 | 说明 |
|---|---|---|---|---|
| 资产目录、详情、搜索 | 15% | ✅ | 约 8 人日 | 目录、搜索、版本历史、治理字段齐全 |
| 数据行浏览 | 15% | ❌ | 已在 P0 | OM 不连库执行 SQL（实测 0 行样例、0 条查询） |
| 表级 + 列级血缘 | 10% | ✅ | 约 5 人日 | 已实测可用 |
| 行级血缘 | 15% | ❌ | 已在 P2 | OM 实体粒度是元数据实体，行不是实体 |
| 血缘自绘 | 10% | ❌ | 已在 P1 | OM 前端不参与 |
| 白名单 / 数据权限 | 10% | ❌ | 已在 P0 | OM 的域/分级不做数据访问控制 |
| **数据标签** | 8% | ✅ | 约 4 人日 | classification/tag、绑定、批量打标、UI 齐全 |
| **分级分类 / 认证** | 7% | ✅ | 约 6 人日 | Tier、Certification、敏感级别、PII 识别（recognizer 接口） |
| **数据质量** | 10% | ✅ | **约 18–22 人日** | 内置测试定义、Test Suite/Case、结果与事故（2.1 新增 incident）、质量维度、看板 |

**量化**：OM 能直接覆盖的需求权重约 **50%**；其中“标签 + 分级分类 + 质量”三项自建约 **28–32 人日**，目录与表/列血缘再省约 13 人日。**保留 OM 直接省下约 41–45 人日**，且质量与分级是持续演进的能力（OM 每个版本都在加测试定义与维度），自建要长期跟。

### 2.2 保留 OM 的成本（实测数字）

| 成本项 | 实测值 |
|---|---|
| 常驻内存 | OM 约 **1.4 GB** + OpenSearch 约 **1.5 GB**（OM 搜索依赖它）≈ **2.9 GB**；`data-asset` 另计约 1 GB |
| 磁盘 | OM 423 MB + JRE 146 MB + OpenSearch 1.3 GB |
| 进程/容器 | **+1**（OM）；OpenSearch 不算在 OM 头上 |
| 元数据库 | `osint` 库 **231 张表**（2.0.7 时 202 张），每次升级都可能变动 |
| 发版节奏 | 实测 2.0.1→2.0.7 共 7 个版本、55 天，**平均 8 天一个补丁**；2.1.0 至今仍是 rc1 |
| 升级成本 | 2.0.7→2.1.0-rc1 全流程实测：下载 409 MB、迁移 2 个版本、启动+验证约 40 分钟 |
| 版本风险 | 2.1 已改过 `getLineage` 返回结构（数组→对象）；`lineageSettings` 等配置键有坑 |
| 运维现状 | **未服务化**：2026-11-02 机器重启后 OM 未自启（8585 无监听）；各组件都是脚本拉起 |
| 同步成本 | 几乎为零：资产元数据 **OM → data-asset**（只读短缓存）；表/列血缘只读转发，不写回；行级解析不落库；标签/分级/质量只在 OM 编辑 |

### 2.3 若不保留 OM，要补什么（范围更新后）

| 自建能力 | 是否本期必需 | 粗估 |
|---|---|---|
| 资产元数据存储与版本 | 是 | 5 人日 |
| 全文搜索 | 是（可复用 OpenSearch 或 `pg_trgm`） | 2–4 人日 |
| **数据标签** | **是** | 4 人日 |
| **分级分类 / 认证（含敏感识别）** | **是** | 6 人日 |
| **数据质量（定义/执行/结果/事故/维度/看板）** | **是** | 18–22 人日 |
| 术语 / 域 / 数据产品（资产页会展示） | 建议 | 6 人日 |
| 多源接入连接器 | 按需 | 每源 2–5 人日 |
| 治理门户（非技术用户） | 建议 | 5 人日 |
| 血缘三层 / 浏览 / 白名单 / 自绘 | 是 | 已计入 P0–P2 |

**关键判断**：仅“标签 + 分级 + 质量”三项就约 **28–32 人日**，加上目录与表/列血缘约 13 人日、术语/域/产品约 6 人日，合计 **39–45 人日**，且质量与分级需要长期跟进（OM 每个版本都在新增测试定义与质量维度）。这已经超过保留 OM 的运行成本，**结论是保留**。

### 2.4 决策矩阵（加权，10 分制）

| 维度 | 权重 | 保留 OM（治理主库） | 去 OM（全部自建） |
|---|---|---|---|
| 当前需求匹配（含标签/分级/质量） | 45% | 9 | 4 |
| 总拥有成本（内存/升级/同步） | 20% | 4 | 9 |
| 风险与安全（rc 版本、暴露面） | 15% | 4 | 8 |
| 未来扩展（治理、多源接入） | 20% | 9 | 5 |
| **加权得分** | 100% | **7.25** | **5.8** |

### 2.5 结论：保留 OM 作治理主库，明确事实源边界

| 数据/能力 | 事实源 | `data-asset` 的行为 |
|---|---|---|
| 资产/列/描述/负责人/版本 | **OM** | 进程内短缓存（60s），不落库 |
| 数据标签、分级分类、认证、术语、域、数据产品 | **OM** | 代理读写（经 OM API，必要时走 2.1 的 `changeRequests` 审批） |
| 数据质量：定义、结果、事故、维度 | **OM** | 展示与跳转；执行方式见 2.6 |
| 表级 + 列级血缘 | **OM** | 只读转发（不落库、不投影） |
| 行级血缘 | **`data-asset`（即时解析）** | 基于 OM 列级血缘 + 业务库按值反查；只做短 TTL 缓存 |
| 数据浏览、白名单、审计 | **`data-asset`** | — |

**保留的代价与对冲**：

| 代价 | 对冲 |
|---|---|
| 常驻 2.9 GB（OM + OS） | 与“自建 39–45 人日”相比可接受；`data-asset` 自身控制在 1 GB |
| 2.1 仍是 rc1、发版频繁 | 锁 2.1.0 GA 后再升；所有 OM 调用封在 `om-client`，只改适配层 |
| 未服务化、重启不自启 | P0 一并做服务化（Windows 服务 / systemd / K8s） |
| 事实源分散 | 用 2.5 的单向边界消除双写；任何字段只允许一个方向写 |

### 2.6 质量执行路线：A 与 B 的本质区别

两条路线的终点一样（结果都落 OM、看板/事故/告警都在 OM），差别在**“谁去连业务库执行 SQL”**。

**路线 A：OM ingestion 执行**

```
DS/Airflow 触发 → openmetadata-ingestion(Python) → 用 OM 里配置的连接信息连业务库
→ 跑 testDefinition → 把结果 POST 回 OM
```

- 执行者：OM 的 ingestion 框架（独立 Python 进程/容器）。
- 凭据：存在 OM 的数据库服务连接里，ingestion 读取使用。
- 调度：OM 的 pipeline service（通常 Airflow）或外部调度调用 ingestion CLI。
- 定义覆盖：25 个内置定义全可用，新增定义随 OM 升级获得。
- 审计：OM 记录 ingestion 运行，但不经过 `data-asset` 的白名单与审计。

**路线 B：data-asset 执行并回写**

```
DS 定时任务 → HTTP 调 data-asset 执行测试
→ data-asset 从 OM 读 testCase/testDefinition 配置
→ 用只读账号 + 白名单 + 限流/超时执行 SQL
→ POST /v1/dataQuality/testCases/testCaseResults/{fqn} 回写 OM
```

- 执行者：`data-asset`（复用浏览用的只读连接池）。
- 凭据：只在 `data-asset` 一处，按数据源最小权限管理。
- 调度：直接用 DS（已有），不需要新增调度器。
- 定义覆盖：需实现常用定义的 SQL 生成；可用 `tableCustomSQLQuery` 兜底任意 SQL。
- 审计：与数据浏览共用一套审计与白名单，谁在什么时候跑了什么 SQL 全程留痕。

| 维度 | 路线 A（OM ingestion） | 路线 B（data-asset 执行） |
|---|---|---|
| 运行时依赖 | Python 3.9+、openmetadata-ingestion、Airflow/pipeline service（当前 0 个） | 仅现有 Java 服务 + 只读连接 |
| 凭据位置 | OM 连接配置（与浏览用的只读账号分开） | 与浏览共用只读账号，集中管理 |
| 调度 | 需新增 Airflow 或外部调度 | 直接用 DS |
| 定义覆盖 | 25 个全量，后续随 OM 升级扩展 | 首期 8–10 个；`tableCustomSQLQuery` 可兜底 |
| 与白名单/审计关系 | 不经过，需单独治理 | 完全复用 |
| 结果回写 | 自动 | 调用 OM API 回写（格式需对齐） |
| 部署增量 | +Python 运行时 +调度器（内存/运维成本更高） | 0 |
| 失败面 | ingestion 运行时、Airflow、网络、凭据多处 | 单服务，失败可重试可审计 |
| 粗估工时 | 部署配置 3–5 人日（不含运维） | 实现断言子集 8–10 人日 |

**决定（2026-11-02）：采用路线 B。** 理由：本机没有 ingestion 运行时（实测 0 pipeline service / 0 ingestion pipeline），路线 A 的“零开发”不成立；路线 B 能复用白名单、审计与只读账号，且调度已在 DS。首期 8 个定义**已确认够用**。

**B 方案落地要点**

| 项 | 约定 |
|---|---|
| 测试配置来源 | 从 OM 读 `testCase`（含 `testDefinition` 与参数值），不在本地另存用例 |
| 首期定义 | `tableRowCountToBeBetween`、`columnValuesToBeNotNull`、`columnValuesToBeUnique`、`columnValuesToBeBetween`、`columnValueLengthsToBeBetween`、`columnValuesToMatchRegex`、`tableColumnCountToBeBetween`、`tableCustomSQLQuery` |
| SQL 生成 | 按定义模板参数化生成，只允许键列/操作符白名单；`tableCustomSQLQuery` 需平台审核后启用 |
| 执行 | 复用浏览的只读连接池；单测超时 60s、扫描行数上限、失败重试 1 次 |
| 调度 | DS 定时工作流（HTTP 任务）调用 `/data-asset/governance/quality/run`，按测试套件批量执行 |
| 结果回写 | `POST /v1/dataQuality/testCases/testCaseResults/{fqn}`；失败也回写失败状态与错误摘要 |
| 幂等 | 以 `testCaseFqn + 执行批次` 为幂等键，重复执行覆盖同一批次结果 |
| 审计 | 记录谁触发、执行 SQL 摘要、耗时、行数、结果；与浏览审计同一套 |
| 后续 | 若 K8s 环境引入 Airflow/ingestion，可让路线 A 承担全量定义，B 保留关键链路 |

## 三、服务边界与职责划分

| 能力 | 承担方 | 改其源码 |
|---|---|---|
| **数据浏览、白名单、行级血缘解析、血缘自绘、审计；其余全部接口转发** | **`data-asset`（新建）** | 新代码 |
| **资产目录、表/列血缘、标签、分级分类、认证、术语、域、数据产品、数据质量、搜索** | **OpenMetadata（治理主库）** | 否 |
| 全文检索 | OM `/search/query` + OpenSearch 3.4.0（OM 依赖，常驻） | 否 |
| 调度与加工执行 | DolphinScheduler / SeaTunnel | 否 |
| 对外数据服务 API | `dolphinscheduler-data-api` | 否 |

`data-asset` 三个单元：

| 单元 | 说明 |
|---|---|
| `data-asset-service` | Spring Boot，端口 12347，前缀 `/data-asset`；只做转发、缓存与增量能力：白名单、只读查询器、行级血缘解析、审计；不落元数据表、不落血缘边表 |
| `data-asset-web` | 独立 SPA（Vue3 + Vite + AntV X6），由服务托管在 `/asset-console` |
| `data-asset-schema` | 独立库 `data_asset`，不与 DS/OM 共表 |

**认证委托、授权自管**：登录仍用 DS 账号（调 DS token 校验接口），能看哪些资产/数据/血缘由本服务白名单与角色决定。

### 3.2 存储与轻量化边界

**原则：业务数据零落库，只存“没有它就无法工作”的配置与合规记录。** 元数据、表/列血缘、标签、分级、质量、搜索索引全部在 OM；浏览到的数据与行级解析路径都不落库。

| 类别 | 内容 | 规模 | 说明 |
|---|---|---|---|
| 存（配置） | 浏览白名单 + 审批记录 | KB–MB | 动态授权必需 |
| 存（配置） | 键映射登记（目标键列 → 上游键列） | KB–MB | 行级反查的键语义 |
| 存（配置） | 行级追踪开关（按表） | KB | 性能与敏感数据保护 |
| 存（配置） | 用户属性映射（region/tenant） | KB | 行策略模板用，可从 IdP 同步 |
| 存（记录） | 审计：浏览、导出、权限裁剪 | 可增长 | append-only，默认保留 90 天，可外置到日志/数仓 |
| **不存** | 资产/列、表/列血缘、标签、分级、质量、搜索索引 | — | 全部来自 OM |
| **不存** | 浏览结果、行级解析路径、数据快照 | — | 只在响应中返回 |
| 缓存 | 进程内 Caffeine（60s，上限 1000 条） | 内存 | 不依赖 Redis；多实例各自缓存 |

**运行时约束**：单进程 Spring Boot；除 OM / DS / 业务库外无新增中间件（不需要 MQ、Redis、Airflow）；无定时批处理（仅保留可关闭的审计清理）；配置在库、服务无状态，可水平扩容；数据库只有 5 张小表。

## 四、总体架构与集成方式

```
浏览器 ── Nginx:8088 ─┬─ /dolphinscheduler/** → DS-API :12345     （不改）
                      ├─ /data-api/**         → data-api :12346   （不改）
                      ├─ /data-asset/**       → data-asset :12347 （新增）
                      │     ├─ 元数据/标签/质量 → OM :8585（转发 + 短 TTL 缓存）
                      │     ├─ 搜索            → OM /search/query（OpenSearch 后端）
                      │     ├─ 表/列血缘      → OM /api/v1/lineage（只读转发）
                      │     ├─ 行级血缘      → OM 列级血缘 + 业务库按值反查
                      │     ├─ 只读查询器      → 业务库（白名单内，只读账号）
                      │     └─ 白名单/审计/键映射 → data_asset 库（仅增量数据）
                      └─ /openmetadata/**      → OM :8585（内网/治理门户）
```

| 调用方 → 被调方 | 协议/端口 | 认证 | 用途 | 超时/缓存 |
|---|---|---|---|---|
| 浏览器 → Nginx | 8088 | DS sessionId/token | 统一入口 | — |
| Nginx → data-asset | 12347 | 透传，服务侧校验 | 控制台与接口 | — |
| data-asset → DS-API | 12345 | 只读校验 token | 身份、任务实例信息 | 3s |
| data-asset → 业务库 | 各库 | **只读专用账号** | 数据浏览、行级按值反查 | 60s；池上限 5 |
| data-asset → data_asset | 5432 | 独立账号 | 白名单、键映射登记、审计、短缓存 | — |
| data-asset → OM | 8585 | Bot JWT | 元数据/标签/分级/质量、搜索、表/列血缘读取 | 5s；元数据缓存 60s |
| OM → OpenSearch | 9200 | 无（本机） | 搜索与索引 | — |

**为什么不让浏览器直连 OM**：Token 外泄、CORS 放开、2.1 已改过 `getLineage` 结构会直接冲击前端、无法按 DS 用户过滤。
**为什么不复用 data-api 做浏览**：data-api 是“已发布接口”模型，即席表浏览要绕；且会把新能力耦合进另一个服务。

## 五、功能设计

### 5.1 资产目录与元数据

| 功能 | 做法 |
|---|---|
| 元数据主数据 | **OM 是唯一事实源**：本服务不采集、不落元数据表，只做接口转发 + 进程内短 TTL 缓存 |
| 目录树 | 用 OM 搜索与实体接口拼装：数据源 → 库 → schema → 表/视图/物化视图 → 列 |
| 搜索 | 代理 OM `/search/query`（OpenSearch 后端），见 5.1.1 |
| 详情 | 聚合 OM 实体（描述/负责人/标签/域/质量）+ 本服务白名单状态与血缘入口 |
| 与 OM 的关系 | 只读转发；标签、分级、术语的编辑走 OM 代理接口，本服务不留副本 |

#### 5.1.1 检索实现：只代理 OM

| 项 | 约定 |
|---|---|
| 实现 | 直接调 OM `/api/v1/search/query`；本服务不建索引、不落资产文档 |
| 缓存 | 仅进程内 Caffeine 缓存（键=查询+主体，TTL 60s，最大 1000 条）；不依赖 Redis |
| 结果处理 | 按可见性与白名单裁剪字段；保留 OM 的高亮与分面结果 |
| 降级 | OM 不可用时返回“目录暂不可用”，不做本地兜底检索（避免维护第二份资产数据） |
| 分页 | 透传 OM 的 `search_after`；若做了可见性过滤，响应标注“已过滤”，总数可能大于当前可见数 |
| 扩展 | 仅保留 `SearchPort` 接口；PG/OpenSearch 适配器**本期不实现**，需要时再评估 |

#### 5.1.2 决策记录（为何不自建索引）

实测（2026-11-02）：PG 16.15 已装 `pg_trgm` 但无中文分词扩展，`to_tsvector('simple','作物长势监测')` 为单个 token、`@@` 匹配失败；OpenSearch 3.4.0 未装 smartcn/IK，中文按字切分。结论：在 OM 已提供索引与搜索接口的前提下，自建索引只会多一份数据副本与同步成本，**不实现**；该记录仅作为未来脱离 OM 时的评估依据。

### 5.2 数据浏览：表级白名单

#### 5.2.1 条目内容（业务语义）

| 分组 | 内容 |
|---|---|
| 定位 | 数据源、库、schema、表（支持 `schema.*`，**禁止**整库/整源通配） |
| 列范围 | include/exclude 列表；命中敏感清单的列默认排除或强制脱敏 |
| 行范围 | 可选行策略模板（如 `region_id = ${user.regionId}`），不支持自由 SQL |
| 可见主体 | DS 用户 / 项目 / 角色，取并集 |
| 能力开关 | 是否允许导出、单页行数、单次最大扫描行数、是否允许即席过滤/排序 |
| 生命周期 | 申请人、审批人、生效时间、有效期（到期自动失效）、状态 |
| 追溯 | 每次变更留痕 |

#### 5.2.2 三种配置入口（并存，落同一份数据）

**① 控制台（日常主用）**：资产详情页 → “开放浏览” → 填列范围/主体/有效期 → 提交审批 → 通过后生效。**审批默认开启**；审批规则见下表。

**② YAML 基线（环境级默认、冷启动、灾备）**：`data-asset/conf/browse-whitelist.yaml`

```yaml
data-asset:
  browse:
    entries:
      - id: agri-dwd-readonly
        datasource: agri_satellite_pg
        database: agri_sat
        schema: public
        tables: [dwd_ndvi_tile, ads_crop_health]      # 明确列举，禁止 "*"
        columns: { mode: include, list: ["*"] }        # 或 mode: exclude, list: [phone, idcard]
        maskColumns: [farm_phone]                      # 掩码展示，不阻断
        rowPolicy: "region_id = ${user.regionId}"      # 可选
        subjects: { dsProjects: [agri_data], dsRoles: [DATA_ANALYST] }
        limits: { pageSize: 200, maxRows: 5000, export: false }
        validUntil: 2026-12-31
        approvedBy: data-owner-agri
```

YAML 与库内条目合并，控制台改动优先级更高并标记“已偏离基线”。

**③ OpenAPI / CLI（批量与准入）**：`POST /data-asset/browse/entries`（带 `id` 幂等 upsert）。新表上线流程里自动登记；CI 可校验“未登记白名单的表不得在目录中开放数据”。**通过 API/CLI 提交的条目默认进入待审批**，与控制台同一审批流；仅 YAML 基线由管理员直接生效。

**首批开放清单（已确认，2026-11-02）**

| 范围 | 表 | 说明 |
|---|---|---|
| 数据浏览 + 行级追踪 | `ods_satellite_scene`、`dwd_ndvi_tile` | ODS→DWD 关键明细，首批仅这两张开启行级 |
| 数据浏览（维度查阅） | `dim_satellite`、`dim_region`、`dim_crop` | 键映射与维度反查所需，只读、限行 |
| 仅元数据可见 | `dws_crop_growth`、`ads_crop_health` | 汇总层首批不开放数据；行级不适用 |
| 后续批次 | 按“可追覆盖率 ≥80%”评估逐批纳入 | 每批必须先有键映射 |

**审批规则（默认开启）**

| 项 | 约定 |
|---|---|
| 审批人 | 资产 Owner；无 Owner 时由平台管理员审批 |
| 禁止自批 | 申请人不能审批自己的条目 |
| 驳回 | 必须填写原因，退回申请人修改 |
| SLA | 默认 1 个工作日提醒；超时只提醒，不自动放行 |
| 生效 | 审批通过后 ≤30s 生效；有效期到期自动失效并通知 Owner |
| YAML 基线 | 由管理员直接写入，视同已审批，但必须填 `approvedBy`；控制台改动仍走审批 |
| 留痕 | 申请、审批、驳回、失效全部进审计 |

#### 5.2.3 生效与校验

```
浏览请求 → 解析用户与项目 → 命中白名单？
   否 → 403 + 审计（表、用户、原因）
   是 → 生成受限 SQL（列白名单 + 行策略 + LIMIT）→ 只读连接执行
        → 列脱敏 → 行数/字节双上限 → 返回 + 审计
```

| 硬约束 | 取值 |
|---|---|
| 缓存刷新 | 条目变更后 ≤30s 生效（版本号轮询） |
| 语句类型 | 仅 `SELECT`，禁分号堆叠与写操作 |
| 单页/单次上限 | 默认 200 / 5000 行，可按条目收紧 |
| 响应体积 | 2 MB，超出截断并标记 |
| 超时 | 60s（JDBC queryTimeout） |
| 深翻页 | 超 1 万行强制主键游标 |
| 导出 | 异步任务 + 独立审批（水印待确认） |
| 审计 | 用户、条目 id、表、列、行数、耗时、结果码 |

**前后对照**：现在要看表数据得去数据库客户端或 magic studio 手写 SQL；上线后在目录里点表 → “数据”页签分页看，每一跳都有白名单判定与审计。

### 5.3 血缘

#### 5.3.1 职责：表/列只转发，行级才做增量

| 层级 | 由谁提供 | `data-asset` 行为 |
|---|---|---|
| 表级 | OM（`getLineage`） | 只读转发 + 结构归一 + 60s 缓存 |
| 列级 | OM（`lineage/table/name` 的 `columnsLineage`） | 同上 |
| 行级 | `data-asset` 即时解析 | 以列级血缘为索引，按值反查源表，逐层递归 |

不写血缘边表、不做任务回调、不做采集；血缘事实只来自 OM 或业务库实时查询。

#### 5.3.2 行级血缘 = 列级血缘 + 按值反查

核心思路：行级关系不新增存储，而是“沿列级血缘，把目标行的键值映射成上游查询条件”。

```
resolve(table, rowKey, depth):
  node = {fqn: table, key: rowKey}
  if depth == 0: return node
  edges = OM.getUpstreamEdges(table)          # 列级血缘（含 sqlQuery）
  for edge in edges:
     mapping = keyMapping(edge)               # 目标键列 ← 上游列
     if mapping.empty:
        node.child(edge.from, status="NO_KEY_LINEAGE")
        continue
     sql = SELECT * FROM edge.from
           WHERE <mapping.sourceCols> = <rowKey.values> LIMIT n
     for r in readOnlyQuery(sql):             # 白名单 + 只读账号 + 行数/超时限制
        node.child(resolve(edge.from, keyOf(r), depth-1))
  return node
```

**键映射从哪里来**：
1. 优先从 `columnsLineage` 里找：目标键列对应的上游列（同列或改名映射）；
2. 找不到则查“键映射登记”（本服务唯一的血缘增量数据）：`表 → 键列 → 上游表.键列`，资产页可人工登记（Owner/管理员）；
3. 聚合（如 `ndvi_mean = avg(...)`）没有单值键映射 → 返回**参与集合**：按分组键反查全部参与行，UI 标注“聚合参与”；
4. 无主键表：用登记的业务唯一键；仍没有 → 不开放行级入口（只到列级），不猜。

**一致性保障（前端单独实现的原因）**：
- 行级路径由 `data-asset-web` 自己渲染，节点、边、样式与表/列级同一套组件，避免 OM 前端与自研页面两套交互；
- 反查是“当前数据快照”：目标行被更新/删除后再查会漂移。**历史一致性本期不考虑**，UI 与导出统一标注“按当前数据解析”及解析时间；后续如确需历史，再单独评估批次快照；
- 解析结果只缓存 60s（可按表失效），不持久化整条路径，避免形成第二套血缘。

**能力边界（验收口径）**：

| 加工类型 | 行级可追 | 说明 |
|---|---|---|
| 过滤 / 投影 / 改名 / 字典关联 | ✅ 精确 | 键列有列级映射 |
| 1:N 展开（JSON 拆行、UNNEST） | ✅ 精确 | 按展开键反查，返回多行 |
| JOIN 主键关联 | ✅ 精确 | 沿关联键反查 |
| GROUP BY 聚合 | ✅ 参与集合 | 无单值键，返回全部参与行 |
| 窗口函数 | ⚠️ 分区内参与行 | 需登记分区键 |
| 去重 / 采样 | ⚠️ 同组候选行 | 需登记去重键 |
| 无键且未登记业务键 | ❌ | 只到列级 |
| 非受控写入 | ❌ | 不在承诺范围 |

#### 5.3.3 本服务仅存的增量数据

| 数据 | 用途 | 维护方式 |
|---|---|---|
| 键映射 | 目标表键列 → 上游表键列（列级血缘不足以判断时） | **平台统一维护，当前免审批**；默认从列级血缘自动推导，缺失时由平台补齐 |
| 行级开关 | 哪些表开放“按行追踪”（性能与敏感数据保护） | 白名单式配置，默认关闭 |
| 内存缓存 | 表/列血缘与行级解析结果 | 进程内 Caffeine，60s TTL，可按键失效；不落库 |

前两类是**配置**，只补“列级血缘里没有的键语义”；缓存是**内存**，不复制 OM 血缘、不持久化行级路径。

#### 5.3.4 血缘页面自绘

| 项 | 选型/约定 |
|---|---|
| 图引擎 | 自绘 SVG（当前实现）；AntV X6 作为 P1 增强项保留，不引 OM 前端 |
| 布局 | 左→右分层（按边松弛求层号），同层按 fqn 排序；列头标注“上游 n / 当前表 / 下游 n” |
| 比例 | 默认原始比例 + 横向滚动（节点文字不缩）；“适应宽度”开关切换为整图缩放 |
| 层级切换 | 表级 / 列级 / 行级；行级从选中行发起 |
| 数据来源 | 表/列：OM 转发；行级：本服务按值反查结果 |
| 节点信息 | 表：分层色条 + 分层徽标 + 列数；列：类型 + 约束；行：键值 + **整行数据**（一条或多条），列与脱敏沿用该表浏览白名单，未授权的表只给键值并标注原因 |
| 边信息 | 手动边蓝色实线、解析边灰色实线、无列映射虚线；边上标注首条列映射与数量 |
| 交互 | 上下游 1–5 层、点击节点切换当前表、列映射标注开关、边明细表内编辑/删除 |
| 性能 | 行级按需展开（默认 3 层）；单次展开 ≤300 节点；每边反查 ≤20 行、5s 超时 |
| 导出 | 暂未实现（PNG / JSON 快照列入 P1） |

#### 5.3.5 手动血缘配置（写入 OM，不建副本）

血缘事实只有 OM 一处：手动配置同样走 OM 的 `PUT /api/v1/lineage`（`AddLineage.edge`），`data-asset` 只做校验、补全与审计。

| 项 | 约定 |
|---|---|
| 入口 | 血缘页“手动配置血缘”按钮 / 边明细表“编辑” |
| 权限 | 仅管理员（DS 角色或 `local` 模式的 admin）；非管理员返回 403 |
| 表校验 | 上下游 fqn 必须能在 OM 解析出 `id`，否则 404；不允许自环 |
| 列校验 | 列必须属于对应表（按 OM `columns` 比对），否则 400 |
| 写入内容 | `lineageDetails.sqlQuery`、`description`、`source=Manual`、`columnsLineage[{fromColumns,toColumn,function}]`；列名按 `{表fqn}.{列}` 补全，与 OM 自动解析的写法一致 |
| 覆盖语义 | 同一对表的边再次保存会整体覆盖该边的 `lineageDetails`（OM 行为），UI 已提示 |
| 删除 | `DELETE /api/v1/lineage/table/name/{from}/table/name/{to}`，删的是整条边（含列映射） |
| 审计 | `LINEAGE_WRITE` / `LINEAGE_DELETE`，记录用户、边、列映射数、耗时 |

**键映射登记**与**行级开关**是本服务仅有的两项血缘配置（对应 `key_mapping`、`row_switch` 两张表）：前者补“列级血缘里没有的键语义”，后者按表控制是否开放按值反查。二者都不复制 OM 的血缘。

#### 5.3.6 选表选字段可以直接查数据源（v1.19）

血缘编辑器原来只能在“OM 已采集的表”里选，未采集的表连不上血缘。现在两侧都可选来源：

| 来源 | 取表 | 取字段 | 说明 |
|---|---|---|---|
| OpenMetadata | `/data-asset/search`（已采集资产） | `/data-asset/columns` | 默认 |
| 数据源 | `/data-asset/datasource/{datasources,schemas,tables}` | `/data-asset/datasource/columns` | 直查业务库 `information_schema`，支持表名搜索 |

- 只读连接池、只读账号；只返回库/模式/表/列名与类型，**不返回数据行**，因此不受浏览白名单约束（白名单仍管 `/rows`）。
- 模式名先经标识符校验，再与 `schemas()` 白名单比对，`pg_catalog`/`information_schema` 等系统模式取不到。
- 保存血缘时若表还不在 OM：`OmTableRegistrar` 依次确保 database → databaseSchema → table（列类型按 `information_schema` 映射到 OM 的 `ColumnDataType`，主键从 `pg_catalog` 兜底取），再写血缘边；返回体带 `registered: [fqn...]`，审计 `LINEAGE_WRITE` 记录同名列表。
- 表在数据源里不存在时返回 400“数据源中没有找到表 …”，不会在 OM 里造一张空表。
- OM 的建表接口有两个实测硬约束：`CreateTable` **不接受** `fullyQualifiedName`（多传会 400 “Invalid request format”），且 `varchar/char/binary` 列必须带 `dataLength`（数值列带 precision/scale）；两者都已按 `information_schema` 的字段填好。
- OM 的表删除是软删：删掉后再按同名登记会 409“已存在”。这类冲突要显式提示，不要当成“表已就绪”。

### 5.4 权限与安全

| 面 | 做法 |
|---|---|
| 身份 | 委托 DS 登录，本服务不存密码 |
| 元数据可见性 | DS 项目/角色 → 资产范围映射，沿用 `da_om_principal_map`（`AUTH-INTEGRATION.md`） |
| 数据可见性 | 完全由 5.2 白名单决定 |
| 血缘可见性 | 行级默认只对开放了该表白名单的主体可见；整行数据的列与脱敏同样受该表浏览白名单约束，未授权时只输出键值 |
| 凭据 | 只读账号、Bot Token 走环境变量/配置中心，不落代码与前端 |
| 审计 | 浏览、导出、白名单变更、血缘采集失败四类全量留痕 |

### 5.5 治理功能（标签 / 分级分类 / 数据质量）

**统一原则**：治理数据只在 OM 编辑，`data-asset` 提供资产页内的操作入口与展示，不做本地副本；所有写操作经 `om-client` 调 OM API，并记本服务审计。

#### 5.5.1 数据标签

| 项 | 做法 |
|---|---|
| 标签体系 | OM `classifications` + `tags`（如 PII、Sensitivity、业务标签）；支持 `recognizers` 自动识别（手机号、身份证等） |
| 资产页操作 | 查看已打标签、按 classification 分组；新增/移除标签（PATCH 实体 `/tags`） |
| 批量操作 | 按搜索结果多选批量打标；走异步任务 + 逐条结果回执 |
| 搜索与过滤 | 复用 OM 搜索的 `tags` 过滤；标签变更后 OM 自动更新索引 |
| 权限 | 打标权限按 DS 角色控制（如仅数据 Owner/管理员可写），读权限跟随资产可见性 |

#### 5.5.2 分级分类与认证

| 项 | 做法 |
|---|---|
| 分类分级 | OM Classification（可建“敏感级别”分类，如 L1–L4），与标签共用一套；支持列级打标 |
| 分级校验 | 资产详情展示“分级/敏感级别”徽标；未分级资产进入治理看板（可按域/负责人统计） |
| Tier | OM 内置 Tier 1–5，资产页可编辑 |
| Certification | 认证状态（如“已认证”）与到期时间，编辑走实体 PATCH |
| 审批（可选） | 2.1 的 `changeRequests` 可承载“分级变更审批”；若不上审批，则记录操作人与时间到本服务审计 |
| 自动识别 | OM `recognizers` 可对列做敏感识别并产出打标建议，本服务展示“待确认建议”列表 |

#### 5.5.3 数据质量

| 项 | 做法 |
|---|---|
| 定义 | 在 OM 建 Test Suite / Test Case（内置定义：行数、空值率、唯一性、取值范围、时效性等）；资产页提供“新建测试”入口 |
| 执行 | **路线 B（已定）**：`data-asset` 只读查询器执行，结果回写 OM；DS 定时调度 |
| 结果 | 展示最近一次结果、通过率、趋势；数据：`GET /v1/dataQuality/testCases/testCaseResults/search/latest` |
| 事故 | 2.1 新增 `testCaseIncidentStatus` 与事故分组；资产页展示“当前事故/最近事故”，可跳 OM 处理 |
| 维度 | 2.1 的 `dataQuality/dimensions` 可按维度（完整性、准确性等）汇总；用于质量看板 |
| 告警 | 由 OM 的告警/通知能力负责；本服务只展示状态，不重复实现告警通道 |
| 失败样本 | OM 支持 `failedRowsSample`；展示前按列白名单裁剪，避免泄露敏感列 |

**质量看板（P1 交付）**：按域/服务/表展示 “测试通过率、未覆盖资产数、当前事故数、最近 7 天趋势”，数据全部来自 OM，不在本地落结果表。

### 5.6 DS 侧入口（已确认：内嵌页面路由）

| 项 | 约定 |
|---|---|
| 形式 | DS UI 新增 `/asset/catalog`（资产目录 + 数据浏览）与 `/asset/lineage`（表/列血缘），侧边栏“数据资产”分组进入；**不使用 iframe、不再由 data-asset 托管控制台** |
| 改动范围 | DS UI 路由、菜单、两个页面与一个 service 模块；后端仍由 `data-asset` 提供 API |
| 升级成本 | DS 升级时需重放 UI 补丁（菜单/路由/页面/服务模块） |
| 数据来源 | Vite 开发代理 `/data-asset` → 12347；生产同源 Nginx 转发 |
| 会话 | 复用 DS 登录态：页面带上 DS `sessionId`，服务端 `ds` 模式校验 |
| 版式 | 三个页面共用一套**主题令牌**：`views/data-asset/theme.ts` 的 `useAssetTheme()` 从 `store/theme` 的 `darkTheme` 与 `themes/modules/{light,dark}` 取色，页面内不写字面色值。头部统一为 `t.banner` 横幅（左：图标块 + 页面标题 + 一行说明；右：搜索与本页操作按钮；下方 `repeat(auto-fit, minmax(172px,1fr))` 的 KPI 卡 `t.tile`，每卡为“图标 + 大数字 + 名称 / 第二行辅助口径”）。标签/筛选不再是一行行文字，而是 `t.cardStyle` 分组面板：组名 + 命中数徽标 + 标签按钮，选中态为主色实心（`t.primary` / `t.onPrimary`）带勾。语义色一律走 `t.tone('info\|teal\|success\|warning\|error\|purple\|neutral')`（浅色 AntD 浅底，深色同色系半透明 + 亮文字），分层色带 `t.hue(layer)`。新增页面照此引用 |

## 六、接口清单（`data-asset-service`）

| 方法 | 路径 | 用途 |
|---|---|---|
| GET | `/data-asset/tree` | 目录树 |
| GET | `/data-asset/search` | 资产搜索（代理 OM `/search/query`，OpenSearch 后端） |
| GET | `/data-asset/detail` | 资产详情（含标签/分级/质量摘要） |
| GET | `/data-asset/columns` | 字段（按白名单裁剪 + 脱敏标记 + 列级标签） |
| GET/PUT | `/data-asset/governance/tags` | 资产标签读取/打标（代理 OM） |
| GET/PUT | `/data-asset/governance/classification` | 分级、Tier、Certification 读取/更新（代理 OM） |
| GET | `/data-asset/governance/recognizer/suggestions` | 敏感识别建议列表（OM recognizers） |
| GET/POST | `/data-asset/governance/quality/tests` | 质量用例读写（代理 OM testCases） |
| POST | `/data-asset/governance/quality/run` | 执行测试套件/用例并回写 OM（路线 B） |
| GET | `/data-asset/governance/quality/results` | 最近结果/趋势（OM testCaseResults） |
| GET | `/data-asset/governance/quality/dashboard` | 质量看板汇总（通过率、覆盖、事故） |
| GET | `/data-asset/governance/quality/incidents` | 当前/最近事故（2.1 incidentStatus） |
| GET | `/data-asset/rows` | 分页浏览（页码或游标） |
| POST | `/data-asset/rows/export` | 异步导出 |
| GET/POST/PUT/DELETE | `/data-asset/browse/entries` | 白名单 CRUD 与批量导入 |
| POST | `/data-asset/browse/entries/{id}/approve` | 审批 |
| GET | `/data-asset/lineage/table` | 表/列血缘（代理 OM，结构归一；节点带分层标签与列数，边带 `source`） |
| POST | `/data-asset/lineage/manual` | **手动血缘**：写 OM `PUT /api/v1/lineage`（表边 + `columnsLineage`），仅管理员 |
| DELETE | `/data-asset/lineage/manual` | 删除一条血缘边（OM `DELETE /lineage/table/name/{from}/table/name/{to}`） |
| GET | `/data-asset/lineage/row/resolve` | 从目标行解析上游路径（列级血缘 + 键映射 + 按值反查） |
| GET | `/data-asset/lineage/row/path` | 两行之间的路径（未实现，P3 收尾） |
| GET/POST/DELETE | `/data-asset/lineage/keys` | 键映射登记（目标键列 → 上游键列），`key_mapping` 表 |
| GET/PUT | `/data-asset/lineage/row-switch` | 按表的行级追踪开关，`row_switch` 表 |
| GET | `/data-asset/datasource/datasources` | 已配置的业务数据源（名称 + 库名） |
| GET | `/data-asset/datasource/schemas?datasource=` | 可 introspect 的模式（系统模式已排除） |
| GET | `/data-asset/datasource/tables?datasource=&schema=&q=&limit=` | 数据源中的表/视图（含完整 fqn） |
| GET | `/data-asset/datasource/columns?datasource=&schema=&table=` | 数据源字段（名称/类型/可空/主键） |
| GET | `/data-asset/datasource/exists?fqn=` | 该表是否已被 OM 采集 |
| POST | `/data-asset/datasource/register?fqn=` | 按数据源元数据把表登记进 OM（库/模式缺失时兜底创建），仅管理员 |
| GET | `/data-asset/audit/browse` | 浏览与导出审计 |
| GET | `/data-asset/admin/health` | OM / DS / 元数据库连通性（本服务无索引可重建） |

OM 侧调用注意（2.1 实测）：`getLineage` 的 `upstreamEdges`/`downstreamEdges` 为对象（键 `from--->to`）且边带顶层 `columns`；`lineageSettings` 的 `config_type` 小写开头、PUT 需回传完整 `config_value`；列级展示需 `lineageLayer=ColumnLevelLineage`。这些差异全部封在 `om-client` 里。

## 七、部署与资源

| 档 | 组成 | 常驻内存 | 容器数 |
|---|---|---|---|
| **默认档（推荐）** | DS + data-api + **data-asset** + OM + OpenSearch | 现状 + 约 **1 GB**（data-asset） | 现状 +1 |
| 精简档（不推荐） | 默认档去掉 OM，标签/分级/质量全部自建 | 省约 2.9 GB | 现状 +1，OM/OS −2 |
| 双源档（不推荐） | 默认档，但表/列血缘改存 OM | 同默认档 | 同默认档 |

**分阶段部署方式（已确认）**：

| 阶段 | 方式 | 说明 |
|---|---|---|
| 当前本地阶段 | **jar 包直接运行**：`java -jar data-asset-service.jar --spring.profiles.active=local` | 配置放 `application-local.yaml`；可选 nssm/Windows 服务托管，不作为交付前提 |
| 最终形态 | **K8s**：Deployment + Service + ConfigMap + Secret + Ingress | 2 副本起步（服务无状态）；readiness/liveness 用 `/data-asset/admin/health`；内存缓存每 Pod 独立，60s TTL 无需 sticky session |
| 路由 | Nginx/Ingress：`/data-asset/**` → 服务；`/asset-console/**` → 前端静态资源 | 与 DS、OM 同域，便于共享会话 |

**运维要求（实测教训）**：2026-11-02 检查时发现机器重启后 DS、data-api、OM、OpenSearch **全部停摆**（8585/9200/12345/12346 均无监听，无 java 进程），因为目前都是脚本方式拉起、未注册为服务。上线时必须：① 本地阶段至少用 nssm/systemd 托管，K8s 阶段用 Deployment；② `data-asset` 启动自检 OM、OpenSearch 与元数据库连通性；③ 行级解析失败可重试并记审计；④ 提供 `/data-asset/admin/health`。

`data_asset` 库只存**增量配置与记录**：白名单、键映射登记、行级开关、用户属性、审批、审计，容量在 **MB 级**；**不存元数据、不存血缘边、不存浏览结果**。缓存全部在进程内存（Caffeine），不引入 Redis 等中间件；行级解析是查询时计算，不随数据量增长。

## 八、分期实施

| 阶段 | 范围 | 验收标准 | 预估 |
|---|---|---|---|
| P0 骨架 + 目录 + 白名单 + 数据浏览 | 新服务、控制台骨架、OM 元数据代理与缓存、OM 搜索代理、白名单三入口与**默认开启的审批流**、只读查询器、审计、服务化自启 | 未登记/未审批的表访问返回 403 且留审计；审批通过后 ≤30s 生效、禁止自批、驳回留痕；已授权表可分页、敏感列掩码、导出受控；OM 不可用时目录降级但浏览可用；机器重启后服务自恢复 | 后端 9 / 前端 5 人日 |
| P1 标签/分级 + 血缘增强 | 标签与分级代理读写、质量结果展示、血缘交互增强（节点下钻、路径高亮、X6 升级） | 资产页可打标/改分级并即时反映到 OM；一张表能看上下游与列级连线；`UNION/CTE/JOIN` 列映射正确率 ≥ 90% | 后端 8 / 前端 11 人日 |
| P2 质量深化 | 质量看板、事故与失败样本（脱敏）、路线 B 执行器（8–10 个内置定义）、DS 调度对接 | 看板数据与 OM 一致；测试结果可回写并在 OM 展示；白名单链路质量覆盖可查 | 后端 9 / 前端 4 人日 |
| P3 行级血缘 | 键映射推导与登记、按值反查解析（含绑定传播）、节点级权限、路径查询、覆盖率看板 | 开启行级的表抽样 100 条目标行，**有键映射链路 100% 追到源行或参与集合**；无映射/无权限节点明确标注；单次解析 ≤3s（键列有索引） | 后端 8 / 前端 5 人日 |
| P4 治理增强与上云 | 资产热度与引用统计、多源接入、K8s 部署与 HPA | 新数据源接入 ≤3 人日/源；K8s 双副本滚动发布、健康检查通过 | 6 人日 |

## 九、风险与待确认

| 项 | 影响 | 处置 |
|---|---|---|
| 中文分词能力不足 | OM 搜索走 OpenSearch，但集群未装中文分析插件，中文资产名/描述相关性一般 | 给 OpenSearch 装 `analysis-smartcn` 或 IK（需匹配 3.4.0 并重启）；短期接受“找得到”即可 |
| OM 成为核心依赖 | OM 不可用时标签/分级/质量展示降级 | `data-asset` 仅保留进程内 60s 缓存；治理区域显示“暂不可用”，OM 恢复后自动回填 |
| 路线 B 断言覆盖有限 | 只有首期 8 个定义，新断言需开发 | `tableCustomSQLQuery` 兜底；按需扩展定义；与 OM 内置定义保持同名同参数 |
| 机器重启后服务全停 | 实测 2026-11-02 DS/OM/OpenSearch 全部停摆 | 五个组件全部服务化（systemd/Windows 服务/K8s），并加启动自检 |
| 行级解析断链 | 列级血缘缺键列映射，或表无主键/业务键 → 只能到列级 | 资产页登记键映射；覆盖率看板；UI 明确标注“不可逐行”，不做猜测 |
| 按值反查性能 | 大表键列无索引时反查慢甚至扫表 | 行级开关按表控制；强制键列有索引；单次 `LIMIT` + 3s 超时；结果 60s 缓存 |
| 历史不可复现 | 反查基于当前数据，源行更新/删除后路径漂移 | **本期不处理**；UI 与导出标注“按当前数据解析”及时间 |
| 大表即席浏览 | 拖垮业务库 | 只读账号 + 只读副本 + 游标翻页 + 行数/字节/超时三重上限 |
| DS 侧无入口 | 不改 DS 源码就没有 DS 菜单项 | 入口为独立控制台 `/asset-console`；若要 DS 侧边栏外链，属一处薄改动，需单独确认 |
| OM 仍是 2.1.0-rc1 | 接口结构可能再变 | 锁 2.1.0 GA 后再升级；所有 OM 调用封在 `om-client`，只改适配层 |

## 十、已确认决定与待办

### 10.1 已确认（2026-11-02）

| # | 决定 | 落点 |
|---|---|---|
| 1 | **保留 OM 作治理主库** | 标签/分级/质量/术语/域/产品/表列血缘/搜索全部在 OM |
| 2 | **首批开放 ODS→DWD 关键明细** | 浏览+行级：`ods_satellite_scene`、`dwd_ndvi_tile`；维度只读：`dim_*`；`dws/ads` 仅元数据可见 |
| 3 | **键映射由平台统一维护，当前免审批** | 默认从列级血缘推导，平台补齐缺口 |
| 4 | **历史一致性暂不考虑** | UI 与导出标注“按当前数据解析”及时间 |
| 5 | **DS 内嵌页面路由** | `/asset/catalog` 与 `/asset/lineage`，侧边栏“数据资产”分组；页面在 DS UI，data-asset 只提供 API |
| 6 | **部署形态** | 当前本地 `java -jar`；最终 K8s（2 副本、无状态、ConfigMap/Secret） |
| 7 | **质量执行采用路线 B** | `data-asset` 执行 + 回写 OM；首期 8 个定义，DS 定时调度 |
| 8 | **首期 8 个质量断言够用** | 行数、非空、唯一、值域、长度、正则、列数、自定义 SQL |
| 9 | **白名单审批默认开启** | 审批人为资产 Owner（无 Owner 由平台管理员）；禁止自批；YAML 基线视同已审批 |

### 10.2 待办

无待确认项，进入实施。实施前的前置动作见 11.4（覆盖率脚本、服务化、会话共享方式）。

## 十一、可行性复核与优化方向（2026-11-02）

### 11.1 总体结论

架构可行：`data-asset` 只做转发与增量、血缘不自建存储、前端独立自绘，这几条在现有 OM 能力上成立。但有 **三个硬约束**必须先接受，否则“行级全覆盖”的预期会落空：

1. **行级可追范围 = 列级血缘里有键映射的范围**。列级血缘缺失的边（实测 `dim_satellite → dwd_ndvi_tile` 的 `columnsLineage` 为空）无法逐行反查；只能标记“不可追”或人工补键映射。
2. **反查读的是当前数据**，本期不做历史复现；UI 与导出必须标注“按当前数据解析”。
3. **质量路线 A 在当前环境不可用**：实测 `pipeline_service=0`、`ingestion_pipeline=0`，OM 发行版不含 Python ingestion，本机 Python 为 Store 占位。必须以路线 B（自建执行器 + 回写）为主。

### 11.2 可行性问题清单与修正

| 问题 | 证据 | 影响 | 修正（已写入方案） |
|---|---|---|---|
| 质量路线 A 缺运行时 | 0 pipeline service / 0 ingestion pipeline；发行版无 Python | 原计划“零开发”不成立 | 2.6 改为**路线 B 为主**，首期实现 8–10 个内置定义，DS 调度，结果回写 OM |
| 维度表/JOIN 断链 | 实测 `dim → dwd` 边 `columnsLineage=[]` | 只能追主链路，维度关联不可追 | 引入**键绑定传播**：先取目标行，沿列映射携带键值；无映射边标记 `NO_KEY_LINEAGE`；维度表按登记外键补映射 |
| 目标行缺上游连接键 | 目标行不一定包含 JOIN 键 | 单次反查查不到上游 | 首期只支持“目标行含键”的链路；两阶段反查（先查中间表取键）列入 P3 增强 |
| 聚合无单值键 | `ndvi_mean←ndvi` 等映射无分组键 | 无法定位单条源记录 | 返回**参与集合**（按 GROUP BY 键反查），UI 标注“聚合参与”；需从 SQL 解析分组键或人工登记 |
| 上游无权限导致信息泄露 | 用户可能只被授权目标表 | 反查会读到未授权上游行 | **节点级权限**：无权限节点只显示“存在上游、无权限查看”，不返回值；所有反查走白名单与脱敏 |
| 搜索后过滤分页失真 | OM 返回后再按可见性过滤 | 总数与页大小不一致 | 先 over-fetch，再过滤；响应标注“已按可见性过滤”；后续可在 OM 实体上加可见性字段 |
| 认证传输未定义 | SPA 与 DS 需共享身份 | 可能出现未登录/重复登录 | 明确：同域部署共享 DS 会话 Cookie；跨域则用短期 token 交换，不把 OM Token 下发浏览器 |
| 性能不可控 | 每跳 1 次 OM + 1 次 SQL，深度 3、扇出 10 可达上百次 | 页面超时 | OM 边缓存 5 min；批量 `IN` 查询；每层并发 ≤4；单次展开 ≤300 节点；总超时 3s |
| 定位与实现冲突 | 5.1.1/5.1.2 仍写自建 PG/OS 索引 | 实施误解、多做一套索引 | 已标注**本期不实现，仅设计预留** |
| 工作量偏乐观 | P1 前端要覆盖标签/分级/质量展示 + X6 三档血缘 | 排期风险 | P1 前端 8→**11**；P3 后端 6→**8**、前端 4→**5**；P2 后端 6→**9**（路线 B） |

### 11.3 优化方向（按优先级）

**P0：先量化“可追覆盖率”，再决定开放范围**
- 写一个离线脚本：遍历 OM 的全部血缘边，检查每条边的 `columnsLineage` 是否包含目标表键列的上游映射；输出“可逐行 / 仅参与集合 / 不可追”三类清单。
- 以覆盖率决定首批开放表，避免上线后才发现大量链路追不到。
- 验收指标：`可追边数 / 总入边数`（目标 ≥80%），并在资产页展示单表覆盖率。

**P1：行级算法分三阶段交付，不要一次做完**
1. 单跳键映射（目标键列 ← 上游键列，目标行含键值）——覆盖最简单也最常见的明细链路；
2. 绑定传播（多跳 JOIN，把已取到的键值带到下一跳）——需要两阶段反查；
3. 聚合参与集合（GROUP BY / 窗口分区）——需要 SQL 解析或人工登记分组键。
每阶段都可独立上线，失败时明确降级到上一阶段，不静默返回空。

**P2：质量先做“能跑、能回写、能看趋势”**
- 复用 OM 已内置的 25 个定义中的 8 个：行数区间、非空、唯一、值域、正则、自定义 SQL、表差异、增量行数；
- 由 DS 定时调度 `data-asset` 执行器，结果 `POST /testCaseResults/{fqn}` 回写；
- 告警仍由 OM 负责，不重复实现通知通道。

**P3：把“全覆盖”变成可运营的指标**
- 资产页显示：键映射覆盖率、可追链路数、最近解析失败原因 TopN；
- 键映射缺失进入治理待办（Owner 处理），而不是藏在代码里。

**P4：性能与安全兜底**
- 行级解析必须带：键列索引校验（无索引直接拒绝）、`LIMIT`、总超时、并发上限、60s 缓存；
- 所有反查语句参数化，键列与操作符白名单，禁止拼接自由条件；
- 审计记录：谁、看了哪一行、沿哪条路径、是否发生权限裁剪。

### 11.4 复核后的结论

- **架构层面可行**，且符合“只做增量 + 转发”的定位；
- **功能层面需降预期**：行级血缘是“按列级血缘可推导的范围”全覆盖，不是物理意义上的全量逐行；
- **排期层面需加缓冲**：P0–P3 合计约 **59 人日**（后端 34 / 前端 25，含联调），P4 另计 6 人日；
- **前置动作**：先跑覆盖率脚本、先做服务化、先确认 DS 会话共享方式，再进入 P1 开发。

## 十二、实施进度（2026-11-04）

### 12.1 P0 已完成

| 项 | 状态 | 证据 |
|---|---|---|
| 独立模块 `data-asset/` | ✅ | 独立 pom（Spring Boot 2.7.18 + JDK 11），不依赖 DS/OM 源码 |
| 服务启动 | ✅ | 端口 12347；`/data-asset/admin/health` 返回 `configDb=ok`、`openMetadata=2.1.0-rc1`、`agri_satellite_pg=ok` |
| 目录 / 搜索 / 详情 / 字段 | ✅ | 代理 OM；目录返回 8 个样例资产，搜索 `dwd_ndvi` 命中 1 条 |
| 白名单 CRUD + 默认审批 | ✅ | 创建为 `PENDING`；审批后 `ACTIVE`；自批返回 400；驳回无理由返回 400 |
| 只读数据浏览 | ✅ | 8 张样例表均已登记并审批；`rows` 均返回数据；未登记表仍返回 403（预期） |
| 列脱敏 | ✅ | `cloud_cover_pct` 展示为 `******` |
| CSV 导出 | ✅ | 需条目开启；返回表头 + 数据行 |
| 审计 | ✅ | 记录 200/403、用户、目标、耗时、IP |
| 只读数据库账号 | ✅ | 新建 `data_asset_ro`：仅 SELECT、`default_transaction_read_only=on`；实测 UPDATE 被拒 |
| DS 页面路由 | ✅ | `/asset/catalog` 卡片化概览（渐变头部 + 统计卡 + 数据产品卡 + 资产卡片 + 抽屉详情）、`/asset/lineage`、`/asset/governance`；截图 `data-asset/docs/ds-asset-*.png` |
| 分层/分级/分类管理 | ✅ | 新建 `DataLayer` 分类（ODS/DWD/DWS/ADS/DIM）；分级用 `AgriDataSensitivity`，认证用 `Certification`，重要性用 `Tier`；均实时写回 OM |
| 治理代理接口 | ✅ | `/data-asset/governance/overview`、`/assets`、`/asset`、`/asset/tag-set`、`/asset/domain`、`/asset/dataProduct`、`/asset/glossaryTerm` 及分类/标签/域/产品/术语创建 |
| 血缘代理 | ✅ | `/data-asset/lineage/table` 归一化 OM `getLineage`；返回 5 节点 / 5 边与列映射 |
| DS 鉴权模式 | ✅ | `ASSET_AUTH_MODE=ds` 独立端口验证：无 session 返回 401，带 DS `sessionId` 返回 200 |
| 单元测试 | ✅ | `QueryBuilderTest` 5/5 通过 |

### 12.2 未完成（按计划推进）

- P1：质量代理（定义/结果/事故展示）、血缘图导出（PNG/JSON 快照）、路径高亮、X6 升级。
- P2：质量执行器（路线 B，8 个定义）+ DS 调度 + 回写 OM。
- P3 剩余：两行之间的路径查询 `/lineage/row/path`、键映射覆盖率看板。
- P0 剩余：YAML 白名单基线、导出水印。

### 12.5 血缘已完成（2026-11-04，v1.12）

| 项 | 状态 | 实测证据 |
|---|---|---|
| 服务栈 | ✅ | OM 2.1.0-rc1（8585）、OpenSearch 3.4.0（9200）、DS 3.4.3（12345）、data-asset（12347）、DS UI（5173）；`/admin/health` 三项均 `ok` |
| 手动血缘写入 | ✅ | `scripts/seed-lineage-demo.mjs` 经接口写 3 条边：ODS→DWD（3 列映射）、DWD→DWS（3 列）、DWS→ADS（2 列），均 200 |
| 手动血缘校验 | ✅ | 未知列 400、自环 400、未知表 404、非管理员 403（`requireAdmin`） |
| 血缘删除 | ✅ | 删除临时边 200；DWD 上游由 5 边降为 4 边（`verify-lineage.mjs`） |
| 键映射登记 | ✅ | 登记 `ads.region_id←dws.region_id`、`ads.report_date←dws.stat_date`；list/delete 200 |
| 行级开关 | ✅ | 4 张表开启；未开启表解析返回 `ROW_LINEAGE_DISABLED` |
| 行级按值反查 | ✅ | ADS 行 `region_id=R001 & crop_code=COTTON`，depth=3 → 5 节点、122–181ms；ADS→DWS 走登记映射命中 1 行，DWS→DWD 仅 `region_id` 可映射（`crop_code` 标为未映射）返回 3 行“聚合参与集合”，DIM 边标 `NO_KEY_LINEAGE` |
| 血缘页改版 | ✅ | 工具栏卡（搜索 + 上/下游层数 + 统计）+ 资产快选条 + 四个页签（血缘图 / 血缘边与列映射 / 键映射与行级开关 / 行级血缘）；自绘 SVG 分层带、手动边蓝色、无列映射虚线、列映射标注开关、适应宽度开关 |
| 血缘页数据核对 | ✅ | 图：5 节点 / 4 边 / 9 列映射 / 4 手动；边明细表 4 条边；配置页 2 条键映射 + 4 条开关；浏览器 console 无报错 |
| 行级血缘页面 | ✅ | 在 `/asset/lineage` → 行级血缘页签实测：`dwd_ndvi_tile` 按 `region_id=R001` 反查，depth=3 → 3 节点 / 399ms，链路 DWD→ODS（命中 2 行、聚合参与集合）→DIM（键链路断开），同键节点标“成环跳过” |
| 行级整行数据 | ✅ | 反查在同一条 SELECT 里带上白名单可见列：DWD 根节点 3 行 × 9 列、每个 ODS 节点 1 行 × 11 列（`cloud_cover_pct` 显示 `******`）；页面按节点“查看整行 / 收起整行”展开，默认展开前两层；未授权表输出 `rowNote` 并只给键值 |
| 审计 | ✅ | 新增 `LINEAGE_WRITE`、`LINEAGE_DELETE`、`KEY_MAPPING_WRITE/DELETE`、`ROW_SWITCH`、`ROW_LINEAGE`；`audit_event.action` 由 16 字符放宽到 32（`schema.sql` 内 `alter`） |
| 错误体统一 | ✅ | `ResponseStatusException` 也返回 `{code,message}`，前端能显示“表不存在/无权限”等原因 |
| 主题令牌 | ✅ | 三个资产页面共 51 + 45 + 12 处写死色值全部改为 `useAssetTheme()` 取值，页面内已无 hex 字面量；深色实测：卡片 `#1f1f1f`、语义徽标半透明底 + 亮文字、SVG 色带与连线跟随主题（`fill/stroke` 无 `[object Object]`）；浅色与原视觉一致；切深色再切回浅色，血缘图仍为 5 节点 / 4 边、边表 4 行、行级页签可用，console 无报错 |
| 头部与标签加厚 | ✅ | 三页头部统一为横幅 + KPI 卡（`t.banner`/`t.tile`，栅格 `auto-fit minmax(172px,1fr)`）：目录 8 资产·已分层 8/8 / 1 产品·关联 7 / 1 域·分类 7 术语 4 / 3 已认证；血缘 5 表 / 4 边（手动 4·解析 0）/ 9 列映射（3/4 条边已配列）/ 4 行级可追·键映射 2；治理 1 域 / 1 产品 / 7 分类 / 23 标签 / 4 术语。标签区改为分组面板（组名 + 命中数徽标 + 选中主色实心带勾）。说明行 `nowrap + ellipsis` 实测高度 19.2px（单行）；深色实测横幅走深色渐变、KPI 卡 `rgba(255,255,255,.06)`；三页 console 无报错，页面内 hex 字面量仍为 0 |
| 启动脚本 | ✅ | `deploy/local/start-local.ps1` 一次拉起六个服务：冷启动实测 DS/data-api/data-asset/UI 全部 `[ ok ]`，`status.ps1` 六行均 200；再跑一次全部 `[skip]`；`-OnlyPorts 12346` 只停 data-api（其余 5 个不受影响）后重跑，仅补齐该服务 |
| 血缘编辑取数 | ✅ | 数据源侧：`datasources/schemas/tables/columns/exists` 全 200（8 张表、ads_crop_health 7 列且主键 report_date,region_id,crop_code 正确）；系统模式与非法标识符均 400；OM 侧写边回归通过（新增 dim_crop→dws_crop_growth 带 1 列映射，随后删除，图恢复 3 条边） |
| 自动登记端到端 | ✅ | 经用户同意在 agri_sat 建临时探针表验证：`POST /datasource/register` 返回 `created=true, columns=4`；未先登记时直接 `POST /lineage/manual` 也成功，`registered:["...t_probe_lineage2"]` 且边带 1 列映射；OM 内列类型正确（`probe_id BIGINT[PRIMARY_KEY]`、`region_id VARCHAR(16)`、`metric NUMERIC`、`note TEXT`）。验证后已清理：删临时血缘边、软删 2 个 OM 探针实体、DROP 3 张 PG 探针表，数据源回到 8 张表、dws 血缘回到 3 条边 |
| 启动脚本缺陷修复 | ✅ | ① OM 启动等待 60s→300s（实测冷启动约 2 分钟，旧脚本会误报失败）；② `stop-local.ps1` 的 `$Ports` 参数被同名内部变量 `$ports` 覆盖（PowerShell 变量名不区分大小写），改名 `-OnlyPorts`；③ `SPRING_DATASOURCE_URL` 从 data-api 漏给 data-asset，导致后者把配置表建进 DS 的 `dsdb`、表现为“白名单全空 + 数据浏览 403”——已改为 `finally` 清除 + data-asset 显式绝对路径 URL，并删掉 `dsdb` 里误建的 6 张表（当时仅 3 行审计，配置数据一直在 `data-asset/data/` 未丢） |

### 12.3 当前运行方式

```powershell
# 一条命令起全栈（幂等：已监听的端口会 [skip]，可反复执行补齐）
cd deploy\local
.\start-local.ps1        # OpenSearch -> OM -> DS -> data-api -> data-asset -> DS UI
.\status.ps1             # 端口 / PID / HTTP 体检
.\stop-local.ps1 -OnlyPorts 12346   # 只重启某个服务；-DryRun 先看
```

```bash
# Linux 形态：DS + data-api + data-asset（OM/OpenSearch 需另行部署）
JAVA_HOME=/opt/jdk-11 DS_HOME=... ST_HOME=... ./deploy/local/start-local.sh
```

页面在 DS UI（Vite 5173）：`/asset/catalog`、`/asset/lineage`、`/asset/governance`；
API 开发入口 `http://127.0.0.1:12346/magic/web`。认证 local 模式：请求头 `X-Debug-User`（默认 admin）。

### 12.4 已知差异

- 当前 `data-asset` 覆盖 P0 + 治理代理 + 血缘（表/列/手动/键映射/行级）；质量（P2）尚未实现。
- 血缘图为自绘 SVG，未使用计划中的 AntV X6；节点数增大时仍靠横向滚动，虚拟渲染待 P1 补。
- 行级反查按“值恒等”假设映射键：加工中发生格式改写（如 `tile_id = scene_id || '_' || 序号`）的列不能直接当键用，需在列级血缘里选真正保持原值的列，否则返回 `ROW_NOT_FOUND`。
- P0 控制台为轻量静态页面（原生 HTML/JS），用于快速验证；P1 再按计划升级为 Vue3 + AntV X6。
- `local` 模式信任调试头，仅限本机；生产必须切 `ASSET_AUTH_MODE=ds`。
- 配置库当前为 H2 文件（`data-asset/data/`）；K8s 阶段改 PostgreSQL 并写 Secret。
- 页面已内嵌 DS UI（`/asset/catalog`、`/asset/lineage`、`/asset/governance`），`data-asset` 不再托管控制台；`VITE_APP_DATA_ASSET_URL` 仅用于生产环境 data-asset API 的 base URL。
- 分层使用 `DataLayer` 分类（ODS/DWD/DWS/ADS/DIM）；认证在 OM 中是独立字段（`certification.tagLabel`），不能按普通 tag 写入，已单独适配。
- 业务库账号 `data_asset_ro` 的口令为本地初始值，上线前必须轮换并放入 Secret。
