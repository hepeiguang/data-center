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
