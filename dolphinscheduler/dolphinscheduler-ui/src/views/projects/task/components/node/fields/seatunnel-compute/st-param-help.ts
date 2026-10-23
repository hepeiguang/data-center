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

import helpCatalog from './connector-param-help.json'
import type { ConnectorKind } from '@/views/projects/workflow/components/dag/seatunnel-compute-utils'

export type StParamOption = {
  name: string
  type?: string
  required?: string
  default?: string
  desc?: string
}

export type StConnectorHelp = {
  plugin: string
  kind: ConnectorKind | string
  summary: string
  tips: string[]
  input: StParamOption[]
  output: StParamOption[]
  process: StParamOption[]
  example?: Record<string, any> | null
  docFound?: boolean
}

export type UpstreamFieldInfo = {
  taskCode: number
  taskName: string
  pluginName?: string
  pluginOutput?: string
  fields: string[]
}

type HelpFile = {
  connectors: Record<string, StConnectorHelp>
  common: Record<string, { input: StParamOption[]; output: StParamOption[]; process: StParamOption[] }>
}

const catalog = helpCatalog as HelpFile

export function getConnectorHelp(
  pluginName: string,
  kind: string
): StConnectorHelp {
  const key = `${pluginName}_${String(kind).toUpperCase()}`
  const hit = catalog.connectors[key]
  if (hit) return hit
  const common = catalog.common[String(kind).toUpperCase()] || {
    input: [],
    output: [],
    process: []
  }
  return {
    plugin: pluginName,
    kind,
    summary: `${pluginName} ${kind}`,
    tips: ['请参考 SeaTunnel 官方文档填写本连接器参数'],
    ...common,
    example: null,
    docFound: false
  }
}

/** Extract usable field / stream names from a connector params object. */
export function extractFieldsFromParams(
  params: Record<string, any> | null | undefined
): { pluginOutput?: string; fields: string[] } {
  if (!params || typeof params !== 'object') return { fields: [] }
  const fields = new Set<string>()
  const pluginOutput =
    params.plugin_output ||
    params.result_table_name ||
    params.pluginOutput ||
    undefined

  const schemaFields = params.schema?.fields
  if (schemaFields && typeof schemaFields === 'object') {
    Object.keys(schemaFields).forEach((k) => fields.add(k))
  }
  if (Array.isArray(params.fields)) {
    params.fields.forEach((f: any) => {
      if (typeof f === 'string') fields.add(f)
      else if (f?.name) fields.add(String(f.name))
    })
  }
  if (Array.isArray(params.replace_fields)) {
    params.replace_fields.forEach((f: string) => fields.add(f))
  }
  if (typeof params.replace_field === 'string') fields.add(params.replace_field)
  if (params.field_mapper && typeof params.field_mapper === 'object') {
    Object.keys(params.field_mapper).forEach((k) => fields.add(k))
    Object.values(params.field_mapper).forEach((v) => {
      if (typeof v === 'string') fields.add(v)
    })
  }
  // Copy transform: { name1: name }
  if (
    params.fields &&
    typeof params.fields === 'object' &&
    !Array.isArray(params.fields)
  ) {
    Object.keys(params.fields).forEach((k) => fields.add(k))
    Object.values(params.fields).forEach((v) => {
      if (typeof v === 'string') fields.add(v)
    })
  }

  return { pluginOutput, fields: [...fields] }
}

function parseTaskParams(task: any): Record<string, any> {
  const tp = task?.taskParams || {}
  if (typeof tp.connectorParams === 'string' && tp.connectorParams.trim()) {
    try {
      return { ...tp, ...JSON.parse(tp.connectorParams) }
    } catch {
      return tp
    }
  }
  const {
    pluginName,
    connectorType,
    localParams,
    resourceList,
    connectorParams,
    ...rest
  } = tp
  return rest || {}
}

/** Collect upstream outputs available for the current node. */
export function collectUpstreamAvailableParams(
  taskDefinitionList: any[] | undefined,
  preTaskCodes: Array<number | string> | undefined
): UpstreamFieldInfo[] {
  if (!taskDefinitionList?.length || !preTaskCodes?.length) return []
  const codeSet = new Set(preTaskCodes.map((c) => Number(c)))
  const result: UpstreamFieldInfo[] = []
  for (const task of taskDefinitionList) {
    if (!codeSet.has(Number(task.code))) continue
    const params = parseTaskParams(task)
    const extracted = extractFieldsFromParams(params)
    result.push({
      taskCode: Number(task.code),
      taskName: task.name || String(task.code),
      pluginName: params.pluginName || task.taskType,
      pluginOutput: extracted.pluginOutput,
      fields: extracted.fields
    })
  }
  return result
}

/** Insert or update a top-level JSON key in connectorParams text. */
export function upsertJsonParam(
  jsonText: string,
  key: string,
  value: any
): string {
  let obj: Record<string, any> = {}
  try {
    obj = jsonText?.trim() ? JSON.parse(jsonText) : {}
  } catch {
    obj = {}
  }
  if (value === undefined) {
    delete obj[key]
  } else {
    obj[key] = value
  }
  return JSON.stringify(obj, null, 2)
}
