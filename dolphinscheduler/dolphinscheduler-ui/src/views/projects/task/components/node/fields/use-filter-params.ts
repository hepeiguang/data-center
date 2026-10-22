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
import { Ref, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../types'

export function useFilterParams({
  model,
  field,
  isSimple,
  name = 'custom_parameters',
  span = 0,
  hideType = false
}: {
  model: { [field: string]: any }
  field: string
  isSimple: boolean
  name?: string
  span?: Ref | number
  hideType?: boolean
}): IJsonItem[] {
  const { t } = useI18n()
  const filterOptions = [
    {
      label: '',
      value: true
    }
  ]

  return [
    {
      type: 'custom-parameters',
      field: field,
      name: name,
      class: 'btn-custom-parameters',
      span,
      children: [
        {
          type: 'checkbox',
          field: 'retain',
          name: '',
          value: [true],
          options: filterOptions,
          single: true,
          span: 2,
          validate: {
            trigger: ['input'],
            validator(validate: any, value: []) {
              if (model.waitCompleteTimeoutEnable && !value.length) {
                return new Error(t('project.node.timeout_strategy_tips'))
              }
            }
          }
        },
        {
          type: 'input',
          field: 'field',
          span: hideType? 9: 6,
          class: 'input-param-key',
          props: {
            placeholder: t('name(必填)'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.prop_tips'))
              }

              const sameItems = model[field].filter(
                (item: { prop: string }) => item.prop === value
              )

              if (sameItems.length > 1) {
                return new Error(t('project.node.prop_repeat'))
              }
            }
          }
        },
        {
          type: 'select',
          field: 'fieldType',
          span: hideType? 0:  6,
          options: TYPE_LIST,
          value: 'VARCHAR',
          props: {
            // disabled: isSimple
          }
        },
        {
          type: 'input',
          field: 'fieldDes',
          span: hideType? 11: 8,
          class: 'input-param-value',
          props: {
            placeholder: t('说明'),
            maxLength: 256
          }
        }
      ]
    }
  ]
}

export const TYPE_LIST = [
  {
    value: 'TINYINT',
    label: 'TINYINT'
  },
  {
    value: 'SMALLINT',
    label: 'SMALLINT'
  },
  {
    value: 'MEDIUMINT',
    label: 'MEDIUMINT'
  },
  {
    value: 'INT',
    label: 'INT'
  },
  {
    value: 'BIGINT',
    label: 'BIGINT'
  },
  {
    value: 'FLOAT',
    label: 'FLOAT'
  },
  {
    value: 'DOUBLE',
    label: 'DOUBLE'
  },
  {
    value: 'DECIMAL',
    label: 'DECIMAL'
  },
  {
    value: 'CHAR',
    label: 'CHAR'
  },
  {
    value: 'VARCHAR',
    label: 'VARCHAR'
  },
  {
    value: 'TINYTEXT',
    label: 'TINYTEXT'
  },
  {
    value: 'TEXT',
    label: 'TEXT'
  },
  {
    value: 'MEDIUMTEXT',
    label: 'MEDIUMTEXT'
  },
  {
    value: 'LONGTEXT',
    label: 'LONGTEXT'
  },
  {
    value: 'TINYBLOB',
    label: 'TINYBLOB'
  },
  {
    value: 'BLOB',
    label: 'BLOB'
  },
  {
    value: 'MEDIUMBLOB',
    label: 'MEDIUMBLOB'
  },
  {
    value: 'LONGBLOB',
    label: 'LONGBLOB'
  },
  {
    value: 'YEAR',
    label: 'YEAR'
  },
  {
    value: 'TIME',
    label: 'TIME'
  },
  {
    value: 'DATE',
    label: 'DATE'
  },
  {
    value: 'DATETIME',
    label: 'DATETIME'
  },
  {
    value: 'TIMESTAMP',
    label: 'TIMESTAMP'
  }
]
