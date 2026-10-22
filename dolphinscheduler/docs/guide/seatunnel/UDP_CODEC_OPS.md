# UDP 算子（JS / 偏移表）操作文档

本文说明本地 SeaTunnel 3.0.0 上 **UDP Source** 的脚本解析、偏移表解析、安装部署、联调与 DS 集成约定。

---

## 1. 能力概览

| 解析模式 `parseMode` | 说明 |
|----------------------|------|
| `none` | 兼容旧逻辑：`format=json\|text` |
| `js` | JavaScript `parse(payload, meta)`，支持文件 **mtime 热加载** |
| `offset_table` | 游标式偏移表 DSL（`read` / `loop` / `switch` / `skip` / `assert`） |

收包层仍为 JDK `DatagramSocket`（非 Netty）。码流以 **原始 `byte[]`** 交给解析器，避免 charset 破坏二进制。

源码位置：`seatunnel_code/seatunnel-connectors-v2/connector-udp/`  
DSL 说明：`connector-udp/docs/UDP_CODEC.md`

---

## 2. 目录与产物

| 路径 | 用途 |
|------|------|
| `apache-seatunnel-3.0.0/connectors/connector-udp-3.0.0.jar` | UDP 插件 JAR（勿放两份不同版本号同名插件） |
| `connectors/plugin-mapping.properties` | 含 `seatunnel.source.UDP` / `seatunnel.sink.UDP` |
| `udf/udp-codec/` | JS / 偏移表 JSON 热插拔目录 |
| `config/jobs/udp-js-console.conf` | JS 联调样例 |
| `config/jobs/udp-offset-console.conf` | 偏移表联调样例 |

环境变量：

```bat
set SEATUNNEL_HOME=D:\HPG\workspace\LHYF\apache-seatunnel-3.0.0
set JAVA_HOME=D:\HPG\workspace\LHYF\tools\jdk-11\jdk-11.0.2
```

DolphinScheduler（API/Worker）配置项：

```yaml
seatunnel:
  home: ${SEATUNNEL_HOME}
  plugin: ${SEATUNNEL_HOME}/plugins
  udp-codec: ${SEATUNNEL_HOME}/udf/udp-codec
```

---

## 3. 编译与安装插件

前置：JDK 11、Maven 3.9+（可用阿里云镜像）。

```bat
set JAVA_HOME=D:\HPG\workspace\LHYF\tools\jdk-11\jdk-11.0.2
set PATH=D:\HPG\software\apache-maven-3.9.6\bin;%JAVA_HOME%\bin;%PATH%
cd D:\HPG\workspace\LHYF\seatunnel_code
mvn -s D:\HPG\workspace\LHYF\tools\maven-settings-aliyun.xml ^
  -pl seatunnel-connectors-v2/connector-udp -am package -DskipTests
```

安装（**只保留一份** connector-udp JAR）：

```bat
copy /Y seatunnel-connectors-v2\connector-udp\target\connector-udp-2.3.10.jar ^
  %SEATUNNEL_HOME%\connectors\connector-udp-3.0.0.jar
```

确认 `plugin-mapping.properties`：

```properties
seatunnel.source.UDP = connector-udp
seatunnel.sink.UDP = connector-udp
```

---

## 4. JS 脚本约定

入口：

```js
/**
 * @param payload Java byte[]
 * @param meta { remoteHost, remotePort, length, ts }
 * @return object | object[]
 */
function parse(payload, meta) {
  return { a: payload[0] & 0xff, b: payload[1] & 0xff, remoteHost: meta.remoteHost };
}
```

放置：`%SEATUNNEL_HOME%\udf\udp-codec\demo_parse.js`  
修改文件后 **无需重启 Job**，下一包按 mtime 重新加载。

作业片段：

```hocon
source {
  UDP {
    host = "127.0.0.1"
    port = 19879
    parseMode = "js"
    scriptPath = "D:/HPG/workspace/LHYF/apache-seatunnel-3.0.0/udf/udp-codec/demo_parse.js"
    schema = { fields { a = "int" b = "int" remoteHost = "string" } }
  }
}
sink { Console {} }
```

---

## 5. 偏移表约定

核心：`steps` 游标前进；`loop.countFrom` / `switch.on` 使用已读字段（控制字）。

示例见 `udf/udp-codec/demo_offset.json`。作业：

```hocon
source {
  UDP {
    host = "127.0.0.1"
    port = 19880
    parseMode = "offset_table"
    offsetTablePath = "D:/HPG/workspace/LHYF/apache-seatunnel-3.0.0/udf/udp-codec/demo_offset.json"
    schema = {
      fields {
        magic = "int"
        ctrl = "int"
        count = "int"
        items = "string"
      }
    }
  }
}
```

嵌套 `items` 数组会序列化为 JSON 字符串写入 `STRING` 字段（便于 Console 查看）。复杂结构可在下游 Transform 再拆。

---

## 6. 联调步骤（本地）

### 6.1 JS

```bat
cd %SEATUNNEL_HOME%
bin\seatunnel.cmd -c config\jobs\udp-js-console.conf -e local
```

另开终端发送：

```powershell
$c=New-Object System.Net.Sockets.UdpClient
$c.Send([byte[]](10,20),2,'127.0.0.1',19879)
$c.Close()
```

期望日志类似：

```text
SeaTunnelRow#kind=INSERT : 10, 20, 127.0.0.1
```

### 6.2 偏移表

```bat
bin\seatunnel.cmd -c config\jobs\udp-offset-console.conf -e local
```

发送（大端：magic=0xABCD, ctrl=1, count=2, 两组 id/val）：

```powershell
$buf=[byte[]](0xAB,0xCD,0x01,0x02,0x00,0x0A,0x00,0x00,0x00,0x64,0x00,0x14,0x00,0x00,0x00,0xC8)
$c=New-Object System.Net.Sockets.UdpClient
$c.Send($buf,$buf.Length,'127.0.0.1',19880)
$c.Close()
```

期望：

```text
INSERT : 43981, 1, 2, [{"id":10,"val":100},{"id":20,"val":200}]
```

### 6.3 已知环境问题

Windows 上若未配置 `HADOOP_HOME`，STREAMING 作业可能在 **checkpoint** 阶段报错并取消；**不影响已成功打印的解析结果**。生产建议配置 checkpoint 存储为本地可用路径，或安装 winutils / 设置 `HADOOP_HOME`。

插件加载报 `Cannot find unique plugin jar`：说明 `connectors` 下存在多个 `connector-udp-*.jar`，删除重复只留一个。

---

## 7. DolphinScheduler 集成

### 7.1 DAG 表单（UDP 输入）

- **解析模式**：`none` / `js` / `offset_table`
- **UDF 名称**：对应 `udf/udp-codec/{name}.js` 或 `.json`
- 亦可直接填 `scriptPath` / `offsetTablePath` / 内联 `scriptContent` / `offsetTable`
- `js` / `offset_table` 时需配置 **schema**（字段与脚本或偏移表输出对齐）

### 7.2 UDF 热插拔

1. 资源中心上传 `.js` / `.json`（类型选 SeaTunnel UDF）
2. API 创建 UDF：文件会落到 `seatunnel.plugin`，并 **同步复制** 到 `seatunnel.udp-codec`
3. UDP 节点 `udfName` 填资源文件名（如 `demo_parse`）
4. Worker 需能访问同一 `SEATUNNEL_HOME`（或共享盘）；覆盖文件后下一包生效

Worker 启动请带上 `SEATUNNEL_HOME`，以便 `SourceUdpPlugin` 解析 `udfName` → 绝对路径。

### 7.3 与官方插件关系

本阶段仅自研 **UDP**；JDBC / Socket / HTTP 等使用官方 connector JAR（已装到 `connectors/`）。官方 Socket 为 **TCP**，不能替代 UDP。

---

## 8. 单元 / Smoke 测试

不依赖完整 Maven 工程时，可用：

```bat
cd D:\HPG\workspace\LHYF\tools\udp-codec-smoke
rem 编译与 ALL_PASS 见历史 smoke 脚本；偏移表 + JS 解析逻辑
```

工程内 JUnit：`connector-udp/src/test/java/.../OffsetTableParserTest.java`、`JsPacketCodecTest.java`  
（`mvnw` 修复后可 `-Dtest=OffsetTableParserTest,JsPacketCodecTest`）

---

## 9. 排障清单

| 现象 | 处理 |
|------|------|
| 找不到 UDP 插件 | 检查 JAR 与 `plugin-mapping`；只保留一个 udp jar |
| JS 无 ScriptEngine | 使用 JDK 11（Nashorn）；更高 JDK 需换 GraalJS |
| schema 对不上 | Console 打出字段后对齐 `schema.fields` |
| udfName 找不到文件 | 看 `udf/udp-codec` 是否同步；文件名与后缀 |
| 端口占用 | 换 `port` 或结束残留 `SeaTunnelClient` 进程 |

---

## 10. 版本说明

当前插件按 **seatunnel_code 2.3.10** 编译，已在本地 **SeaTunnel 3.0.0** 引擎上完成 JS / 偏移表收包联调。后续若升级 connector API，需针对 3.0.0 重新编译对齐 SPI。

后续可选：Python 脚本解析、Netty 收发、DS 侧 UDF 下拉列表自动枚举 `udp-codec` 目录。
