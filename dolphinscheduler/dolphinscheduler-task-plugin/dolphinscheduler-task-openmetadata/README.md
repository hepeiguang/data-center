# dolphinscheduler-task-openmetadata

DolphinScheduler 任务插件：在工作流中把**表级血缘**上报到 OpenMetadata（OM）。

- 任务类型：`OPENMETADATA`
- 不修改 DS 核心；通过 `TaskChannelFactory` SPI 扩展
- 可作为 ETL/SQL/SeaTunnel 任务的**下游成功节点**，避免 ETL 失败时写错血缘

## 1. 构建与安装

```bash
cd dolphinscheduler
./mvnw -pl dolphinscheduler-task-plugin/dolphinscheduler-task-openmetadata -am \
  -DskipTests -Dspotless.skip=true -Djacoco.skip=true package

cp dolphinscheduler-task-plugin/dolphinscheduler-task-openmetadata/target/\
dolphinscheduler-task-openmetadata-3.4.3.jar \
  $DS_HOME/plugins/task-plugins/
```

重启 DS（standalone/master/worker/api）。启动日志应出现：

```
Success register task plugin: OPENMETADATA
```

已实测本机 `apache-dolphinscheduler-3.4.3-bin` 注册成功。

## 2. 任务参数

| 参数 | 必填 | 默认 | 说明 |
|---|---|---|---|
| `openMetadataUrl` | 是 | `http://127.0.0.1:8585` | OM 地址 |
| `token` | 否 | `OM_TOKEN` 环境变量 | OM Bot/管理员 JWT；任务参数里存明文，生产优先用环境变量 |
| `fromFqn` | 是 | — | 上游实体 FQN |
| `toFqn` | 是 | — | 下游实体 FQN |
| `fromType` / `toType` | 否 | `table` | 支持 `table`、`apiEndpoint`、`apiCollection`、`dashboard`、`pipeline` 等 |
| `sql` | 否 | — | 写入血缘边的 SQL；可为 INSERT ... SELECT 或 SeaTunnel SQL transform 的 SELECT |
| `seatunnelConfig` | 否 | — | SeaTunnel 作业配置；`sql` 为空时自动提取 `query/sql` 字面量 |
| `columnsLineage` | 否 | — | 显式列级血缘 JSON；为空时由 SQL 自动解析 |
| `parseSql` | 否 | `true` | 是否从 SQL 自动推导列级血缘 |
| `description` | 否 | — | 边描述 |
| `connectTimeout` / `readTimeout` | 否 | 10000 / 30000 | 毫秒 |

任务行为：

1. 通过 `GET /api/v1/<type>s/name/{fqn}` 把 FQN 解析成实体 ID；
2. 若 `sql` 为空且提供 `seatunnelConfig`，提取其中的 SQL；
3. 用 JSQLParser 解析 `INSERT ... SELECT` / `SELECT`，生成 `columnsLineage`；
4. 调用 `PUT /api/v1/lineage` 写表级 + 列级边；
5. HTTP 非 2xx 或 FQN 找不到 → 任务失败（让 DS 告警/重试生效）。

列级血缘示例：

```json
[
  {
    "fromColumns": ["agri_satellite_pg.agri_sat.public.ods_satellite_scene.scene_id"],
    "toColumn": "agri_satellite_pg.agri_sat.public.dwd_ndvi_tile.tile_id"
  }
]
```

## 3. 工作流配置示例

任务定义 JSON（DS API/导入用）：

```json
{
  "name": "om-lineage-dim-to-dwd",
  "taskType": "OPENMETADATA",
  "taskParams": {
    "openMetadataUrl": "http://127.0.0.1:8585",
    "fromFqn": "agri_satellite_pg.agri_sat.public.dim_satellite",
    "toFqn": "agri_satellite_pg.agri_sat.public.dwd_ndvi_tile",
    "description": "scene feeds NDVI tiles",
    "sql": "insert into dwd_ndvi_tile (tile_id, ndvi_mean) select scene_id, cloud_cover_pct from ods_satellite_scene"
  },
  "workerGroup": "default",
  "environmentCode": -1
}
```

推荐 DAG：

```
[SQL / SeaTunnel ETL] ──成功──► [OPENMETADATA 血缘上报]
```

- 放在成功分支，不要放在失败分支；
- 一条边一个任务；多条边可并行或串行多个任务；
- `token` 建议由 Worker 环境变量 `OM_TOKEN` 提供，避免工作流定义里保存 JWT。

## 4. 验证

### 4.1 单元测试 + 真实 OM 联调

```bash
cd dolphinscheduler
OM_TOKEN="<JWT>" OM_URL="http://127.0.0.1:8585" \
  ./mvnw -o -pl dolphinscheduler-task-plugin/dolphinscheduler-task-openmetadata \
  -Dspotless.skip=true -Djacoco.skip=true test
```

覆盖：

- SPI 注册：`TaskPluginManager` 能取到 `OPENMETADATA`
- FQN 解析 + `PUT /lineage` 请求体
- 上游表不存在时任务失败
- 真实 OM：`dim_satellite → ods_satellite_scene` 写入成功

### 4.2 DS 工作流端到端

本机已实际跑通：

1. 创建项目 `om_plugin_demo`；
2. 创建并上线工作流，单任务类型 `OPENMETADATA`；
3. 触发实例，任务状态 `SUCCESS`；
4. Worker 日志：
   ```
   OpenMetadata lineage response: HTTP 200
   OpenMetadata lineage pushed: ...dim_satellite -> ...dwd_ndvi_tile
   ```
5. OM 血缘接口可查到该边。

复现脚本：`D:\HPG\workspace\LHYF\openmetadata\samples\ds-plugin-workflow.mjs`。

## 4.3 DS UI 与部署

UI 增加 `OPENMETADATA` 任务表单，字段包括 OM 地址、Token、上下游 FQN/类型、SQL、
SeaTunnel 配置、列级血缘 JSON、自动解析开关。

```bash
# UI
cd dolphinscheduler-ui
npx vite build --mode production
rm -rf $DS_HOME/ui/* && cp -r dist/* $DS_HOME/ui/

# 插件
cp .../dolphinscheduler-task-openmetadata-3.4.3.jar $DS_HOME/plugins/task-plugins/

# 任务类型配置（DS 3.4.3 DataIntegration->cloud 映射 bug 的兼容）
cp dolphinscheduler-api/src/main/resources/dynamic-task-type-config.yaml \
  $DS_HOME/standalone-server/conf/
```

已实测：

- DS 启动日志注册 `OPENMETADATA`
- `/dynamic/DataIntegration/taskTypes` 返回 `OPENMETADATA`
- UI 构建产物包含任务表单
- DS 工作流任务 `SUCCESS`
- OM 血缘边含 2 条列映射（`scene_id→tile_id`、`cloud_cover_pct→ndvi_mean`）

## 5. 边界

- 只上报**显式给出**的 FQN/SQL，不解析任务 SQL；动态 SQL 需人工/外部解析后传参。
- 表级血缘为主；列级血缘需扩展参数（OM `columnsLineage` 字段）。
- 插件注册在 Worker/API 的 `plugins/task-plugins` 目录；升级 DS 时重新放置 jar。
- 当前未增加 UI 任务卡片/表单，使用 API/工作流导入；如需在画布中选择，需补 DS UI 任务表单与 `dynamic-task-type-config.yaml`。
- token 参数会进 DS 元库/工作流定义，生产请用 `OM_TOKEN` 环境变量或密钥挂载。
