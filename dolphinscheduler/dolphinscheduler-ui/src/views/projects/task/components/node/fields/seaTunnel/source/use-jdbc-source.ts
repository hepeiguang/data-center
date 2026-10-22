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
import {
  ref,
  onMounted,
  onUnmounted,
  computed,
  watch,
  watchEffect,
  nextTick, h
} from 'vue'
import { useI18n } from 'vue-i18n'
import { useCustomParams, useResources } from '.'
import type { IJsonItem } from '../../../types'
import {
  queryDataSourceListPaging,
  getDatasourceTablesById,
  getDatasourceTableColumnDetailsById
} from '@/service/modules/data-source'

export function useJdbcSource(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  const options = ref(
    [] as { label: string; value: number; connectionParams?: any }[]
  )
  const tableOptions = ref([] as { label: string; value: string }[])
  const sqlSpan = computed(() => (model.is_custom_sql ? 24 : 0))
  const notCustomSpan = computed(() => (model.is_custom_sql ? 0 : 24))
  let stopWatch: (() => void) | null = null

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
        options.value.push({
          label: item.name,
          value: item.id,
          connectionParams: JSON.parse(item.connectionParams)
        })
      })
      // stopWatch = watch(model, (newValue, oldValue) => {
      //   console.log('model: ', model);
      //   if (newValue.datasourceId) {
      //     const activeSource = options.value.find(source => source.value === newValue.datasourceId)
      //     const database = activeSource?.connectionParams.database
      //     model.database = database
      //     database && getTableList(newValue.datasourceId, database)
      //     debugger
      //     if (!model.is_custom_sql && model.datasourceId && model.database && model.table_path) {
      //       getColumnList({ datasourceId: Number(model.datasourceId), database: model.database, tableName: model.table_path })
      //     }
      //   }
      // }, { deep: true, immediate: true },
      // {
      //   flush: 'post'
      // })
      watchEffect(
        () => {
          if (model.datasourceId) {
            const activeSource = options.value.find(
              (source) => source.value === model.datasourceId
            )
            const database = activeSource?.connectionParams.database
            model.database = database
            database && getTableList(model.datasourceId, database)
            if (
              !model.is_custom_sql &&
              model.datasourceId &&
              model.database &&
              model.table_path
            ) {
              getColumnList({
                datasourceId: Number(model.datasourceId),
                database: model.database,
                tableName: model.table_path
              })
            }
          }
        },
        {
          flush: 'post'
        }
      )
    }
  }
  const getTableList = async (datasourceId: number, database: string) => {
    const listRes = await getDatasourceTablesById(datasourceId, database)
    if (listRes && listRes.length) {
      tableOptions.value = []
      listRes.forEach((item: any) => {
        tableOptions.value.push({
          label: item.label,
          value: database + '.' + item.value
        })
      })
    }
  }

  const columnOptions = ref([] as { label: string; value: string }[])
  // 修改getColumnList函数
  const getColumnList = async (params: any) => {
    if(!model.is_custom_sql && params.tableName.indexOf('.') > -1){
      params.tableName = params.tableName.split('.')[1]
    }
    const listRes = await getDatasourceTableColumnDetailsById(params)
    if (listRes && listRes.length) {
      const temp = listRes.map((item: any) => item.column_name)
      // model.outputFields = temp
      // 更新columnOptions
      columnOptions.value = temp.map((col) => ({ label: col, value: col }))
      if (model.is_custom_sql) {
        model.table_path = model.database + '.' + listRes[0].table_name
      }
      // 初始化选中字段（可选）
      if (model.selectedColumns.length === 0) {
        model.selectedColumns = [...temp]
      }
    }
  }

  const handleMouseLeave = () => {
    if (model.is_custom_sql && model.query) {
      model.selectedColumns = []
      getColumnList({
        datasourceId: Number(model.datasourceId),
        database: model.database,
        sql: model.query
      })
    }
  }
  const onChange = () => {
    if (!model.is_custom_sql) {
      model.selectedColumns = []
    }
  }

  const datasourceChange = () => {
    model.table_path = ''
    model.selectedColumns = []
  }
  onMounted(() => {
    getDataSourceList()
  })

  onUnmounted(() => {
    if (stopWatch) stopWatch()
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
        'on-update:value': datasourceChange,
        filterable: true,
      }
    },
    {
      type: 'switch',
      field: 'is_custom_sql',
      name: t('是否自定义sql')
    },
    {
      type: 'input',
      field: 'query',
      name: t('查询语句'),
      props: {
        type: 'textarea',
        placeholder: t('示例：SELECT * FROM 表名'),
        maxLength: 256,
        onBlur: handleMouseLeave
      },
      span: sqlSpan,
      validate: {
          trigger: ['input', 'blur'],
          required: true,
          validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请输入sql语句'))
          }
        }
      }
    },
    {
      type: 'select',
      field: 'table_path',
      name: t('表名称'),
      options: tableOptions,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请选择表名称'))
          }
        }
      },
      props: {
        'on-update:value': onChange,
        filterable: true,
        tag: true
      },
      span: notCustomSpan
    },
    {
      type: 'input',
      field: 'where_condition',
      name: t('筛选条件'),
      props: {
        placeholder: t(
          '所有表/查询的常见行筛选条件必须以where开头,例如where id > 100'
        )
      },
      span: notCustomSpan
    },
    {
      type: 'input-number',
      field: 'parallelism',
      name: t('并行数'),
      tooltip: t('指任务并行处理的能力，即同时处理多个任务的数量')
    },
    {
      type: 'select',
      field: 'selectedColumns',
      name: t('输出字段'),
      options: columnOptions,
      props: {
        multiple: true,
        filterable: true,
        placeholder: t('请选择需要输出的字段'),
        clearable: true, // 启用原生清除按钮
        // 添加自定义底部插槽
        vSlots: {
          footer: () => [
            h(
              'div',
              {
                class: 'select-footer',
                style: {
                  padding: '6px',
                  borderTop: '1px solid #ebeef5',
                  textAlign: 'right'
                }
              },
              [
                h(
                  'el-button',
                  {
                    size: 'mini',
                    type: 'text',
                    onClick: () => {
                      model.selectedColumns = []
                    },
                    disabled: model.selectedColumns?.length === 0
                  },
                  t('一键清空')
                )
              ]
            )
          ]
        }
      },
      validate: {
        trigger: ['change'],
        validator: (validate: any, value: string[]) => {
          if (!value || value.length === 0) {
            return new Error(t('至少选择一个字段'))
          }
        }
      },
      span: computed(() =>
        columnOptions.value.length > 0 && !model.is_custom_sql ? 24 : 0
      )
    }
    // {
    //     type: 'input',
    //     field: 'plugin_output',
    //     name: t('插件输出'),
    //     props: {
    //       placeholder: t('请输入输出'),
    //     },
    // },
    // ...useCustomParams({ model, field: 'localParams', isSimple: false })
  ]
}
