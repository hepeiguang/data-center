# UDP 接收与解析扩展（Netty + Codec SPI）

## 传输层（transport）

| 值 | 说明 |
|----|------|
| `netty`（默认） | `NioDatagramChannel` 收包，队列交给 SeaTunnel `pollNext` |
| `jdk` | 兼容旧实现：`DatagramSocket.receive` |

相关参数：`workerThreads`、`receiveQueueSize`、`soRcvBuf`、`bufferSize`。

## parseMode

| 值 | 说明 |
|----|------|
| `none` | 兼容旧逻辑：`format=json\|text` |
| `js` | JavaScript：`parse(payload, meta)`，支持文件热加载 |
| `offset_table` | 偏移表 DSL（游标解析，支持循环/控制字/分支） |
| `string` | 内置：payload → UTF-8 `content` |
| `bytes` | 内置：payload → Base64 `content` |
| `custom` | `decoderClass`（FQCN）或 `decoderName`（SPI 名） |

`parseErrorHandleWay`：`fail`（默认，作业失败）| `skip`（丢弃坏包并打日志）。

## 自定义解码器 SPI

实现接口：

```java
public interface PacketCodec {
  List<Map<String, Object>> decode(byte[] payload, PacketMeta meta) throws Exception;
}

public interface PacketCodecProvider {
  String name();
  PacketCodec create(UdpSourceConfig config) throws Exception;
}
```

注册方式任选其一：

1. **SPI**：`META-INF/services/org.apache.seatunnel.connectors.seatunnel.udp.codec.PacketCodecProvider`
2. **配置**：`parseMode=custom` + `decoderClass=com.example.MyCodec`（实现 `PacketCodec` 或 `PacketCodecProvider`，需无参构造）
3. **按名**：`parseMode=custom` + `decoderName=myCodec`（匹配 SPI `name()`）

示例：

```hocon
source {
  UDP {
    host = "0.0.0.0"
    port = 9999
    transport = netty
    workerThreads = 2
    parseMode = custom
    decoderClass = "com.example.udp.MyBinaryCodec"
    parseErrorHandleWay = skip
  }
}
```

## JS 契约

```js
/**
 * @param payload Java byte[] 原始 UDP 载荷
 * @param meta { remoteHost, remotePort, length, ts }
 * @return object | object[]  字段将转为 SeaTunnelRow（经 JSON）
 */
function parse(payload, meta) {
  return { msgType: payload[0] & 0xff, value: payload[1] & 0xff };
}
```

配置：`parseMode=js`，二选一：

- `udfName=my_codec` → `$SEATUNNEL_HOME/udf/udp-codec/my_codec.js`（流程编排推荐）
- `scriptPath` 指向任意 `.js`（变更后按 mtime 热加载）

## 偏移表 DSL

配置：`parseMode=offset_table`，二选一：`udfName`（→ `udf/udp-codec/<name>.json`）或 `offsetTablePath` / 内联 `offsetTable`。

```json
{
  "byteOrder": "BIG_ENDIAN",
  "steps": [
    { "op": "read", "name": "magic", "type": "uint16" },
    { "op": "read", "name": "ctrl", "type": "uint8" },
    {
      "op": "switch",
      "on": "ctrl",
      "cases": [
        {
          "when": 1,
          "steps": [
            { "op": "read", "name": "count", "type": "uint8" },
            {
              "op": "loop",
              "countFrom": "count",
              "as": "items",
              "steps": [
                { "op": "read", "name": "id", "type": "uint16" },
                { "op": "read", "name": "val", "type": "int32" }
              ]
            }
          ]
        },
        {
          "when": 2,
          "steps": [
            { "op": "read", "name": "flag", "type": "uint8" },
            { "op": "skip", "length": 3 }
          ]
        },
        { "default": true, "steps": [{ "op": "skip", "length": 4 }] }
      ]
    },
    { "op": "assert", "field": "magic", "eq": 43981 }
  ]
}
```

### 操作

| op | 含义 |
|----|------|
| `read` | 读字段；可选 `offset` 绝对定位，否则游标；`length`/`lengthFrom` 用于 bytes/string |
| `loop` | 循环：`count` 固定次数，或 `countFrom` 取已读控制字；结果写入 `as` 数组 |
| `switch` | 按已读字段分支：`on` + `cases[].when` / `default` |
| `skip` | 跳过字节：`length` 或 `lengthFrom` |
| `assert` | 断言字段值：`eq`（失败抛错） |

### 类型

`uint8` `int8` `uint16` `int16` `uint32` `int32` `int64` `float32` `float64` `bytes` `string` `hex`

`byteOrder`: `BIG_ENDIAN`（默认）| `LITTLE_ENDIAN`
