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
import { computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'

export function useUdpSource(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  if (!model.parseMode) {
    model.parseMode = 'none'
  }

  const needSchema = computed(
    () =>
      model.format === 'json' ||
      model.parseMode === 'js' ||
      model.parseMode === 'offset_table'
  )
  const schemaSpan = computed(() => (needSchema.value ? 24 : 0))
  const replyMessageSpan = computed(() => (model.replyEnabled ? 24 : 0))
  const jsSpan = computed(() => (model.parseMode === 'js' ? 24 : 0))
  const offsetSpan = computed(() => (model.parseMode === 'offset_table' ? 24 : 0))
  const legacyFormatSpan = computed(() => (model.parseMode === 'none' ? 24 : 0))
  const udfSpan = computed(() =>
    model.parseMode === 'js' || model.parseMode === 'offset_table' ? 24 : 0
  )

  watch(
    () => model.schemaVo,
    (val) => {
      console.log('schemaVo', val)
    },
    { deep: true }
  )

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
      type: 'select',
      field: 'parseMode',
      name: t('seaTunnel.parseMode'),
      options: PARSE_MODES,
      props: {
        clearable: false
      }
    },
    {
      type: 'select',
      field: 'format',
      name: t('seaTunnel.format'),
      options: FOTMATS,
      span: legacyFormatSpan
    },
    {
      type: 'input',
      field: 'udfName',
      name: t('seaTunnel.udfName'),
      span: udfSpan,
      tooltip: t('seaTunnel.udfNameTooltip')
    },
    {
      type: 'input',
      field: 'scriptPath',
      name: t('seaTunnel.scriptPath'),
      span: jsSpan,
      tooltip: t('seaTunnel.scriptPathTooltip')
    },
    {
      type: 'mirror',
      field: 'scriptContent',
      name: t('seaTunnel.scriptContent'),
      span: jsSpan,
      props: {
        language: 'javascript'
      }
    },
    {
      type: 'input',
      field: 'offsetTablePath',
      name: t('seaTunnel.offsetTablePath'),
      span: offsetSpan
    },
    {
      type: 'mirror',
      field: 'offsetTable',
      name: t('seaTunnel.offsetTable'),
      span: offsetSpan,
      tooltip: t('seaTunnel.offsetTableTooltip'),
      props: {
        language: 'json'
      }
    },
    {
      type: 'mirror',
      field: 'schemaVo',
      name: 'schema',
      tooltip:
        '请输入标准json,例如：\n{\n    "code": "int",\n    "data": "string",\n    "success": "boolean"\n}',
      span: schemaSpan,
      validate: {
        trigger: ['input', 'trigger'],
        required: true,
        validator(validate: any, value: string) {
          if (needSchema.value && value) {
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
      field: 'bufferSize',
      name: t('seaTunnel.bufferSize')
    },
    {
      type: 'select',
      field: 'charset',
      name: t('seaTunnel.charset'),
      options: CHARSETS,
      span: legacyFormatSpan,
      props: {
        filterable: true,
        tag: true
      }
    },
    {
      type: 'switch',
      field: 'replyEnabled',
      name: t('seaTunnel.replyEnabled')
    },
    {
      type: 'input',
      field: 'replyMessage',
      name: t('seaTunnel.replyMessage'),
      span: replyMessageSpan,
      props: {
        placeholder: t('seaTunnel.pleaseEnter') + t('seaTunnel.replyMessage')
      }
    }
  ]
}

export const PARSE_MODES = [
  { label: 'none（text/json）', value: 'none' },
  { label: 'js（脚本解析）', value: 'js' },
  { label: 'offset_table（偏移表）', value: 'offset_table' }
]

export const FOTMATS = [
  { label: 'json', value: 'json' },
  { label: 'text', value: 'text' }
]

export const CHARSETS = [
  { label: 'UTF-8', value: 'UTF-8' },
  { label: 'UTF-16', value: 'UTF-16' },
  { label: 'ISO-8859-1', value: 'ISO-8859-1' },
  { label: 'GBK', value: 'GBK' },
  { label: 'GB2312', value: 'GB2312' },
  { label: 'ASCII', value: 'ASCII' }
]
