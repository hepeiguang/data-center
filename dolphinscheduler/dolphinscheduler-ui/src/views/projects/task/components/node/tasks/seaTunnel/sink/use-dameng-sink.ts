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

export function useDamengSink({
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
    taskType: 'DAMENG_SINK',
    datasourceId: '',
    is_custom_sql: false,
    is_partition:  false,
    partition_column :10,
    max_retries: 0,
    query: '',
    is_exactly_once:false,
    database:'',
    table:'',
    primary_keys:[],
    support_upsert_by_query_primary_key_exist:false,
    connection_check_timeout_sec:30,
    batch_size:1000,
    generate_sink_sql:false,
    xa_data_source_class_name:'',
    max_commit_attempts:3,
    transaction_timeout_sec:-1,
    auto_commit:true,
    field_ide:'',
    properties:{},
    schema_save_mode:'CREATE_SCHEMA_WHEN_NOT_EXIST',
    data_save_mode:'APPEND_DATA',
    custom_sql:'',
    enable_upsert:true
  } as INodeData)

  return {
    json: [
      Fields.useName(from),
      ...Fields.useDamengSink(model),
    ] as IJsonItem[],
    model
  }
}
