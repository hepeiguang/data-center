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
import {computed, Ref, watch} from 'vue'
import {useI18n} from 'vue-i18n'
import type {IJsonItem} from '../types'
import {filter} from 'lodash'
import {c} from 'naive-ui'

export function useCustomSqlfilter({
                                     model,
                                     field,
                                     name = 'custom_parameters',
                                     span = 24,
                                     options
                                   }: {
  model: { [field: string]: any }
  field: string
  name?: string
  span?: Ref | number
  options:any
}): IJsonItem[] {
  const {t} = useI18n()
  const compareOptions = [
    {label: t('='), value: '='},
    {label: t('!='), value: '!='},
    {label: t('>'), value: '>'},
    {label: t('>='), value: '>='},
    {label: t('<'), value: '<'},
    {label: t('<='), value: '<='},
    // {label: t('是null'), value: 'is null'},
    // {label: t('不是null'), value: 'is not null'},
  ]

  watch(
    () => model[field],
    (newVal: Array<{ status: string }> | undefined) => {
      if (!newVal) return
      newVal.forEach((item) => {
        // item.value = item.status == 'is null' || item.status == 'is not null' ? 'null' : item.value
        // item.type = item.status == 'is null' || item.status == 'is not null' ? 'null' : item.type
      })
    },
    {deep: true, immediate: true}
  )

  return [
    {
      type: 'custom-parameters',
      field: field,
      // name: t(``),
      class: 'btn-custom-parameters',
      span,
      children: [
        {
          type: 'select',
          field: 'com',
          span: 24,
          options: DIRECT_LIST,
          value: '',
          props: {
            placeholder: t('请选择运算符'),
            // hidden: model[field].filter((item:{com:string}) => item.com== '').length >0
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
          field: 'prop',
          span: 6,
          class: 'input-param-key',
          options,
          props: {
            placeholder: t('字段名'),
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
          field: 'status',
          span: 4,
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
          field: 'value',
          span: 6,
          class: 'input-param-value',
          props: {
            placeholder: t('字段值'),
            maxLength: 256,
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
          field: 'type',
          span: 6,
          options: [
            {label: t('数值型'), value: 'num'},
            {label: t('字符串型'), value: 'string'},
            {label: t('日期型'), value: 'date'},
            {label: t('布尔型'), value: 'bool'},
          ],
          props: {
            placeholder: t('字段值类型'),
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
      ]
    }
  ]
}

export const DIRECT_LIST = [
  {
    value: 'AND',
    label: 'AND'
  },
  {
    value: 'OR',
    label: 'OR'
  }
]
