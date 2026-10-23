/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import catalogFallback from '@/views/projects/task/components/node/fields/seatunnel-compute/connectors-catalog.json'
import iconMapJson from './seatunnel-icon-map.json'

export type ConnectorKind = 'SOURCE' | 'TRANSFORM' | 'SINK'

const ST_ICON_MAP = iconMapJson as Record<string, string>

/**
 * Resolve task-icon basename (no extension) for sidebar / canvas.
 * Known connectors use dedicated assets; others fall back to seatunnel.png.
 */
export function resolveTaskIconKey(type?: string): string {
  if (!type) return 'seatunnel'
  const key = String(type).toLocaleLowerCase()
  if (key === 'seatunnel_compute' || key === 'seatunnel') return 'seatunnel'
  if (ST_ICON_MAP[key]) return ST_ICON_MAP[key]
  if (isSeaTunnelChildType(String(type))) return 'seatunnel'
  return key
}

export function resolveTaskIconFile(type?: string, hover = false): string {
  const base = resolveTaskIconKey(type)
  return hover ? `${base}_hover.png` : `${base}.png`
}

export interface StComputeNode {
  code: number
  name: string
  type: ConnectorKind
  pluginName: string
  params: Record<string, any>
  preTaskcode?: number | string
  postTaskcode?: number | string
}

export interface StComputeGraph {
  env: Record<string, any>
  nodes: StComputeNode[]
}

const KIND_RE = /_(SOURCE|SINK|TRANSFORM)$/

export function isSeaTunnelChildType(type?: string): boolean {
  return !!type && KIND_RE.test(type)
}

export function isSeaTunnelComputeParent(type?: string): boolean {
  return type === 'SEATUNNEL_COMPUTE'
}

/** Default size for SEATUNNEL_COMPUTE visualize group frame on canvas. */
export const ST_COMPUTE_GROUP_DEFAULT_WIDTH = 900
export const ST_COMPUTE_GROUP_DEFAULT_HEIGHT = 480

export function parseSeaTunnelChildType(taskType: string): {
  pluginName: string
  kind: ConnectorKind
} {
  const m = taskType.match(/^(.*)_(SOURCE|SINK|TRANSFORM)$/)
  if (!m) {
    return { pluginName: taskType, kind: 'SOURCE' }
  }
  return { pluginName: m[1], kind: m[2] as ConnectorKind }
}

export function toSeaTunnelChildType(
  pluginName: string,
  kind: ConnectorKind
): string {
  return `${pluginName}_${kind}`
}

export function normalizeSeaTunnelEnv(
  env: Record<string, any> = {}
): Record<string, any> {
  const next: Record<string, any> = { parallelism: 1, ...env }
  if (next.jobMode != null && next['job.mode'] == null) {
    next['job.mode'] = next.jobMode
    delete next.jobMode
  }
  if (next['job.mode'] == null) next['job.mode'] = 'BATCH'
  next['job.mode'] = String(next['job.mode']).toUpperCase()
  if (next.parallelism == null) next.parallelism = 1
  return next
}

/** SeaTunnel stream / table label: letters, digits, underscore. */
export function toSeaTunnelStreamName(
  name: string | undefined,
  fallback: string
): string {
  const raw = (name || fallback || 'stream').trim()
  let s = raw.replace(/[^A-Za-z0-9_]/g, '_')
  if (!/^[A-Za-z_]/.test(s)) s = `t_${s}`
  return s || 'stream'
}

/**
 * Minimal runnable defaults for common connectors (FakeSource → Replace → Console).
 */
export function defaultConnectorParams(
  pluginName: string,
  kind: ConnectorKind
): Record<string, any> {
  if (pluginName === 'FakeSource' && kind === 'SOURCE') {
    return {
      'row.num': 10,
      schema: {
        fields: {
          id: 'int',
          name: 'string',
          age: 'int'
        }
      }
    }
  }
  if (pluginName === 'Replace' && kind === 'TRANSFORM') {
    return {
      // SeaTunnel 3.x prefers replace_fields (list); replace_field is deprecated
      replace_fields: ['name'],
      pattern: '.+',
      replacement: 'ST',
      is_regex: true,
      replace_first: true
    }
  }
  if (pluginName === 'Copy' && kind === 'TRANSFORM') {
    return {
      fields: {
        name1: 'name',
        age1: 'age'
      }
    }
  }
  if (pluginName === 'Sql' && kind === 'TRANSFORM') {
    return {
      query: 'select id, name, age from dual'
    }
  }
  if (pluginName === 'Filter' && kind === 'TRANSFORM') {
    return {
      fields: ['id', 'name', 'age']
    }
  }
  if (pluginName === 'Console' && kind === 'SINK') {
    return {}
  }
  return {}
}

export type StComputeConnect = {
  preTaskCode: number | string
  postTaskCode: number | string
  name?: string
  conditionType?: string
  conditionParams?: Record<string, any>
}

function isValidTaskCode(code: number | string | null | undefined): boolean {
  if (code == null || code === '') return false
  const n = Number(code)
  return Number.isFinite(n) && n > 0
}

/** Source → Transform → Sink chain used when canvas has no edges yet. */
export function buildDefaultConnects(
  nodes: StComputeNode[]
): StComputeConnect[] {
  const sources = nodes.filter((n) => n.type === 'SOURCE')
  const transforms = nodes.filter((n) => n.type === 'TRANSFORM')
  const sinks = nodes.filter((n) => n.type === 'SINK')
  const chain = [...sources, ...transforms, ...sinks]
  const connects: StComputeConnect[] = []
  for (let i = 1; i < chain.length; i++) {
    connects.push({
      preTaskCode: Number(chain[i - 1].code),
      postTaskCode: Number(chain[i].code),
      name: '',
      conditionType: 'NONE',
      conditionParams: {}
    })
  }
  return connects
}

function hasRealConnects(connects?: StComputeConnect[]): boolean {
  return !!(connects || []).some(
    (c) => isValidTaskCode(c.preTaskCode) && isValidTaskCode(c.postTaskCode)
  )
}

function buildIncomingMap(
  nodes: StComputeNode[],
  connects?: StComputeConnect[]
): Map<string, string[]> {
  const codeSet = new Set(nodes.map((n) => String(n.code)))
  const incoming = new Map<string, string[]>()
  const push = (post: string, pre: string) => {
    // Ignore DS placeholder edges (preTaskCode=0) and dangling codes
    if (!isValidTaskCode(pre) || !isValidTaskCode(post)) return
    if (!codeSet.has(pre) || !codeSet.has(post)) return
    if (!incoming.has(post)) incoming.set(post, [])
    const list = incoming.get(post)!
    if (!list.includes(pre)) list.push(pre)
  }

  const effective = hasRealConnects(connects)
    ? connects!
    : buildDefaultConnects(nodes)

  effective.forEach((c) => {
    if (c?.preTaskCode == null || c?.postTaskCode == null) return
    push(String(c.postTaskCode), String(c.preTaskCode))
  })

  // Also honor per-node pre/post from folded graph (ds_ui_code style)
  nodes.forEach((n) => {
    if (n.preTaskcode != null) push(String(n.code), String(n.preTaskcode))
    if (n.postTaskcode != null) push(String(n.postTaskcode), String(n.code))
  })

  return incoming
}

export function parseStComputeGraph(
  taskParams: Record<string, any> | undefined
): StComputeGraph {
  const env = normalizeSeaTunnelEnv({ 'job.mode': 'BATCH', parallelism: 1 })
  if (!taskParams) return { env, nodes: [] }

  const graphStr = taskParams.stComputeGraph
  if (graphStr) {
    try {
      const parsed = JSON.parse(graphStr)
      if (parsed?.nodes) {
        return {
          env: normalizeSeaTunnelEnv({ ...env, ...(parsed.env || {}) }),
          nodes: (parsed.nodes || []).map((n: any) => ({
            code: Number(n.code),
            name: n.name || n.pluginName,
            type: (n.type || 'SOURCE').toUpperCase() as ConnectorKind,
            pluginName: n.pluginName,
            params: n.params || {},
            preTaskcode: n.preTaskcode,
            postTaskcode: n.postTaskcode
          }))
        }
      }
    } catch {
      /* fall through */
    }
  }

  // Legacy: visualize node array stored in rawScript (ds_ui_code style)
  if (taskParams.rawScript) {
    try {
      const parsed = JSON.parse(taskParams.rawScript)
      if (Array.isArray(parsed) && parsed[0]?.pluginName) {
        return {
          env,
          nodes: parsed.map((n: any) => ({
            code: Number(n.code),
            name: n.name || n.pluginName,
            type: (n.type || 'SOURCE').toUpperCase() as ConnectorKind,
            pluginName: n.pluginName,
            params: n.params || {},
            preTaskcode: n.preTaskcode,
            postTaskcode: n.postTaskcode
          }))
        }
      }
    } catch {
      /* ignore */
    }
  }

  return { env, nodes: [] }
}

/**
 * Build stock SeaTunnel JSON that the worker writes as job config.
 * Wires plugin_output / plugin_input from canvas edges (or Source→Transform→Sink order).
 */
export function assembleStockSeaTunnelConfig(
  env: Record<string, any>,
  nodes: StComputeNode[],
  connects?: StComputeConnect[]
): Record<string, any> {
  const source: Record<string, any>[] = []
  const transform: Record<string, any>[] = []
  const sink: Record<string, any>[] = []
  const byCode = new Map(nodes.map((n) => [String(n.code), n]))
  const incoming = buildIncomingMap(nodes, connects)

  const streamOf = (n: StComputeNode) =>
    toSeaTunnelStreamName(
      (n.params && n.params.plugin_output) || n.name,
      `${n.pluginName}_${n.code}`
    )

  nodes.forEach((n) => {
    const defaults = defaultConnectorParams(n.pluginName, n.type)
    const userParams = { ...(n.params || {}) }
    // Drop UI-only keys if any leaked
    delete userParams.pluginName
    delete userParams.connectorType
    delete userParams.localParams
    delete userParams.resourceList

    const cfg: Record<string, any> = {
      ...defaults,
      ...userParams,
      plugin_name: n.pluginName
    }

    const outName = streamOf(n)
    if (n.type !== 'SINK') {
      cfg.plugin_output = outName
    } else {
      delete cfg.plugin_output
    }

    const preCodes = incoming.get(String(n.code)) || []
    if (n.type !== 'SOURCE' && preCodes.length) {
      const inputs = preCodes
        .map((code) => {
          const pre = byCode.get(code)
          return pre ? streamOf(pre) : null
        })
        .filter(Boolean) as string[]
      // SeaTunnel 3.x recommends list form for plugin_input
      if (inputs.length) cfg.plugin_input = inputs
    }
    // Migrate deprecated Replace key if user still has old defaults
    if (n.pluginName === 'Replace' && cfg.replace_field && !cfg.replace_fields) {
      cfg.replace_fields = [cfg.replace_field]
      delete cfg.replace_field
    }

    if (n.type === 'SOURCE') source.push(cfg)
    else if (n.type === 'TRANSFORM') transform.push(cfg)
    else if (n.type === 'SINK') sink.push(cfg)
  })

  const config: Record<string, any> = {
    env: normalizeSeaTunnelEnv(env),
    source,
    sink
  }
  if (transform.length) config.transform = transform
  return config
}

export function buildDataComputationSidebarTree(parentTask: any) {
  const catalog = catalogFallback as {
    sources: string[]
    sinks: string[]
    transforms: string[]
  }
  const mkLeaf = (pluginName: string, kind: ConnectorKind) => ({
    taskType: toSeaTunnelChildType(pluginName, kind),
    type: toSeaTunnelChildType(pluginName, kind),
    taskName: pluginName,
    taskCategory: kind === 'SOURCE' ? 'Source' : kind === 'SINK' ? 'Sink' : 'Transform',
    collection: false,
    starHover: false,
    leaf: true
  })
  return {
    taskType: 'seatunnel_compute_group',
    taskName: parentTask?.taskType || 'SEATUNNEL_COMPUTE',
    type: 'SEATUNNEL_COMPUTE',
    children: [
      {
        ...parentTask,
        type: 'SEATUNNEL_COMPUTE',
        taskType: 'SEATUNNEL_COMPUTE',
        taskName: parentTask?.taskType || 'SEATUNNEL_COMPUTE',
        leaf: true
      },
      {
        taskType: 'Source',
        taskName: 'Source',
        type: 'Source',
        children: (catalog.sources || []).map((p) => mkLeaf(p, 'SOURCE'))
      },
      {
        taskType: 'Transform',
        taskName: 'Transform',
        type: 'Transform',
        children: (catalog.transforms || []).map((p) => mkLeaf(p, 'TRANSFORM'))
      },
      {
        taskType: 'Sink',
        taskName: 'Sink',
        type: 'Sink',
        children: (catalog.sinks || []).map((p) => mkLeaf(p, 'SINK'))
      }
    ]
  }
}

export function taskDefFromStNode(node: StComputeNode): any {
  return {
    code: Number(node.code),
    name: node.name || node.pluginName,
    taskType: toSeaTunnelChildType(node.pluginName, node.type),
    flag: 'YES',
    taskParams: {
      ...(node.params || {}),
      pluginName: node.pluginName,
      connectorType: node.type,
      localParams: [],
      resourceList: []
    }
  }
}
