import { ref, onMounted, onUnmounted, computed, watch, watchEffect, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
import { queryDataSourceListPaging, getDatasourceItemsById } from '@/service/modules/data-source'
export function useMinioFileSink(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  const options = ref([] as { label: string; value: number; connectionParams?: any }[])
  const itemOptions = ref([] as { label: string; value: string }[])
  let stopWatch: (() => void) | null = null

  const advancedSpan = computed(() => (model.advanced == true ? 24 : 0))
  const excelSpan = computed(() => (model.advanced == true && model.file_format_type == 'excel' ? 24 : 0))
  const textSpan = computed(() => (model.advanced == true && model.file_format_type == 'text' ? 24 : 0))
  const rowSpan = computed(() => (model.advanced == true && (model.file_format_type == 'text' || model.file_format_type == 'csv' || model.file_format_type == 'json') ? 24 : 0))
  const headerSpan = computed(() => (model.advanced == true && (model.file_format_type == 'text' || model.file_format_type == 'csv') ? 24 : 0))
  const csvSpan = computed(() => (model.advanced == true && model.file_format_type == 'csv' ? 24 : 0))
  const xmlSpan = computed(() => (model.advanced == true && model.file_format_type == 'xml' ? 24 : 0))
  const encodingSpan = computed(() => (model.advanced == true && (model.file_format_type == 'text' || model.file_format_type == 'csv' || model.file_format_type == 'json' || model.file_format_type == 'xml') ? 24 : 0))
  const customSpan = computed(() => (model.custom_filename == true ? 24 : 0))
  const partitionSpan = computed(() => (model.have_partition == true ? 24 : 0))
  const singleSpan = computed(() => (model.single_file_mode == false ? 24 : 0))

  const getDataSourceList = async () => {
    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        if (item.type != 'MINIO') {
          return
        }
        options.value.push({ label: item.name, value: item.id, connectionParams: JSON.parse(item.connectionParams) })
      })
      watchEffect(() => {
        if (model.datasourceId) {
          getItemList(model.datasourceId)
        }
      }, {
        flush: 'post'
      })
    }
  }
  const getItemList = async (datasourceId: number) => {
    const listRes = await getDatasourceItemsById(datasourceId)
    if (listRes && listRes.length) {
      itemOptions.value = []
      listRes.forEach((item: any) => {
        itemOptions.value.push({ label: item.label, value: item.value })
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
      type: 'select',
      field: 'bucket',
      name: t('seaTunnel.bucket'),
      options: itemOptions,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('seaTunnel.bucket') + t('project.node.required'))
          }
        }
      },
      props: {
        filterable: true,
        tag: true,
      }
    },
    {
      type: 'input',
      field: 'path',
      name: t('seaTunnel.file_path'),
      props: {
        placeholder: t('/2025'),
        maxLength: 256
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
            return new Error(t('seaTunnel.file_path') + t('project.node.required'))
          }
        }
      }
    },
    {
      type: 'switch',
      field: 'have_partition',
      name: t('seaTunnel.have_partition')
    },
    {
      type: 'input',
      field: 'partitionByVo',
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
      type: 'switch',
      field: 'single_file_mode',
      name: t('seaTunnel.single_file_mode'),
      tooltip: '当此参数开启时，batch_size将不会生效，输出文件名不会有文件块后缀',
    },
    {
      type: 'input-number',
      field: 'batch_size',
      name: t('seaTunnel.batch_size'),
      props: {
        placeholder: t('1000000')
      },
      span: singleSpan
    },
    {
      type: 'switch',
      field: 'advanced',
      name: t('seaTunnel.advanced')
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
      field: 'tmp_path',
      name: t('seaTunnel.tmp_path'),
      tooltip: '结果文件将首先写入临时路径，然后使用mv将临时目录提交到目标目录',
      props: {
        placeholder: t('/tmp/seatunnel')
      },
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
      field: 'row_delimiter',
      name: t('seaTunnel.row_delimiter'),
      props: {
        placeholder: t('\\n')
      },
      span: rowSpan
    },
    {
      type: 'switch',
      field: 'enable_header_write',
      name: t('seaTunnel.enable_header_write'),
      span: headerSpan
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
      type: 'select',
      field: 'csv_string_quote_mode',
      name: t('seaTunnel.csv_string_quote_mode'),
      options: CSV_STRING_QUOTE_MODE,
      span: csvSpan
    },
    {
      type: 'input',
      field: 'xml_root_tag',
      name: t('seaTunnel.xml_root_tag'),
      props: {
        placeholder: t('RECORDS')
      },
      span: xmlSpan
    },
    {
      type: 'input',
      field: 'xml_row_tag',
      name: t('seaTunnel.xml_row_tag'),
      props: {
        placeholder: t('RECORD')
      },
      span: xmlSpan
    },
    {
      type: 'switch',
      field: 'xml_use_attr_format',
      name: t('seaTunnel.xml_use_attr_format'),
      span: xmlSpan
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
      type: 'switch',
      field: 'create_empty_file_when_no_data',
      name: t('seaTunnel.create_empty_file_when_no_data'),
      span: advancedSpan
    },
    {
      type: 'select',
      field: 'schema_save_mode',
      name: t('seaTunnel.schema_save_mode'),
      options: SCHEMA_SAVE_MODE,
      span: advancedSpan
    },
    {
      type: 'select',
      field: 'data_save_mode',
      name: t('seaTunnel.data_save_mode'),
      options: DATA_SAVE_MODE,
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
export const CSV_STRING_QUOTE_MODE = [
  {
    label: 'ALL',
    value: 'ALL'
  },
  {
    label: 'MINIMAL',
    value: 'MINIMAL'
  },
  {
    label: 'NONE',
    value: 'NONE'
  }
]
export const SCHEMA_SAVE_MODE = [
  {
    label: 'RECREATE_SCHEMA',
    value: 'RECREATE_SCHEMA'
  },
  {
    label: 'CREATE_SCHEMA_WHEN_NOT_EXIST',
    value: 'CREATE_SCHEMA_WHEN_NOT_EXIST'
  },
  {
    label: 'ERROR_WHEN_SCHEMA_NOT_EXIST',
    value: 'ERROR_WHEN_SCHEMA_NOT_EXIST'
  },
  {
    label: 'IGNORE',
    value: 'IGNORE'
  }
]
export const DATA_SAVE_MODE = [
  {
    label: 'DROP_DATA',
    value: 'DROP_DATA'
  },
  {
    label: 'APPEND_DATA',
    value: 'APPEND_DATA'
  },
  {
    label: 'ERROR_WHEN_DATA_EXISTS',
    value: 'ERROR_WHEN_DATA_EXISTS'
  }
]