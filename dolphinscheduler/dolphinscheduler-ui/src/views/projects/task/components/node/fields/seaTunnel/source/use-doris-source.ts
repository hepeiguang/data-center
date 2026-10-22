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

export function useDorisSource(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  const options = ref(
    [] as { label: string; value: number; connectionParams?: any }[]
  )
  const tableOptions = ref([] as { label: string; value: string }[])
  const sqlSpan = computed(() => (model.is_custom_sql ? 12 : 0))
  let stopWatch: (() => void) | null = null

  const getDataSourceList = async () => {
    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        if (item.type == 'DORIS') {
          options.value.push({
            label: item.name,
            value: item.id,
            connectionParams: JSON.parse(item.connectionParams)
          })
        }
      })
      watchEffect(
        () => {
          console.log("watchEffect>>>", model.table_path)
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
        // if(params.tableName.indexOf('.') > -1){
        //   params.tableName = params.tableName.split('.')[1]
        // }
    const listRes = await getDatasourceTableColumnDetailsById(params)
    if (listRes && listRes.length) {
      const temp = listRes.map((item: any) => item.column_name)
      // model.outputFields = temp
      // 更新columnOptions
      columnOptions.value = temp.map((col) => ({ label: col, value: col }))
      // if (model.is_custom_sql) {
      //   model.table_path = model.database + '.' + listRes[0].table_name
      // }
      // 初始化选中字段（可选）
      if (model.dorisReadField.length === 0) {
        model.dorisReadField = [...temp]
      }
    }
  }

  const onChange = () => {
    model.dorisReadField = []
  }

  const datasourceChange = () => {
    model.table_path = ''
    model.dorisReadField = []
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
        tag: true
      },
      span: 24
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
            return new Error(t('请选择表名称'),)
          }
        }
      },
      props: {
        'on-update:value': onChange,
        filterable: true,
        tag: true
      },
      span: 24
    },
    {
      type: 'select',
      field: 'dorisReadField',
      name: t('表字段'),
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
                      model.dorisReadField = []
                    },
                    disabled: model.dorisReadField?.length === 0
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
      span: 24
    },
    // {
    //   type: 'select',
    //   field: 'dorisReadField',
    //   name: t('表字段'),
    //   options: columnOptions,
    //   props: {
    //     filterable: true,
    //     tag: true
    //   },
    //   validate: {
    //     trigger: ['input', 'blur'],
    //     required: true,
    //     validator(validate: any, value: string) {
    //       if (!value) {
    //         return new Error(t('请选择表字段'),)
    //       }
    //     }
    //   },
    //   span: 24
    // },
    {
      type: 'input',
      field: 'dorisFilterQuery',
      name: t('数据过滤'),
      props: {
        placeholder: t('格式："字段 = 值", 例如：doris.filter.query = "F_ID > 2"'),
      },
      span: 24,
    },
    {
      type: 'input',
      field: 'feHttpPort',
      name: t('FE节点的HTTP服务端口'),
      value: 8030,
      span: 24,
    },
    {
      type: 'switch',
      field: 'is_custom_sql',
      name: t('高级功能字段'),
    },
    {
      type: 'input-number',
      field: 'dorisBatchSize',
      name: t('每次能够从BE中读取到的最大行数'),
      value: 1024,
      span: sqlSpan,
      slots: {
        suffix: () => t('行')
      }
    },
    {
      type: 'input-number',
      field: 'dorisExecMemLimit',
      name: t('单个be扫描请求可以使用的最大内存'),
      value: 2147483648,
      span: sqlSpan,
      props: {
        placeholder: t('默认内存为2G'),
      },
      slots: {
        suffix: () => t('Byte')
      }
    },
    {
      type: 'input-number',
      field: 'dorisRequestRetries',
      name: t('请求Doris FE的重试次数'),
      value: 3,
      span: sqlSpan,
      slots: {
        suffix: () => t('次')
      }
    },
    {
      type: 'input-number',
      field: 'dorisRequestReadTimeoutMs',
      name: t('请求Doris 读取超时时间'),
      value: 30000,
      span: sqlSpan,
      slots: {
        suffix: () => t('秒')
      }
    },
    {
      type: 'input-number',
      field: 'dorisRequestConnectTimeoutMs',
      name: t('请求Doris 连接超时时间'),
      value: 30000,
      span: sqlSpan,
      slots: {
        suffix: () => t('秒')
      }
    },
    {
      type: 'input-number',
      field: 'dorisRequestQueryTimeouts',
      name: t('Doris扫描数据的超时时间'),
      value: 3600,
      span: sqlSpan,
      slots: {
        suffix: () => t('秒')
      }
    },
    // ...useCustomParams({ model, field: 'localParams', isSimple: false })
  ]
}
