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
import { useCustomParams } from '.'
import type { IJsonItem } from '../types'

export function useWebhook(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  return [
    {
      type: 'input',
      class: 'input-url-name',
      field: 'url',
      name: t('project.node.http_url'),
      props: {
        placeholder: t('project.node.http_url_tips')
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('project.node.http_url_tips'))
          }
          if (value.search(new RegExp(/http[s]{0,1}:\/\/\S*/, 'i'))) {
            return new Error(t('project.node.http_url_validator'))
          }
        }
      }
    },
    {
      type: 'select',
      field: 'webHookMethod',
      span: 12,
      name: t('project.node.http_method'),
      options: HTTP_METHODS
    },
    {
      type: 'input',
      field: 'response',
      name: t('响应内容'),
      props: {
        type: 'textarea',
        placeholder: t('请填写响应内容')
      }
    },
    ...useCustomParams({
      model,
      field: 'localParams',
      isSimple: true
    })
  ]
}

const HTTP_METHODS = [
  {
    value: 'GET',
    label: 'GET'
  },
  {
    value: 'POST',
    label: 'POST'
  },
  {
    value: 'HEAD',
    label: 'HEAD'
  },
  {
    value: 'PUT',
    label: 'PUT'
  },
  {
    value: 'DELETE',
    label: 'DELETE'
  }
]

