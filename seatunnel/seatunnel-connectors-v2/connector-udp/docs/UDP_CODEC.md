# UDP 解析扩展（JS + 偏移表）

## parseMode

| 值 | 说明 |
|----|------|
| `none` | 兼容旧逻辑：`format=json\|text` |
| `js` | JavaScript：`parse(payload, meta)`，支持文件热加载 |
| `offset_table` | 偏移表 DSL（游标解析，支持循环/控制字/分支） |

## parseErrorHandleWay

| 值 | 说明 |
|----|------|
| `skip`（默认） | 单包解析失败只打日志并丢弃该包，继续收包 |
| `fail` | 单包解析失败抛错，作业失败 |

截断包、越界 `offset`/`length`、非法 `loop.count`、`assert` 失败均走该策略。

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

配置：`parseMode=js`，`scriptPath` 指向 `.js`（变更后按 mtime 热加载）。

## 偏移表 DSL

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

- `bytes`：输出 **Base64 字符串**（便于 JSON / schema）
- `hex`：输出大写十六进制文本

`byteOrder`: `BIG_ENDIAN`（默认）| `LITTLE_ENDIAN`

读字段 / skip / loop 会校验 remaining 与非负长度；`loop.count`/`countFrom` 上限 `1000000`。
