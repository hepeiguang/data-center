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

import { h, ref, reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import type { Router } from 'vue-router'
import type { TableColumns } from 'naive-ui/es/data-table/src/interface'
import { NSpace, NTooltip, NButton, NPopconfirm,NEllipsis} from 'naive-ui'
import { EditOutlined, DeleteOutlined } from '@vicons/antd'
import { useAsyncState } from '@vueuse/core'
import {
  queryUdfFuncListPaging,
  deleteUdfFunc
} from '@/service/modules/resources'
import {
  COLUMN_WIDTH_CONFIG,
  calculateTableWidth,
  DefaultTableWidth
} from '@/common/column-width-config'
import type { IUdfFunctionParam } from './types'

export function useTable() {
  const { t } = useI18n()
  const router: Router = useRouter()

  const variables = reactive({
    columns: [],
    tableWidth: DefaultTableWidth,
    row: {},
    tableData: [],
    // here is id not prefix because udf function is still stored in db
    id: ref(Number(router.currentRoute.value.params.id) || -1),
    fullName: ref(String(router.currentRoute.value.query.prefix || '')),
    page: ref(1),
    pageSize: ref(10),
    searchVal: ref(),
    totalPage: ref(1),
    showRef: ref(false),
    loadingRef: ref(false),

    //用于控制hive/seatunnel选项是否禁用
    mutiSelectIsDisabled:ref(false),
  })

  const createColumns = (variables: any) => {
    variables.columns = [
      {
        title: '#',
        key: 'id',
        render: (_row, index) => index + 1,
        ...COLUMN_WIDTH_CONFIG['index']
      },

      //增加名称类型列
      {
        title: t('resource.function.funcNameOrDisplayName'),
        key: 'funcName',
        ...COLUMN_WIDTH_CONFIG['name'],
      },
      //增加资源类型列
      {
        title: t('resource.file.type'),
        key: 'type',
        ...COLUMN_WIDTH_CONFIG['type'],
        render: (row: any) => {
          if (!row.directory) {
            if (row.type === 'HIVE') return t('resource.file.type_hive')
            if (row.type === 'SEATUNNEL')
              return t('resource.file.type_seatunnel')
            // 默认返回未知类型文案
            return t('resource.file.type_unknow')
          }
          return t('resource.file.type_file')
        }
      },

      {
        title: t('resource.function.udf_function_name'),
        key: 'funcName',
        ...COLUMN_WIDTH_CONFIG['name']
      },
      {
        title: t('resource.function.user_name'),
        ...COLUMN_WIDTH_CONFIG['userName'],
        key: 'userName'
      },
      {
        title: t('resource.function.class_name'),
        key: 'className',
        ...COLUMN_WIDTH_CONFIG['name']
      },
      // {
      //   title: t('resource.function.type'),
      //   key: 'type',
      //   ...COLUMN_WIDTH_CONFIG['type']
      // },
      {
        title: t('resource.function.description'),
        key: 'description',
        ...COLUMN_WIDTH_CONFIG['note']
      },
      {
        title: t('resource.function.jar_package'),
        key: 'resourceName',
        ...COLUMN_WIDTH_CONFIG['name']
      },
      {
        title: t('resource.function.update_time'),
        key: 'updateTime',
        ...COLUMN_WIDTH_CONFIG['time']
      },
      {
        title: t('resource.function.operation'),
        key: 'operation',
        ...COLUMN_WIDTH_CONFIG['operation'](2),
        render: (row) => {
          return h(NSpace, null, {
            default: () => [
              h(
                NTooltip,
                {},
                {
                  trigger: () =>
                    h(
                      NButton,
                      {
                        circle: true,
                        type: 'info',
                        size: 'tiny',
                        class: 'btn-edit',
                        onClick: () => {
                          handleEdit(row)
                        }
                      },
                      {
                        icon: () => h(EditOutlined)
                      }
                    ),
                  default: () => t('resource.function.edit')
                }
              ),
              h(
                NPopconfirm,
                {
                  onPositiveClick: () => {
                    handleDelete(row.id, row.fullName)
                  }
                },
                {
                  trigger: () =>
                    h(
                      NTooltip,
                      {},
                      {
                        trigger: () =>
                          h(
                            NButton,
                            {
                              circle: true,
                              type: 'error',
                              size: 'tiny',
                              class: 'btn-delete'
                            },
                            {
                              icon: () => h(DeleteOutlined)
                            }
                          ),
                        default: () => t('resource.function.delete')
                      }
                    ),
                  default: () => t('resource.function.delete_confirm')
                }
              )
            ]
          })
        }
      }
    ] as TableColumns<any>
    if (variables.tableWidth) {
      variables.tableWidth = calculateTableWidth(variables.columns)
    }
  }

  //获得数据
  const getTableData = (params: IUdfFunctionParam) => {
    if (variables.loadingRef) return
    variables.loadingRef = true
    //useAsyncState？
    //相比直接使用 await，它的优势在于：自动管理加载/错误状态提供响应式状态对象可与其他 Composition API 更好配合
    const { state } = useAsyncState(
      queryUdfFuncListPaging({ ...params }).then((res: any) => {
        variables.totalPage = res.totalPage
        variables.tableData = res.totalList.map((item: any) => {
          return { ...item }
        })
        variables.loadingRef = false
      }),
      { total: 0, table: [] }
    )
    return state
  }

  //回显
  const handleEdit = (row: any) => {
    variables.showRef = true
    variables.row = row
    //修改禁用状态
    variables.mutiSelectIsDisabled = true
  }

  const handleDelete = (id: number, fullName: string) => {
    /* after deleting data from the current page, you need to jump forward when the page is empty. */
    if (variables.tableData.length === 1 && variables.page > 1) {
      variables.page -= 1
    }

    deleteUdfFunc(id, { fullName: fullName }).then(() =>
      getTableData({
        fullName: variables.fullName,
        id: variables.id,
        pageSize: variables.pageSize,
        pageNo: variables.page,
        searchVal: variables.searchVal
      })
    )
  }

  return {
    variables,
    createColumns,
    getTableData
  }
}
