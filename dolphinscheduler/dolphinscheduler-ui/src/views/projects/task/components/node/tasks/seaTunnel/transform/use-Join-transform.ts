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

export function useJoinTransform({
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
  // console.log(projectCode)
  // console.log(readonly)
  // console.log(data)
  const model = reactive({
    taskType: 'Join_TRANSFORM',
    //输出的字段列表(用于展示)
    select_fields: [],
    //数据源id
    datasourceId: 0,
    //关联关系结果
    relation: [],
    //表一选择结果
    left_table: '',
    //表二选择结果
    right_table: '',
    //关联方式选择结果
    join_type: ''
  } as INodeData)

  return {
    json: [
      Fields.useName(from),
      ...Fields.useJoinTransform(model)
    ] as IJsonItem[],
    model
  }
}
