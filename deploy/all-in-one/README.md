# 单容器 Docker 部署（All-in-One）

一个镜像、一个容器，内部用 supervisor 管理 4 个进程：

| 进程 | 端口 | 说明 |
|---|---:|---|
| DolphinScheduler standalone | 12345 | 含 API / Master / Worker / Alert / UI |
| data-api | 12346 | magic 脚本发布、ACL、审计、OpenAPI |
| Nginx | 8088 | 统一入口：DS UI/API、data-api、magic 控制台 |
| SeaTunnel | — | **local 模式**，由 DS Worker 按任务拉起 `seatunnel.sh`，不常驻 |

适用：演示、单机试点、一体化交付。**不适合生产 HA**（H2 单写、无副本、控制台与计算同容器）。

## 1. 准备构件

需要三样东西（不随仓库提交）：

| 构件 | 默认路径 / 来源 |
|---|---|
| DS 3.4.3 发行版 | `D:\HPG\workspace\LHYF\apache-dolphinscheduler-3.4.3-bin`（Linux 用 `DS_HOME` 覆盖） |
| SeaTunnel 3.0.0 发行版 | `D:\HPG\workspace\LHYF\apache-seatunnel-3.0.0`（`ST_HOME`） |
| data-api jar | `dolphinscheduler/dolphinscheduler-data-api/target/dolphinscheduler-data-api-3.4.3.jar` |

构建 jar：

```bash
cd dolphinscheduler
# data-api 已脱离 DS 反应堆，独立 Spring Boot 3.0.13 工程，需 JDK 17 构建
JAVA_HOME=<jdk17> mvn -f dolphinscheduler-data-api/pom.xml -DskipTests -Dspotless.skip=true -Djacoco.skip=true package
```

## 2. 构建与启动

```powershell
# Windows
cd deploy\all-in-one
.\build.ps1
docker compose up -d
```

```bash
# Linux / macOS
cd deploy/all-in-one
DS_HOME=/opt/apache-dolphinscheduler-3.4.3-bin \
ST_HOME=/opt/apache-seatunnel-3.0.0 \
./build.sh
docker compose up -d
```

`build.*` 会把 DS/ST 发行版、data-api jar、`data/magic-api` 脚本一并放进 `build/`，再执行镜像构建。
镜像约 2–3 GB，构建需要数分钟。

## 3. 访问

| 地址 | 说明 |
|---|---|
| `http://<host>:8088/dolphinscheduler/ui/` | DS 控制台（首次登录后立即改密） |
| `http://<host>:8088/data-api/health` | 数据服务健康 + 已加载数据源数量 |
| `http://<host>:8088/data-api/openapi.json` | OpenAPI（需 token） |
| `http://<host>:8088/magic/web` | magic 控制台（生产只应内网开放） |

直连调试可在 compose 中放开 `12345`/`12346` 端口。

## 4. 首次启动行为

1. 容器检测到 `/opt/ds/data/dsdb.mv.db` 不存在时，DS 以
   `spring.sql.init.mode=always` 初始化 H2（脚本会 DROP/CREATE 表，因此只在空库执行）。
2. data-api 等待 DS 的 12345 端口可访问后再启动，并复用同一 H2 文件（`AUTO_SERVER=TRUE`）。
3. data-api 自动创建 ACL/审计/发布/血缘等扩展表，并加载 `data/magic-api` 下的脚本。
4. 后续重启不再执行 DS 建表脚本，数据保留在 `ds-data` volume。

## 5. 持久化与配置

| Volume | 容器路径 | 内容 |
|---|---|---|
| `ds-data` | `/opt/ds/data` | H2 元库、DS 资源文件 |
| `ds-logs` | `/opt/ds/logs` | DS 日志 |
| `api-logs` | `/opt/data-api/logs` | data-api 日志 |
| `st-checkpoint` | `/opt/seatunnel/checkpoint` | SeaTunnel checkpoint（已从 `file:///tmp` 改到此处） |
| `st-logs` | `/opt/seatunnel/logs` | SeaTunnel 日志 |

常用覆盖方式：

```yaml
environment:
  TZ: Asia/Shanghai
  # Spring 环境变量可直接覆盖 data-api 配置
  DATA_API_RATE_LIMIT_PERMITS_PER_MINUTE: "300"
  DATA_API_MASK_FIELDS: "password,phone,id_token"
```

修改 DS 配置：进入容器改 `/opt/ds/standalone-server/conf/`，或构建前改本机发行版；改完
`docker compose restart`。

## 6. 资源建议

| 项 | 最低 | 推荐 |
|---|---|---|
| CPU | 4 核 | 8 核 |
| 内存 | 8 GB | 16 GB |
| 磁盘 | 50 GB | 100 GB SSD |
| JVM 堆 | DS 1 G + data-api 512 M | DS 2 G + data-api 1 G |

- 并发 SeaTunnel 作业按每作业 +0.5~1 核 / +0.5~1 G 估算；`local` 模式作业与 DS 同容器竞争内存。
- compose 已设 limits `8C/16G`、reservations `4C/8G`，按机器规格调整。
- 高并发或大数据量请改用 K8s 拆分 + PostgreSQL + 对象存储。

## 7. 验证

```bash
docker compose ps
curl -s http://127.0.0.1:8088/data-api/health
curl -s -o /dev/null -w '%{http_code}\n' http://127.0.0.1:8088/dolphinscheduler/ui/
docker compose logs -f --tail=100
```

期望：health 返回 `status=UP`；DS UI 返回 200/302；容器 healthcheck 为 healthy。

## 8. 已知边界

- **单写 H2**：同一时间只适合一个容器实例；扩容/多副本必须换 PostgreSQL（data-api 用 `cluster` profile）。
- **SeaTunnel 为 local 模式**：无独立 master/worker，作业失败重试与资源隔离弱于集群模式。
- **控制台暴露**：`/magic/` 在单容器里由 Nginx 直接暴露，生产需加内网限制或关闭。
- **镜像构建未在本机实测**：当前开发机未安装 Docker；`Dockerfile`/脚本按现有发行版路径编写，
  首次构建请预留排错时间（DS 路径、插件 jar、JVM 参数）。
- 默认账号 `admin/dolphinscheduler123` 必须首次登录后修改。
