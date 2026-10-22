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
import { queryDataSourceListPaging } from '@/service/modules/data-source'
export function useHdfsfileSink(model: { [field: string]: any }): IJsonItem[] {
  const options = ref(
    [] as { label: string; value: number; connectionParams?: any }[]
  )
  const { t } = useI18n()
  const textSpan = computed(() => (model.file_format_type == 'text' ? 24 : 0))
  const textOrJsonSpan = computed(() => (model.file_format_type == 'text' || model.file_format_type == 'csv' || model.file_format_type == 'json' ? 24 : 0))
  const excelSpan = computed(() => (model.file_format_type == 'excel' ? 24 : 0))
  const advancedSpan = computed(() => (model.advanced == true ? 24 : 0))
  const advancedTextOrJsonSpan = computed(() => (model.advanced == true && (model.file_format_type == 'text' || model.file_format_type == 'json') ? 24 : 0))
  const customSpan = computed(() => (model.custom_filename == true ? 24 : 0))
  const partitionSpan = computed(() => (model.have_partition == true ? 24 : 0))

  const getDataSourceList = async () => {
    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        if (item.type == 'HDFS_FILE') {
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
      type: 'input',
      field: 'path',
      name: t('seaTunnel.file_path'),
      props: {
        placeholder: t('/tmp/seatunnel')
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(
              t('seaTunnel.file_path') + ' ' + t('project.node.required')
            )
          }
        }
      }
    },
    {
      type: 'select',
      field: 'file_format_type',
      name: t('seaTunnel.file_format_type'),
      options: FILE_FORMAT_TYPE,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(
              t('seaTunnel.file_format_type') + ' ' + t('project.node.required')
            )
          }
        }
      }
    },
    {
      type: 'input-number',
      field: 'batch_size',
      name: t('seaTunnel.batch_size'),
      props: {
        placeholder: t('1000000')
      }
    },
    {
      type: 'switch',
      field: 'create_empty_file_when_no_data',
      name: t('seaTunnel.create_empty_file_when_no_data')
    },
    {
      type: 'input',
      field: 'field_delimiter',
      name: t('seaTunnel.field_delimiter'),
      props: {
        placeholder: t('\\001')
      },
      span: textSpan
    },
    {
      type: 'input',
      field: 'row_delimiter',
      name: t('seaTunnel.row_delimiter'),
      props: {
        placeholder: t('\\n')
      },
      span: textOrJsonSpan
    },
    {
      type: 'switch',
      field: 'enable_header_write',
      name: t('seaTunnel.enable_header_write'),
      span: textSpan
    },
    {
      type: 'input',
      field: 'sheet_name',
      name: t('seaTunnel.sheet_name'),
      props: {
        placeholder: t('Sheet${随机数}')
      },
      span: excelSpan
    },
    {
      type: 'input-number',
      field: 'max_rows_in_memory',
      name: t('seaTunnel.max_rows_in_memory'),
      props: {
        placeholder: t('1000')
      },
      span: excelSpan
    },
    {
      type: 'switch',
      field: 'advanced',
      name: t('seaTunnel.advanced')
    },
    {
      type: 'input',
      field: 'tmp_path',
      name: t('seaTunnel.tmp_path'),
      tooltip: '结果文件将首先写入临时路径，然后使用mv将临时目录提交到目标目录',
      props: {
        placeholder: t('/tmp/seatunnel')
      },
      span: advancedSpan
    },
    {
      type: 'switch',
      field: 'custom_filename',
      name: t('seaTunnel.custom_filename'),
      span: advancedSpan
    },
    {
      type: 'input',
      field: 'file_name_expression',
      name: t('seaTunnel.file_name_expression'),
      props: {
        placeholder: t('${transactionId}')
      },
      span: customSpan
    },
    {
      type: 'input',
      field: 'filename_time_format',
      name: t('seaTunnel.filename_time_format'),
      props: {
        placeholder: t('yyyy.MM.dd')
      },
      span: customSpan
    },
    {
      type: 'switch',
      field: 'have_partition',
      name: t('seaTunnel.have_partition'),
      span: advancedSpan
    },
    {
      type: 'input',
      field: 'partition_dir_expression',
      name: t('seaTunnel.partition_dir_expression'),
      tooltip: '请输入标准json array,例如：["value1","value2"]',
      span: partitionSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
          if (value) {
            try {
              if (!Array.isArray(JSON.parse(value))) {
                return new Error('输入的内容不是合法的JSONArray格式')
              }
            } catch (error) {
              return new Error('输入的内容不是合法的JSONArray格式')
            }
          }
        }
      }
    },
    {
      type: 'switch',
      field: 'is_partition_field_write_in_file',
      name: t('seaTunnel.is_partition_field_write_in_file'),
      span: partitionSpan
    },
    {
      type: 'input',
      field: 'sinkColumnsVo',
      name: t('seaTunnel.sinkColumnsVo'),
      tooltip: '请输入标准json array,例如：["value1","value2"]',
      span: advancedSpan,
      validate: {
        trigger: ['input', 'trigger'],
        validator(validate: any, value: string) {
          if (value) {
            try {
              if (!Array.isArray(JSON.parse(value))) {
                return new Error('输入的内容不是合法的JSONArray格式')
              }
            } catch (error) {
              return new Error('输入的内容不是合法的JSONArray格式')
            }
          }
        }
      }
    },
    {
      type: 'input',
      field: 'compress_codec',
      name: t('seaTunnel.compress_codec'),
      props: {
        placeholder: t('lzo/none')
      },
      span: advancedSpan
    },
    {
      type: 'input',
      field: 'encoding',
      name: t('seaTunnel.encoding'),
      props: {
        placeholder: t('UTF-8')
      },
      span: advancedTextOrJsonSpan
    }
  ]
}

export const FILE_FORMAT_TYPE = [
  {
    label: 'text',
    value: 'text'
  },
  {
    label: 'csv',
    value: 'csv'
  },
  {
    label: 'parquet',
    value: 'parquet'
  },
  {
    label: 'orc',
    value: 'orc'
  },
  {
    label: 'json',
    value: 'json'
  },
  {
    label: 'excel',
    value: 'excel'
  }
]