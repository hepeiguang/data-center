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

import axios from 'axios'

const magicBase = import.meta.env.VITE_APP_DATA_API_MAGIC_URL || '/magic/web'
const dataApiBase = import.meta.env.VITE_APP_DATA_API_BASE || '/data-api'
const gatewayPrefix = `${dataApiBase}/gateway`
const successCode = 1

export const ROOT_GROUP_ID = '0'

export type MagicAuth = { sessionId: string }
export type ResourceFolder = 'api' | 'function'

export interface Definition {
  name: string
  value?: unknown
  dataType?: string | null
  type?: string | null
  description?: string | null
  required?: boolean
  defaultValue?: string | null
  validateType?: string | null
  error?: string | null
  expression?: string | null
  children?: Definition[] | null
}

export interface ResourceNode {
  id: string
  name: string
  type?: string
  parentId?: string
  groupId?: string
  path?: string
  method?: string
  lock?: string | null
  createBy?: string | null
  updateBy?: string | null
  createTime?: number | null
  updateTime?: number | null
  paths?: Definition[]
  options?: Definition[]
}

export interface MagicTreeNode {
  node: ResourceNode
  children: MagicTreeNode[]
}

export interface ResourceDetail {
  id?: string | null
  groupId: string
  name: string
  path: string
  method?: string
  description?: string | null
  script?: string | null
  parameters?: Definition[]
  headers?: Definition[]
  paths?: Definition[]
  options?: Definition[]
  requestBody?: string | null
  responseBody?: string | null
  requestBodyDefinition?: Definition | null
  responseBodyDefinition?: Definition | null
  returnType?: string | null
  mappingPath?: string | null
  lock?: string | null
  createBy?: string | null
  updateBy?: string | null
  createTime?: number | null
  updateTime?: number | null
}

export interface GroupInfo {
  id: string
  name: string
  path: string
  parentId: string
  depth: number
  label: string
}

export interface BackupItem {
  id: string
  createDate: number
  name?: string
  tag?: string
  type?: string
  createBy?: string
}

export interface GatewayCall {
  auth: MagicAuth
  method: string
  path: string
  query?: Record<string, string>
  headers?: Record<string, string>
  body?: string
  scriptId?: string
  clientId?: string
}

export interface GatewayResult {
  status: number
  statusText: string
  durationMs: number
  size: number
  body: string
  contentType: string
  headers: Record<string, string>
  error?: string
}

/**
 * magic-api persists resources through /resource/file/{folder}/save with the entity
 * sent as ROT13(base64(UTF-8 JSON)); the console rejects a plain JSON body.
 */
const rot13 = (input: string) =>
  input.replace(/[a-zA-Z]/g, (char) => {
    const base = char <= 'Z' ? 65 : 97
    return String.fromCharCode(((char.charCodeAt(0) - base + 13) % 26) + base)
  })

export function encodeEntity(entity: unknown): string {
  const bytes = encodeURIComponent(JSON.stringify(entity)).replace(
    /%([0-9A-F]{2})/g,
    (unusedMatch: string, hex: string) => String.fromCharCode(parseInt(hex, 16))
  )
  return rot13(window.btoa(bytes))
}

function magicClient(auth: MagicAuth) {
  return axios.create({
    baseURL: magicBase,
    timeout: 60000,
    validateStatus: () => true,
    headers: { 'magic-token': auth.sessionId || '' }
  })
}

async function unwrap<T>(
  request: Promise<{ status: number; data: any }>
): Promise<T> {
  let response: { status: number; data: any }
  try {
    response = await request
  } catch (error: any) {
    throw new Error(`MAGIC_UNREACHABLE:${error?.message || error}`)
  }
  if (response.status === 401 || response.status === 403) {
    throw new Error('MAGIC_UNAUTHORIZED')
  }
  const body = response.data
  if (typeof body === 'string') {
    if (response.status >= 400) {
      throw new Error(body || `HTTP ${response.status}`)
    }
    return body as unknown as T
  }
  if (body && typeof body.code === 'number') {
    // magic-api answers console auth failures as HTTP 200 + code=401, so the status says nothing
    if (body.code === 401 || body.code === 403) {
      throw new Error('MAGIC_UNAUTHORIZED')
    }
    if (body.code !== successCode) {
      throw new Error(body.message || `magic-api code=${body.code}`)
    }
    return body.data as T
  }
  return body as T
}

const postParams = (params: Record<string, string | number>) => ({ params })

export function fetchMagicTree(auth: MagicAuth) {
  return unwrap<Record<string, MagicTreeNode>>(
    magicClient(auth).post('/resource')
  )
}

export function fetchResource(auth: MagicAuth, id: string) {
  return unwrap<ResourceDetail>(
    magicClient(auth).get(`/resource/file/${encodeURIComponent(id)}`)
  )
}

export function saveResource(
  auth: MagicAuth,
  folder: ResourceFolder,
  entity: ResourceDetail,
  auto = false
) {
  return unwrap<string | null>(
    magicClient(auth).post(
      `/resource/file/${folder}/save?auto=${auto ? 0 : 1}`,
      JSON.stringify(encodeEntity(entity)),
      { headers: { 'Content-Type': 'application/json' } }
    )
  )
}

export function saveGroup(auth: MagicAuth, group: ResourceNode) {
  return unwrap<string | null>(
    magicClient(auth).post('/resource/folder/save', {
      id: group.id,
      name: group.name,
      parentId: group.parentId,
      type: group.type,
      path: group.path,
      paths: group.paths || [],
      options: group.options || []
    })
  )
}

export function deleteResource(auth: MagicAuth, id: string) {
  return unwrap<boolean>(
    magicClient(auth).post('/resource/delete', null, postParams({ id }))
  )
}

export function moveResource(auth: MagicAuth, src: string, groupId: string) {
  return unwrap<boolean>(
    magicClient(auth).post('/resource/move', null, postParams({ src, groupId }))
  )
}

export function lockResource(auth: MagicAuth, id: string) {
  return unwrap<boolean>(
    magicClient(auth).post('/resource/lock', null, postParams({ id }))
  )
}

export function unlockResource(auth: MagicAuth, id: string) {
  return unwrap<boolean>(
    magicClient(auth).post('/resource/unlock', null, postParams({ id }))
  )
}

export function searchScripts(auth: MagicAuth, keyword: string) {
  return unwrap<{ id: string; text: string; line: number }[]>(
    magicClient(auth).post('/search', null, postParams({ keyword }))
  )
}

export function reloadResources(auth: MagicAuth) {
  return unwrap<boolean>(magicClient(auth).get('/reload'))
}

export function fetchMagicOptions(auth: MagicAuth) {
  return unwrap<string[][]>(magicClient(auth).get('/options'))
}

export function fetchMagicConfig() {
  return axios
    .get(`${magicBase}/config.json`, { validateStatus: () => true })
    .then((r) => r.data as { prefix?: string; version?: string; web?: string })
}

export function checkMagicSession(auth: MagicAuth) {
  return unwrap<boolean>(magicClient(auth).post('/login'))
}

export function fetchBackups(auth: MagicAuth, id: string) {
  return unwrap<BackupItem[]>(
    magicClient(auth).get(`/backup/${encodeURIComponent(id)}`)
  )
}

export function fetchBackupScript(
  auth: MagicAuth,
  id: string,
  timestamp: number
) {
  return unwrap<string>(
    magicClient(auth).get('/backup', postParams({ id, timestamp }))
  )
}

export function rollbackBackup(auth: MagicAuth, id: string, timestamp: number) {
  return unwrap<boolean>(
    magicClient(auth).post(
      '/backup/rollback',
      null,
      postParams({ id, timestamp })
    )
  )
}

export function backupEverything(auth: MagicAuth) {
  return unwrap<boolean>(magicClient(auth).post('/backup/full'))
}

/**
 * Zips magic resources: a whole group when groupId is given, otherwise everything.
 * The console endpoint answers with an octet-stream, so the blob is returned as-is.
 */
export async function exportResources(
  auth: MagicAuth,
  groupId?: string
): Promise<{ blob: Blob; error?: string }> {
  const response = await magicClient(auth).post('/download', null, {
    params: groupId ? { groupId } : {},
    // the endpoint binds an optional @RequestBody, so JSON has to be declared
    headers: { 'Content-Type': 'application/json' },
    responseType: 'blob'
  })
  const type = String(response.data?.type || '')
  if (type.includes('json') || type.includes('text')) {
    return { blob: response.data, error: await response.data.text() }
  }
  return { blob: response.data }
}

export function importResources(auth: MagicAuth, file: File, mode: string) {
  const form = new FormData()
  form.append('file', file)
  form.append('mode', mode)
  return unwrap<boolean>(
    magicClient(auth).post('/upload', form, {
      headers: { 'Content-Type': 'multipart/form-data' },
      timeout: 120000
    })
  )
}

export const collapseSlash = (path: string) => path.replace(/\/{2,}/g, '/')

const withLeadingSlash = (path: string) =>
  !path ? '' : path.startsWith('/') ? path : `/${path}`

/** Walk the parentId chain upward, mirroring magic's getGroupPath(). */
export function groupPathOf(groups: GroupInfo[], groupId: string): string {
  const index = new Map(groups.map((group) => [group.id, group]))
  const chain: string[] = []
  let cursor = groupId
  const guard = new Set<string>()
  while (cursor && !guard.has(cursor)) {
    guard.add(cursor)
    const group = index.get(cursor)
    if (!group) break
    if (group.path) chain.unshift(group.path)
    cursor = group.parentId
  }
  return collapseSlash(chain.join('/'))
}

export function apiPathOf(
  detail: { path?: string; groupId: string },
  groups: GroupInfo[]
): string {
  return collapseSlash(
    `${groupPathOf(groups, detail.groupId)}${withLeadingSlash(
      detail.path || ''
    )}`
  )
}

export function gatewayUrlOf(
  detail: { path?: string; groupId: string },
  groups: GroupInfo[]
): string {
  return `${gatewayPrefix}${withLeadingSlash(apiPathOf(detail, groups))}`
}

export function flattenGroups(
  tree: MagicTreeNode | undefined,
  folder: ResourceFolder
): GroupInfo[] {
  const result: GroupInfo[] = []
  const walk = (node: MagicTreeNode, depth: number) => {
    if (
      !!node.node.type &&
      node.node.type === folder &&
      node.node.id !== ROOT_GROUP_ID
    ) {
      result.push({
        id: node.node.id,
        name: node.node.name,
        path: node.node.path || '',
        parentId: node.node.parentId || ROOT_GROUP_ID,
        depth,
        label: `${'　'.repeat(depth)}${node.node.name}`
      })
    }
    ;(node.children || []).forEach((child) => walk(child, depth + 1))
  }
  if (tree) {
    ;(tree.children || []).forEach((child) => walk(child, 0))
  }
  return result
}

/**
 * Runs a saved API through the magic runtime prefix, the same way the magic console
 * "测试" button does: request the real gateway URL with Magic-Request-* headers.
 */
export async function callGateway(call: GatewayCall): Promise<GatewayResult> {
  const started = Date.now()
  const headers: Record<string, string> = {
    token: call.auth.sessionId,
    sessionId: call.auth.sessionId,
    'magic-token': call.auth.sessionId
  }
  Object.entries(call.headers || {}).forEach(
    ([key, value]) => value !== '' && (headers[key] = value)
  )
  if (call.scriptId) {
    headers['Magic-Request-Script-Id'] = call.scriptId
    headers['Magic-Request-Client-Id'] = call.clientId || 'ds-studio'
  }
  const method = (call.method || 'GET').toUpperCase()
  const url = `${gatewayPrefix}${withLeadingSlash(call.path)}`
  try {
    const response = await axios.request({
      url,
      method,
      params: method === 'GET' || method === 'DELETE' ? call.query : undefined,
      data:
        method === 'GET' || method === 'DELETE'
          ? undefined
          : call.body || undefined,
      headers: {
        ...headers,
        ...(call.body && method !== 'GET'
          ? { 'Content-Type': 'application/json' }
          : {})
      },
      timeout: 120000,
      validateStatus: () => true,
      responseType: 'text',
      transformResponse: []
    })
    const raw =
      typeof response.data === 'string'
        ? response.data
        : JSON.stringify(response.data)
    return {
      status: response.status,
      statusText: response.statusText || '',
      durationMs: Date.now() - started,
      size: new Blob([raw]).size,
      body: raw,
      contentType: String(response.headers?.['content-type'] || ''),
      headers: Object.entries(response.headers || {}).reduce(
        (acc, [key, value]) => ({ ...acc, [key]: String(value) }),
        {}
      )
    }
  } catch (error: any) {
    return {
      status: 0,
      statusText: '',
      durationMs: Date.now() - started,
      size: 0,
      body: '',
      contentType: '',
      headers: {},
      error: error?.message || String(error)
    }
  }
}
