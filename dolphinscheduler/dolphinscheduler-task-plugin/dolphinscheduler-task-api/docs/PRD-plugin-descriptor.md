# PRD：任务插件前端自动化扩展（Plugin Descriptor v1）

| 项目 | 内容 |
|---|---|
| 文档名称 | 任务插件描述符契约与前端自动化扩展 PRD |
| 版本 | V1.1（新增 §5.0「上游侵入度」强制原则、R20 fork 隔离层；据此裁定决策点 D3） |
| 日期 | 2026-11-06 |
| 代码基线 | `data-center-mono`，分支 `chore/upgrade-ds-seatunnel`；DS 3.4.3 / SeaTunnel 3.0.0 / magic-api 2.2.2 |
| 目标读者 | **数据架构/设计评审**、**数据方向研发 leader**、DS UI 负责人、插件开发者、安全评审 |
| 涉及模块 | `dolphinscheduler-task-plugin/dolphinscheduler-task-api`（SPI 契约）、`dolphinscheduler-api`（描述符下发与校验）、`dolphinscheduler-ui`（描述符渲染）、`dolphinscheduler-task-plugin/dolphinscheduler-task-seatunnel`（首个接入方） |
| 编写目的 | 消除"新增一类任务插件必须手工改前端代码"的扩展成本，并把该方案与既有优化项合并为一份可排期的完整需求清单 |
| 证据口径 | 本文所有"现状"结论均来自当前工作区代码实测，附 `文件:行号`；未经代码证实的判断在 §12 明确标注为待确认 |

---

## 1. Summary

当前在本 fork 中新增一类任务插件，后端只需"构建 jar + 放入 `plugins/task-plugins`"，但**前端必须手工改 8–10 个文件、新建 2–3 个文件**，且需随 UI 发版；两套元数据（后端 SPI、前端类型表）还要人工保持同步。

本 PRD 提出的方案是：**不做"前端热加载插件 JS"，改为定义"插件描述符契约（`ds-plugin/v1`）"**。插件 jar 自带一份 `META-INF/ds-plugin.yaml`，声明任务类型、分类、文案、图标、表单字段与参数 DTO 约束；api-server **以 ZipFile 只读解析该文件、不加载任何插件类**，通过新接口下发；前端用**已有的** `IJsonItem → getElementByJson` 表单引擎渲染。

由此得到三个核心结果：

1. **标准插件接入的前端改动文件数从 8–10 降为 0**，插件开发者不需提交任何 UI 代码。
2. **前端表单热生效、无需重启**（读结构不依赖类加载）；执行体仍按现有 classpath 机制在重启后接管。
3. **顺带修复一个数据完整性缺陷**：现状下后端存在但前端未注册的任务类型会**静默 fallback 到 SHELL 表单**，编辑既有工作流时可能造成 `taskParams` 覆盖丢失。方案将其改为只读视图 + 显式告警 + 保存拒绝。

极少数需要复杂交互的场景（SeaTunnel 图编辑、UDF 上传）**不通过 jar 携带自定义代码解决**，而是通过**平台组件注册表**：描述符用 `widget: <name>` 引用平台拥有、带 props 契约与版本的可复用组件。

**贯穿全篇的强制原则（§5.0）**：尽量少改动开源既有代码，把改动压进"新增模块 / 新增文件 / 声明式登记"，以此降低后续升级到上游新版本时的合并影响。据此，本方案全部需求的落地形态被约束为：**Phase 0–2 合计对上游既有文件的改动 ≤ 5 个文件、≤ 20 行**，其余全部为零侵入（明细见附录 A 的侵入度列，核对方式见 AC-11/AC-12）。

---

## 2. Background and Evidence

### 2.1 现状成本：新增一个插件类型的前端 touch list

以下为实测的必改清单（路径相对 `dolphinscheduler/dolphinscheduler-ui/`）：

| # | 文件 | 位置 | 必须做什么 |
|---|---|---|---|
| 1 | `src/store/project/types.ts` | :23 | `TaskType` 联合类型加一项 |
| 2 | `src/store/project/task-type.ts` | :25–171 | 加 `TASK_TYPES_MAP_BASE` 条目（alias / helperLinkDisable / taskExecuteType） |
| 3 | 同上 | **:212** | `setTaskTypes` **会过滤掉后端返回的未知类型** |
| 4 | `src/views/projects/task/constants/task-type.ts` | :17–56 + :60–211 | **第二份重复映射**，被 `detail-modal.tsx:41,161` 用于帮助链接 |
| 5 | `.../task/components/node/tasks/index.ts` | :18–100 | 在 `nodes` 表注册 `FOO: useFoo`；缺条目时 `use-task.ts:67` **静默 fallback SHELL** |
| 6 | `.../node/tasks/use-foo.ts` | 新建 | 命令式返回 `{model, json}` |
| 7 | `.../node/fields/index.ts` | :18–96 | 导出新字段构造器 |
| 8 | `.../node/format-data.ts` | :42–522 | `formatParams` 加 `if (data.taskType === 'FOO')` 分支 |
| 9 | `.../workflow/components/dag/dag.module.scss` | :196–305 | 硬编码 `.icon-foo` 与 hover 规则 |
| 10 | `public/images/task-icons/` | — | `foo.png` + `foo_hover.png` 二进制资源 |
| 11 | `src/locales/{en_US,zh_CN}/project.ts` | — | 每个字段 label/tip 的 i18n key |

**合计：修改 8–10 个文件 + 新建 2–3 个文件，且必须随 UI 一起发版。**

### 2.2 现状能力：已存在、可直接复用的机制

| 能力 | 位置 | 可复用价值 |
|---|---|---|
| **表单引擎已是数据驱动** | `src/components/form/get-elements-by-json.tsx`；`use-task.ts:66–69` → 工厂返回 `IJsonItem[]` → 按 `type`（input/select/code-editor/custom）映射 Naive UI 控件 | **不需要新造表单引擎**，缺的只是"schema 从哪来" |
| **本 fork 已有动态类型雏形** | `dolphinscheduler-api/.../controller/DynamicTaskTypeController.java:61,76`（`GET /dynamic/taskCategories`、`/dynamic/{category}/taskTypes`）+ `DynamicTaskInfo{name,hover,icon,json}` | `json` 字段就是**表描述符资源 URL**，机制方向一致 |
| **描述符 schema 形状已被验证** | `dolphinscheduler-api/src/main/resources/static/shell/shell.json`（`task / locales / apis / forms` 四段；`forms` 含 label、type、field、defaultValue、validate、options） | 契约直接沿用该形状，迁移成本最低 |
| **前端已会按 URL 拉描述符** | `src/service/modules/dynamic-dag/index.ts`（`queryDynamicTaskResource(url)`） | UI 侧取数管道已通 |
| **上传→目录扫描→即时可见先例** | `SeaTunnelConnectorServiceImpl.java:99–131`（GET 扫 `${SEATUNNEL_HOME}/udf/udp-codec`）；:134–176（POST 上传，扩展名白名单 :140、名称清洗 :205–209、路径穿越防护 :159） | 插件中心的安全校验逻辑可直接泛化 |

### 2.3 现状阻塞点（方案必须逐个解决）

1. **SPI 不暴露任何 UI/配置元数据**。`dolphinscheduler-task-plugin/dolphinscheduler-task-api/.../TaskChannelFactory.java`：`getName()` :31、`create()` :33、`getIdentify()` :25–29（仅 `SPIIdentify{name, priority}`）；`TaskChannel.java:22–27` 仅 `createTask/parseParameters`。**无任何 schema 类方法（已确认）。**
2. **任务类型清单不来自 jar**。`GET /fav-task/taskTypes`（`FavTaskController.java:68`）→ `FavTaskServiceImpl:46–52` → `TaskTypeConfiguration.java:39–40` → **打包在 api jar 内的 `task-type-config.yaml:18–33`**。结果是需要人工对齐的 3 个真值源：SPI 名 / YAML 清单 / UI 描述符 JSON。
3. **无运行时 jar 加载器**。`PrioritySPIFactory.java:33` 使用普通 classpath `java.util.ServiceLoader`；jar 由 `script/install-plugins.sh:52,58` 放入 `${DOLPHINSCHEDULER_HOME}/plugins/{task-plugins,...}`，依赖由 `dolphinscheduler-dist/src/main/assembly/assembly-plugins.sh:62–75` 合并进共享 `libs/`。**没有 URLClassLoader，没有 jar 扫描。** 这正是"表单热生效要绕开类加载"的原因。
4. **taskParams 反序列化按插件硬编码类**。`ShellTaskChannel.java:33–34` `JSONUtils.parseObject(taskParams, ShellParameters.class)`；分发靠 `TaskPluginManager.java:93–101` 的 taskType 字符串。**任务侧无 Jackson 多态注册表**（`@JsonTypeInfo` 仅存在于 `ITaskInstanceContext.java:25–27` 等无关处）。
5. **数据源中心更差，不能当先例**。`src/views/datasource/list/use-form.ts:363–511` 硬编码 `datasourceType` 常量 + ~20 个布尔可见性开关（:60–77）；SPI 侧类型注册是硬编码 `DbType` 枚举。**结论：本 PRD 不复用数据源机制。**
6. **图标无动态来源**。`dag-sidebar.tsx:311` 等以 `styles['icon-' + type.toLowerCase()]` 解析 SCSS 类，缺类即空白。SeaTunnel 另有 `dag/seatunnel-icon-map.json`（:35–73）+ CSS 变量注入，但映射表仍是硬编码 JSON + 打包 PNG。
7. **后端类型仅"侧边栏可见"**。`GET /favourite/taskTypes` 已让后端注册的插件出现在 DAG 侧边栏（`dag-menu/index.ts:20–25`，`dag-sidebar.tsx:124`），但一旦拖到画布：表单 fallback SHELL（`use-task.ts:67`）、下拉选项来自硬编码表（`use-task-type.ts:29`）、未知类型被 store 过滤（`task-type.ts:212`）。**后端类型只被信任用于展示，从不用于表单渲染 —— 这就是本方案要闭合的缺口。**

### 2.4 SeaTunnel 侧的同类痛点（本方案的验证场）

- `fields/seatunnel-compute/connectors-catalog.json`（219 行）**不是参数 schema**，只是三个字符串数组，供 `seatunnel-compute-utils.ts:450–507` 在客户端拼侧边栏树。
- `connector-param-help.json`（约 2.9 万行）**已含** `connectors.<PLUGIN>.{plugin,kind,summary,tips,input,output,process,example}` 与每参数 `{name,type,required,default,desc}`，但 `st-param-help.ts:56–77` 只把它当**只读文档面板**渲染。
- `StConnectorParamPanel.tsx:48–83` 右侧实际是**裸 Monaco JSON 编辑器**，仅校验"能否 parse JSON"（`use-st-connector.ts:46–59`）。**零类型化控件、零条件可见、零校验。**
- 默认值硬编码在 `seatunnel-compute-utils.ts:150–245` 的 `defaultConnectorParams`。
- UDP 的 UDF 面板是特例：`StUdpUdfPanel.tsx`，由硬编码 `isUdpPlugin()`（`SeatunnelComputeEditor.tsx:414`、`StUdpUdfPanel.tsx:272–278`）门控，且显式排除 sink。
- `GET /seatunnel/connectors` 接口存在（`SeaTunnelConnectorController.java:59`），数据来自打包资源 `seatunnel-connectors-catalog.json`（`SeaTunnelConnectorServiceImpl.java:53,69–77`），**但 UI 无任何调用方**（`service/modules/seatunnel/index.ts:20–25` 定义了 `getSeaTunnelConnectors` 却未被引用）—— 死接口。
- **装配逻辑双实现**：Java 侧 `plugin/` 装配框架已不被调用（`SeatunnelTask.java` 不再调 `SeatunnelPluginManager`，旧接线仅存 `.custom-bak`），装配迁至前端 `seatunnel-compute-utils.ts:380 assembleStockSeaTunnelConfig()`。

---

## 3. Goal and Success Criteria

### 目标

让"插件 jar"成为任务类型元数据（含表单结构）的**唯一真值源**，使标准插件的接入无需前端改动、无需 UI 发版、无需重启服务即可配置。

### 成功指标

| 指标 | 现状基线 | 目标 | 测量方式 |
|---|---|---|---|
| 接入一个标准插件类型的前端改动文件数 | 8–10（+2–3 新建） | **0**（Tier A） | 以样例插件走查 PR diff 文件清单 |
| 端到端接入耗时 | 约 2–3 人日 | ≤ 0.5 人日 | 试点插件的工时记录 |
| 未注册类型导致的参数丢失 | 静默发生 | **0** | R4 回归用例 + 线上工单计数 |
| 描述符 ↔ DTO 一致性校验覆盖率 | 无此机制 | 100% 已注册插件 | 启动自检报告 |
| 需前端发版才能改的字段（文案/默认值/必填） | 全部 | 仅新增 widget 需前端 | 变更类型统计 |
| 插件 jar 元数据真值源数量 | 3（SPI 名 / YAML / UI 描述符） | 1（jar） | 架构走查 |

### 不达标即视为未完成

- 若某插件类型仍需改 `format-data.ts` 或 `dag.module.scss` 才能工作，则该类型不算完成迁移。
- 若 `use-task.ts` 的 SHELL fallback 分支仍存在，则 R4 视为未完成。

---

## 4. Users and Scenarios

| 角色 | 场景 | 当前痛点 | 目标体验 |
|---|---|---|---|
| **插件开发者** | 交付一类新的任务插件（如某种专有协议接收器） | 写完 jar 还要学前端 11 处接线、等 UI 发版窗口 | 在 jar 内放 `ds-plugin.yaml` + 图标，构建即完成 |
| **DS 平台管理员** | 现场新增插件到已部署环境 | 手工放 jar + 重启 + UI 不识别则表单空白 | 插件中心上传 → 校验报告 → 表单立即可配 → 执行体重启生效 |
| **数据开发工程师** | 在工作流里配置第三方插件节点 | 未注册类型渲染成 SHELL 表单，改错参数且不知晓 | 正确表单；无描述符时明确只读 + 告警，不会被误导 |
| **SeaTunnel 连接器使用者** | 配置 UDP 源参数 | 面对裸 Monaco JSON，靠猜字段名 | 类型化控件 + 校验 + 文档同源 |
| **平台 UI 维护者** | 上游 DS 升级 | 硬编码映射表逐条合并冲突 | 接线数量大幅下降，冲突面收敛到 override 层 |
| **安全评审** | 评估"jar 上传"能力 | 执行体可被任意代码替换的风险 | 只读解析不执行代码 + 管理员独占 + 审计 + 可选签名 |

---

## 5. Scope

### 5.0 强制设计原则：上游侵入度最小化（不可协商）

**原则**：尽量少改动开源（Apache DolphinScheduler / SeaTunnel）既有代码，把改动尽量放进**新增文件、新增模块、声明式登记**中，以降低后续升级到上游新版本时的合并冲突与维护成本。

**成本判据（评审时逐条填写）**：侵入成本只发生在**上游既有文件**上；本 fork 自己新增的文件（如 `dolphinscheduler-data-api`、`seatunnel-edge-agent`、`seatunnel-compute-utils.ts`）**升级时不构成冲突面**，不计入侵入。

#### 侵入度分级

| 级别 | 定义 | 合并风险 | 预算 |
|---|---|---|---|
| **L0 零侵入** | 新增独立模块 / 新增文件 / 新增 jar；不改上游任何既有文件 | 无冲突可能 | 目标 ≥ 70% |
| **L1 声明式登记** | 只改构建/装配清单：`pom.xml` 模块行、`assembly-*.xml`、`spring.factories`、`vite.config.ts` alias、`plugin-mapping.properties` | 单行冲突，机械可解 | 与 L0 合计 ≥ 90% |
| **L2 单点 patch** | 上游既有文件内 ≤ 10 行的最小改动，且必须登记进 patch 台账并可脚本重放 | 可控，可复现 | 仅允许有明确安全/正确性理由者 |
| **L3 结构改造** | 大面积重写/移动上游既有文件（含"顺手重构"） | 高，易静默丢改动 | **本 PRD 禁止**；如需，须 leader 单独审批并拆为独立版本窗口 |

#### 落地机制（工程可执行的四条手段）

1. **后端一律走新增模块**：描述符解析与接口放入新模块（建议 `dolphinscheduler-plugin-descriptor`），通过 `META-INF/spring.factories` 的 `EnableAutoConfiguration` 被 api-server 装配 —— api 的 `@SpringBootApplication` 只扫描 `org.apache.dolphinscheduler.api` 包，故自动配置是**唯一零 Java 侵入的接入方式**（R2）。上游侧仅允许 `dolphinscheduler-api/pom.xml` 加一行依赖（L1）。
2. **前端走 fork overlay**：自定义代码集中在 `dolphinscheduler-ui/src/fork/`，通过 `vite.config.ts` 的 `resolve.alias` 把特定上游模块指向 fork 实现，使上游文件**保持逐字节不变**（替代现状的人工 `.custom-bak` 备份做法 —— 见 `dolphinscheduler-task-plugin/dolphinscheduler-task-seatunnel/src/main/java/.../SeatunnelTask.java.custom-bak`、`SeatunnelParameters.java.custom-bak`）。alias 名单即"已分叉清单"，CI 可枚举校验（R20）。
3. **必须落在上游文件上的改动，登记为可重放 patch**：沿用仓内既有约定 `dolphinscheduler/patches/ds-<version>-<topic>/`（现成先例 `patches/ds-3.4.3-udf-resource/`，见其 `README.md:12–19`），存放 diff 文件 + apply 脚本 + 上游文件 hash，升级时先重放再解冲突。
4. **不修改上游 SPI 接口**：任何"给 `TaskChannelFactory` 加方法"的方案一律否决，改为 jar 内声明式文件 + ZipFile 只读解析 —— 这同时是本方案能"不重启即生效"的技术前提（裁定 D3）。

#### 升级验收门（本原则的唯一量化出口）

在下一轮上游升级（DS 3.4.3 → 3.5.x）演练中：
- 冲突文件数 ≤ 5，且**全部**能在 patch 台账 / alias 清单中对应到条目；
- 未登记的冲突 = 本原则执行失败，需回溯补登并重开评审。

### In scope

1. `ds-plugin/v1` 描述符契约定义（含 JSON Schema 自描述）与版本演进规则。
2. api-server 的 jar 描述符只读解析器与三个下发接口（清单 / 单个 / 图标）。
3. 描述符 ↔ Parameters DTO 的反射一致性校验（启动时 + 上传时）。
4. 前端描述符渲染器，复用 `getElementByJson`；通用 model↔taskParams 转换替代 `format-data.ts` 分支。
5. 未知/无描述符类型的**只读降级 + 显式告警**（替换 SHELL fallback）。
6. 平台组件注册表：首批 5 个 widget 及其 props 契约、快照测试。
7. 插件中心（上传 / 校验报告 / 激活状态 / 描述符回滚），仅管理员。
8. 兼容层：与现有 `dynamic-task-type-config.yaml`、`static/**/*.json`、`task-type-config.yaml`、UI 硬编码映射的合并优先级与迁移路径。
9. SeaTunnel 连接器表单描述符化（dogfood），并处理死接口 `/seatunnel/connectors`。
10. 需求清单合并：本设计的 R1–R10 与既有优化项 R11–R19 统一编号、分期、依赖。
11. **fork 隔离层建设（R20）**：`src/fork/` overlay + `vite.config.ts` alias 清单 + `patches/` 上游改动台账 + CI 侵入度校验。

### Out of scope

- 执行体真热加载（每插件隔离 `URLClassLoader`）—— 见 §12 决策点 D1。
- 插件自带自定义前端组件包（任意 JS 执行）。
- 数据源中心（`DbType` 枚举 + `use-form.ts`）的 schema 化改造 —— 明确不复用其现状机制，可作为后续独立需求。
- DS 上游版本升级本身。
- 插件市场 / 计费 / 第三方分发。

### Dependencies

| 依赖 | 影响 |
|---|---|
| `dolphinscheduler-task-api` SPI 接口扩展（加 default 方法，非破坏） | 所有插件模块需重编译 |
| api-server 可读到 `plugins/task-plugins` 目录（路径来自 `conf/plugins_config`） | 部署形态相关，standalone/cluster 均需覆盖 |
| 前端表单引擎需新增 `when` 条件可见语义 | 引擎侧唯一改动，见风险 §12.6 |
| 既有工作流的 `taskParams` 兼容性 | 决定 `params.strict` 的灰度节奏 |
| 安全评审对 jar 上传的态度 | 阻塞 R7 |

### Assumptions（显式标注）

- A1：描述符用 YAML 承载、由 jar 打包（`META-INF/` 下），而非由 SPI 方法返回对象。**待工程评审确认。**
- A2：本 fork 现有 `static/shell/shell.json` 的四段结构足以演进为正式契约，无需另起炉灶。**已由代码证实其 `forms` 即字段 schema。**
- A3：现网存在"后端有插件、前端未注册"的真实案例，因此 R4 属线上风险。**需运维确认案例数。**

---

## 6. Non-Goals

1. **不让插件携带可执行前端代码。** 任何"jar 里放个 JS 就能渲染"的方案均被否决：XSS / 供应链风险 + 无法保证表单引擎升级后的兼容性 + 无法统一 i18n 与视觉规范。
2. **不做通用低代码表单搭建器。** 描述符只描述"这一个插件的这一个表单"，不提供拖拽建表单的产品能力。
3. **不追求一次性删除全部前端硬编码。** 保留 override 层，允许历史类型渐进迁移。
4. **不把 OpenMetadata / 数据服务纳入本契约范围。** 它们的元数据与鉴权模型不同，另见 `dolphinscheduler-data-api/docs/DATA-API-OPENMETADATA-PLAN.md`。
5. **不在本期引入插件签名强制校验。** 提供可选启用位，默认关闭。
6. **不做上游代码的"顺手重构"。** 包括：统一代码风格、抽取公共函数、合并重复定义、调整目录结构 —— 即使客观上更优，只要落在上游既有文件上且非本需求必需，一律排除（理由见 §5.0：这类改动不带来用户价值，却把合并冲突面从 1 处扩大到 N 处）。

---

## 7. User Flow

### 7.1 插件开发者（主路径）

```
1. 实现 TaskChannelFactory（不变）
2. 在 jar 内新增 META-INF/ds-plugin.yaml        ← 唯一新增工作
   （声明 type/category/name/locales/form/params）
3. 在 jar 内新增 META-INF/ds-plugin/icon.png    ← 替代 SCSS + PNG 两处硬编码
4. mvn package → 产出 jar
5. 本地起 api-server → GET /task/plugin-descriptors/{type} 自检
   → 返回 descriptorStatus=VALID 即完成
```

**空/异常态**：yaml 缺失 → `descriptorStatus=MISSING`；yaml 不合 schema → `INVALID` + 逐条字段错误；`field` 不在 DTO 属性集 → `INVALID` 并列出差集。错误信息必须可行动（指出行号/字段名/建议）。

### 7.2 平台管理员（插件中心）

```
设置 → 插件中心 → 上传 jar
  → 校验阶段（同步）：结构（含 TaskChannelFactory SPI？）
                     ds-plugin.yaml schema
                     描述符 ↔ DTO 一致
                     sha256 / 大小上限 / zip 条目上限
  → 校验报告：PASS / FAIL(逐条)
  → PASS → 落 plugins/task-plugins → 状态 PENDING_ACTIVATION
      · 表单已可预览与配置（不依赖类加载）
      · 执行体在 api/worker/master 重启后转 ACTIVE
  → 已有同 type → 走版本更新，保留上一份 yaml 可回滚
```

**权限**：仅管理员。所有动作写审计（操作人、jar sha256、type、动作、结果、trace_id）。
**通知**：`PENDING_ACTIVATION` 状态在插件中心列表显著标注"需重启生效"，避免管理员误判已上线。

### 7.3 数据开发工程师（画布配置）

```
DAG 侧边栏（来自 /fav-task/taskTypes + 描述符合并）
  → 拖入节点
  → 有描述符 → 渲染类型化表单（含条件可见、默认值、校验、图标、双语）
  → 无描述符 → 只读 JSON 视图 + 顶部告警条
                "该任务类型未完成描述符化，编辑存在风险，已禁用保存"
                （不再渲染 SHELL 表单）
```

### 7.4 Demo / 原型方向

Phase 1 交付一个**纵向切片**证明全链路：选一个最简单的现有插件（建议 `JAVA` 或 `PIG`），把它从 `static/*.json` 迁到 jar 内 `ds-plugin.yaml`，走通"上传 → 只读解析 → 前端渲染 → 保存 → 执行"，并同步实现 R4 的只读降级。

**需设计/工程验证的点**：
- 设计：描述符表单与现有硬编码表单的视觉一致性（控件宽度、分组、必填标记位置）。
- 工程：`getElementByJson` 加 `when` 语义的改动量；图标由 SCSS class 改 `<img>` 对 DAG 渲染性能影响；大表单（SeaTunnel 图）的 custom widget 挂载方式。

---

## 8. Functional Requirements

### Requirement R1：`ds-plugin/v1` 描述符契约定义

- **User story**：作为插件开发者，我希望用一份 jar 内文件声明该插件的全部展示与表单信息，以便不接触前端代码。
- **Scenario**：新建/升级任意任务插件。
- **Functional behavior**：定义 `apiVersion: ds.plugin/v1`，五段结构 `task / icon / locales / params / form`；`task.type` 必须等于 `TaskChannelFactory.getName()`；`form[].type` 取值集合与 `getElementByJson` 支持集合一一对应；新增两类语义 `form[].when`（条件可见）与 `form[].widget`（平台组件引用）；`params.dtoClass + strict` 作为后端校验与保存约束。契约见 §8.20。
- **Data/field changes**：jar 内新增 `META-INF/ds-plugin.yaml`、`META-INF/ds-plugin/icon.png`。
- **Edge cases**：同一 jar 多份 yaml；yaml 含未知顶层字段；`type` 与 SPI 名不一致；locales 缺某种语言。
- **Acceptance criteria**：
  1. 契约文档化并有机器可读 JSON Schema（可校验 yaml 本身）。
  2. 未知顶层字段被拒绝（避免拼写错误静默生效）。
  3. `task.type` 与 SPI 名不一致时校验 FAIL。
  4. `apiVersion` 演进规则写入文档（不兼容变更需升版本号，禁止静默改语义）。
  5. locales 缺失语言时按语言回落链（`en_US → key 原文`）有明确定义并被测试覆盖。
- **Priority**：P0

### Requirement R2：api-server 只读解析与描述符下发接口

- **User story**：作为平台，我希望在不加载插件类的前提下读出表单结构，以便新插件的表单即时生效。
- **Scenario**：api-server 启动扫描插件目录；管理员上传 jar；前端拉取描述符。
- **Functional behavior**：以 `ZipFile` 读 entry 文本 + 图标字节，**不实例化任何插件类**；三个接口：`GET /task/plugin-descriptors`（清单）、`GET /task/plugin-descriptors/{type}`（单个 + 状态）、`GET /task/plugin-descriptors/{type}/icon`（图标字节）；解析结果缓存并按 jar 指纹失效；与 `dynamic-task-type-config.yaml` / `static/**/*.json` 合并，**jar 优先，冲突记 WARN**。
- **Placement（依 §5.0，强制）**：解析器 + 控制器 + DTO 全部落在**新模块** `dolphinscheduler-plugin-descriptor`（L0），通过该模块内 `META-INF/spring.factories` 的 `EnableAutoConfiguration` 被 api-server 自动装配；上游侧仅允许 `dolphinscheduler-api/pom.xml` 增加一行依赖 + `dolphinscheduler-dist` assembly 一行（L1）。**明确禁止**修改 `TaskChannelFactory` / `TaskChannel` / `TaskPluginManager`（L3，且会破坏"不重启即生效"）。
- **Data/field changes**：无 DB 变更；新增内存缓存 + 磁盘状态记录（Phase 2 插件中心才落表）。
- **Edge cases**：yaml 畸形、超大 yaml、zip-bomb（条目爆炸/解压比）、entry 路径穿越（`../`）、同 type 多 jar 版本、jar 无 SPI。
- **Acceptance criteria**：
  1. **未重启时，新放入的 jar 其表单可被前端渲染成功。**
  2. 单个 jar 解析失败不影响其他类型，该类型返回 `descriptorStatus=INVALID` + 原因，其余照常。
  3. 单测覆盖：畸形 yaml、超大 yaml、zip-bomb、路径穿越、无 SPI、无 yaml 六种。
  4. **有断言证明解析过程不加载插件类**（如自定义类加载器计数或 `Class.forName` 拦截测试）。
  5. 接口有独立限流（复用 data-api `rate-limit-permits-per-minute` 模式）。
- **Priority**：P0

### Requirement R3：前端描述符渲染器

- **User story**：作为数据开发工程师，我希望插件节点渲染出正确的类型化表单，而无需 UI 为每个插件重新发版。
- **Scenario**：画布上打开任意已描述符化插件节点。
- **Functional behavior**：`form[] → IJsonItem[] → getElementByJson`；model↔taskParams 走通用转换（读 `params` 段推导），不再进 `format-data.ts` 加分支；`locales` 优先于 `src/locales`；图标走 `icon` 接口 URL 而非 SCSS 类。
- **Placement（依 §5.0，强制）**：渲染器、通用转换、`when` 求值、widget 注册表均为 `src/fork/` 下的**新增文件**（L0）。接入点需要改上游文件的地方**只允许 2 处、各 ≤ 5 行**（L2，登记进 patch 台账）：① `node/tasks/index.ts` 的工厂查找处（插入"先查描述符，未命中再走原逻辑"）；② `dag-sidebar.tsx` 的图标 class 计算处（插入"有描述符图标则用 `<img>`"）。`format-data.ts`、`store/project/task-type.ts`、`constants/task-type.ts`、`dag.module.scss`、`locales/*` 本期**不改**（现状靠描述符覆盖其内容，见 R8）。
- **Data/field changes**：无。
- **Edge cases**：描述符字段多于 DTO、少于 DTO；同一表单内 `when` 循环依赖；`widget` 未注册；接口超时。
- **Acceptance criteria**：
  1. **核心指标：以 Tier A 描述符接入一个新类型，前端改动文件数 = 0。**
  2. 与迁移前的同类硬编码表单做逐字段差分快照，行为一致。
  3. `widget` 缺失/版本不符时告警且其余字段仍可用（不整表崩溃）。
  4. 接口失败时进入 R4 的只读降级，不空白。
- **Priority**：P0

### Requirement R4：未知类型显式降级（安全修复，最高优先）

- **User story**：作为数据开发工程师，我不能接受在不知情的情况下被错误表单覆盖参数。
- **Scenario**：后端存在某 taskType，前端无对应条目/描述符。
- **Functional behavior**：删除 `use-task.ts:67` 的 SHELL fallback；渲染**只读 JSON 视图 + 顶部告警条**（"该类型未完成描述符化，编辑有风险，已禁用保存"）；`store/project/task-type.ts:212` 的 `setTaskTypes` 不再丢弃未知类型；保存时若原 `taskParams` 与渲染模型不一致则拒绝并保留原值。
- **Placement（依 §5.0）**：**L2，本 PRD 唯一带安全理由的上游既有文件改动** —— 上游侧改动限于 2 个文件、合计 ≤ 10 行（`use-task.ts` fallback 分支、`task-type.ts:212` 过滤行）。只读视图组件本体是 `src/fork/` 新增文件（L0）。理由：该缺陷会导致用户参数被静默覆盖，属于正确性问题，不能因"少动上游"而推迟；但改动范围必须压到最小并登记进 patch 台账，供上游升级时重放。
- **Data/field changes**：无 schema 变更；行为变更影响保存路径。
- **Edge cases**：只读视图下的工作流复制/导入；子流程引用该节点；试运行（调度侧仍应可执行，只是 UI 不可编辑）。
- **Acceptance criteria**：
  1. 回归用例：**仅后端存在、前端未注册的类型，"加载 → 不改 → 保存"后 `taskParams` 逐字节不变。**
  2. 该场景下 UI 不出现 SHELL 表单（可用组件测试断言）。
  3. 未知类型出现在侧边栏与下拉选项中，带"未描述符化"标记。
  4. 试运行/调度执行不受影响（执行侧本就不依赖 UI 注册）。
  5. 缺陷工单类别"参数丢失"可归零跟踪。
- **Priority**：**P0-Blocker**（其余需求可排后，此项不应等排期）

### Requirement R5：描述符 ↔ Parameters DTO 反射校验

- **User story**：作为平台，我希望字段名漂移在上线前被拦下，而不是运行时参数丢失。
- **Scenario**：api-server 启动自检；插件中心上传；CI。
- **Functional behavior**：校验 `form[].field ⊆ params.dtoClass` 属性集；`strict=true` 时保存未声明字段返回 400；`strict=false`（过渡态）记 WARN 并保留字段。
- **Placement（依 §5.0，强制）**：校验器落在 R2 的新模块（L0）。**不改** `TaskChannel.parseParameters`、不加 Jackson 多态注册表、不动 `TaskPluginManager` —— 校验通过反射读取 `dtoClass` 属性完成，与上游反序列化路径解耦。若校验需介入保存链路，复用 R3 已登记的 2 处 L2 接入点，不新增上游改动。
- **Data/field changes**：无。
- **Edge cases**：DTO 含嵌套对象字段；继承属性；泛型；`@JsonIgnore` 字段。
- **Acceptance criteria**：
  1. 覆盖率报告：已注册插件 100% 完成校验。
  2. 人为制造字段名漂移 → 校验 FAIL 并列出差集。
  3. `strict` 灰度：先全量 `warn`，统计一个迭代后再按类型开启 `strict`；开启清单有记录与截止日期。
- **Priority**：P0

### Requirement R6：平台组件注册表与 props 契约

- **User story**：作为插件开发者，我希望复用平台已有复杂控件；作为平台方，我希望控件由平台统一维护与版本化。
- **Scenario**：描述符引用 `widget`。
- **Functional behavior**：首批 **6** 个组件 —— `code-editor`、`json-editor`、`datasource-picker`（现 `fields/use-datasource.ts`）、`udf-picker`（现 `StUdpUdfPanel.tsx`，含上传）、`seatunnel-compute-graph`（现 `SeatunnelComputeEditor.tsx`）、`resource-path`；每个组件有 props schema、版本号、组件级快照测试；描述符可声明 `minWidgetVersion`。
- **Placement（依 §5.0，强制）**：注册表与组件包装全部落在 `src/fork/`（L0）。其中 `datasource-picker` 的包装采取"引用上游字段构造器 / 在 fork 侧重新实现"的零改动方式；若最终必须动 `fields/use-datasource.ts`，则升为 L2 并登记台账。
- **Data/field changes**：无。
- **Edge cases**：同一表单多实例 widget；widget 内部异步取数失败。
- **Acceptance criteria**：
  1. 5 个组件（含 props schema + 快照测试）落地并可在描述符中引用。
  2. 引用不存在或版本不兼容 → 明确降级（告警 + 该字段只读），不整表崩溃。
  3. 新增 widget 的流程文档化（平台发版，不由插件方提交）。
- **Priority**：P1

### Requirement R7：插件中心（上传 / 校验 / 激活 / 回滚）

- **User story**：作为管理员，我希望在现场自助增加插件，并清楚知道"已配置但需重启才生效"。
- **Scenario**：已部署环境新增插件。
- **Functional behavior**：上传 jar → 同步校验报告 → 落 `plugins/task-plugins` → `PENDING_ACTIVATION` → 重启后 `ACTIVE`；同 type 更新保留上一份 yaml 可回滚；泛化 `SeaTunnelConnectorServiceImpl` 已有的安全逻辑（扩展名白名单、名称清洗、路径穿越防护、sha256、大小上限）。
- **Data/field changes**：新增插件登记表（type / jar 名 / sha256 / 描述符版本 / 状态 / 上传人 / 上传时间 / 上一版本指针）。
- **Edge cases**：同名不同 sha256；激活失败回滚；jar 被外部手工替换（与表记录不一致）；并发上传。
- **Acceptance criteria**：
  1. 仅管理员可访问，越权返回 403 且入审计。
  2. 所有动作审计可查（操作人、sha256、type、动作、结果、trace_id）。
  3. 校验拒绝无 SPI / 无 yaml 的 jar，错误信息可行动。
  4. 回滚后表单恢复上一版本，且列表状态正确。
  5. 与磁盘实际内容不一致时（手工替换 jar），启动自检报漂移。
- **Priority**：P1（依赖安全评审结论）

### Requirement R8：兼容与迁移

- **User story**：作为平台维护者，我希望渐进迁移，不搞一次性大爆炸。
- **Scenario**：历史类型共存。
- **Functional behavior**：`dynamic-task-type-config.yaml` + `static/**/*.json` 保留为 **override 层**，jar 为真值源，冲突记 WARN；`task-type-config.yaml` 降级为"允许清单"；UI 侧 `TASK_TYPES_MAP_BASE` 与 `constants/task-type.ts` 的重复映射**本期原样保留、不改**（二者均为上游既有文件，改属 L3 顺手重构，见 §6 Non-Goal 6）；fork 侧新增单一合并视图 `src/fork/task-type-registry.ts`，由描述符 + 两份上游映射派生，读取方通过 R3 的接入点走 fork 视图；SCSS 图标类仅作兜底。
- **Data/field changes**：无。
- **Edge cases**：override 与 jar 同时存在且文案不同；上游 DS 升级带来新硬编码类型。
- **Acceptance criteria**：
  1. 现有全部任务类型迁移前后表单行为逐字段一致（差分快照）。
  2. `constants/task-type.ts` 与 `store/project/task-type.ts` **零改动**（PR diff 中不得出现这两个文件）；重复映射的收敛改为 fork 视图派生，并记录为待上游修复项（可选向 Apache 提 issue）。
  3. 优先级规则（jar > override YAML > 上游硬编码映射 > 内置兜底）文档化并有单测。
- **Priority**：P1

### Requirement R9：SeaTunnel 连接器表单描述符化（dogfood）

- **User story**：作为数据开发工程师，我希望配置 UDP 源时看到类型化控件而不是裸 JSON。
- **Scenario**：SeaTunnel 计算任务的 UDP 源节点。
- **Functional behavior**：`connector-param-help.json` 从"只当文档"（`st-param-help.ts:56–77`）升级为 schema 来源；`defaultConnectorParams`（`seatunnel-compute-utils.ts:150–245`）硬编码默认值迁到描述符；参数区由裸 Monaco JSON（`StConnectorParamPanel.tsx:48–83`）改为类型化控件 + 前端校验，**保留"高级模式"JSON 编辑入口**；UDF 面板改由 `widget: udf-picker` 引用而非 `isUdpPlugin()` 硬编码门控。
- **Placement（依 §5.0）**：**L0** —— `seatunnel-compute-utils.ts`、`StConnectorParamPanel.tsx`、`StUdpUdfPanel.tsx`、`SeatunnelComputeEditor.tsx`、`connector-param-help.json`、`connectors-catalog.json` 全部是本 fork 新增文件，改动不产生上游冲突。唯一例外：`SeaTunnelConnectorServiceImpl` / `SeaTunnelConnectorController` 属上游 api 模块的 fork 新增文件（同样 L0）。本需求是"侵入成本最低、用户价值最高"的组合，**建议作为 Phase 2 的首个交付**。
- **Data/field changes**：无 DB；描述符文件新增。
- **Edge cases**：连接器无 help 条目 → 回落 `common` 段并保留 JSON 高级模式；多数据源脚本引用。
- **Acceptance criteria**：
  1. UDP 源全字段类型化（`parseMode` 下拉、`transport`、`workerThreads`、`soRcvBuf`、`udfName` 走 udf-picker）。
  2. 非法值（超范围/必填缺失）在保存前被拦截并指出字段。
  3. 高级模式仍可手工编辑最终 JSON（不剥夺专家能力）。
  4. `isUdpPlugin()` 硬编码门控删除。
  5. 死接口 `/seatunnel/connectors` 要么接上（目录来自描述符），要么删除并清理客户端。
- **Priority**：P1

### Requirement R10：执行体热加载（可选）

- **User story**：作为管理员，我希望新插件放入后不必重启。
- **Scenario**：频繁增删插件的环境。
- **Functional behavior**：每插件隔离 `URLClassLoader` + `ServiceLoader(classpath, loader)` 改造。
- **Acceptance criteria**：与现有 classpath ServiceLoader 及共享 `libs/` 无 ClassCastException；卸载后无类泄漏（内存快照验证）。
- **Priority**：P2 — **本期默认不做**，见决策点 D1。

### Requirement R11：收敛 SeaTunnel 作业装配为单一权威

- **User story**：作为平台维护者，我不能接受连接器参数改动要同时改两处。
- **Scenario**：新增/修改连接器参数映射。
- **Functional behavior**：Java 侧 `plugin/` 装配框架（现不被 `SeatunnelTask` 调用，旧接线仅存 `.custom-bak`）与前端 `assembleStockSeaTunnelConfig()`（`seatunnel-compute-utils.ts:380`）二选一为权威；**产品建议：以 Java 侧为唯一权威**（worker 执行体本为 Java，R5 的 DTO 校验也需后端权威，且 jar 作为真值源与方向一致），前端只保留编辑器与描述符渲染。
- **Placement（依 §5.0）**：装配框架本体（`plugin/` 包）与前端 `seatunnel-compute-utils.ts` 均为 fork 新增文件（L0）；但"**让 `SeatunnelTask` 重新调用 Java 装配**"这一处必须改上游文件 `SeatunnelTask.java`（**L2**，登记 patch 台账）。删除 `.custom-bak` 两个文件属清理，不计侵入。若 D2 决策改为"保留前端为权威"，则本需求可做到 **纯 L0**（只删 Java 死代码，不动上游）—— **这是 D2 决策的加分项，需一并权衡**。
- **Acceptance criteria**：
  1. 单一装配实现，另一份代码与 `.custom-bak` 删除。
  2. 装配结果回归对比（同图 → 同配置 JSON 逐字节一致）。
  3. 连接器参数变更只需改一处（含用例证明）。
- **Priority**：P0（依赖 D2 决策）

### Requirement R12：`UDP_PARSING_UDF` 定生死

- **User story**：作为用户，我不希望文档承诺的函数实际不生效。
- **Scenario**：在 transform SQL 中调用 `UDP_PARSING_UDF`。
- **现状缺陷**：`seatunnel/seatunnel-udpparsing/.../UdpParsingUDF.java:33–104` 类型转换逻辑**整段被注释**，实际原样返回入参；且 `log.info` 直接打印 UDP 载荷（:28,31），违反 SeaTunnel `AGENTS.md` 的"NEVER log sensitive information"约束；`resultType` 在 `for` 内首次迭代即 return（:21–26），写法可疑。
- **Acceptance criteria**：
  1. 决策记录：实现 or 下线，二者择一。
  2. 若实现：单测覆盖各 `SqlType` 分支；载荷日志移除或降级为长度/哈希。
  3. 若下线：从文档、`plugin-mapping`、UI 目录移除，并给出迁移说明。
  4. 无论哪种，日志中不出现原始 payload 内容。
- **Priority**：P0

### Requirement R13：清理硬编码环境值

- **Acceptance criteria**：
  1. `dolphinscheduler-task-seatunnel/src/main/resources/task.properties` 中内网 IP `10.11.15.149:30209` 移除，改配置项。
  2. `seatunnel-compute-utils.ts:206,231-232` 的 `D:/HPG/workspace/LHYF/...` Windows 路径改为运行时注入。
  3. CI 增加禁止性检查（内网 IP / 本地盘符路径 / `D:\` 模式）。
- **Priority**：P0

### Requirement R14：建立可追溯 git 基线

- **现状**：全仓仅 1 个 commit（整体导入），无法区分上游 diff 与自研 diff。
- **Acceptance criteria**：上游 tag（DS 3.4.3 / ST 3.0.0）+ 自研分支建立；`patches/ds-3.4.3-udf-resource/README.md:12–19` 列出的三处 in-tree 修复可被 diff 定位。
- **Priority**：P2

### Requirement R15：运行时产物出库

- **Acceptance criteria**：`.gitignore` 覆盖 `dolphinscheduler-data-api/logs/`、`dolphinscheduler-data-api/data/magic-api/`；清理 `seatunnel/${project.basedir}` 构建泄漏目录；CI 校验无日志/数据文件入库。
- **Priority**：P2

### Requirement R16：测试位置与静态状态规范化

- **Acceptance criteria**：
  1. `dolphinscheduler-task-seatunnel/src/main/test/**`（非标准路径，Maven 不执行）迁至 `src/test/java` 并确认真的被执行（`Tests run > 0`）。
  2. `getTaskTypeFactoryMaptest()` 暴露的可变静态状态移除。
  3. 清理 "这里是你要写开发者姓名" 之类占位注释。
- **Priority**：P2

### Requirement R17：数据服务多实例默认形态

- **Acceptance criteria**：`dolphinscheduler-data-api/README.md:113` 所述"本地文件存储、多实例各一份"改为文档默认推荐 `cluster` profile（共享库 `magic_api_file`，见 M14）；给出单实例→cluster 的迁移步骤与验证清单。
- **Priority**：P2

### Requirement R18：对齐 Phase 边界与对外口径

- **现状**：`dolphinscheduler-data-api/README.md:158–163` 将 app_key、OpenMetadata 列为"Phase 1 范围外"，但 `DATA-API-OPENMETADATA-PLAN.md:31–44` 的 M18（`da_app_credential` + `X-App-Id/X-App-Secret`）与 M22（血缘）已落地，且 `dolphinscheduler-task-plugin/dolphinscheduler-task-openmetadata/`（含 `SqlLineageParser`、`ColumnLineageParam`、`SeatunnelSqlExtractor`）已交付列级血缘。
- **Acceptance criteria**：README 的"范围外"清单与 PLAN 的实施结果一致；已交付项转为"已交付 + 边界"表述。
- **Priority**：P1

### Requirement R19：插件开发者接入指南

- **Acceptance criteria**：
  1. 端到端样例（jar + yaml + 图标 → 零前端代码接入）。
  2. widget 清单、props 契约、版本与申请流程。
  3. 契约演进规则（不兼容变更升 `apiVersion`）。
  4. 常见校验失败的排查指引。
- **Priority**：P1（依赖 R1–R5）

### Requirement R20：fork 隔离层与上游改动台账（原则落地载体）

- **User story**：作为研发 leader，我希望任何对开源代码的改动都被显式登记、可重放、可度量，以便升级到上游新版本时不丢改动、不陷入大范围冲突。
- **Scenario**：新需求接入、代码评审、上游版本升级演练。
- **Functional behavior**：
  1. 前端建立 `dolphinscheduler-ui/src/fork/`，自定义代码一律落此；`vite.config.ts` 用 `resolve.alias` 把需要替换的上游模块指向 fork 实现（替代 `.custom-bak` 人工备份）。
  2. 后端新增模块统一走 `META-INF/spring.factories` 自动装配（R2），避免改上游 Java 类。
  3. `dolphinscheduler/patches/` 沿用既有约定扩展为**上游改动台账**：每个 L2 改动一个目录，内含 diff/patch 文件、apply 脚本、被改上游文件的 hash 与行号锚点（现成先例 `patches/ds-3.4.3-udf-resource/`）。
  4. CI 校验：① alias 清单与实际 fork 文件一致；② 台账内 hash 与被改文件一致，不一致即失败；③ PR 中出现上游既有文件的改动时，必须在台账有对应条目，否则阻断。
- **Data/field changes**：新增台账目录结构与 CI 配置；无 DB。
- **Edge cases**：上游文件被移动/删除导致 patch 无法重放；同一文件多个 patch 叠加顺序；alias 与上游新增同名模块冲突。
- **Acceptance criteria**：
  1. `src/fork/` + alias 清单 + `patches/` 台账 + CI 三项校验全部落地并能在示例改动上跑通（含故意制造冲突使其失败）。
  2. `.custom-bak` 两个文件迁移进台账并被删除。
  3. 一次**模拟上游升级演练**（打上游 3.4.4/3.5.0 tag 做 merge）产出量化报告：冲突文件数、其中已登记比例（要求 100%）、重放耗时。
  4. 评审模板新增必填字段「侵入度（L0/L1/L2/L3）+ 台账条目链接」；出现 L3 即评审不通过（除 leader 特批）。
- **Priority**：**P0**（本原则的载体；R2/R3/R4/R5/R8 的 AC 均依赖它才能被验证）

---

### 8.20 描述符契约示例（`META-INF/ds-plugin.yaml`）

```yaml
apiVersion: ds.plugin/v1

task:
  type: SEATUNNEL_COMPUTE              # 必须 == TaskChannelFactory.getName()，后端强校验
  category: dataIntegration
  name: SeaTunnel 数据计算
  order: 20
  executeType: [BATCH, STREAM]
  helpLink: /docs/seatunnel/compute
  descriptorVersion: 1

icon:
  ref: META-INF/ds-plugin/icon.png     # 经 /task/plugin-descriptors/{type}/icon 下发
  fallback: default                    # 缺省时回落平台内置，不再需要 SCSS 类

locales:
  zh_CN:
    form.startupScript: 启动脚本
    form.parseMode: 解析模式
    form.udfName: 编解码脚本
    tip.parseMode: none 兼容旧逻辑；js 走脚本；offset_table 走偏移表 DSL
  en_US:
    form.startupScript: Startup Script
    form.parseMode: Parse Mode
    form.udfName: Codec Script
    tip.parseMode: none for legacy; js for script; offset_table for DSL

params:
  dtoClass: org.apache.dolphinscheduler.plugin.task.seatunnel.SeatunnelParameters
  strict: true                         # 未声明字段保存时拒绝（R5）；过渡期可设 false

form:
  - field: startupScript
    type: select
    labelKey: form.startupScript
    required: true
    optionsFrom: api:/task/plugin-descriptors/SEATUNNEL_COMPUTE/options/startup-script

  - field: stComputeGraph
    type: custom
    widget: seatunnel-compute-graph    # 平台组件，非插件自带代码
    widgetMinVersion: 1

  - field: pluginName
    type: select
    labelKey: form.pluginName
    optionsFrom: api:/task/plugin-descriptors/UDP/options/plugin-name

  - field: parseMode
    type: select
    labelKey: form.parseMode
    options: [none, js, offset_table, string, bytes, custom]
    defaultValue: none
    tipKey: tip.parseMode
    when: { pluginName: UDP }          # 条件可见（R3 需引擎侧新增语义）

  - field: udfName
    type: custom
    widget: udf-picker
    props: { api: /seatunnel/udp-codecs, accept: [".js", ".json"] }
    when: { parseMode: [js, offset_table] }

  - field: transport
    type: select
    options: [netty, jdk]
    defaultValue: netty
    when: { pluginName: UDP }

  - field: workerThreads
    type: input
    inputType: number
    defaultValue: 2
    validate: { min: 1, max: 32 }
    when: { transport: netty }

  - field: rawScript
    type: code-editor
    language: json
```

**要点说明**

| 段落 | 为什么这样设计 |
|---|---|
| `form[].type` 复用现有取值 | 避免新造表单引擎；`getElementByJson` 是唯一实现点 |
| `when` | 替代现状"闭包调 `updateElements` 硬写可见性"（`fields/use-datasource.ts`）与 `isUdpPlugin()` 特例 |
| `widget` | 复杂交互的受控扩展点；平台拥有、带 props schema 与快照测试 |
| `params.dtoClass` | 让 R5 反射校验有依据；这是把"前端字段"与"后端 DTO"钉在一起的唯一锚点 |
| `params.strict` | 参数丢失的根治开关；但会破坏既有工作流保存，必须灰度（见 R5 AC3） |
| `locales` 内嵌 | 文案改动不再需要前端发版；缺失时回落 `src/locales` |

---

## 9. Data and Permission Requirements

### 9.1 数据

| 对象 | 变更 | 说明 |
|---|---|---|
| 插件登记表（Phase 2） | 新增 | type / jar 名 / sha256 / 描述符版本 / 状态(`PENDING_ACTIVATION`/`ACTIVE`/`INVALID`) / 上传人 / 上传时间 / 上一版本指针 |
| `task-type-config.yaml` | 语义降级 | 从"清单真值源"降为"允许清单" |
| `dynamic-task-type-config.yaml` | 角色变更 | 从主源降为 override 层 |
| `static/**/*.json` | 角色变更 | 保留为 override，长期目标迁入 jar |
| `task_definition.task_params` | **无 schema 变更** | 但 R5 `strict` 会改变保存时的接受集合 |
| SeaTunnel `connector-param-help.json` | 角色升级 | 从文档源变为 schema 源（R9） |
| `da_api_invoke_log` | 复用口径 | 插件中心操作审计沿用其 trace_id / 异步落库模式 |

### 9.2 权限矩阵

| 动作 | 管理员 | 普通用户 | 插件开发者 |
|---|---|---|---|
| 查看描述符清单/单个 | ✅ | ✅ | ✅ |
| 获取图标 | ✅ | ✅ | ✅ |
| 配置已描述符化插件 | ✅ | 按项目权限 | — |
| 查看未描述符化插件（只读视图） | ✅ | ✅（+告警） | — |
| 上传/更新/回滚 jar | ✅ | ❌（403 + 审计） | — |
| 修改 override 层 YAML | ❌（需重新打包） | ❌ | — |

### 9.3 安全要求

1. **只读解析不执行代码**：R2 必须有断言证明不加载插件类（AC4）。
2. **jar 落盘仍是执行体入口**：worker 重启后会加载执行 → 管理员独占 + 审计 + 状态可见 + 可选签名（默认关闭）。
3. **zip 防护**：条目数上限、解压比上限、单 entry 大小上限、路径穿越拒绝（泛化 `SeaTunnelConnectorServiceImpl:159`）。
4. **接口限流**：描述符接口独立限流，插件中心接口更严格。
5. **不记录敏感内容**：R12 要求载荷不入日志；插件中心日志记 sha256 不记内容。

---

## 10. Acceptance Criteria（整体交付）

| # | 路径 | 可执行验证 | 归属 |
|---|---|---|---|
| AC-1 | Happy path | 取一个未描述符化的简单插件，仅加 jar 内 `ds-plugin.yaml` + 图标，**不提交任何 UI 代码**，在画布上渲染出正确类型化表单并保存成功；记录 PR diff 文件清单为 0。 | R1,R2,R3 |
| AC-2 | 热生效 | api-server 不重启，放入新 jar → 刷新 → 表单可渲染并可保存（执行需重启，状态显示 `PENDING_ACTIVATION`）。 | R2,R7 |
| AC-3 | Primary error | yaml 字段名与 DTO 不一致 → 上传返回 FAIL 并列出差集；前端不出现错误表单。 | R5,R7 |
| AC-4 | 数据完整性 | 后端有类型 / 前端未注册：加载→不改→保存，`taskParams` 逐字节不变；UI 显示只读 + 告警；保存按钮禁用。 | **R4** |
| AC-5 | 权限 | 普通用户调用插件中心上传接口 → 403 且审计留痕。 | R7 |
| AC-6 | 兼容 | 现有全部任务类型迁移前后表单行为逐字段差分快照一致。 | R8 |
| AC-7 | 一致性收敛 | 修改一个 SeaTunnel 连接器参数映射，只需改一处并能被用例证明。 | R11 |
| AC-8 | 环境无关 | CI 扫描无内网 IP / 本地盘符路径；在 Linux 与 Windows 现场均可跑通 UDP 源配置。 | R13 |
| AC-9 | 可观测 | 描述符解析失败数、`strict` 拒绝数、只读降级命中次数有指标可查。 | R2,R4,R5 |
| AC-10 | 安全 | 畸形 yaml / zip-bomb / 路径穿越 单测全部 FAIL 且不产生副作用。 | R2 |
| AC-11 | **上游侵入度** | Phase 1+2 全部 PR 合并后，统计被改动的**上游既有文件**清单：数量 ≤ 5、每个 ≤ 10 行、且 100% 出现在 `patches/` 台账；`constants/task-type.ts`、`store/project/task-type.ts`、`format-data.ts`、`dag.module.scss`、`locales/*`、`TaskChannelFactory.java`、`TaskChannel.java`、`TaskPluginManager.java` **不得出现在清单中**。 | R20 |
| AC-12 | 升级可重放 | 模拟上游 3.4.4/3.5.0 merge：patch 全部自动重放成功，剩余冲突 ≤ 5 文件且全部已登记；重放后跑通 AC-1/AC-4 回归。 | R20 |

---

## 11. Rollout and Changelog Notes

### 分期

| 阶段 | 内容 | 交付判据 |
|---|---|---|
| **Phase 0（立即，不等排期）** | R4 只读降级；R13 硬编码清理；**R20 台账骨架先行**（先建目录与 CI 校验，即使暂时只有 1 条记录） | 参数丢失路径关闭；现场无内网 IP 依赖；上游改动开始可登记 |
| **Phase 1** | R1 契约、R2 只读解析+接口（新模块）、R3 渲染器（fork overlay）、R5 校验、R8 兼容层、**R20 全量落地** | AC-1/2/3/6/10/11 通过；纵向切片 1 个插件迁移 |
| **Phase 2** | R6 组件注册表、R9 SeaTunnel dogfood（纯 L0）、R11 装配收敛（视 D2 可能纯 L0）、R7 插件中心（需安全评审）、R12、R18、R19 | AC-7 通过；UDP 源全字段类型化 |
| **Phase 3（可选）** | R10 真热加载、R14–R17 工程卫生 | AC-12 升级演练 + 视 D1 决策与现场反馈 |

### 灰度策略

1. `params.strict` 先全量 `warn` 一个迭代 → 按类型逐个开启（R5 AC3）。
2. 渲染器与硬编码表共存期：override 层优先返回既有硬编码表单，逐类型切换并留开关。
3. 插件中心默认关闭，配置项 `plugin-center.enabled=false` 起步，现场验证后开启。

### 回滚

- 描述符渲染器有全局开关，关闭后回退硬编码路径（共存期内保留）。
- 描述符版本可按类型回滚上一份 yaml（R7 AC4）；jar 本体回滚需运维手工替换 + 重启。

### Changelog 用户可见条目（草案）

- 新增插件类型不再需要平台发版即可配置。
- SeaTunnel UDP 等连接器参数由手写 JSON 改为类型化表单，带实时校验与中英语提示。
- 无法识别的任务类型不再显示错误的表单，改为只读视图并明确提示。
- 管理员可在插件中心上传/更新/回滚任务插件，并看到激活状态。

---

## 12. Risks and Open Questions

### 12.1 风险

| 风险 | 等级 | 说明 | 缓解 |
|---|---|---|---|
| `strict=true` 打破既有工作流保存 | 高 | 历史 `taskParams` 含未声明字段时会被拒 | R5 AC3 灰度：先 warn 一个迭代再按类型开启 |
| jar 上传=执行体入口 | 高 | 只读解析安全，但重启后被 worker 执行 | 管理员独占 + 审计 + 可选签名 + `PENDING_ACTIVATION` 显性化 |
| `when` 语义引入的引擎改动 | 中 | R3 唯一引擎侧改动 | 降级方案：Phase 1 不支持条件可见，用 `optionsFrom` 兜住 |
| SPI 加 default 方法的重编译影响 | — | 触及上游 `TaskChannelFactory`，属 L3 | **已由 §5.0 排除**：改走 jar 内声明式文件 + ZipFile 只读解析（同时换来"不重启即生效"） |
| fork overlay 与上游文件漂移 | 中 | alias 指向的 fork 实现在上游升级后不再等价，静默行为分叉 | CI hash 校验（R20 AC1）+ alias 清单尽量小 + 升级演练 AC-12 |
| "顺手重构上游"的执行纪律 | 中 | 评审时容易被"这样更干净"说服，导致侵入面扩大 | §6 Non-Goal 6 + 评审模板必填侵入度字段 + AC-11 禁止文件清单 |
| SeaTunnel 装配权威切换（R11）工作量 | 高 | 从前端迁回 Java 是重活 | 作为 D2 决策项单独评估，不阻塞 Phase 1 |
| 上游 DS 升级带来新硬编码类型 | 中 | 新类型仍走旧路 | 描述符机制作为 override 层之上的默认 |
| zip 解析 DoS | 中 | 恶意/超大 jar | 条目/解压比/大小上限 + 独立限流（AC-10） |
| UI 与描述符视觉不一致 | 低 | 控件宽度/分组差异 | 设计走查 + 差分快照 |

### 12.2 决策点（需人工确认，阻塞排期）

| 编号 | 问题 | 产品建议 | 决策人 |
|---|---|---|---|
| **D1** | 执行体真热加载（R10）是否纳入本期？ | **本期不做** —— "前端表单热生效 + 执行体重启"已解决用户实际痛点，而 `URLClassLoader` 与共享 `libs/` 的 ClassCastException/依赖冲突成本高 | 平台工程负责人 |
| **D2** | SeaTunnel 作业装配权威侧（R11）：Java 还是前端？ | **迁回 Java 侧** —— worker 执行体是 Java、R5 的 DTO 校验需后端权威、且与"jar 为唯一真值源"方向一致 | 平台工程负责人 |
| **D3** | 描述符承载方式：jar 内 yaml（A1）vs 扩展 SPI 方法返回对象？ | **已裁定：yaml。** 两条理由同向 —— ① ZipFile 只读解析是"不重启即生效"的技术前提；② 扩展 SPI 需改上游 `TaskChannelFactory`，属 §5.0 禁止的 L3。请架构评审复核此裁定而非重开选项 | 数据架构 / 设计评审 |
| **D4** | 是否引入 jar 签名体系？ | 提供配置位、默认关闭，现场有外部插件来源时再启用 | 安全评审 |
| **D5** | widget 申请是否需走平台发版窗口？ | 是 —— 这是保证表单引擎可演进的前提 | UI 负责人 |

### 12.3 待确认信息（证据缺口）

- G1：现网"后端有插件 / 前端未注册"的真实案例数量（影响 R4 的紧迫度评级）。
- G2：是否存在插件 jar 由第三方提供（影响 D4）。
- G3：`connector-param-help.json` 的生成方式（手工 / 脚本），决定 R9 中它作为 schema 源的可信度。
- G4：上游 DS 3.5 的插件 UI 机制是否已有官方方案（若有，应优先对齐而非自研）—— **属竞品/上游跟踪，可另开 research 任务**。

### 12.4 明确的证据边界

本文 §2 全部为代码实测（带 `文件:行号`）。§12.3 各项为缺口，未作臆测。`UDP_PARSING_UDF`、死接口 `/seatunnel/connectors`、双装配三处结论，均已在会话中经二次读取源码确认。

---

## 附录 A：需求追溯矩阵

| ID | 标题 | 优先级 | 依赖 | 阶段 | 侵入度 | 上游侧动作 | 来源 |
|---|---|---|---|---|---|---|---|
| R1 | 描述符契约 ds-plugin/v1 | P0 | — | P1 | L0 | 文档 + JSON Schema（新增） | 本设计 |
| R2 | 只读解析 + 下发接口 | P0 | R1 | P1 | **L1** | `dolphinscheduler-api/pom.xml` +1 依赖、dist assembly +1 行 | 本设计 |
| R3 | 前端描述符渲染器 | P0 | R1,R2 | P1 | **L2 ×2** | `node/tasks/index.ts`、`dag-sidebar.tsx` 各 ≤5 行 | 本设计 |
| R4 | 未知类型显式降级 | **P0-Blocker** | R3 | **P0** | **L2 ×2** | `use-task.ts` fallback、`task-type.ts:212` 过滤行 | 本次代码勘查新发现 |
| R5 | 描述符↔DTO 校验 | P0 | R1 | P1 | L0 | 无（复用 R2/R3 已登记点） | 本设计 |
| R6 | 平台组件注册表 | P1 | R1 | P2 | L0（`datasource-picker` 可能 L2） | 尽量无 | 本设计 |
| R7 | 插件中心 | P1 | R2,R5,R20 | P2 | **L1** | `pom.xml` 模块行 + 新表 DDL（`dao/resources/sql` 追加，不改既有脚本） | 本设计 + 安全评审 |
| R8 | 兼容与迁移 | P1 | R2,R3 | P1 | L0 | **两份重复映射原样保留**，改由 fork 视图派生 | 本设计 |
| R9 | SeaTunnel 表单描述符化 | P1 | R3,R5,R6 | P2 | **L0** | 无（目标文件全是 fork 新增） | 本设计 |
| R10 | 执行体热加载 | P2 | R7 | P3 | **L3 风险** | 需改 `TaskPluginManager`/`PrioritySPIFactory` → **原则下不建议本期做** | 本设计（D1） |
| R11 | 装配收敛单一权威 | P0 | R9 | P2 | L2 或 L0（取决于 D2） | `SeatunnelTask.java` 一处，或仅删 Java 死代码 | 上一轮工程分析 |
| R12 | `UDP_PARSING_UDF` 定生死 | P0 | — | P2 | L0 | 无（`seatunnel-udpparsing` 为 fork 新增模块） | 上一轮工程分析 |
| R13 | 清理硬编码环境值 | P0 | — | **P0** | L0 | 无（`task.properties`/`seatunnel-compute-utils.ts` 均 fork 新增） | 上一轮工程分析 |
| R14 | git 基线可追溯 | P2 | — | P3 | — | 分支/tag 策略 | 上一轮工程分析 |
| R15 | 运行时产物出库 | P2 | — | P3 | L1 | `.gitignore`（fork 已改文件） | 上一轮工程分析 |
| R16 | 测试位置与静态状态 | P2 | — | P3 | L0 | 无（fork 新增插件模块内） | 上一轮工程分析 |
| R17 | 数据服务多实例默认态 | P2 | — | P3 | L0 | 无（`dolphinscheduler-data-api` 为 fork 新增模块） | 上一轮工程分析 |
| R18 | Phase 边界口径对齐 | P1 | — | P2 | L0 | 无 | 上一轮工程分析 |
| R19 | 插件开发者接入指南 | P1 | R1–R5 | P2 | L0 | 无 | 本设计 |
| R20 | fork 隔离层与上游改动台账 | **P0** | — | P0 起步 / P1 全量 | **L1** | `vite.config.ts` alias、`patches/` 台账、CI 配置 | §5.0 原则 |

**侵入度汇总（Phase 0–2）**：上游既有文件改动 **预计 ≤ 5 个文件、合计 ≤ 20 行**（R3 ×2 + R4 ×2 + R11 ×1），其余全部为 L0/L1。这就是本方案"降低后续合并影响"的量化形态，也是 AC-11 的核对基线。

## 附录 B：参考文件索引

**前端接线（待消除）**：`store/project/types.ts:23`、`store/project/task-type.ts:25-171,212`、`views/projects/task/constants/task-type.ts:17-56,60-211`、`node/tasks/index.ts:18-100`、`node/fields/index.ts:18-96`、`node/format-data.ts:42-522`、`dag/dag.module.scss:196-305`、`locales/{en_US,zh_CN}/project.ts`

**可复用引擎**：`src/components/form/get-elements-by-json.tsx`、`src/service/modules/dynamic-dag/index.ts`

**后端现状**：`TaskChannelFactory.java:25-33`、`TaskChannel.java:22-27`、`FavTaskController.java:68`、`TaskTypeConfiguration.java:39-40`、`task-type-config.yaml:18-33`、`DynamicTaskTypeController.java:61,76`、`PrioritySPIFactory.java:33`、`TaskPluginManager.java:47,63-101`、`ShellTaskChannel.java:33-34`、`install-plugins.sh:52,58`、`assembly-plugins.sh:62-75`

**先例**：`SeaTunnelConnectorController.java:59,69,79`、`SeaTunnelConnectorServiceImpl.java:53,69-77,99-131,134-176,178-198,205-209`

**SeaTunnel dogfood 面**：`connectors-catalog.json`、`connector-param-help.json`、`st-param-help.ts:56-77`、`StConnectorParamPanel.tsx:48-83`、`StUdpUdfPanel.tsx:272-278`、`SeatunnelComputeEditor.tsx:414`、`seatunnel-compute-utils.ts:35-73,150-245,380,450-507`、`use-st-connector.ts:46-59`

**待修缺陷**：`seatunnel-udpparsing/.../UdpParsingUDF.java:21-26,28,31,33-104`、`task.properties`（内网 IP）

---

## 附录 C：交接与开发约束（给数据设计评审 + 数据方向研发 leader）

### C.1 本次请求你们做的三件事

| 对象 | 请求 | 阻塞项 |
|---|---|---|
| **数据设计 / 架构评审** | ① 复核 §5.0 侵入度分级与 L3 禁令是否作为平台级规范采纳；② 复核 D3 裁定（yaml 承载）；③ 给出 `ds-plugin/v1` 契约的评审意见（R1 AC1–AC5）；④ 描述符表单与现有表单的视觉一致性走查（§7.4） | 阻塞 R1 冻结 |
| **数据方向研发 leader** | ① 决策 D1（执行体热加载是否本期做，产品建议不做）与 **D2（SeaTunnel 装配权威侧）**；② 按 Phase 0/1/2 排期并指派 api、ui、插件三块 owner；③ 认可 AC-11/AC-12 作为合并成本的验收门（即允许把"上游零冲突"写进 DoD）；④ R7 送安全评审（D4） | D2 阻塞 R9/R11；owner 指派阻塞 Phase 1 启动 |
| **双方共同** | 确认 §12.3 四个证据缺口（G1–G4）由谁补齐 | 影响 R4 紧迫度评级与 R9 的 schema 可信度 |

### C.2 开发必须遵守的硬约束（评审时逐条核对）

1. **Java 1.8**：后端不得假设 11+ API（`dolphinscheduler-api-test` 是唯一 Java 11 孤岛）。
2. **Spring Boot 2.6.1**：新模块用 `META-INF/spring.factories` 的 `EnableAutoConfiguration` 接入 api-server，**不要**指望被 `org.apache.dolphinscheduler.api` 包扫描命中，也不要改上游启动类。
3. **Spotless 覆盖 Java / pom / Markdown**：提交前 `./mvnw spotless:apply`，CI `spotless:check` 会失败；本 PRD 文件本身也在覆盖范围。
4. **commit 规范** `[Type-ISSUE_ID][Scope] Subject`；上游既有文件的改动必须在 `patches/` 台账留条目并在 commit message 引用条目号。
5. **不得静默破坏 wire / DB 兼容**：`extract-*` RPC、`dao` 实体、枚举值、`spi.DbType` 属禁止清单；本方案刻意不触碰它们。
6. **PR 范围收紧**：一个 PR 一个 concern；R20 的 CI 校验应先于 R2/R3 的功能 PR 合入。
7. **不动上游即默认正确**：任何 PR 若 diff 中出现 §12 之外的上游既有文件，评审直接打回，除非附 leader 特批。

### C.3 建议的最小启动集（若资源紧张）

只投 3 个人 · 一个迭代即可完成：
- **R4**（0.5 人 · Phase 0，纯修复线上风险，不依赖任何新功能）
- **R20**（0.5 人 · 台账 + alias + CI，后续所有需求的 DoD 载体）
- **R1 + R2 + R3 + R5 的纵向切片**（2 人 · 只迁 1 个简单插件到描述符，跑通 AC-1/AC-2/AC-11）

其余（R6–R19）在纵向切片通过后再排，避免契约未稳定就大规模迁移。
