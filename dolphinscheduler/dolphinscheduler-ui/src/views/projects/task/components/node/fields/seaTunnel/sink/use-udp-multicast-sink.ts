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
import { useKeyValues } from '@/views/projects/task/components/node/fields'
import { queryDataSourceListPaging } from '@/service/modules/data-source'
export function useUdpMulticastSink(model: {
  [field: string]: any
}): IJsonItem[] {
  const options = ref([] as { label: string; value: number; connectionParams?: any }[])

  const { t } = useI18n()
  computed(() => (model.format == 'json' ? 24 : 0))
  const asyncThreadsSpan = computed(() => (model.asyncEnabled ? 24 : 0))

  const getDataSourceList = async () => {
    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        if (item.type == 'UDP_MULTICAST') {
          try {
            // 解析连接参数，判断参数中UDP组播类型
            const params = JSON.parse(item.connectionParams)
            if (params.udpMulticastType === 'SINK') {
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
            return new Error(t('seaTunnel.datasourceId') + t('project.node.required'))
          }
        }
      },
      props: {
        filterable: true,
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
      field: 'retryIntervalMs',
      name: t('seaTunnel.retryIntervalMs'),
      props: {
        placeholder: t('seaTunnel.pleaseEnter') + t('seaTunnel.retryIntervalMs')
      }
    },
    {
      type: 'switch',
      field: 'asyncEnabled',
      name: t('seaTunnel.asyncEnabled')
    },
    {
      type: 'input-number',
      field: 'asyncThreads',
      name: t('seaTunnel.asyncThreads'),
      span: asyncThreadsSpan,
      props: {
        placeholder: t('seaTunnel.pleaseEnter') + t('seaTunnel.asyncThreads')
      }
    },
    ...useKeyValues({
      model,
      tooltip:
        '附加参数：\n' +
        'ttl: 多播数据包的生存时间(TTL)值含义(1=本地网络，32=站点内，255=无限制)\n' +
        'sendBufferSize：套接字发送缓冲区的大小（以字节为单位）\n' +
        'waitForReply：发送后是否等待回复\n' +
        'replyTimeoutMs：等待回复的超时时间（毫秒）\n' +
        'batchSize：发送前的消息批量处理数量\n' +
        'batchIntervalMs：批量发送的最大时间间隔（毫秒）\n' +
        'batchQueueSize：批量队列的最大消息数量 \n' +
        'asyncQueueSize: 异步发送队列的最大消息数量\n' +
        'asyncWaitNanos: 异步队列最大等待时间（毫秒）\n' +
        'charsetName: 字符编码\n' +
        'maxPacketSize: UDP数据包最大大小（字节）\n',
      field: 'otherConfig',
      name: 'seaTunnel.config'
    })
  ]
}
