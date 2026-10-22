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
import * as Fields from '../../../fields/index'
import type { IJsonItem, INodeData, ITaskData } from '../../../types'

export function useDorisSink({
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
  const model = reactive({
    taskType: 'DORIS_SINK',
    datasourceId: '',
    
    query: '',
    database: '',
    table: '',

    schemaSaveMode: '',
    dataSaveMode: '',
    format: '',
    readJsonByLine: '',
    columnSeparator: '',
    sinkEnableDelete: false,
    needsUnsupportedTypeCasting: false,
    sink: '',
    sinkEnable2pc: false,
    sinkCheckInterval: '',
    sinkMaxRetries: '',
    sinkBufferSize: '',
    sinkBufferCount: '',
    dorisBatchSize: '',

    primary_keys: [],
    batch_size: 1000,
    transaction_timeout_sec: -1,
    field_ide: 'ORIGINAL',
    plugin_input: '',
    parallelism: 6,
    schema_save_mode: 'CREATE_SCHEMA_WHEN_NOT_EXIST',
    data_save_mode: 'APPEND_DATA',
    custom_sql: '',
    enable_upsert: true,
    use_copy_statement: false,
    feHttpPort:'',
    customSql: '',
    // outputFields: []
    // localParams: [],
  } as INodeData)

  return {
    json: [
      Fields.useName(from),
      ...Fields.useDorisSink(model),
      // Fields.usePreTasks()
    ] as IJsonItem[],
    model
  }
}
