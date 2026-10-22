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
import { ref, defineComponent, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
export function useFakeSourceSource(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  const advancedSpan = computed(() => (model.advanced == true ? 24 : 0))
  const stringTemplateSpan = computed(() => (model['string.fake.mode'] == 'template' ? 24 : 0))
  const tinyintTemplateSpan = computed(() => (model['tinyint.fake.mode'] == 'template' ? 24 : 0))
  const tinyintRangeSpan = computed(() => (model['tinyint.fake.mode'] == 'range' ? 24 : 0))
  const smallintTemplateSpan = computed(() => (model['smallint.fake.mode'] == 'template' ? 24 : 0))
  const smallintRangeSpan = computed(() => (model['smallint.fake.mode'] == 'range' ? 24 : 0))
  const intTemplateSpan = computed(() => (model['int.fake.mode'] == 'template' ? 24 : 0))
  const intRangeSpan = computed(() => (model['int.fake.mode'] == 'range' ? 24 : 0))
  const bigintTemplateSpan = computed(() => (model['bigint.fake.mode'] == 'template' ? 24 : 0))
  const bigintRangeSpan = computed(() => (model['bigint.fake.mode'] == 'range' ? 24 : 0))
  const floatTemplateSpan = computed(() => (model['float.fake.mode'] == 'template' ? 24 : 0))
  const floatRangeSpan = computed(() => (model['float.fake.mode'] == 'range' ? 24 : 0))
  const doubleTemplateSpan = computed(() => (model['double.fake.mode'] == 'template' ? 24 : 0))
  const doubleRangeSpan = computed(() => (model['double.fake.mode'] == 'range' ? 24 : 0))

  return [
    {
      type: 'input-number',
      field: 'row.num',
      name: t('seaTunnel.row_num'),
      props: {
        placeholder: t('5')
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('数据总行数') + ' ' + t('project.node.required'))
          }
        }
      }
    },
    {
      type: 'mirror',
      field: 'schemaVo',
      name: t('seaTunnel.schemaVo'),
      tooltip: '请输入标准json,例如：{ "key": "value" }',
      validate: {
        trigger: ['input', 'trigger'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('数据模式') + ' ' + t('project.node.required'))
          }
          if (value) {
            try {
              JSON.parse(value)
            } catch (error) {
              return new Error('输入的内容不是合法的 JSON 格式')
            }
          }
        }
      },
      props: {
        language: 'sql'
      }
    },
    {
      type: 'input-number',
      field: 'split.num',
      name: t('seaTunnel.split_num'),
      props: {
        placeholder: t('1')
      }
    },
    {
      type: 'input-number',
      field: 'split.read-interval',
      name: t('seaTunnel.split_read_interval'),
      props: {
        placeholder: t('1')
      }
    },
    {
      type: 'switch',
      field: 'advanced',
      name: t('seaTunnel.advanced'),
    },
    {
      type: 'input-number',
      field: 'map.size',
      name: t('seaTunnel.map_size'),
      props: {
        placeholder: t('5')
      },
      span: advancedSpan
    },
    {
      type: 'input-number',
      field: 'array.size',
      name: t('seaTunnel.array_size'),
      props: {
        placeholder: t('5')
      },
      span: advancedSpan
    },
    {
      type: 'input-number',
      field: 'bytes.length',
      name: t('seaTunnel.bytes_length'),
      props: {
        placeholder: t('5')
      },
      span: advancedSpan
    },
    {
      type: 'input-number',
      field: 'string.length',
      name: t('seaTunnel.string_length'),
      props: {
        placeholder: t('5')
      },
      span: advancedSpan
    },
    {
      type: 'select',
      field: 'string.fake.mode',
      name: t('seaTunnel.string_fake_mode'),
      options: DATA_MODE_TYPE,
      span: advancedSpan
    },
    {
      type: 'input',
      field: 'stringTemplateVo',
      name: t('seaTunnel.stringTemplateVo'),
      tooltip: '请输入标准json array,例如：["value1","value2"]',
      span: stringTemplateSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
          if (value) {
            try {
              if (!Array.isArray(JSON.parse(value))) {
                return new Error('输入的内容不是合法的JSONArray格式')
              }
            } catch (error) {
              return new Error('输入的内容不是合法的JSONArray格式')
            }
          }
        }
      }
    },
    {
      type: 'select',
      field: 'tinyint.fake.mode',
      name: t('seaTunnel.tinyint_fake_mode'),
      options: DATA_MODE_TYPE,
      span: advancedSpan
    },
    {
      type: 'input-number',
      field: 'tinyint.min',
      name: t('seaTunnel.tinyint_min'),
      props: {
        placeholder: t('0')
      },
      span: tinyintRangeSpan
    },
    {
      type: 'input-number',
      field: 'tinyint.max',
      name: t('seaTunnel.tinyint_max'),
      options: DATA_MODE_TYPE,
      props: {
        placeholder: t('127')
      },
      span: tinyintRangeSpan
    },
    {
      type: 'input',
      field: 'tinyintTemplateVo',
      name: t('seaTunnel.tinyintTemplateVo'),
      tooltip: '请输入标准json array,例如：[value1,value2]',
      span: tinyintTemplateSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
          if (value) {
            try {
              if (!Array.isArray(JSON.parse(value))) {
                return new Error('输入的内容不是合法的JSONArray格式')
              }
            } catch (error) {
              return new Error('输入的内容不是合法的JSONArray格式')
            }
          }
        }
      }
    },
    {
      type: 'select',
      field: 'smallint.fake.mode',
      name: t('seaTunnel.smallint_fake_mode'),
      options: DATA_MODE_TYPE,
      span: advancedSpan
    },
    {
      type: 'input-number',
      field: 'smallint.min',
      name: t('seaTunnel.smallint_min'),
      props: {
        placeholder: t('0')
      },
      span: smallintRangeSpan
    },
    {
      type: 'input-number',
      field: 'smallint.max',
      name: t('seaTunnel.smallint_max'),
      options: DATA_MODE_TYPE,
      props: {
        placeholder: t('32767')
      },
      span: smallintRangeSpan
    },
    {
      type: 'input',
      field: 'smallintTemplateVo',
      name: t('seaTunnel.smallintTemplateVo'),
      tooltip: '请输入标准json array,例如：[value1,value2]',
      span: smallintTemplateSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
          if (value) {
            try {
              if (!Array.isArray(JSON.parse(value))) {
                return new Error('输入的内容不是合法的JSONArray格式')
              }
            } catch (error) {
              return new Error('输入的内容不是合法的JSONArray格式')
            }
          }
        }
      }
    },
    {
      type: 'select',
      field: 'int.fake.mode',
      name: t('seaTunnel.int_fake_mode'),
      options: DATA_MODE_TYPE,
      span: advancedSpan
    },
    {
      type: 'input-number',
      field: 'int.min',
      name: t('seaTunnel.int_min'),
      props: {
        placeholder: t('0')
      },
      span: intRangeSpan
    },
    {
      type: 'input-number',
      field: 'int.max',
      name: t('seaTunnel.int_max'),
      options: DATA_MODE_TYPE,
      props: {
        placeholder: t('0x7fffffff')
      },
      span: intRangeSpan
    },
    {
      type: 'input',
      field: 'intTemplateVo',
      name: t('seaTunnel.intTemplateVo'),
      tooltip: '请输入标准json array,例如：[value1,value2]',
      span: intTemplateSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
          if (value) {
            try {
              if (!Array.isArray(JSON.parse(value))) {
                return new Error('输入的内容不是合法的JSONArray格式')
              }
            } catch (error) {
              return new Error('输入的内容不是合法的JSONArray格式')
            }
          }
        }
      }
    },
    {
      type: 'select',
      field: 'bigint.fake.mode',
      name: t('seaTunnel.bigint_fake_mode'),
      options: DATA_MODE_TYPE,
      span: advancedSpan
    },
    {
      type: 'input-number',
      field: 'bigint.min',
      name: t('seaTunnel.bigint_min'),
      props: {
        placeholder: t('0')
      },
      span: bigintRangeSpan
    },
    {
      type: 'input-number',
      field: 'bigint.max',
      name: t('seaTunnel.bigint_max'),
      options: DATA_MODE_TYPE,
      props: {
        placeholder: t('0x7fffffffffffffff')
      },
      span: bigintRangeSpan
    },
    {
      type: 'input',
      field: 'bigintTemplateVo',
      name: t('seaTunnel.bigintTemplateVo'),
      tooltip: '请输入标准json array,例如：[value1,value2]',
      span: bigintTemplateSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
          if (value) {
            try {
              if (!Array.isArray(JSON.parse(value))) {
                return new Error('输入的内容不是合法的JSONArray格式')
              }
            } catch (error) {
              return new Error('输入的内容不是合法的JSONArray格式')
            }
          }
        }
      }
    },
    {
      type: 'select',
      field: 'float.fake.mode',
      name: t('seaTunnel.float_fake_mode'),
      options: DATA_MODE_TYPE,
      span: advancedSpan
    },
    {
      type: 'input-number',
      field: 'float.min',
      name: t('seaTunnel.float_min'),
      props: {
        placeholder: t('0')
      },
      span: floatRangeSpan
    },
    {
      type: 'input-number',
      field: 'float.max',
      name: t('seaTunnel.float_max'),
      options: DATA_MODE_TYPE,
      props: {
        placeholder: t('0x1.fffffeP+127')
      },
      span: floatRangeSpan
    },
    {
      type: 'input',
      field: 'floatTemplateVo',
      name: t('seaTunnel.floatTemplateVo'),
      tooltip: '请输入标准json array,例如：[value1,value2]',
      span: floatTemplateSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
          if (value) {
            try {
              if (!Array.isArray(JSON.parse(value))) {
                return new Error('输入的内容不是合法的JSONArray格式')
              }
            } catch (error) {
              return new Error('输入的内容不是合法的JSONArray格式')
            }
          }
        }
      }
    },
    {
      type: 'select',
      field: 'double.fake.mode',
      name: t('seaTunnel.double_fake_mode'),
      options: DATA_MODE_TYPE,
      span: advancedSpan
    },
    {
      type: 'input-number',
      field: 'double.min',
      name: t('seaTunnel.double_min'),
      props: {
        placeholder: t('0')
      },
      span: doubleRangeSpan
    },
    {
      type: 'input-number',
      field: 'double.max',
      name: t('seaTunnel.double_max'),
      options: DATA_MODE_TYPE,
      props: {
        placeholder: t('0x1.fffffffffffffP+1023')
      },
      span: doubleRangeSpan
    },
    {
      type: 'input',
      field: 'doubleTemplateVo',
      name: t('seaTunnel.doubleTemplateVo'),
      tooltip: '请输入标准json array,例如：[value1,value2]',
      span: doubleTemplateSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
          if (value) {
            try {
              if (!Array.isArray(JSON.parse(value))) {
                return new Error('输入的内容不是合法的JSONArray格式')
              }
            } catch (error) {
              return new Error('输入的内容不是合法的JSONArray格式')
            }
          }
        }
      }
    },
    {
      type: 'input-number',
      field: 'vector.dimension',
      name: t('seaTunnel.vector_dimension'),
      tooltip: '不包括二进制向量',
      props: {
        placeholder: t('4')
      },
      span: advancedSpan
    },
    {
      type: 'input-number',
      field: 'binary.vector.dimension',
      name: t('seaTunnel.binary_vector_dimension'),
      props: {
        placeholder: t('4')
      },
      span: advancedSpan
    },
    {
      type: 'input-number',
      field: 'vector.float.min',
      name: t('seaTunnel.vector_float_min'),
      props: {
        placeholder: t('0')
      },
      span: advancedSpan
    },
    {
      type: 'input-number',
      field: 'vector.float.max',
      name: t('seaTunnel.vector_float_max'),
      options: DATA_MODE_TYPE,
      props: {
        placeholder: t('0x1.fffffeP+127')
      },
      span: advancedSpan
    }
  ]
}

export const DATA_MODE_TYPE = [
  {
    label: 'range',
    value: 'range'
  },
  {
    label: 'template',
    value: 'template'
  }
]