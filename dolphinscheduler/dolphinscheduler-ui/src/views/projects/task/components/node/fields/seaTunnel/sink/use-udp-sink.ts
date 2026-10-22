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
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
export function useUdpSink(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  const schemaSpan = computed(() => (model.format == 'json' ? 24 : 0))
  const asyncThreadsSpan = computed(() => (model.asyncEnabled ? 24 : 0))

  return [
    {
      type: 'input',
      field: 'host',
      name: t('seaTunnel.host'),
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('seaTunnel.host') + ' ' + t('project.node.required'))
          }
        }
      },
      props: {
        placeholder: t('seaTunnel.pleaseEnter') + t('seaTunnel.host')
     }
    },
    {
      type: 'input-number',
      field: 'port',
      name: t('seaTunnel.port'),
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('seaTunnel.port') + t('project.node.required'))
          }
        }
      },
      props: {
        placeholder: t('seaTunnel.pleaseEnter') + t('seaTunnel.port')
      }
    },
    {
      type: 'input-number',
      field: 'maxRetries',
      name: t('seaTunnel.maxRetries'),
      props: {
        placeholder: t('seaTunnel.pleaseEnter') + t('seaTunnel.maxRetries')
      }
    },
    {
      type: 'input-number',
      field: 'timeoutMs',
      name: t('seaTunnel.timeoutMs'),
      props: {
        placeholder: t('seaTunnel.pleaseEnter') + t('seaTunnel.timeoutMs')
      }
    },
    {
      type: 'input-number',
      field: 'retryIntervalMs',
      name: t('seaTunnel.retryIntervalMs'),
      props: {
        placeholder: t('seaTunnel.pleaseEnter') + t('seaTunnel.retryIntervalMs')
      }
    },
    {
      type: 'switch',
      field: 'enableMessageReply',
      name: t('seaTunnel.enableMessageReply'),
    },
    {
      type: 'switch',
      field: 'asyncEnabled',
      name: t('seaTunnel.asyncEnabled'),
    },
    {
      type: 'input-number',
      field: 'asyncThreads',
      name: t('seaTunnel.asyncThreads'),
      span: asyncThreadsSpan,
      props: {
        placeholder: t('seaTunnel.pleaseEnter') + t('seaTunnel.asyncThreads')
      }
    }
  ]
}