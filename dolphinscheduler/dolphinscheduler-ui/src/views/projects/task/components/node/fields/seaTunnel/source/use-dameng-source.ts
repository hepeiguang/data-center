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
import type { IJsonItem } from '../../../types'
import {
  queryDataSourceListPaging,
  getDatasourceTablesById,
  getDatasourceTableColumnDetailsById
} from '@/service/modules/data-source'
import { placeholder } from '@codemirror/view'
export function useDamengSource(model: { [field: string]: any }): IJsonItem[] {
    const { t } = useI18n()
    const partitionSpan = computed(() => (model.is_partition ? 24 : 0))
    const partitionSpanf = computed(() => (model.is_partition ? 10 : 0))
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
          pageSize: 100,
        })
        if (listRes.totalList && listRes.totalList.length) {
          listRes.totalList.forEach((item: any) => {
            if (item.type == 'KAFKA') {
              return
            }
            if(item.type == 'DAMENG'){
              options.value.push({
                label: item.name,
                value: item.id,
                connectionParams: JSON.parse(item.connectionParams)
              })
            }
            
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
        const datasourceChange = () => {
          model.table_path = ''
          model.selectedColumns = []
        }
    
      const columnOptions = ref([] as { label: string; value: string }[])
      // 修改getColumnList函数
      const getColumnList = async (params: any) => {
        if(params.tableName.indexOf('.') > -1){
          params.tableName = params.tableName.split('.')[1]
        }
        const listRes = await getDatasourceTableColumnDetailsById(params)
        if (listRes && listRes.length) {
          const temp = listRes.map((item: any) => item.column_name)
          // model.outputFields = temp
          // 更新columnOptions
          columnOptions.value = temp.map((col) => ({ label: col, value: col }))
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
            return new Error(t('请选择数据源'))
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
      field: 'query',
      name: t('查询语句'),
      props: {
        type: 'textarea',
        placeholder: t('示例：SELECT * FROM 表名'),
        maxLength: 256,
        onBlur: handleMouseLeave
      },
      span: sqlSpan
    },
        {
      type: 'input',
      field: 'whereCondition',
      name: t('筛选条件'),
      props: {
        placeholder: t(
          '所有表/查询的常见行筛选条件必须以where开头,例如where id > 100'
        )
      },
      span: notCustomSpan
    },
    {
      type:'custom-parameters',
      field: 'properties',
      name: t('自定义参数'),
      tooltip: t('额外的连接配置参数，当 properties 和 URL 具有相同的参数时，优先级由驱动的具体实现决定'),
      class: 'btn-custom-parameters',
      children: [
        {
          type: 'input',
          field: 'properties_key',
          props: { placeholder: 'key'},
          validate: {
              trigger: ['input', 'blur'],
              required: true,
              validator(validate: any, value: string) {
                if (!value) {
                  return new Error(t('该字段为必填'))
                }
              }
            },
          span: 11
        },
        {
          type: 'input',
          field: 'properties_value',
          props: { placeholder: 'value' },
          validate: {
              trigger: ['input', 'blur'],
              required: true,
              validator(validate: any, value: string) {
                if (!value) {
                  return new Error(t('该字段为必填'))
                }
              }
            },
          span: 11
        },
      ]
    },
    {
      type: 'input-number',
      field: 'partition_num',
      name: t('并行数'),
      tooltip: t('指任务并行处理的能力，即同时处理多个任务的数量')
      ,
      props: { min: 1, max: 1000, step: 1 ,
        placeholder: 10
      }
    },
    {
      type:'switch',
      field: 'is_partition',
      name: t('高级配置'),
    },
    {
      type: 'input',
      field: 'partition_column',
      tooltip: t('用于并行度分区的列名，仅支持数字类型，仅支持数字类型的主键，并且只能配置一列'),
      name: t('分区字段'),
      props: { placeholder: 'id' },
      span: partitionSpanf,
    },    
     {
      type: 'input-number',
      field: 'partition_lower_bound',
      span: partitionSpan,
      name: t('分区字段最小值'),
      props: { min: 0,  step: 0.01 },
      validate: {
        validator(_, value:  number) {
          // if (!value && value !=0) {
          //   return new Error(t('该字段为必填'))
          // }
          if(model.partition_upper_bound && value > model.partition_upper_bound)
          return new Error(t('分区字段最小值不能大于分区字段最大值'))
        }
      }
    },
    {
      type: 'input-number',
      field: 'partition_upper_bound',
      name: t('分区字段最大值'),
      span: partitionSpan,
      props: { min: 0,  step: 0.01 },
      validate: {
        validator(_, value: number) {
          // if (!value && value !=0) {
          //   return new Error(t('该字段为必填'))
          // }
          if(model.partition_lower_bound && value < model.partition_lower_bound)
          return new Error(t('分区字段最大值不能小于分区字段最小值'))
        }
      }
    },
     {
      type: 'input-number',
      field: 'connection_check_timeout_sec',
      name: t('连接超时时间(秒)'),
      span: partitionSpan,
      props: { min: 0, placeholder: '0' }
    },
    {
      type: 'input-number',
      field: 'fetch_size',
      name: t('查询行数'),
      span: partitionSpan,
      tooltip: t('设置为零时表示使用JDBC默认值'),
      props: { min: 0, placeholder: '0' }
    },
    {
      type: 'input-number',
      field: 'split.size',
      name: t('拆分数量'),
      span: partitionSpan,
      tooltip: t('读取表时，捕获的表的拆分大小（行数）将被拆分为多个拆分。'),
      props: { min: 1, placeholder: '8096' }
    },
    {
      type: 'input-number',
      field: 'split.even-distribution.factor.lower-bound',
      name: t('拆分数量下限'),
      tooltip: t('如果计算出分布因子大于或等于此下限(即 (MAX(id) - MIN(id) + 1) / 行计数)，则表块将进行优化以实现均匀分布。否则，如果分布因子较小，则认为表分布不均匀，如果估计的分片计数超过 指定的值，则使用基于采样的分片策略。'),
      span: partitionSpan,
      props: { min: 0, step: 0.01,placeholder: '0.05' }
    },
    {
      type: 'input-number',
      field: 'split.even-distribution.factor.upper-bound',
      name: t('拆分数量上限'),
      tooltip: t('果计算出分布因子小于或等于此上限(即 (MAX(id) - MIN(id) + 1) / 行计数)，则表块将进行优化以实现均匀分布。否则，如果分布因子较大，则认为表分布不均匀，如果估计的分片计数超过 指定的值，则使用基于采样的分片策略。'),
      span: partitionSpan,
      value: 100,
      props: { min: 0, step: 0.01,placeholder: '100' }
    },
    {
      type: 'input-number',
      field: 'split.sample-sharding.threshold',
      name: t(' 随机采样分片阈值'),
      tooltip: t('随机采样分片阈值，当分片数量小于此阈值时，使用随机采样分片策略。'),
      span: partitionSpan,
      value: 10000,
      props: { min: 1, placeholder: '10000' }
    },
    {
        type:'input-number',
        field: 'split.inverse-sampling.rate',
        name: t('逆采样分片阈值'),
        tooltip: t('样本分片策略中使用的采样率的倒数。例如，如果此值设置为 1000，则表示在采样过程中应用 1/1000 采样率。此选项可以灵活地控制采样的粒度，从而影响分片的最终数量。'),
        span: partitionSpan,
        value: 1000,
        props: { min: 1, placeholder: '1000' }
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
  ]
}