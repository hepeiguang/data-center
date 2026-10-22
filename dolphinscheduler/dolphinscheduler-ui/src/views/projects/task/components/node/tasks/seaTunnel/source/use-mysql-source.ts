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

export function useMysqlSource({
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
    taskType: 'MYSQL_SOURCE',
    datasourceId:  '',
    query: '',
    partition_column: '',
    partition_lower_bound: null,
    partition_upper_bound: null,
    fetch_size: 0,
    table_path: '',
    'split.size': 8096,
    where_condition: '',
    whereCondition: '',
    connection_check_timeout_sec: 30,
    partition_num: 6,
    properties:[],
    is_partition:  false,
    is_custom_sql: false,
    selectedColumns:null
  } as INodeData)

  return {
    json: [
      Fields.useName(from),
      ...Fields.useMysqlSource(model),
    ] as IJsonItem[],
    model
  }
}