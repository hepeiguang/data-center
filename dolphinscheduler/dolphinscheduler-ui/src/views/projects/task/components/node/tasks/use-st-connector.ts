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

import { reactive } from 'vue'
import * as Fields from '../fields/index'
import {
  defaultConnectorParams,
  parseSeaTunnelChildType
} from '@/views/projects/workflow/components/dag/seatunnel-compute-utils'
import type { IJsonItem, INodeData, ITaskData } from '../types'

export function useStConnector({
  projectCode,
  from = 0,
  readonly,
  data
}: {
  projectCode: number
  from?: number
  readonly?: boolean
  data?: ITaskData
}) {
  const taskType = data?.taskType || 'FakeSource_SOURCE'
  const parsed = parseSeaTunnelChildType(taskType)
  const params = (data?.taskParams || {}) as Record<string, any>
  const {
    pluginName: _pn,
    connectorType: _ct,
    localParams,
    resourceList,
    ...restParams
  } = params

  const pluginName = params.pluginName || parsed.pluginName
  const connectorType = params.connectorType || parsed.kind
  const mergedParams =
    restParams && Object.keys(restParams).length
      ? restParams
      : defaultConnectorParams(pluginName, connectorType)

  const model = reactive({
    name: '',
    taskType,
    flag: 'YES',
    description: '',
    timeoutFlag: false,
    localParams: localParams || [],
    environmentCode: null,
    failRetryInterval: 1,
    failRetryTimes: 0,
    workerGroup: 'default',
    cpuQuota: -1,
    memoryMax: -1,
    delayTime: 0,
    timeout: 30,
    timeoutNotifyStrategy: ['WARN'],
    pluginName,
    connectorType,
    connectorParams: JSON.stringify(mergedParams || {}, null, 2)
  } as INodeData)

  return {
    json: [
      Fields.useName(from),
      Fields.useRunFlag(),
      Fields.useDescription(),
      ...Fields.useStConnector(model),
      Fields.usePreTasks()
    ] as IJsonItem[],
    model
  }
}
