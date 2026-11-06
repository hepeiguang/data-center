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

import service from './index'

export interface AssetSummary {
  fqn: string
  name: string
  displayName?: string
  description?: string
  service?: string
  database?: string
  schema?: string
}

export const searchAssets = (q: string, size = 100) =>
  service.get('/search', { params: { q, size } }).then((r) => r.data)

export const getTree = () =>
  service.get('/tree').then((r) => r.data)

export const getDetail = (fqn: string) =>
  service.get('/detail', { params: { fqn } }).then((r) => r.data)

export const getColumns = (fqn: string) =>
  service.get('/columns', { params: { fqn } }).then((r) => r.data)

export const getRows = (fqn: string, page: number, size: number) =>
  service.get('/rows', { params: { fqn, page, size } }).then((r) => r.data)

export const listEntries = () =>
  service.get('/browse/entries').then((r) => r.data)

export const createEntry = (payload: any) =>
  service.post('/browse/entries', payload).then((r) => r.data)

export const approveEntry = (id: string) =>
  service.post(`/browse/entries/${id}/approve`).then((r) => r.data)

export const rejectEntry = (id: string, comment: string) =>
  service.post(`/browse/entries/${id}/reject`, { comment }).then((r) => r.data)

export const getLineage = (fqn: string, upstreamDepth = 2, downstreamDepth = 2) =>
  service
    .get('/lineage/table', { params: { fqn, upstreamDepth, downstreamDepth } })
    .then((r) => r.data)

export const getGovernanceOverview = () =>
  service.get('/governance/overview').then((r) => r.data)

export const getGovernanceAssets = () =>
  service.get('/governance/assets').then((r) => r.data)

export const getAssetGovernance = (fqn: string) =>
  service.get('/governance/asset', { params: { fqn } }).then((r) => r.data)

export const setClassificationTag = (
  fqn: string,
  classification: string,
  tagFqn: string
) =>
  service
    .post('/governance/asset/tag-set', { fqn, classification, tagFqn })
    .then((r) => r.data)

export const addTag = (fqn: string, tagFqn: string) =>
  service
    .post('/governance/asset/tag', { fqn, tagFqn, action: 'add' })
    .then((r) => r.data)

export const removeTag = (fqn: string, tagFqn: string) =>
  service
    .post('/governance/asset/tag', { fqn, tagFqn, action: 'remove' })
    .then((r) => r.data)

export const attachDomain = (fqn: string, domainFqn: string, action: string) =>
  service
    .post('/governance/asset/domain', { fqn, domainFqn, action })
    .then((r) => r.data)

export const attachDataProduct = (
  fqn: string,
  dataProductFqn: string,
  action: string
) =>
  service
    .post('/governance/asset/dataProduct', {
      fqn,
      dataProductFqn,
      action
    })
    .then((r) => r.data)

export const attachGlossaryTerm = (
  fqn: string,
  termFqn: string,
  action: string
) =>
  service
    .post('/governance/asset/glossaryTerm', { fqn, termFqn, action })
    .then((r) => r.data)

export const createClassification = (name: string, description: string) =>
  service
    .post('/governance/classification', { name, description })
    .then((r) => r.data)

export const createTag = (
  classification: string,
  name: string,
  description: string
) =>
  service
    .post('/governance/tag', { classification, name, description })
    .then((r) => r.data)

export const createDomain = (name: string, description: string) =>
  service.post('/governance/domain', { name, description }).then((r) => r.data)

export const createDataProduct = (
  name: string,
  description: string,
  domainFqn: string
) =>
  service
    .post('/governance/dataProduct', { name, description, domainFqn })
    .then((r) => r.data)

export const createGlossaryTerm = (
  glossary: string,
  name: string,
  description: string
) =>
  service
    .post('/governance/glossaryTerm', { glossary, name, description })
    .then((r) => r.data)

export const saveManualLineage = (payload: any) =>
  service.post('/lineage/manual', payload).then((r) => r.data)

export const removeManualLineage = (fromFqn: string, toFqn: string) =>
  service
    .delete('/lineage/manual', { params: { fromFqn, toFqn } })
    .then((r) => r.data)

export const resolveRowLineage = (
  fqn: string,
  keys: Record<string, string>,
  depth: number
) =>
  service
    .get('/lineage/row/resolve', {
      params: { fqn, depth, key: Object.entries(keys).map(([k, v]) => `${k}=${v}`) },
      paramsSerializer: { indexes: null }
    })
    .then((r) => r.data)

export const listKeyMappings = (targetFqn?: string) =>
  service
    .get('/lineage/keys', { params: targetFqn ? { targetFqn } : {} })
    .then((r) => r.data)

export const addKeyMapping = (payload: {
  targetFqn: string
  keyColumn: string
  sourceFqn: string
  sourceKeyColumn: string
}) =>
  service
    .post('/lineage/keys', null, {
      params: {
        targetFqn: payload.targetFqn,
        keyColumn: payload.keyColumn,
        sourceFqn: payload.sourceFqn,
        sourceKeyColumn: payload.sourceKeyColumn
      }
    })
    .then((r) => r.data)

export const removeKeyMapping = (id: number) =>
  service.delete(`/lineage/keys/${id}`).then((r) => r.data)

export const listRowSwitches = () =>
  service.get('/lineage/row-switch').then((r) => r.data)

export const setRowSwitch = (tableFqn: string, enabled: boolean) =>
  service
    .put('/lineage/row-switch', null, { params: { tableFqn, enabled } })
    .then((r) => r.data)

export const listDatasources = () =>
  service.get('/datasource/datasources').then((r) => r.data)

export const listDatasourceSchemas = (datasource: string) =>
  service.get('/datasource/schemas', { params: { datasource } }).then((r) => r.data)

export const listDatasourceTables = (
  datasource: string,
  schema: string,
  q = '',
  limit = 100
) =>
  service
    .get('/datasource/tables', { params: { datasource, schema, q, limit } })
    .then((r) => r.data)

export const getDatasourceColumns = (
  datasource: string,
  schema: string,
  table: string
) =>
  service
    .get('/datasource/columns', { params: { datasource, schema, table } })
    .then((r) => r.data)

export const existsInOpenMetadata = (fqn: string) =>
  service.get('/datasource/exists', { params: { fqn } }).then((r) => r.data)
