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
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../types'
import { watch, ref, computed } from 'vue'

// const STANDARD_TYPE = [
//   {
//     label: '值域',
//     value: 'RANGE'
//   },
//   {
//     label: '时间戳',
//     value: 'TIMESTAMP'
//   },
//   {
//     label: '数值精度',
//     value: 'NUMACCURACY'
//   },
//   {
//     label: '命名标准',
//     value: 'NAMESPACE'
//   },
// ]
const TIME_TYPE = [
  {
    label: 'yyyy',
    value: 'yyyy'
  },
  {
    label: 'yyyy-MM',
    value: 'yyyy-MM'
  },
  {
    label: 'yyyy-MM-dd',
    value: 'yyyy-MM-dd'
  },
  {
    label: 'yyyy-MM-dd HH',
    value: 'yyyy-MM-dd HH'
  },
  {
    label: 'yyyy-MM-dd HH:mm',
    value: 'yyyy-MM-dd HH:mm'
  },
  {
    label: 'yyyy-MM-dd HH:mm:ss',
    value: 'yyyy-MM-dd HH:mm:ss'
  }
]

export function useNormalize(
  model: { [field: string]: any },
  fieldsSpan: any,
  functionOptions: any
): IJsonItem[] {
  console.log(model,'aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa')

  const { t } = useI18n()
  const typeSpan = computed(() => (!fieldsSpan.value ? 0 : 24))
  const minMaxSpan = computed(() =>
    !fieldsSpan.value ? 0 : model.dictType === 'RANGE' ? 12 : 0
  )
  const timeTypeSpan = computed(() =>
    !fieldsSpan.value ? 0 : model.dictType === 'TIMESTAMP' ? 24 : 0
  )
  const numAccuracySpan = computed(() =>
    !fieldsSpan.value ? 0 : model.dictType === 'NUMACCURACY' ? 24 : 0
  )
  // const dictCodeSpan = computed(() => (!fieldsSpan.value ? 0 : model.dictType === 'NAMESPACE' ? 24 : 0))
  const dictCodeArr = computed(
    () =>
      Array.isArray(functionOptions.value) &&
      functionOptions.value.find((item: any) => item.value === 'NAMESPACE')
        ?.children
  )
  const typeCodeArr = computed(
    () =>
      Array.isArray(functionOptions.value) &&
      functionOptions.value.map((item: any) => {
        return { label: item.label, value: item.value, apiUrl: item.apiUrl }
      })
  )
  watch(
    () => model.dictId,
    () => {
      model.dictType = functionOptions.value.find(
        (ele: any) => ele.id === model.dictId
      )?.dictType
    }
  )

  return [
    {
      type: 'select',
      field: 'dictId',
      name: '标准类型',
      span: typeSpan,
      required: true,
      options: typeCodeArr,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请选择标准类型'))
          }
        }
      }
    },
    {
      type: 'input-number',
      field: 'min_num',
      name: t('最小值域'),
      span: minMaxSpan,
      props: { min: 0, placeholder: '10000' },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value && value != '0') {
            return new Error(t('请输入最小值域'))
          } else if (model.max_num < value && model.max_num > 0) {
            return new Error(t('最小值域需小于最大值域'))
          }
        }
      }
    },
    {
      type: 'input-number',
      field: 'max_num',
      name: t('最大值域'),
      span: minMaxSpan,
      props: { min: 1, placeholder: '10000' },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请输入最大值域'))
          } else if (model.min_num > value) {
            return new Error(t('最大值域需大于最小值域'))
          }
        }
      }
    },
    {
      type: 'select',
      field: 'time_type',
      name: '时间戳',
      span: timeTypeSpan,
      options: TIME_TYPE,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请选择时间戳'))
          }
        }
      }
    },
    {
      type: 'input-number',
      field: 'num_accuracy',
      name: '数值精度',
      required: true,
      span: numAccuracySpan,
      props: {
        min: 0
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value && value != '0') {
            return new Error(t('请输入数值精度'))
          }
        }
      },
      slots: {
        suffix: () => '位小数'
      }
    }
    // {
    //   type: 'tree-select',
    //   field: 'dictCode',
    //   name: t('字典选择'),
    //   span: dictCodeSpan,
    //   options: dictCodeArr,
    //   props: {
    //     overrideDefaultNodeClickBehavior: true,
    //     keyField: 'value',
    //     labelField: 'label'
    //   },
    //   validate: {
    //     trigger: ['input', 'blur'],
    //     required: true,
    //     validator(validate: any, value: string) {
    //       if (!value) {
    //         return new Error(t('请选择字典'))
    //       }
    //     }
    //   }
    // },
  ]
}
