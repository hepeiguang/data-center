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
import { ref, onMounted, watchEffect, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
import { queryDataSourceListPaging, getDatasourceTableColumnDetailsById, getDatasourceTablesById } from '@/service/modules/data-source'
export function useJdbcSink(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  const options = ref([] as { label: string; value: number; connectionParams?: any }[])
  const tableOptions = ref([] as { label: string; value: string }[])
  const sqlSpan = computed(() => (model.is_custom_sql ? 24 : 0))
  const notCustomSpan = computed(() => (model.is_custom_sql ? 0 : 24))
  const customSQLSpan = computed(() => (model.data_save_mode === 'CUSTOM_PROCESSING' ? 24 : 0))

  const getDataSourceList = async () => {
    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        if (item.type == 'KAFKA') {
          return
        }
        options.value.push({ label: item.name, value: item.id, connectionParams: JSON.parse(item.connectionParams) })
      })
      watchEffect(() => {
        if (model.datasourceId) {
          const activeSource = options.value.find(source => source.value === model.datasourceId)
          const database = activeSource?.connectionParams.database
          model.database = database
          database && getTableList(model.datasourceId, database)
          if (!model.is_custom_sql && model.datasourceId && model.database && model.table) {
            getColumnList({ datasourceId: Number(model.datasourceId), database: model.database, tableName: model.table })
          }
        }
      },
        {
          flush: 'post'
        })
    }
  }
  const getTableList = async (datasourceId: number, database: string) => {
    tableOptions.value = []
    const listRes = await getDatasourceTablesById(datasourceId, database)
    if (listRes && listRes.length) {
      listRes.forEach((item: any) => {
        tableOptions.value.push({ label: item.label, value: item.value })
      })
    }
  }

  const getColumnList = async (params: any) => {
        if(params.tableName.indexOf('.') > -1){
          params.tableName = params.tableName.split('.')[1]
        }
    const listRes = await getDatasourceTableColumnDetailsById(params)
    if (listRes && listRes.length) {
      // let temp: any[] = []
      // listRes.forEach((item: any) => {
      //   temp.push(item.column_name)
      // })
      // model.outputFields = temp
    }
  }

  const handleMouseLeave = () => {
    if (model.is_custom_sql && model.query) {
      getColumnList({ datasourceId: Number(model.datasourceId), database: model.database, sql: model.query })
    }
  }

  onMounted(() => {
    getDataSourceList()
  })

  return [
    {
      type: 'select',
      field: 'datasourceId',
      name: t('数据源'),
      options: options,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('project.node.value_required_tips'))
          }
        }
      },
      props: {
        filterable: true,
      }
    },
    {
      type: 'switch',
      field: 'is_custom_sql',
      name: t('是否自定义sql'),
    },
    {
      type: 'input',
      field: 'query',
      name: t('sql'),
      props: {
        type: 'textarea',
        placeholder: t('示例：INSERT INTO 表名 (字段1, 字段2, 字段3) VALUES (值1, 值2, 值3)'),
        maxLength: 256,
        onBlur: handleMouseLeave
      },
      span: sqlSpan
      // validate: {
      //     trigger: ['input', 'blur'],
      //     validator(validate: any, value: string) {
      //         if (!value) {
      //             return new Error(t('project.node.value_required_tips'))
      //         }
      //     }
      // }
    },
    {
      type: 'select',
      field: 'table',
      name: t('表名称'),
      options: tableOptions,
      tooltip: t('表如果已存在，请确认表字段和输出字段保持一致'),
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请选择表名称'),)
          }
        }
      },
      props: {
        filterable: true,
        tag: true
      },
      span: notCustomSpan
    },
    {
      type: 'select',
      field: 'outputFields',
      name: '',
      span: 0
    },
    // {
    //   type: 'select',
    //   field: 'primary_keys',
    //   name: t('主键'),
    //   options: PRIMARY_MODE,
    //   props: {
    //     multiple: true,
    //     filterable: true
    //   },
    //   validate: {
    //     trigger: ['input', 'blur'],
    //     required: true,
    //     validator(validate: any, value: string) {
    //       if (!value) {
    //         return new Error(t('project.node.value_required_tips'))
    //       }
    //     }
    //   }
    // },
    {
      type: 'select',
      field: 'field_ide',
      name: t('字段转换'),
      // props: {
      //     multiple: true,
      //     filterable: true
      // },
      options: FIELD_IDE_MODE
    },
    // {
    //     type: 'input',
    //     field: 'plugin_input',
    //     name: t('对应数据集'),
    //     props: {
    //       placeholder: t('请输入对应数据集 '),
    //     },
    // },
    // {
    //   type: 'select',
    //   field: '',
    //   name: t(''),
    //   options: [
    //     {label: '', value: ''},
    //   ]
    // },
    // {
    //   type: 'select',
    //   field: '',
    //   name: t(''),
    //   options: [
    //     {label: '', value: ''},
    //   ]
    // },
    // {
    //   type: 'select',
    //   field: '',
    //   name: t(''),
    //   options: [
    //     {label: '', value: ''},
    //   ]
    // },
    {
      type: 'select',
      field: 'schema_save_mode',
      name: t('表处理方案'),
      options: SCHEMA_SAVE_MODE_OPTIONS
    },
    {
      type: 'select',
      field: 'data_save_mode',
      name: t('数据处理方案'),
      options: DATA_SAVE_MODE_OPTIONS
    },
    {
      type: 'input',
      field: 'custom_sql',
      name: t('自定义sql'),
      props: {
        type: 'textarea',
        placeholder: t('示例：DELETE FROM 表名'),
        maxLength: 256
      },
      validate: {
        trigger: ['input', 'blur'],
        validator(validate: any, value: string) {
          if (model.data_save_mode === 'CUSTOM_PROCESSING' && !value) {
            return new Error(t('project.node.value_required_tips'))
          }
        }
      },
      span: customSQLSpan
    },
    {
      type: 'switch',
      field: 'enable_upsert',
      name: t('启用主键更新插入')
    },
    {
      type: 'input-number',
      field: 'parallelism',
      name: t('并行数'),
      tooltip: t('指任务并行处理的能力，即同时处理多个任务的数量')
    },
    // {
    //   type: 'switch',
    //   field: 'use_copy_statement',
    //   name: t('使用 COPY语句导入')
    // },

    // ...useCustomParams({ model, field: 'localParams', isSimple: false })
  ]
}

export const PRIMARY_MODE = [
  {
    label: 'insert',
    value: 'insert'
  },
  {
    label: 'delete',
    value: 'delete'
  },
  {
    label: 'update',
    value: 'update'
  }
]

export const FIELD_IDE_MODE = [
  {
    label: '不转换',
    value: 'ORIGINAL'
  },
  {
    label: '转大写字母',
    value: 'UPPERCASE'
  },
  {
    label: '转小写字母',
    value: 'LOWERCASE'
  }
]

export const SCHEMA_SAVE_MODE_OPTIONS = [
  {
    // label: '当表不存在时会创建，当表已存在时会删除并重建',
    label: '表存在时删除并重建',
    value: 'RECREATE_SCHEMA'
  },
  {
    label: '当表不存在时会创建，当表已存在时则跳过创建',
    value: 'CREATE_SCHEMA_WHEN_NOT_EXIST'
  },
  {
    label: '当表不存在时将抛出错误',
    value: 'ERROR_WHEN_SCHEMA_NOT_EXIST'
  },
  {
    label: '忽略对表的处理',
    value: 'IGNORE'
  }
]

export const DATA_SAVE_MODE_OPTIONS = [
  {
    label: '保留数据库结构，删除数据',
    value: 'DROP_DATA'
  },
  {
    label: '保留数据库结构，保留数据',
    value: 'APPEND_DATA'
  },
  {
    label: '允许用户自定义数据处理方式',
    value: 'CUSTOM_PROCESSING'
  },
  {
    label: '当有数据时抛出错误',
    value: 'ERROR_WHEN_DATA_EXISTS'
  }
]