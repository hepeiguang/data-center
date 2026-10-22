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
import { computed, onMounted, ref } from "vue";
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
import { queryDataSourceListPaging } from "@/service/modules/data-source";

export function useHttpServerSource(model: {
  [field: string]: any
}): IJsonItem[] {
  const { t } = useI18n()
  const options = ref(
    [] as { label: string; value: number; connectionParams?: any }[]
  )

  const schemaSpan = computed(() => (model.format == 'json' ? 24 : 0))
  const authSpan = computed(() => (model.authType != 'none' ? 24 : 0))

  computed(() => (model.replyEnabled ? 24 : 0))

  const getDataSourceList = async () => {
    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        if (item.type == 'HTTP_SERVER') {
          options.value.push({
            label: item.name,
            value: item.id,
            connectionParams: JSON.parse(item.connectionParams)
          })
        }
      })
    }
  }

  onMounted(() => {
    getDataSourceList()
  })
  return [
    {
      type: 'select',
      field: 'datasourceId',
      name: t('seaTunnel.datasourceId'),
      options: options,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(
              t('seaTunnel.datasourceId') + t('project.node.required')
            )
          }
        }
      },
      props: {
        filterable: true,
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
    }
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
