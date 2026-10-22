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
import { Ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../types'

export function useJsonPathMapping({
  model,
  field,
  name = 'custom_jsonpath',
  span = 24,
  options
}: {
  model: { [field: string]: any }
  field: string
  name?: string
  span?: Ref | number
  options?: any
}): IJsonItem[] {
  const { t } = useI18n()
  if (!model[field]?.length) {
    model[field] = [{
      src_field: '',
      path: '',
      dest_field: '',
      dest_type: 'STRING',
      column_error_handle_way: 'FAIL'
    }]
  }

  return [
    {
      type: 'custom-parameters',
      field: field,
      name: t(`字段配置`),
      tooltip: t(`示例：
      源字段：json_data (数据类型:STRING,BYTES,ARRAY,MAP,ROW)
      目标字段：c1_string
      path：$.data.c_string
      目标字段类型：STRING`),
      class: 'btn-mapping-parameters',
      span,
      children: [
        {
          type: 'select',
          field: 'src_field',
          options,
          span: 6,
          class: 'input-src-field',
          props: {
            placeholder: t('源字段'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填项'))
              }
              const sameItems = model[field].filter(
                (item: { src_field: string }) => item.src_field === value
              )
            }
          }
        },
        {
          type: 'input',
          field: 'dest_field',
          span: 6,
          class: 'input-dest-field',
          props: {
            placeholder: t('目标字段'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填项'))
              }
              const sameItems = model[field].filter(
                (item: { dest_field: string }) => item.dest_field === value
              )
              // if (sameItems.length > 1) {
              //   return new Error(t('project.node.prop_repeat'))
              // }
            }
          }
        },
        {
          type: 'select',
          field: 'dest_type',
          span: 6,
          options: DATA_TYPE_LIST,
          value: 'STRING',
          props: {
            filterable: true,
            allowCreate: true
          }
        },
        {
          type: 'input',
          field: 'path',
          span: 6,
          class: 'input-json-path',
          props: {
            placeholder: t('path'),
            maxLength: 512
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填项'))
              }
              const sameItems = model[field].filter(
                (item: { path: string }) => item.path === value
              )
              // if (sameItems.length > 1) {
              //   return new Error(t('project.node.prop_repeat'))
              // }
            }
          }
        },
        {
            type:'select',
            field:'column_error_handle_way',
            span:6,
            options:COLUMN_ERROR,
            value: 'FAIL',
            props: {
                filterable: true,
                placeholder: t('列数据异常处理'),
                allowCreate: true
            }

        }
      ]
    }
  ]
}

export const DATA_TYPE_LIST = [
  { value: 'string', label: 'STRING' },
  { value: 'boolean', label: 'BOOLEAN' },
  { value: 'int', label: 'INTEGER' },
  { value: 'float', label: 'FLOAT' },
  { value: 'double', label: 'DOUBLE' },
  { value: 'decimal(4,2)', label: 'DECIMAL' },
  { value: 'date', label: 'DATE' },
  { value: 'time', label: 'TIMESTAMP' },
  { value: 'array<string>', label: 'ARRAY<STRING>' },
  { value: 'array<map<string, string>>', label: 'ARRAY<MAP<STRING,STRING>>' }
]
export const COLUMN_ERROR = [
    { value: 'FAIL', label: '抛出异常' },
    { value: 'SKIP', label: '跳过错误列' },
    { value: 'SKIP_ROW', label: '跳过错误行' }
]
