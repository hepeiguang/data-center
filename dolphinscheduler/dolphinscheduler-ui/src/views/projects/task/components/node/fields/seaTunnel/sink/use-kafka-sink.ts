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
import { useKeyValues } from '../../index'
import { queryDataSourceListPaging } from '@/service/modules/data-source'
export function useKafkaSink(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  const options = ref([] as { label: string; value: number; connectionParams?: any }[])
  const is_partitionSpan = computed(() => (model.is_partition ? 24 : 0))
  const not_is_partitionSpan = computed(() => (model.is_partition ? 0 : 24))

  const getDataSourceList = async () => {
    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        if (item.type == 'KAFKA') {
          options.value.push({ label: item.name, value: item.id, connectionParams: JSON.parse(item.connectionParams) })
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
      type: 'input',
      field: 'topic',
      name: t('topic'),
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('topic') + ' ' + t('project.node.required'))
          }
        }
      },
      props: {
        placeholder: t('seaTunnel.topicPlaceholder')
      }
    },
    {
      type: 'select',
      field: 'semantics',
      name: t('seaTunnel.semantics'),
      options: SEMANTICS_MODES
    },
    {
      type: 'switch',
      field: 'is_partition',
      name: t('seaTunnel.is_partition'),
    },
    {
      type: 'input-number',
      field: 'partition',
      name: t('seaTunnel.partition'),
      span: is_partitionSpan,
      tooltip: t('seaTunnel.partitionTooltip')
    },
    {
      type: 'input',
      field: 'partition_key_fields',
      name: t('seaTunnel.partition_key_fields'),
      tooltip: t('seaTunnel.partition_key_fieldsTooltip'),
      span: not_is_partitionSpan,
      props: {
        placeholder: t('seaTunnel.partition_key_fieldsTooltip')
      }
    },
    {
      type: 'select',
      field: 'format',
      name: t('seaTunnel.format'),
      options: FOTMATS
    },
    {
      type: 'input',
      field: 'field_delimiter',
      name: t('seaTunnel.field_delimiter'),
      props: {
        placeholder: t('seaTunnel.field_delimiter_placeholder')
      }
    },
    {
      type: 'input-number',
      field: 'parallelism',
      name: t('seaTunnel.parallelism'),
      tooltip: t('')
    },
    ...useKeyValues({model, field: 'kafka.config',name: t('seaTunnel.config')} )
  ]
}


export const SEMANTICS_MODES = [
    {
      label: '每条消息将被精确处理一次',
      value: 'EXACTLY_ONCE'
    },
    {
      label: '每条消息至少会被处理一次',
      value: 'AT_LEAST_ONCE'
    },
    {
      label: '不保证消息传递的准确性',
      value: 'NON'
    }
  ]


export const FOTMATS = [
  {
    label: 'json',
    value: 'json'
  },
  {
    label: 'text',
    value: 'text'
  },
  {
    label: 'canal_json',
    value: 'canal_json'
  },
  {
    label: 'debezium_json',
    value: 'debezium_json'
  },
  {
    label: 'ogg_json',
    value: 'ogg_json'
  },
  {
    label: 'maxwell_json',
    value: 'maxwell_json'
  },
  {
    label: 'avro',
    value: 'avro'
  },
  {
    label: 'native',
    value: 'native'
  },
]

