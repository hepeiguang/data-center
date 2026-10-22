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
import { computed, onMounted, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
import { useKeyValues } from '@/views/projects/task/components/node/fields'
import { queryDataSourceListPaging } from '@/service/modules/data-source'
import {
  CHARSETS,
  FOTMATS
} from '@/views/projects/task/components/node/fields/seaTunnel/source/use-udp-source'
export function useUdpMulticastSource(model: {
  [field: string]: any
}): IJsonItem[] {
  const options = ref(
    [] as { label: string; value: number; connectionParams?: any }[]
  )

  const schemaSpan = computed(() => (model.format == 'json' ? 24 : 0))
  const { t } = useI18n()
  computed(() => (model.format == 'json' ? 24 : 0))
  const replyMessageSpan = computed(() => (model.replyEnabled ? 24 : 0))

  const getDataSourceList = async () => {
    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        if (item.type == 'UDP_MULTICAST') {
          try {
            // 解析连接参数，判断参数中UDP组播类型,SOURCE
            const params = JSON.parse(item.connectionParams)
            if (params.udpMulticastType === 'SOURCE') {
              options.value.push({
                label: item.name,
                value: item.id,
                connectionParams: params
              })
            }
          } catch (e) {
            return
          }
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
      options: FOTMATS
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
      type: 'select',
      field: 'charsetName',
      name: t('seaTunnel.charset'),
      options: CHARSETS,
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
    },
    ...useKeyValues({
      model,
      tooltip:
        '附加参数：\n' +
        'ttl: 多播数据包的生存时间(TTL)值含义(1=本地网络，32=站点内，255=无限制)\n' +
        'maxPacketSize: UDP数据包最大大小（字节）\n',
      field: 'otherConfig',
      name: 'seaTunnel.config'
    })
  ]
}
