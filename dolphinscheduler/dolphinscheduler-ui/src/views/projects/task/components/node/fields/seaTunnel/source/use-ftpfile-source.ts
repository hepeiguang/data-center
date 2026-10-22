import { ref, onMounted, onUnmounted, computed, watch, watchEffect, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
import { queryDataSourceListPaging } from '@/service/modules/data-source'
export function useFtpFileSource(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  const options = ref([] as { label: string; value: number; connectionParams?: any }[])
  let stopWatch: (() => void) | null = null

  const schemaSpan = computed(() => (model.file_format_type == 'text' || model.file_format_type == 'json' || model.file_format_type == 'excel' || model.file_format_type == 'xml' || model.file_format_type == 'csv' ? 24 : 0))
  const advancedSpan = computed(() => (model.advanced == true ? 24 : 0))
  const textSpan = computed(() => (model.advanced == true && model.file_format_type == 'text' ? 24 : 0))
  const excelSpan = computed(() => (model.advanced == true && model.file_format_type == 'excel' ? 24 : 0))
  const xmlSpan = computed(() => (model.advanced == true && model.file_format_type == 'xml' ? 24 : 0))
  const csvSpan = computed(() => (model.advanced == true && model.file_format_type == 'csv' ? 24 : 0))
  const skipSpan = computed(() => (model.advanced == true && (model.file_format_type == 'text' || model.file_format_type == 'csv' || model.file_format_type == 'excel') ? 24 : 0))
  const encodingSpan = computed(() => (model.advanced == true && (model.file_format_type == 'text' || model.file_format_type == 'json' || model.file_format_type == 'csv' || model.file_format_type == 'xml') ? 24 : 0))
  const nullFormatSpan = computed(() => (model.advanced == true && model.file_format_type == 'text' ? 24 : 0))

  const getDataSourceList = async () => {
    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        if (item.type != 'FTP') {
          return
        }
        options.value.push({ label: item.name, value: item.id, connectionParams: JSON.parse(item.connectionParams) })
      })
    }
  }

  onMounted(() => {
    getDataSourceList()
  })

  onUnmounted(() => {
    if (stopWatch)
      stopWatch()
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
      field: 'path',
      name: t('seaTunnel.file_path'),
      props: {
        placeholder: t('/data/seatunnel')
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
            return new Error(t('文件类型') + ' ' + t('project.node.required'))
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
      type: 'select',
      field: 'connection_mode',
      name: t('seaTunnel.connection_mode'),
      options: CONNECTION_MODE_TYPE
    },
    {
      type: 'switch',
      field: 'parse_partition_from_path',
      name: t('seaTunnel.parse_partition_from_path')
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
      tooltip: t('用于过滤文件，该模式遵循标准正则表达式'),
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
export const CONNECTION_MODE_TYPE = [
  {
    label: '主动模式',
    value: 'active_local'
  },
  {
    label: '被动模式',
    value: 'passive_local'
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