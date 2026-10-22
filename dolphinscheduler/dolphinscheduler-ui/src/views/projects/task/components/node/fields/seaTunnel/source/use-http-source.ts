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
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
import { useKeyValues } from '@/views/projects/task/components/node/fields'
export function useHttpSource(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  const schemaSpan = computed(() => (model.format == 'json' ? 24 : 0))
  const pageingSpan = computed(() => (model.isPage ? 24 : 0))

  computed(() => (model.replyEnabled ? 24 : 0))
  return [
    {
      type: 'input',
      field: 'url',
      name: t('seaTunnel.url'),
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(
              t('seaTunnel.url') + ' ' + t('project.node.required')
            )
          }
        }
      },
      props: {
        placeholder: t('seaTunnel.pleaseEnter') + t('seaTunnel.url')
      }
    },
    {
      type: 'select',
      field: 'method',
      name: t('seaTunnel.method'),
      options: METHODS
    },
    {
      type: 'custom-parameters',
      field: 'httpHeaders',
      name: t('seaTunnel.headers'),
      children: [
        {
          type: 'input',
          field: 'prop',
          span: 8,
          props: {
            placeholder: t('project.node.prop_tips'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.prop_tips'))
              }

              const sameItems = model.httpHeaders.filter(
                (item: { prop: string }) => item.prop === value
              )

              if (sameItems.length > 1) {
                return new Error(t('project.node.prop_repeat'))
              }
            }
          }
        },
        {
          type: 'input',
          field: 'value',
          span: 10,
          props: {
            placeholder: t('project.node.value_tips'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.value_required_tips'))
              }
            }
          }
        }
      ]
    },
    {
      type: 'custom-parameters',
      field: 'httpParams',
      name: t('seaTunnel.params'),
      children: [
        {
          type: 'input',
          field: 'prop',
          span: 8,
          props: {
            placeholder: t('project.node.prop_tips'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.prop_tips'))
              }

              const sameItems = model.httpParams.filter(
                (item: { prop: string }) => item.prop === value
              )

              if (sameItems.length > 1) {
                return new Error(t('project.node.prop_repeat'))
              }
            }
          }
        },
        {
          type: 'input',
          field: 'value',
          span: 10,
          props: {
            placeholder: t('project.node.value_tips'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.value_required_tips'))
              }
            }
          }
        }
      ]
    },
    {
      type: 'input',
      field: 'body',
      name: t('seaTunnel.body'),
      props: {
        type: 'textarea',
        placeholder: t('project.node.http_body')
      }
    },
    {
      type: 'select',
      field: 'format',
      name: t('seaTunnel.format'),
      options: FORMATS
    },
    {
      type: 'mirror',
      field: 'schemaVo',
      name: 'schema',
      tooltip: '请输入标准json,例如：{ "key": "value" }',
      span: schemaSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
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
      type: 'mirror',
      field: 'jsonFieldVo',
      name: 'json_field',
      tooltip: t('seaTunnel.http_source_json_field_tips'),
      span: schemaSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
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
      type: 'switch',
      field: 'isPage',
      name: t('seaTunnel.pageing')
    },
    {
      type: 'mirror',
      field: 'pageingVo',
      name: '分页参数',
      tooltip: '请输入标准json,例如：{ "key": "value" }',
      span: pageingSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
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
    ...useKeyValues({
      model,
      tooltip:
        '附件参数：\n' +
        'content_json: 获取部分JSON数据。\n' +
        'poll_interval_millis：流式模式下请求HTTP API的时间间隔（毫秒）。\n' +
        'retry：如果HTTP请求返回IOException时的最大重试次数。\n' +
        'retry_backoff_multiplier_ms：HTTP请求失败时的重试退避时间乘数（毫秒）。\n' +
        'retry_backoff_max_ms：HTTP请求失败时的最大重试退避时间（毫秒）。\n' +
        'connect_timeout_ms：连接超时设置，默认12秒。\n' +
        'socket_timeout_ms：套接字超时设置，默认60秒。',
      field: 'httpExtra',
      name: 'seaTunnel.config'
    })
  ]
}

export const FORMATS = [
  {
    label: 'json',
    value: 'json'
  },
  {
    label: 'text',
    value: 'text'
  }
]

export const METHODS = [
  {
    label: 'GET',
    value: 'GET'
  },
  {
    label: 'POST',
    value: 'POST'
  }
]
