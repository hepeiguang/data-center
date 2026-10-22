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

export function useMinioFileSink({
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
    taskType: 'MINIOFILE_SINK',
    datasourceId: '',
    bucket: '',
    path: '',
    file_format_type: 'text',
    have_partition: false,
    is_partition_field_write_in_file: false,
    single_file_mode: false,
    custom_filename: false,
    create_empty_file_when_no_data: false,
    enable_header_write: false,
    xml_use_attr_format: false
  } as INodeData)

  return {
    json: [
      Fields.useName(from),
      ...Fields.useMinioFileSink(model),
    ] as IJsonItem[],
    model
  }
}
