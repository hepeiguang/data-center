# data-asset（数据资产服务）

独立的数据资产控制台与数据浏览服务。**不修改 DolphinScheduler / OpenMetadata / magic-api 源码**，只通过公开 REST 接口集成。

方案文档：`../dolphinscheduler/dolphinscheduler-data-api/docs/DS-OM-ASSET-LINEAGE-PLAN.md`（v1.3）

## 定位

| 能力 | 实现 |
|---|---|
| 资产目录、搜索、标签、分级、质量 | 代理 OpenMetadata（只读 + 代理写） |
| 表级 / 列级血缘 | 代理 OpenMetadata（不自建存储） |
| 数据浏览（白名单） | 本服务只读查询器 |
| 行级血缘 | 基于 OM 列级血缘 + 按值反查（P3，规划中） |
| 审计 | 本服务记录 |
| 配置数据 | 仅白名单、审批、键映射、行级开关、用户属性（MB 级） |

## 运行（本地阶段：jar）

```bash
# JDK 11 + Maven 3.9
export JAVA_HOME=D:/HPG/workspace/LHYF/tools/jdk-11/jdk-11.0.2
cd data-asset
"$JAVA_HOME/bin/java" -version

# 打包
"D:/HPG/software/apache-maven-3.9.6/bin/mvn.cmd" -s D:/HPG/workspace/LHYF/tools/maven-settings-aliyun.xml \
  -DskipTests package

# 运行（local 模式：信任 X-Debug-User 头，仅开发用）
"$JAVA_HOME/bin/java" -jar target/data-asset-service.jar
```

页面不在本服务中托管。启动 DS UI（`dolphinscheduler-ui`，Vite 5173）后访问：
`http://127.0.0.1:5173/asset/catalog`（资产目录）与 `/asset/lineage`（数据血缘）。
Vite 已把 `/data-asset` 代理到本服务。

K8s 阶段：同一 jar 打进镜像，`Deployment` 2 副本，`readiness/liveness` 用
`/data-asset/admin/health`，配置走 ConfigMap/Secret，无状态、无需 Redis。

## 配置

`src/main/resources/application.yaml`：

| 配置 | 说明 |
|---|---|
| `data-asset.om.base-url` | OpenMetadata 地址（默认 `http://127.0.0.1:8585`） |
| `data-asset.om.token` | Bot/服务 Token；为空时用管理员账号登录换取（仅本地） |
| `data-asset.auth.mode` | `local`（信任 `X-Debug-User`）或 `ds`（校验 DS `sessionId`/`token`） |
| `data-asset.auth.ds-base-url` | DS API 地址 |
| `data-asset.datasources.*` | 业务库只读连接（url/user/password/pool-size） |
| `data-asset.query.timeout-seconds` | 查询超时（默认 60s） |

配置库默认 H2 文件：`./data/data-asset`（首次启动自动建表）；生产可改 PostgreSQL。

## 认证

- `local`：请求头 `X-Debug-User`，缺省 `admin`；**仅本地开发**。
- `ds`：请求头 `sessionId`（或 `token`），服务端调用
  `GET {ds-base-url}/users/get-user-info` 验证，不落 OM Token 到浏览器。

## 接口（P0 已实现）

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/data-asset/search?q=&size=` | 资产搜索（代理 OM） |
| GET | `/data-asset/tree` | 目录树 |
| GET | `/data-asset/detail?fqn=` | 资产详情 + 是否开放浏览 |
| GET | `/data-asset/columns?fqn=` | 字段（可见/脱敏标记） |
| GET | `/data-asset/rows?fqn=&page=&size=&filter=col:op:value` | 白名单分页浏览 |
| POST | `/data-asset/rows/export` | CSV 导出（需条目开启） |
| GET/POST | `/data-asset/browse/entries` | 白名单查询/申请 |
| POST | `/data-asset/browse/entries/{id}/approve\|reject\|disable` | 审批 |
| GET | `/data-asset/audit/browse?limit=` | 浏览/导出审计 |
| GET | `/data-asset/admin/health` | 健康检查 |
| GET | `/data-asset/lineage/table?fqn=&upstreamDepth=&downstreamDepth=` | 表/列血缘（节点带分层标签与列数，边带来源与列映射） |
| POST | `/data-asset/lineage/manual` | 手动血缘：写 OM `PUT /api/v1/lineage`（表边 + 列映射），仅管理员 |
| DELETE | `/data-asset/lineage/manual?fromFqn=&toFqn=` | 删除一条血缘边 |
| GET | `/data-asset/lineage/row/resolve?fqn=&key=col=value&depth=` | 行级血缘：沿列级血缘按值反查 |
| GET/POST/DELETE | `/data-asset/lineage/keys` | 键映射登记（目标键列 → 上游键列） |
| GET/PUT | `/data-asset/lineage/row-switch` | 按表的行级追踪开关 |
| GET | `/data-asset/datasource/{datasources,schemas,tables,columns}` | 直查业务库的表/字段（血缘编辑器选表用，只读元数据，不含数据行） |
| GET/POST | `/data-asset/datasource/{exists,register}` | 表是否已被 OM 采集 / 按数据源元数据登记进 OM（仅管理员） |

P1/P2 预留：`/governance/quality/*`（质量定义、执行与结果）。

## 安全约定

- 只用只读数据库账号，且连接 `readOnly=true`。本机已创建 `data_asset_ro`：
  仅 `agri_sat.public` 的 SELECT 权限，`default_transaction_read_only=on`，实测 UPDATE 被拒。
- 白名单默认需要审批；申请人不能审批自己的条目；所有浏览/导出/审批都留审计。
- 行级策略模板（`rowPolicy`）中的 `${user.xxx}` 由 `user_attr` 表解析，值参数化绑定，禁止拼接。
- `data_asset_ro` 的口令是本地初始值，上线前必须轮换并放入 Secret / 环境变量。

## 当前状态（2026-11-04）

- ✅ P0：目录/搜索/详情/字段、白名单 CRUD + 审批（默认开启、禁自批、驳回需理由）、
  只读分页浏览、列脱敏、CSV 导出、审计、健康检查。
- ✅ DS UI 页面：`/asset/catalog`（资产目录 + 分层/分级/认证筛选 + 数据浏览 + 治理编辑）、
  `/asset/lineage`（表/列血缘自绘）、`/asset/governance`（域/数据产品/分类标签/术语管理）。
- ✅ 血缘代理：`/data-asset/lineage/table`（读 OM `getLineage`，输出 nodes/edges/列映射）。
- ✅ 治理代理：`/data-asset/governance/*`（分层/分级/认证/分类/域/产品/术语的读取、赋值与创建）。
- ✅ 手动血缘：`/lineage/manual` 写 OM（表边 + `columnsLineage`），带表/列存在性校验、禁自环、仅管理员、审计。
- ✅ 键映射与行级开关：`key_mapping`、`row_switch` 两张配置表 + 对应 CRUD 接口。
- ✅ 行级血缘：`/lineage/row/resolve` 列级血缘 + 键映射 + 只读按值反查，输出逐行树（含参与集合、断链标注）。
- ⏳ P1：质量代理、血缘图导出（PNG/JSON）、路径高亮、X6。
- ⏳ P2：质量执行器（路线 B，8 个定义）。
- ⏳ P3 剩余：两行之间的路径查询、键映射覆盖率看板。

演示数据初始化：

```bash
bash scripts/seed-demo.sh            # 白名单（8 张表）
node scripts/seed-governance.mjs     # 分层/分级/Tier/认证
node scripts/seed-lineage-demo.mjs   # 手动血缘边 + 列映射 + 键映射 + 行级开关
node scripts/verify-lineage.mjs      # 血缘接口回归（写入/校验/键映射/行级反查/删除）
```

`seed-demo.sh` 为 8 张样例表创建并审批白名单条目；未登记的表会返回 403，这是预期行为。
`seed-governance.mjs` 创建 `DataLayer` 分类与 ODS/DWD/DWS/ADS/DIM 标签，并给样例表设置分层、分级、Tier、认证。
`seed-lineage-demo.mjs` 通过手动血缘接口重建 ODS→DWD→DWS→ADS 的列映射，登记 2 条键映射并开启 4 张表的行级追踪。
页面截图：`docs/ds-asset-cards.png`、`docs/ds-asset-card-drawer.png`、
`docs/ds-asset-governance-page.png`、`docs/ds-asset-lineage.png`（后者为 v1.11 版式，v1.12 血缘页改版后未重新截图）。

已验证（本机）：`health` 三依赖均 ok；目录 8 个资产；`dwd_ndvi_tile` 分页返回 8 行；
未授权表 403；`cloud_cover_pct` 脱敏为 `******`；CSV 导出正常；自批/无理由驳回均 400。

