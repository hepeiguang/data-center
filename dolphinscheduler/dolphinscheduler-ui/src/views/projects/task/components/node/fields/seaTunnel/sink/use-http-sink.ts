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
import type { IJsonItem } from '../../../types'
export function useHttpSink(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
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
      type: 'input-number',
      field: 'connectTimeoutMs',
      name: t('seaTunnel.connectTimeoutMs'),
      props: {
        placeholder:
          t('seaTunnel.pleaseEnter') + t('seaTunnel.connectTimeoutMs')
      }
    },
    {
      type: 'input-number',
      field: 'socketTimeoutMs',
      name: t('seaTunnel.socketTimeoutMs'),
      props: {
        placeholder: t('seaTunnel.pleaseEnter') + t('seaTunnel.socketTimeoutMs')
      }
    },
    {
      type: 'input-number',
      field: 'retry',
      name: t('seaTunnel.retry'),
      props: {
        placeholder: t('seaTunnel.pleaseEnter') + t('seaTunnel.retry')
      }
    },
    {
      type: 'input-number',
      field: 'retryBackoffMultiplierMs',
      name: t('seaTunnel.retryBackoffMultiplierMs'),
      props: {
        placeholder:
          t('seaTunnel.pleaseEnter') + t('seaTunnel.retryBackoffMultiplierMs')
      }
    },
    {
      type: 'input-number',
      field: 'retryBackoffMaxMs',
      name: t('seaTunnel.retryBackoffMaxMs'),
      props: {
        placeholder:
          t('seaTunnel.pleaseEnter') + t('seaTunnel.retryBackoffMaxMs')
      }
    }
  ]
}
