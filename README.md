# data-center

DolphinScheduler + SeaTunnel monorepo for the data center platform.

| Folder | Description |
|--------|-------------|
| `dolphinscheduler/` | DolphinScheduler (DS) with SeaTunnel UDP source UI/API |
| `seatunnel/` | SeaTunnel (ST) with connector-udp JS / offset_table codecs |
| `deploy/` | 单容器 Docker 与单机最小化部署物 |

## 部署形态

| 形态 | 适用场景 | 进程/容器 | 最低资源 | 推荐资源 |
|---|---|---|---|---|
| **单容器 Docker** | 演示、小规模、一体化交付 | 1 个容器内 3 个常驻进程（DS standalone、data-api、Nginx）+ 按需拉起 SeaTunnel local | 4C / 8G / 50G | 8C / 16G / 100G SSD |
| **单机最小化** | 本机开发、单机试点 | DS standalone（含 API/Master/Worker/Alert/UI）+ data-api，Nginx 可选 | 4C / 8G / 50G | 8C / 16G / 100G |
| **K8s 拆分** | 生产、HA、横向扩容 | 18 个常驻容器 + 1 个初始化 Job（DS 4 角色、ST 2 角色、data-api、Nginx、PG） | 3 节点 × 4C8G | 3 节点 × 8C16G |

> 单容器与单机形态共用同一套 H2 元库（`AUTO_SERVER`），只适合单写；生产请切 PostgreSQL 并拆分部署。
> 详细说明：单容器 [`deploy/all-in-one/README.md`](deploy/all-in-one/README.md)、
> 单机 [`deploy/local/README.md`](deploy/local/README.md)、
> data-api 网关/双实例 [`dolphinscheduler/dolphinscheduler-data-api/deploy/README.md`](dolphinscheduler/dolphinscheduler-data-api/deploy/README.md)、
> K8s 拆分见 `dolphinscheduler/deploy/kubernetes` 与 `seatunnel/deploy/kubernetes`。

### 方式一：单容器 Docker（推荐演示）

```bash
# 1) 先把 DS 发行版、SeaTunnel 发行版、data-api jar 暂存到 build/
cd deploy/all-in-one
./build.sh                      # Windows: .\build.ps1

# 2) 启动
docker compose up -d
```

| 地址 | 说明 |
|---|---|
| `http://<host>:8088/dolphinscheduler/ui/` | DS 控制台（默认 admin/dolphinscheduler123） |
| `http://<host>:8088/data-api/health` | 数据服务健康检查 |
| `http://<host>:8088/magic/web` | magic 控制台（生产请仅内网开放） |

数据持久化在 `ds-data`、`ds-logs`、`api-logs`、`st-checkpoint` 四个 volume。首次启动自动初始化 H2 并迁移 `magic-api` 脚本。

### 方式二：单机最小化（本机）

前置：JDK 11、DS 3.4.3 发行版、SeaTunnel 3.0.0 发行版、data-api jar、data-asset jar；
全栈还需要本机裸装的 OpenMetadata（+ OpenSearch）与已 `npm install` 的 `dolphinscheduler-ui`。

```powershell
cd deploy/local
.\start-local.ps1                # 默认档：DS + data-api + UI（不拉 OM/OpenSearch）
.\start-local.ps1 -Profile asset # 需要资产页：DS + data-asset + OM + OpenSearch + UI
.\start-local.ps1 -Profile all   # 六个全起
.\start-local.ps1 -WithNginx
.\status.ps1                     # 端口 / PID / HTTP 体检
.\stop-local.ps1 -OnlyPorts 12346        # 只重启 data-api 时用；-DryRun 先看不动手
```

默认端口：OpenSearch `9200`、OM `8585`、DS `12345`、data-api `12346`、data-asset `12347`、
UI dev `5173`、Nginx `8088`（可选）。脚本幂等：已在监听的端口直接 `[skip]`，
所以栈起了一半时再跑一次即可补齐；PID 与日志写到 `deploy/local/run/`。
`start-all.cmd` / `stop-all.cmd` / `status.cmd` 是同一套 `.ps1` 的薄封装，方便双击。

### 方式三：K8s 拆分

DS 使用 `dolphinscheduler/deploy/kubernetes/dolphinscheduler` Helm chart，ST 使用
`seatunnel/deploy/kubernetes/seatunnel`；data-api 用 `cluster` profile（脚本存共享元库），
Nginx 作为统一入口。生产建议：注册中心用 JDBC（省 3 个 ZK 容器）、对象存储外置，
DS Worker 镜像预装 SeaTunnel 与自定义 connector。

## 资源建议

| 组件 | CPU | 内存 | 磁盘 | 说明 |
|---|---|---|---|---|
| DS standalone（单机/单容器） | 1–2 核 | 1–2 G（堆默认 512 M） | 5 G | 含 API/Master/Worker/Alert/UI |
| SeaTunnel local 单作业 | 0.5–1 核 | 0.5–1 G | 1–5 G | 并发作业数叠加 |
| data-api | 0.2–0.5 核 | 256 M（堆默认，实占 Old 41 M） | 1 G | magic 脚本、审计 |
| data-asset | 0.2 核 | 256 M（堆默认，实占 Old 12 M） | 1 G | 只存白名单等配置 |
| OpenMetadata + OpenSearch | 2 核 | 1 G + 640 M（堆默认） | 10 G | 元库可为远程 PostgreSQL |
| Nginx | 0.1 核 | 64–128 M | — | 可忽略 |
| PostgreSQL（生产） | 2 核 | 4 G | 50 G+ | DS 元库 + data-api 扩展表 |
| H2（单机/单容器） | — | 随 JVM | 10–50 G | 单写，不适合生产 |

各堆上限都由 `jstat -gc` 的实测存活集定档，含"为什么不能把中间件塞进 2C2G 线上机"的实测数据，见
`deploy/local/README.md` 第 4 节。

**总账**：单容器/单机建议 **4C8G 起、8C16G 稳**；并发跑 2–3 个 SeaTunnel 作业或数据量较大时按每作业 +1C/1G 估算。
K8s HA 建议 **3 × 8C16G**（requests 约 16C/24G，limits 约 30C/60G），磁盘按对象存储/日志单独规划。

## 构建

```bash
cd dolphinscheduler
./mvnw -pl dolphinscheduler-data-api -am -DskipTests -Dspotless.skip=true -Djacoco.skip=true package
```
