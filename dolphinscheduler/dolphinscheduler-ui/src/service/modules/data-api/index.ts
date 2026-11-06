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

const dataApiBase = import.meta.env.VITE_APP_DATA_API_BASE || '/data-api'

type AuthHeaders = { sessionId?: string; token?: string }

function client(headers: AuthHeaders) {
  return axios.create({
    baseURL: dataApiBase,
    timeout: 30000,
    // data-api returns 4xx on rejection; keep the body so pages can show code/message
    validateStatus: () => true,
    headers: {
      sessionId: headers.sessionId || '',
      token: headers.token || headers.sessionId || ''
    }
  })
}

export function listDataApiDatasources(headers: AuthHeaders) {
  return client(headers)
    .get('/datasources')
    .then((r) => r.data)
}

export function reloadDataApiDatasources(headers: AuthHeaders) {
  return client(headers)
    .post('/datasources/reload')
    .then((r) => r.data)
}

export function pingDataApiDatasource(key: string, headers: AuthHeaders) {
  return client(headers)
    .get(`/datasources/${encodeURIComponent(key)}/ping`)
    .then((r) => r.data)
    .catch((err) => {
      const data = err?.response?.data
      if (data) {
        return data
      }
      throw err
    })
}

export function listDataApiAcl(headers: AuthHeaders, apiPath?: string) {
  return client(headers)
    .get('/acl', { params: apiPath ? { apiPath } : {} })
    .then((r) => r.data)
}

export function createDataApiAcl(
  headers: AuthHeaders,
  body: {
    apiPath: string
    subjectType: string
    subjectId: string
    effect?: string | number
    remark?: string
  }
) {
  return client(headers)
    .post('/acl', body)
    .then((r) => r.data)
}

export function deleteDataApiAcl(headers: AuthHeaders, id: number) {
  return client(headers)
    .delete(`/acl/${id}`)
    .then((r) => r.data)
}

export function listDataApiPublishedApis(headers: AuthHeaders) {
  return client(headers)
    .get('/acl/apis')
    .then((r) => r.data)
}

export function listDataApiReleases(headers: AuthHeaders, apiPath?: string) {
  return client(headers)
    .get('/releases', { params: apiPath ? { apiPath } : {} })
    .then((r) => r.data)
}

export function snapshotDataApiRelease(headers: AuthHeaders, remark: string) {
  return client(headers)
    .post('/releases/snapshot', { remark })
    .then((r) => r.data)
}

export function rollbackDataApiRelease(headers: AuthHeaders, id: number) {
  return client(headers)
    .post(`/releases/${id}/rollback`)
    .then((r) => r.data)
}

export function offlineDataApiRelease(headers: AuthHeaders, id: number) {
  return client(headers)
    .post(`/releases/${id}/offline`)
    .then((r) => r.data)
}

// ---------- business query console (plan §7.1) ----------

export function listConsoleDatasources(headers: AuthHeaders) {
  return client(headers)
    .get('/console/datasources')
    .then((r) => r.data)
}

export function listConsoleHistory(headers: AuthHeaders, limit?: number) {
  return client(headers)
    .get('/console/history', { params: limit ? { limit } : {} })
    .then((r) => r.data)
}

export function executeConsoleSql(
  headers: AuthHeaders,
  body: { datasourceId: number; sql: string }
) {
  return client(headers)
    .post('/console/execute', body, { timeout: 120000 })
    .then((r) => r.data)
}

export function cancelConsoleQuery(headers: AuthHeaders, queryId: string) {
  return client(headers)
    .post('/console/cancel', { queryId })
    .then((r) => r.data)
}

/**
 * Streaming CSV export. Errors arrive as JSON in the same blob channel
 * (validateStatus never rejects), so callers get either a Blob or the parsed
 * `{code, message}` envelope.
 */
export async function exportConsoleCsv(
  headers: AuthHeaders,
  body: { datasourceId: number; sql: string }
): Promise<Blob | { code: number; message: string }> {
  const res = await client(headers).post('/console/export', body, {
    responseType: 'blob',
    timeout: 600000
  })
  const blob: Blob = res.data
  const type = String(blob.type || '')
  if (res.status >= 400 || type.includes('application/json')) {
    const text = await blob.text()
    try {
      return JSON.parse(text)
    } catch (e) {
      return { code: res.status, message: text || 'export failed' }
    }
  }
  return blob
}
