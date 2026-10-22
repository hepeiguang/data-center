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
import { reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
import { useUdfs } from '../../use-udfs'
export function useSqlrowtocolumnTransform(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  const compareOptions = [
    { value: '=', label: '=' },
    { value: '!=', label: '!=' },
    { value: '>', label: '>' },
    { value: '>=', label: '>=' },
    { value: '<', label: '<' },
    { value: '<=', label: '<=' }
  ]
  const aggregationOptions = [
    { value: 'COUNT', label: 'COUNT' },
    { value: 'SUM', label: 'SUM' },
    { value: 'AVG', label: 'AVG' },
    { value: 'MIN', label: 'MIN' },
    { value: 'MAX', label: 'MAX' }
  ]

  return [
    {
      type: 'input',
      field: 'groupField',
      span: 24,
      name: t(`关键字段/分组字段`),
      props: {
        placeholder: t('关键字段/分组字段'),
        maxLength: 256
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('该字段为必填项'))
          }
        }
      }
    },
    {
      type: 'custom-parameters',
      field: 'targetFields',
      //name: t(`数据映射设置`),
      span: 24,
      children: [
        {
          type: 'input',
          field: 'dataField',
          span: 8,
          props: {
            placeholder: t('数据字段'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填项'))
              }
            }
          }
        },
        {
          type: 'input',
          field: 'srcValue',
          span: 8,
          props: {
            placeholder: t('数据字段值'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填项'))
              }
            }
          }
        },
        {
          type: 'select',
          field: 'expression',
          span: 6,
          options: compareOptions,
          value: '=',
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填项'))
              }
            }
          }
        },
        {
          type: 'input',
          field: 'targetField',
          span: 8,
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
            }
          }
        },
        {
          type: 'input',
          field: 'targetFieldValue',
          span: 8,
          props: {
            placeholder: t('目标字段值'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填项'))
              }
            }
          }
        },
        {
          type: 'select',
          field: 'aggregateFunctions',
          span: 6,
          options: aggregationOptions,
          value: 'COUNT',
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填项'))
              }
            }
          }
        },
      ]
    },
    // //////////////////////////////////
    useUdfs(model)
  ]
}
