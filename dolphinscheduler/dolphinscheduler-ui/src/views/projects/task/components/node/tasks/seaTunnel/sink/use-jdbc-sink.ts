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

export function useJdbcSink({
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
    taskType: 'JDBC_SINK',
    datasourceId: '',
    is_custom_sql: false,
    query: '',
    database: '',
    table: '',
    primary_keys: [],
    batch_size: 1000,
    is_exactly_once: false,
    transaction_timeout_sec: -1,
    field_ide: 'ORIGINAL',
    plugin_input: '',
    parallelism: 6,
    schema_save_mode: 'CREATE_SCHEMA_WHEN_NOT_EXIST',
    data_save_mode: 'APPEND_DATA',
    custom_sql: '',
    enable_upsert: true,
    use_copy_statement: false,
    // outputFields: []
    // localParams: [],
  } as INodeData)

  return {
    json: [
      Fields.useName(from),
      // ...Fields.useTaskDefinition({ projectCode, from, readonly, data, model }),
      // Fields.useRunFlag(),
      // Fields.useCache(),
      // Fields.useDescription(),
      // Fields.useTaskPriority(),
      // Fields.useWorkerGroup(projectCode),
      // Fields.useEnvironmentName(model, !data?.id),
      // ...Fields.useTaskGroup(model, projectCode),
      // ...Fields.useFailed(),
      // ...Fields.useResourceLimit(),
      // Fields.useDelayTime(model),
      // ...Fields.useTimeoutAlarm(model),
      ...Fields.useJdbcSink(model),
      // Fields.usePreTasks()
    ] as IJsonItem[],
    model
  }
}
