/**
 * Generate connector-param-help.json from SeaTunnel zh docs option tables.
 * Usage: node scripts/gen-st-connector-param-help.mjs
 */
import fs from 'fs'
import path from 'path'
import { fileURLToPath } from 'url'

const __dirname = path.dirname(fileURLToPath(import.meta.url))
const UI_ROOT = path.resolve(__dirname, '..')
const ST_DOCS = path.resolve(UI_ROOT, '../../seatunnel/docs/zh')
const CATALOG = path.resolve(
  UI_ROOT,
  'src/views/projects/task/components/node/fields/seatunnel-compute/connectors-catalog.json'
)
const OUT = path.resolve(
  UI_ROOT,
  'src/views/projects/task/components/node/fields/seatunnel-compute/connector-param-help.json'
)

const stripCell = (s) =>
  String(s || '')
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
    .replace(/`([^`]+)`/g, '$1')
    .replace(/\s+/g, ' ')
    .trim()

function parseOptionTables(md) {
  const lines = md.split(/\r?\n/)
  const options = []
  let i = 0
  while (i < lines.length) {
    const line = lines[i]
    if (
      /^\|\s*名称/.test(line) ||
      /^\|\s*参数名/.test(line) ||
      /^\|\s*Name\s*\|/i.test(line) ||
      /^\|\s*配置项/.test(line) ||
      /^\|\s*Option\s*\|/i.test(line)
    ) {
      const header = line
        .split('|')
        .map((c) => c.trim())
        .filter(Boolean)
      i++
      if (i < lines.length && /^\|\s*-+/.test(lines[i])) i++
      while (i < lines.length && lines[i].startsWith('|')) {
        const cols = lines[i]
          .split('|')
          .map((c) => c.trim())
          .filter((_, idx, arr) => idx > 0 && idx < arr.length - 1)
        i++
        if (cols.length < 2) continue
        const name = stripCell(cols[0])
        if (!name || /^-{2,}$/.test(name) || name === 'common-options') continue
        // Heuristic column map: name, type, required, default, desc
        let type = '',
          required = '',
          def = '',
          desc = ''
        if (cols.length >= 5) {
          type = stripCell(cols[1])
          required = stripCell(cols[2])
          def = stripCell(cols[3])
          desc = stripCell(cols.slice(4).join(' | '))
        } else if (cols.length === 4) {
          type = stripCell(cols[1])
          required = stripCell(cols[2])
          desc = stripCell(cols[3])
        } else {
          desc = stripCell(cols.slice(1).join(' | '))
        }
        options.push({ name, type, required, default: def, desc })
      }
      continue
    }
    i++
  }
  // de-dupe by name (keep first richer desc)
  const map = new Map()
  for (const o of options) {
    if (!map.has(o.name)) map.set(o.name, o)
  }
  return [...map.values()]
}

function firstParagraph(md) {
  const body = md
    .replace(/^---[\s\S]*?---\n/, '')
    .replace(/^import .+$/gm, '')
    .replace(/^# .+$/m, '')
  const m = body.match(/\n([^\n|#>][^\n]{20,400})/)
  return m ? stripCell(m[1]) : ''
}

function findDoc(kind, plugin) {
  const folders =
    kind === 'TRANSFORM'
      ? [path.join(ST_DOCS, 'transforms')]
      : [
          path.join(ST_DOCS, 'connectors', kind === 'SOURCE' ? 'source' : 'sink')
        ]
  const candidates = [
    `${plugin}.md`,
    `${plugin.toLowerCase()}.md`,
    plugin.replace(/([a-z])([A-Z])/g, '$1-$2').toLowerCase() + '.md',
    plugin.replace(/_/g, '-').toLowerCase() + '.md'
  ]
  // special maps
  const aliases = {
    FakeSource: ['FakeSource.md', 'fakesource.md'],
    'LocalFile': ['LocalFile.md', 'localfile.md'],
    Sql: ['sql.md'],
    JsonPath: ['jsonpath.md'],
    FilterRowKind: ['filter-rowkind.md'],
    FieldMapper: ['field-mapper.md'],
    FieldRename: ['field-rename.md'],
    DynamicCompile: ['dynamic-compile.md'],
    DefineSinkType: ['define-sink-type.md'],
    DataValidator: ['data-validator.md'],
    RegexExtract: ['regexextract.md'],
    RowKindExtractor: ['rowkind-extractor.md'],
    TableFilter: ['table-filter.md'],
    TableMerge: ['table-merge.md'],
    TableRename: ['table-rename.md'],
    TextChunk: ['text-chunk.md'],
    AmazonDynamodb: ['AmazonDynamoDB.md'],
    Maxcompute: ['Maxcompute.md', 'MaxCompute.md']
  }
  if (aliases[plugin]) candidates.unshift(...aliases[plugin])

  for (const folder of folders) {
    if (!fs.existsSync(folder)) continue
    const files = fs.readdirSync(folder)
    for (const c of candidates) {
      const hit = files.find((f) => f.toLowerCase() === c.toLowerCase())
      if (hit) return path.join(folder, hit)
    }
  }
  return null
}

function loadCommon(kind) {
  const file =
    kind === 'SOURCE'
      ? path.join(ST_DOCS, 'connectors/common-options/source-common-options.md')
      : kind === 'SINK'
        ? path.join(ST_DOCS, 'connectors/common-options/sink-common-options.md')
        : null
  if (!file || !fs.existsSync(file)) {
    // transform common: plugin_input / plugin_output
    return [
      {
        name: 'plugin_input',
        type: 'String',
        required: '是',
        default: '-',
        desc: '上游数据集名称，对应上游节点的 plugin_output'
      },
      {
        name: 'plugin_output',
        type: 'String',
        required: '否',
        default: '-',
        desc: '本节点输出数据集名称，供下游 plugin_input 引用'
      }
    ]
  }
  return parseOptionTables(fs.readFileSync(file, 'utf8'))
}

function classifyOptions(options, kind) {
  const inputNames = new Set([
    'plugin_input',
    'source_table_name',
    'result_table_name'
  ])
  const outputNames = new Set([
    'plugin_output',
    'result_table_name',
    'schema',
    'fields'
  ])
  const input = []
  const output = []
  const process = []
  for (const o of options) {
    const n = o.name
    if (inputNames.has(n) || (kind !== 'SOURCE' && n === 'plugin_input')) {
      input.push(o)
    } else if (outputNames.has(n) || n === 'plugin_output') {
      output.push(o)
    } else {
      process.push(o)
    }
  }
  // ensure IO basics present
  if (kind !== 'SOURCE' && !input.find((x) => x.name === 'plugin_input')) {
    input.unshift({
      name: 'plugin_input',
      type: 'String',
      required: '是',
      default: '-',
      desc: '上游输出流/表名（填写上游节点的 plugin_output）'
    })
  }
  if (kind !== 'SINK' && !output.find((x) => x.name === 'plugin_output')) {
    output.unshift({
      name: 'plugin_output',
      type: 'String',
      required: '否',
      default: '-',
      desc: '本节点输出流/表名，下游通过 plugin_input 引用'
    })
  }
  return { input, output, process }
}

const CUSTOM_FALLBACKS = {
  Decrypt_TRANSFORM: {
    summary: '字段解密 Transform（LHYF 扩展）',
    tips: ['配置待解密字段与算法/密钥相关参数', 'plugin_input 指向上游 plugin_output'],
    example: { plugin_input: 'upstream', fields: ['secret_col'] }
  },
  Encrypt_TRANSFORM: {
    summary: '字段加密 Transform（LHYF 扩展）',
    tips: ['配置待加密字段映射', '输出字段可在下游继续使用'],
    example: { plugin_input: 'upstream', fields: ['name'] }
  },
  Desensitize_TRANSFORM: {
    summary: '脱敏 Transform（LHYF 扩展）',
    tips: ['按规则对敏感字段打码/替换'],
    example: { plugin_input: 'upstream', fields: ['phone'] }
  },
  Dict_TRANSFORM: {
    summary: '字典映射 Transform（LHYF 扩展）',
    tips: ['将字段值按字典表转换'],
    example: { plugin_input: 'upstream' }
  },
  JavaScript_TRANSFORM: {
    summary: 'JavaScript Transform（LHYF 扩展）',
    tips: ['在脚本中读写行字段；注意性能与沙箱限制'],
    example: { plugin_input: 'upstream', script: 'return row;' }
  },
  Range_TRANSFORM: {
    summary: '区间映射 Transform（LHYF 扩展）',
    tips: ['按数值区间映射到标签字段'],
    example: { plugin_input: 'upstream' }
  },
  Sequence_TRANSFORM: {
    summary: '序列号 Transform（LHYF 扩展）',
    tips: ['为每行生成递增序列'],
    example: { plugin_input: 'upstream', result_field: 'seq' }
  },
  SplitColumn_TRANSFORM: {
    summary: '列拆分 Transform（LHYF 扩展）',
    tips: ['按分隔符将一列拆成多列'],
    example: { plugin_input: 'upstream', separator: ',' }
  },
  SwitchCase_TRANSFORM: {
    summary: '条件分支字段映射（LHYF 扩展）',
    tips: ['按条件写不同字段值'],
    example: { plugin_input: 'upstream' }
  },
  UDP_SOURCE: {
    summary: 'UDP Source（LHYF 扩展）',
    tips: ['配置监听 host/port、解析模式与字符集'],
    example: { host: '0.0.0.0', port: 9999, charset: 'UTF-8' }
  },
  UdpMulticast_SOURCE: {
    summary: 'UDP 组播 Source（LHYF 扩展）',
    tips: ['配置组播地址与端口'],
    example: { host: '224.0.0.1', port: 9999 }
  },
  SocketServer_SOURCE: {
    summary: 'Socket Server Source（LHYF 扩展）',
    tips: ['作为服务端接收上游推送数据'],
    example: { port: 9998 }
  },
  HttpServer_SOURCE: {
    summary: 'HTTP Server Source（LHYF 扩展）',
    tips: ['作为 HTTP 服务接收请求体数据'],
    example: { port: 8089 }
  }
}

const catalog = JSON.parse(fs.readFileSync(CATALOG, 'utf8'))
const result = {
  generatedAt: new Date().toISOString(),
  common: {
    SOURCE: classifyOptions(loadCommon('SOURCE'), 'SOURCE'),
    SINK: classifyOptions(loadCommon('SINK'), 'SINK'),
    TRANSFORM: classifyOptions(loadCommon('TRANSFORM'), 'TRANSFORM')
  },
  connectors: {}
}

function buildEntry(plugin, kind) {
  const key = `${plugin}_${kind}`
  const doc = findDoc(kind, plugin)
  let options = []
  let summary = ''
  if (doc) {
    const md = fs.readFileSync(doc, 'utf8')
    summary = firstParagraph(md)
    options = parseOptionTables(md)
  }
  const common = loadCommon(kind)
  // merge common + specific (specific overrides)
  const mergedMap = new Map()
  for (const o of [...common, ...options]) mergedMap.set(o.name, o)
  const classified = classifyOptions([...mergedMap.values()], kind)
  const custom = CUSTOM_FALLBACKS[key]
  return {
    plugin,
    kind,
    summary: custom?.summary || summary || `${plugin} ${kind}`,
    tips: custom?.tips || [
      kind === 'SOURCE'
        ? '配置数据源连接与读取范围；建议设置 plugin_output 供下游引用'
        : kind === 'TRANSFORM'
          ? 'plugin_input 必须等于上游 plugin_output；处理后设置 plugin_output'
          : 'plugin_input 指向上游输出；按 Sink 要求映射字段写入目标'
    ],
    ...classified,
    example: custom?.example || null,
    docFound: !!doc
  }
}

for (const plugin of catalog.sources || []) {
  result.connectors[`${plugin}_SOURCE`] = buildEntry(plugin, 'SOURCE')
}
for (const plugin of catalog.sinks || []) {
  result.connectors[`${plugin}_SINK`] = buildEntry(plugin, 'SINK')
}
for (const plugin of catalog.transforms || []) {
  result.connectors[`${plugin}_TRANSFORM`] = buildEntry(plugin, 'TRANSFORM')
}

fs.writeFileSync(OUT, JSON.stringify(result, null, 2), 'utf8')
const keys = Object.keys(result.connectors)
const withDoc = keys.filter((k) => result.connectors[k].docFound).length
console.log(
  `Wrote ${OUT}\nconnectors=${keys.length} withDoc=${withDoc} missingDoc=${keys.length - withDoc}`
)
