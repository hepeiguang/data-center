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
import { queryDataSourceListPaging, getDatasourceTablesById } from '@/service/modules/data-source'
export function useKafkaSource(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  const options = ref([] as { label: string; value: number; connectionParams?: any }[])
  const debezium_record_table_filterSpan = computed(() => (model.format == 'debezium_json' ? 24 : 0))

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
      type: 'switch',
      field: 'pattern',
      name: t('seaTunnel.pattern'),
    },
    {
      type: 'input',
      field: 'topic',
      name: t('topic'),
      tooltip: t('seaTunnel.topicPlaceholder'),
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
      type: 'input',
      field: 'consumer.group',
      name: t('seaTunnel.consumer_group'),
    },
    {
      type: 'select',
      field: 'start_mode',
      name: t('seaTunnel.start_mode'),
      options: START_MODES
    },
    {
      type: 'select',
      field: 'format',
      name: t('seaTunnel.format'),
      options: FOTMATS
    },
    // ...useKeyValues({model, field: 'debezium_record_table_filter',name: t('seaTunnel.debezium_record_table_filter'), span: debezium_record_table_filterSpan} ),
    // ...useKeyValues({model, field: 'schema',name: 'schema'} ),
    {
      type: 'mirror',
      field: 'debezium_record_table_filter',
      name: t('seaTunnel.debezium_record_table_filter'),
      tooltip: '请输入标准json,例如：{ "key": "value" }',
      span: debezium_record_table_filterSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
          if (value) {
            try {
              const parsedData = JSON.parse(value);
            } catch (error) {
              return new Error('输入的内容不是合法的 JSON 格式')
            }
          }
        }
      },
      props: {
        language: 'sql',
      }
    },
    {
      type: 'mirror',
      field: 'schemaVo',
      name: 'schema',
      tooltip: '请输入标准json,例如：{ "key": "value" }',
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
          if (value) {
            try {
              const parsedData = JSON.parse(value);
            } catch (error) {
              return new Error('输入的内容不是合法的 JSON 格式')
            }
          }
        }
      },
      props: {
        language: 'sql',
      }
    },
    {
      type: 'select',
      field: 'format_error_handle_way',
      name: t('seaTunnel.format_error_handle_way'),
      options: FOTMAT_ERR_HANDLE_WAYS
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
      tooltip: t('seaTunnel.parallelismTooltip')
    },
    ...useKeyValues({model, field: 'kafka.config',name: 'seaTunnel.config'} )
  ]
}


export const START_MODES = [
    {
      label: '最早的消息开始消费',
      value: 'earliest'
    },
    {
      label: '消费者组偏移量开始消费',
      value: 'group_offsets'
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

export const FOTMAT_ERR_HANDLE_WAYS = [
    {
      label: '阻塞并抛出异常',
      value: 'fail'
    },
    {
      label: '跳过此行数据',
      value: 'skip'
    }
  ]
