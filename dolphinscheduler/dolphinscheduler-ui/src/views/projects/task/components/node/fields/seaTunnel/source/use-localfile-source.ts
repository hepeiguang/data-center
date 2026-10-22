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
import { ref, defineComponent, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
export function useLocalFileSource(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  const schemaSpan = computed(() => (model.file_format_type == 'text' || model.file_format_type == 'json' || model.file_format_type == 'excel' || model.file_format_type == 'xml' || model.file_format_type == 'csv' ? 24 : 0))
  const textSpan = computed(() => (model.file_format_type == 'text' ? 24 : 0))
  const excelSpan = computed(() => (model.file_format_type == 'excel' ? 24 : 0))
  const xmlSpan = computed(() => (model.file_format_type == 'xml' ? 24 : 0))
  const csvSpan = computed(() => (model.file_format_type == 'csv' ? 24 : 0))
  const skipSpan = computed(() => (model.file_format_type == 'text' || model.file_format_type == 'csv' || model.file_format_type == 'excel') ? 24 : 0)

  const advancedSpan = computed(() => (model.advanced == true ? 24 : 0))
  const encodingSpan = computed(() => (model.advanced == true && (model.file_format_type == 'text' || model.file_format_type == 'json' || model.file_format_type == 'csv' || model.file_format_type == 'xml') ? 24 : 0))
  const nullFormatSpan = computed(() => (model.advanced == true && model.file_format_type == 'text' ? 24 : 0))


  return [
    {
      type: 'input',
      field: 'path',
      name: t('seaTunnel.file_path'),
      tooltip: '支持文件路径和目录路径，当目录路径时对当前目录及子目录中的文件进行处理',
      props: {
        placeholder: t('/tmp/hive/warehouse/test2')
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('seaTunnel.file_path') + t('project.node.required'))
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
            return new Error(t('seaTunnel.file_format_type') + t('project.node.required'))
          }
        }
      }
    },
    {
      type: 'mirror',
      field: 'schemaVo',
      name: t('seaTunnel.schemaVo'),
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
      type: 'switch',
      field: 'parse_partition_from_path',
      name: t('seaTunnel.parse_partition_from_path')
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
      field: 'skip_header_row_number',
      name: t('seaTunnel.skip_header_row_number'),
      props: {
        placeholder: t('0')
      },
      span: skipSpan
    },
    {
      type: 'input',
      field: 'sheet_name',
      name: t('seaTunnel.sheet_name'),
      props: {},
      span: excelSpan
    },
    {
      type: 'select',
      field: 'excel_engine',
      name: t('seaTunnel.excel_engine'),
      options: EXCEL_ENGINE_TYPE,
      span: excelSpan
    },
    {
      type: 'input',
      field: 'xml_row_tag',
      name: t('seaTunnel.xml_row_tag'),
      span: xmlSpan
    },
    {
      type: 'switch',
      field: 'xml_use_attr_format',
      name: t('seaTunnel.xml_use_attr_format'),
      span: xmlSpan
    },
    {
      type: 'switch',
      field: 'csv_use_header_line',
      name: t('seaTunnel.csv_use_header_line'),
      tooltip: t('文件包含与RFC 4180匹配的标题行时使用'),
      span: csvSpan
    },
    {
      type: 'switch',
      field: 'advanced',
      name: t('seaTunnel.advanced')
    },
    {
      type: 'input',
      field: 'file_filter_pattern',
      name: t('seaTunnel.file_filter_pattern'),
      tooltip: t('用于过滤文件名称，例如以T开头的文件：T*.*，该模式遵循标准正则表达式'),
      props: {},
      span: advancedSpan
    },
    {
      type: 'input',
      field: 'filename_extension',
      name: t('seaTunnel.filename_extension'),
      tooltip: t('用于过滤具有特定扩展名的文件'),
      props: {},
      span: advancedSpan
    },
    {
      type: 'select',
      field: 'date_format',
      name: t('seaTunnel.date_format'),
      options: DATE_FORMAT_TYPE,
      span: advancedSpan
    },
    {
      type: 'select',
      field: 'time_format',
      name: t('seaTunnel.time_format'),
      options: TIME_FORMAT_TYPE,
      span: advancedSpan
    },
    {
      type: 'select',
      field: 'datetime_format',
      name: t('seaTunnel.datetime_format'),
      options: DATETIME_FORMAT_TYPE,
      span: advancedSpan
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
      field: 'archive_compress_codec',
      name: t('seaTunnel.archive_compress_codec'),
      props: {
        placeholder: t('ZIP/TAR/TAR_GZ/GZ/NONE')
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
      span: encodingSpan
    },
    {
      type: 'input',
      field: 'null_format',
      name: t('seaTunnel.null_format'),
      props: {
        placeholder: t('\\n')
      },
      span: nullFormatSpan
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
  },
  {
    label: 'xml',
    value: 'xml'
  },
  {
    label: 'binary',
    value: 'binary'
  }
]
export const EXCEL_ENGINE_TYPE = [
  {
    label: 'POI',
    value: 'POI'
  },
  {
    label: 'EasyExcel',
    value: 'EasyExcel'
  }
]
export const DATE_FORMAT_TYPE = [
  {
    label: 'yyyy-MM-dd',
    value: 'yyyy-MM-dd'
  },
  {
    label: 'yyyy.MM.dd',
    value: 'yyyy.MM.dd'
  },
  {
    label: 'yyyy/MM/dd',
    value: 'yyyy/MM/dd'
  }
]
export const TIME_FORMAT_TYPE = [
  {
    label: 'HH:mm:ss',
    value: 'HH:mm:ss'
  },
  {
    label: 'HH:mm:ss.SSS',
    value: 'HH:mm:ss.SSS'
  }
]
export const DATETIME_FORMAT_TYPE = [
  {
    label: 'yyyy-MM-dd HH:mm:ss',
    value: 'yyyy-MM-dd HH:mm:ss'
  },
  {
    label: 'yyyy.MM.dd HH:mm:ss',
    value: 'yyyy.MM.dd HH:mm:ss'
  },
  {
    label: 'yyyy/MM/dd HH:mm:ss',
    value: 'yyyy/MM/dd HH:mm:ss'
  },
  {
    label: 'yyyyMMddHHmmss',
    value: 'yyyyMMddHHmmss'
  }
]