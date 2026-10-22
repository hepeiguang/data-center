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
import {
  ListReq,
  IDataSource,
  UserIdReq,
  TypeReq,
  NameReq,
  IdReq
} from './types'

// 创建axios实例统一配置
const apiClient = axios.create({
  // baseURL: `${import.meta.env.VITE_ENCRYPT_URL}`,
  // baseURL: 'http://10.11.15.149:30209/api/system/inner/data/security',
  baseURL: `${window.location.host}/api/system/inner/data/security`,

  headers: {
    'apiKey': 'test123',
    'Content-Type': 'application/json'
  }
})

export function getAlgorithmTree(): any {
  return apiClient.get('/algorithm/rule/settings/tree')
}

export function getTableStoreEncrypt(params: {
  datasourceId: number
  tableName: string
}): any {
  return apiClient.get('/single/table/store/encrypt/data', { params })
}

export function getTableDecryptData(params: {
  datasourceId: string
  tableName: string
}): any {
  return apiClient.get('/single/table/decrypt/data', { params })
}

export function getAlgorithmSecret(params: {
  operationType: string
  algorithms: string[]
}): any {
  return apiClient.get('/secretkey/data', {
    params: {
      ...params,
      algorithms: params.algorithms.join(',')
    }
  })
}

