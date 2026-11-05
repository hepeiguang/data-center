# 单机最小化部署

不引入 Docker / K8s，直接在本机把整套跑起来。**默认 `-Profile all` 起全栈**；OM + OpenSearch 两个进程就吃 ~1.4GB 常驻，所以按手上的活分档：

| Profile | 起哪些 | 用在 |
|---|---|---|
| `all`（默认） | 六个全起 | 全链路联调 |
| `api` | DS + data-api + DS UI | 做 API 开发/网关/ACL，不碰资产页 |
| `asset` | DS + data-asset + OpenMetadata + OpenSearch + DS UI | 做目录/血缘/治理，不碰 magic |

DS 与 UI 在两档里都在：UI 需要 DS 登录才能进任何页面，而 data-api 与 DS 共用同一个 H2 元库文件（`AUTO_SERVER` 只在本机有意义），**不能把 DS 和 data-api 拆到两台机器**。

| 进程 | 端口 | 说明 |
|---|---:|---|
| OpenSearch | 9200 | OM 的检索后端（本机裸装，非容器） |
| OpenMetadata | 8585 | 治理主库：目录、标签/分级、血缘、质量定义 |
| DolphinScheduler standalone | 12345 | 含 API / Master / Worker / Alert / UI，注册中心为 JDBC，元库 H2 |
| data-api | 12346 | magic 发布、ACL、审计、OpenAPI |
| data-asset | 12347 | 资产/血缘/治理代理，只存白名单等配置，不复制 OM 数据 |
| DS UI（Vite dev） | 5173 | 源码目录启动，含 `/asset/*` 三个页面 |
| Nginx（可选） | 8088 | 统一入口；不启用时直接访问各端口 |
| SeaTunnel | — | local 模式，由 DS Worker 按任务调用 `seatunnel.sh`，无常驻进程 |

适用：本机开发、单机试点、离线环境。**不适用生产 HA**（H2 单写、无副本）。

## 1. 前置

| 依赖 | 要求 |
|---|---|
| JDK | 11（H2 2.3 需要 11+；DS 3.4.3 可用 11） |
| DS | `apache-dolphinscheduler-3.4.3-bin`（含自定义 task/UI 插件） |
| SeaTunnel | `apache-seatunnel-3.0.0`（含 UDP connector / transform） |
| data-api jar | `dolphinscheduler/dolphinscheduler-data-api/target/dolphinscheduler-data-api-3.4.3.jar` |
| data-asset jar | `data-asset/target/data-asset-service.jar`（`cmd /c data-asset\build.cmd -DskipTests package`） |
| OpenMetadata | `..\openmetadata\openmetadata-2.1.0-rc1` + `start-om.cmd`；元库为远程 PostgreSQL |
| OpenSearch | `..\opensearch\opensearch-3.4.0` + `start-opensearch.cmd` |
| DS UI | `dolphinscheduler/dolphinscheduler-ui`，需已 `npm install` |
| 元库 | H2 文件，默认 `D:\HPG\workspace\LHYF\data\dsdb`（Windows）或 `/data/dsdb`（Linux） |
| Git Bash（Windows） | DS 执行 SeaTunnel shell 任务需要 `bash`；OM 启动脚本也走 bash |
| Hadoop（可选） | Windows 下 SeaTunnel `file://` checkpoint 需要 `hadoop.dll`/winutils |

构建 data-api jar：

```bash
cd dolphinscheduler
./mvnw -pl dolphinscheduler-data-api -am -DskipTests -Dspotless.skip=true -Djacoco.skip=true package
```

## 2. 启动

### Windows

```powershell
cd deploy\local
.\start-local.ps1                    # 全栈：OS -> OM -> DS -> data-api -> data-asset -> UI
.\start-local.ps1 -Profile api       # 只做起 API 开发需要的三个（省 ~1.4GB）
.\start-local.ps1 -Profile asset     # 只做资产/血缘（不起 data-api）
.\start-local.ps1 -WithNginx         # 额外起 8088 网关
.\start-local.ps1 -Force             # 端口被占用时直接报错，而不是跳过
```

不想用 PowerShell 就双击 `start-all.cmd` / `stop-all.cmd` / `status.cmd`，它们只是薄封装，
真正的逻辑只在 `.ps1` 里，避免两份实现漂移。

**脚本是幂等的**：已在监听的端口会被 `[skip]`，所以栈起了一半时再跑一次即可补齐；
反过来，只想重启某一个服务（比如刚重新打包）：

```powershell
.\stop-local.ps1 -OnlyPorts 12346    # 只停 data-api
.\start-local.ps1                    # 只补齐缺的那一个
.\stop-local.ps1 -DryRun             # 先看会停哪些，不真的杀进程
.\stop-local.ps1 -WithOpenMetadata   # 连 OM 与 OpenSearch 一起停（重启要几分钟）
```

多端口一次停要用 `-Command`，不能用 `-File`：`powershell -File x.ps1 -OnlyPorts 12347 8585`
只会把第一个端口绑进 `int[]`（实测只剩 12347），照下面这样传：

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -Command "& './stop-local.ps1' -OnlyPorts 12347,8585,9200"
```

首次运行会检查 `dsdb.mv.db`：不存在时给 DS 追加 `--spring.sql.init.mode=always` 初始化 H2；
已存在则正常启动，不会重建表。

可用参数：

```powershell
.\start-local.ps1 `
  -JavaHome "D:\...\jdk-11" `
  -DsHome  "D:\...\apache-dolphinscheduler-3.4.3-bin" `
  -StHome  "D:\...\apache-seatunnel-3.0.0" `
  -DataDir "D:\...\data" `
  -DsPort 12345 -ApiPort 12346 -AssetPort 12347 -UiPort 5173 -WithNginx
```

### Linux

`start-local.sh` 只覆盖 DS + data-api + data-asset；OpenMetadata 与 OpenSearch 需要单独部署（或走 K8s 方案）。

```bash
JAVA_HOME=/opt/jdk-11 \
DS_HOME=/opt/apache-dolphinscheduler-3.4.3-bin \
ST_HOME=/opt/apache-seatunnel-3.0.0 \
DATA_DIR=/data \
OM_BASE_URL=http://127.0.0.1:8585 \
./start-local.sh
```

停止：

```powershell
.\stop-local.ps1 -WithNginx
```

```bash
./stop-local.sh
```

## 3. 访问

| 地址 | 说明 |
|---|---|
| `http://127.0.0.1:5173/` | DS UI 开发态（含数据资产三个页面） |
| `http://127.0.0.1:5173/asset/catalog` | 资产目录 + 数据浏览 + 治理编辑 |
| `http://127.0.0.1:5173/asset/lineage` | 血缘图 / 手动血缘配置 / 键映射 / 行级血缘 |
| `http://127.0.0.1:5173/asset/governance` | 域、数据产品、分类标签、术语表 |
| `http://127.0.0.1:12345/dolphinscheduler/ui/` | DS 控制台（默认 `admin/dolphinscheduler123`，首次登录后改密） |
| `http://127.0.0.1:12346/data-api/health` | data-api 健康 |
| `http://127.0.0.1:12346/magic/web` | magic 控制台 |
| `http://127.0.0.1:12347/data-asset/admin/health` | data-asset 三依赖连通性 |
| `http://127.0.0.1:8585/` | OpenMetadata 控制台 |
| `http://127.0.0.1:8088/...` | 启用 `-WithNginx` 后的统一入口 |

日志与 PID：

```
deploy/local/run/
├── opensearch.out.log / .err.log
├── openmetadata.out.log / .err.log        # OM 自身日志在 openmetadata\logs\
├── ds.out.log / ds.err.log / ds.pid
├── data-api.out.log / data-api.err.log / data-api.pid
├── data-asset.out.log / data-asset.err.log / data-asset.pid
├── ds-ui.out.log / ds-ui.err.log / ds-ui.pid
└── nginx.conf / logs/                     # 仅 -WithNginx
```

一键体检：`.\status.ps1`（或双击 `status.cmd`）打印每个服务的端口、PID 与 HTTP 探测结果。

## 3.1 更新 DS 插件与 UI

新增/升级 OpenMetadata 血缘插件后：

```bash
# 1) 构建并放置任务插件
cd dolphinscheduler
./mvnw -pl dolphinscheduler-task-plugin/dolphinscheduler-task-openmetadata -am \
  -DskipTests -Dspotless.skip=true -Djacoco.skip=true package
cp dolphinscheduler-task-plugin/dolphinscheduler-task-openmetadata/target/\
dolphinscheduler-task-openmetadata-3.4.3.jar \
  $DS_HOME/plugins/task-plugins/

# 2) 重建 UI（任务表单/任务卡片）
cd dolphinscheduler-ui
npx vite build --mode production
rm -rf $DS_HOME/ui/*
cp -r dist/* $DS_HOME/ui/

# 3) 重启 DS
```

Older `dynamic-task-type-config.yaml` 在 DS 3.4.3 中把 DataIntegration 映射到了 cloud 列表；
仓库配置已同时把 `OPENMETADATA` 放在 cloud/dataIntegration 两处，确保 UI 可见。
生产打包时应修正 `DynamicTaskTypeConfiguration` 后重新构建 API jar。

## 4. 资源配置建议

2026-11-05 用 `jstat -gc` 量了各 JVM 的**实际存活集**（不是堆上限），据此定档；下面是"上限 / 实占 Old / 实占 Metaspace"三列对照，改参数前先看这张表：

| 进程 | 原上限 | Old 实占 | Metaspace 实占 | 现上限（默认值） |
|---|---|---|---|---|
| OpenMetadata | 1g / 2g | 214 MB | 163 MB | 512m / **1024m** / Meta 320m / Xss 512k |
| OpenSearch | 1g / 1g | 201 MB | 171 MB | 512m / **640m** / Meta 256m / Xss 512k |
| DolphinScheduler | 512m / 1024m + Xmn256m | 264 MB | 142 MB | 256m / **512m** / Meta 256m / Xss 512k（去掉 Xmn，交 G1） |
| data-api | 256m / 512m | 41 MB | 85 MB | 128m / **256m** / Meta 192m / Xss 512k |
| data-asset | 128m / 512m | 12 MB | 50 MB | 64m / **256m** / Meta 192m / Xss 512k |

OpenSearch 特意留 640m 而不是 512m：它的 Old 区已常驻 201MB（Lucene 段元数据 + 请求缓存都在堆上），512m 只剩 2.5 倍余量，OM 重建索引时会 GC 抖动。

改完的实测效果（同一台 15.3GB 的开发机，只重启了 OM/OS/data-api/data-asset，DS 未动）：

| 进程 | 改前 Private | 改后 Private | 差 |
|---|---|---|---|
| OpenMetadata | 1539 MB | 1014 MB | -525 |
| OpenSearch | 1572 MB | 1036 MB | -536 |
| data-api | 521 MB | 363 MB | -158 |
| data-asset | 293 MB | 220 MB | -73 |
| 小计 | | | **-1292 MB 提交内存** |

RSS 合计只从 3346 → 3249，这是 Windows 的记账方式：**空闲进程的 RSS 会被裁剪进 standby**，刚重启的 JVM 看起来反而更"高"；判断容量请看 Private Bytes（提交）或 `-Xmx` 之和。DS 重启后再省 ~400MB（它还带着 1024m 的老参数）。

分档才是更大的杠杆：`-Profile api` 不拉 OM+OpenSearch，提交内存约 **4.4GB → 2.4GB**；`-Profile asset` 不拉 data-api，约省 0.4GB。

| 项 | 最低 | 推荐 | 说明 |
|---|---|---|---|
| CPU | 4 核 | 8 核 | SeaTunnel 作业与 DS 共用 |
| 内存 | 8 GB | 16 GB | 全栈 + 余量给 SeaTunnel 作业（每并发 +0.5~1G） |
| 磁盘 | 50 GB | 100 GB SSD | H2 + 资源文件 + 任务日志 + checkpoint |
| 并发 SeaTunnel 作业 | 1 | 2–3 | |

### 4.1 线上 2C2G 的现状（2026-11-05 收敛：只留 PostgreSQL）

线上是 `ecs.e-c1m1.large`（2C2G，`123.57.2.64`，实例 ID `i-2ze0rgz4qb7e2cyyuo8o`，可用 `workbench exec -i <id> -c "..."` 操作）。

| 指标 | 初始 | 关 caddy/casdoor/ftp/backend/webhook/fwupd | 再停 MySQL 容器 |
|---|---|---|---|
| available | 493 MB | 863 MB | **1014 MB** |
| used | 864 MB | 495 MB | **360 MB** |
| 常驻容器 | 6 个 | 2 个（pg + mysql） | **1 个：postgres:16** |

另已 `systemctl disable --now`：`unattended-upgrades`、`apt-daily(.timer)`、`apt-daily-upgrade(.timer)`、`packagekit`、`udisks2`、`multipathd`；`fwupd` 与 `fwupd-refresh.timer` 是 static 单元，需 `mask` 才不回活。`snapd`(69MB) **没动**——`lxd` 由 snap 提供，牵着 MySQL 容器的 uid 映射，收益/风险不对等。`docker.service` 与 PG 的 `restart=unless-stopped` 保留，机器重启后 PG 自起。

数据源连带变更：DS 源中心删除 `osint_mysql`（DS 是软删，可恢复），新增 `agri_sat`(POSTGRESQL，账号 `data_asset_ro`)；data-api 的 `load-db-types` 加了 `POSTGRESQL`，magic 现只有 `agri_sat` 一个键。实测 PG 上**没有任何 osint 业务表**（`osint_article/entity/ioc` 计数 0），原 7 个打 MySQL 的示例接口已按 `agri_sat` 的 7 张表重写为 6 个农业接口，见 `dolphinscheduler-data-api/README.md`。

被停掉的都是 osint 项目的对外面：Caddy 的 443 反代（→ backend:8000 / → casdoor:8000）、Casdoor SSO、pure-ftpd（21 + 30000-30009）、backend（24h 内只有来自 127.0.0.1 的健康检查自打，无真实请求），以及 **Gitee push 自动部署的 webhook(:9000)**。恢复逐条来：

```bash
# 容器；或按项目 docker compose -f /opt/{nginx,sso,osint,mysql}/docker-compose.yml up -d
docker update --restart=unless-stopped osint-caddy sso-casdoor osint-ftp-server osint-backend osint-mysql
docker start osint-mysql osint-backend osint-casdoor osint-caddy osint-ftp-server
systemctl enable --now osint-deploy-webhook        # Gitee 部署通道
systemctl enable --now unattended-upgrades apt-daily.timer apt-daily-upgrade.timer packagekit udisks2 multipathd
systemctl unmask fwupd.service fwupd-refresh.timer
# 接口要退回 MySQL：DS 源中心重建 osint_mysql，并把 magic 脚本键改回 db.osint_mysql
```

**OpenSearch 搬迁：2026-11-05 实测装不上，原因不是内存。** 余量已够（1014MB vs `-Xmx448m` 时 OS 约 800MB），但三条分发路径全断：Docker Hub 直连超时（`registry-1.docker.io` → 000）；机器上已配的 mirror（`docker.1ms.run`/`docker.m.daocloud.io`/`mirror.ccs.tencentyun.com`）对该仓库停在 `Already exists` 不完成；`artifacts.opensearch.org` 的 tar **从本机和服务器两侧都返 403**（换浏览器 UA、去掉 Range 都一样）。`/opt/search/docker-compose.yml` 已写好并 `config` 校验通过，任一通道打通后 `docker compose -f /opt/search/docker-compose.yml up -d` 即可。compose 里 9200 是 `0.0.0.0` 发布：要么开 security 插件（HTTPS+口令，且 demo 证书 SAN 需含公网 IP），要么先安全组锁 IP，**别裸挂公网**。

三个遗留风险（要在控制台/SSH 里处理，不归本脚本管）：

1. `5432` 仍对 `0.0.0.0` 开放，且用的是弱口令账号（口令只存在于 `openmetadata/env.sh` 与 data-asset 配置中，不要抄进文档）；`3306` 随 MySQL 容器停止而关闭。建议安全组只放行本机出口 IP。
2. **按你的要求停了 `unattended-upgrades` 与 apt 定时器，系统补丁从此不会自动装**；安全组收口之后建议恢复。
3. 停用前实测 `:9000` 部署 webhook 上挂着一条来自 `195.140.214.25` 的外网保持连接；恢复它之前先确认那是你们自己的 Gitee 出口。

调优位置（不要再改文件，用参数覆盖）：

- Windows：`.\start-local.ps1 -OsJvmOpts "-Xms512m -Xmx1g" -DsJvmOpts ... -OmJvmOpts ... -ApiJvmOpts ... -AssetJvmOpts ...`
- Linux：`DS_OPTS=... API_OPTS=... ASSET_OPTS=... ./start-local.sh`
- OM 的启动脚本 `..\openmetadata\env.sh` 已改为尊重外部传入的 `OM_JAVA_OPTS`，默认 `-Xms512m -Xmx1024m`；不传就是它自己的默认值。
- SeaTunnel 客户端/引擎堆：`$ST_HOME/config/jvm_client_options`、`jvm_options`。
- H2：单文件建议 <20 GB；超过后查询与备份都会变慢，应切 PostgreSQL。

## 5. 验证

```powershell
.\status.ps1
```

```bash
curl -s http://127.0.0.1:12346/data-api/health
curl -s http://127.0.0.1:12347/data-asset/admin/health
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:12345/dolphinscheduler/actuator/health
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:5173/
```

期望：data-api `status=UP`；data-asset 的 `configDb / openMetadata / datasources` 三项均 `ok`；
DS 与 UI 返回 200。`status.ps1` 里任何一行 `down` 都说明该服务没起来，日志在 `run\` 下同名 `.out.log`。
再用 DS 控制台建一个 SeaTunnel 任务，确认 Worker 能调用 `seatunnel.sh`。

## 6. 已知边界

- H2 只能单写：不要同时起第二个 data-api/DS 指向同一个库。
- **`SPRING_DATASOURCE_URL` 是进程环境变量，会被后面启动的服务继承**：data-api 需要它指向 DS 的
  `dsdb`，而 data-asset 必须用自己的配置库。脚本里该变量只在 data-api 一段生效（`finally` 清除），
  并且 data-asset 显式传绝对路径的 `--spring.datasource.url`。新增服务时务必照此处理，
  否则 data-asset 会把 `browse_entry` 等表建到 `dsdb` 里，表现为“白名单全空、数据浏览 403”。
- 没有副本与故障转移；进程挂了跑一次 `start-local.ps1` 补齐即可（脚本幂等）。上线时应改为服务托管（nssm/systemd/K8s），不能依赖人工。
- OpenMetadata 冷启动约 2 分钟（元库在远程 PostgreSQL），脚本给它 360s；超时不代表失败，用 `status.ps1` 复查。
- `data-asset` 默认 `ASSET_AUTH_MODE=local`，信任 `X-Debug-User` 头，**仅限本机**；共享环境必须切 `ds`。
- Windows 上 SeaTunnel 的 `file://` checkpoint 依赖 Hadoop winutils；Linux 无此问题。
- 控制台 `admin` 默认口令必须修改；`/magic/` 不要直接暴露到公网。
- 生产请改用 PostgreSQL + 多实例 + Nginx，见
  `dolphinscheduler/dolphinscheduler-data-api/deploy/README.md` 或 K8s 拆分方案。
