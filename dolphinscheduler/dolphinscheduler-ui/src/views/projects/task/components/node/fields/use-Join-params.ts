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
import { Ref, computed, ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../types'
import {
  getDatasourceTableColumnDetailsById,
  getDatasourceTablesById,
  queryDataSource
} from '@/service/modules/data-source'

export function useJoinParams({
  model,
  Fields,
  span = 0
}: {
  model: { [field: string]: any }
  Fields: any
  span?: Ref | number
}): IJsonItem[] {
  const { t } = useI18n()
  const options = ref({})
  const optionsSelect = ref([])
  const outOptions = ref(
    [] as {
      selected: boolean
      table_name: string
      column_name: string
      new_name: string
    }[]
  )
  const left_table = ref([] as { label: string; value: string }[])
  const left_column = ref([] as { label: string; value: string }[])
  const right_table = ref([] as { label: string; value: string }[])
  const right_column = ref([] as { label: string; value: string }[])

  const typeList_com = computed(() => {
    return [
      {
        value: 'LEFT',
        label: '左连接'
      },
      {
        value: 'RIGHT',
        label: '右连接'
      },
      {
        value: 'INNER',
        label: '内连接'
      },
      {
        value: 'FULL',
        label: '全连接',
        disabled: !['POSTGRESQL', 'SQLSERVER', 'ORACLE', 'DB2', 'OPENGAUSS' ].includes(options?.value?.type)
      }
    ]
  })

  const getDataSourceList = async () => {
    if (!model.datasourceId) {
      return
    }
    const listRes = await queryDataSource(model.datasourceId)
    console.log('listRes', listRes)
    if (listRes) {
      options.value = listRes
      optionsSelect.value.push({
        label: listRes.name,
        value: listRes.id
      })
    }
    //页面渲染时判断表一和表二是否已经选择表然后更新关联关系选项
    if (model.left_table.length > 0 && model.left_table) {
      const data = {
        database: options.value.database,
        datasourceId: options.value.id,
        tableName: model.left_table
      }
      getColumnList(data, 'left_column')
    }
    if (model.right_table.length > 0 && model.right_table) {
      const data = {
        database: options.value.database,
        datasourceId: options.value.id,
        tableName: model.right_table
      }
      getColumnList(data, 'right_column', false)
    }
    //算出表一选项内容
    Fields.value.forEach((item: any) => {
      left_table.value.push({
        label: item.tableName.slice(item.tableName.indexOf('.') + 1) as string,
        value: item.tableName as string
      })
    })
    //算出表二选项内容
    getTableList(options.value.id, options.value.database)

    //监听关联表一确定关联关系表1选项
    watch(
      () => model.left_table,
      (newValue) => {
        //切换表时清空关联关系
        model.relation = []
        // 防御性检查：确保值有效
        if (typeof newValue !== 'string' || newValue.trim() === '') {
          left_column.value = [] // 清空字段选项
          return
        }
        model.relation.push({})
        // 清空旧数据（关键修复点）
        left_column.value = []
        if (newValue.length > 0 && newValue) {
          const data = {
            database: options.value.database,
            datasourceId: options.value.id,
            tableName: newValue
          }
          getColumnList(data, 'left_column')
        }
      }
    )
    //监听关联表2确定关联关系表2选项并且增加展示的输出字段
    watch(
      () => model.right_table,
      (newValue) => {
        console.log('right_table 变化:', newValue)
        //切换表时清空关联关系
        model.relation = []
        // 防御性检查：确保值有效
        if (typeof newValue !== 'string' || newValue.trim() === '') {
          right_column.value = [] // 清空字段选项
          console.log(newValue)
          model.select_fields = []
          outOptions.value = []
          return
        }
        setTimeout(() => {
          model.relation.push({})
        }, 500)
        model.select_fields = []
        outOptions.value = []
        // 清空旧数据（关键修复点）
        right_column.value = []
        if (newValue.length > 0 && newValue) {
          const data = {
            database: options.value.database,
            datasourceId: options.value.id,
            tableName: newValue
          }
          getColumnList(data, 'right_column', true)
        }
      }
    )
  }
  onMounted(() => {
    getDataSourceList()
  })
  //根据数据源查表
  const getTableList = async (datasourceId: number, database: string) => {
    const listRes = await getDatasourceTablesById(datasourceId, database)
    if (listRes && listRes.length) {
      right_table.value = []
      listRes.forEach((item: any) => {
        right_table.value.push({
          label: item.label,
          value: database + '.' + item.value
        })
      })
      // 2. 将 oldTable 转换为 JSON 字符串集合（用于深度比对）
      const oldEntries = new Set(
        left_table.value.map((item) => JSON.stringify(item))
      )
      // 3. 过滤 newTable，排除与 oldTable 完全相同的对象
      right_table.value = right_table.value.filter(
        (newItem) => !oldEntries.has(JSON.stringify(newItem))
      )
    }
  }
  //根据数据源和表名查字段
  const getColumnList = async (params: any, name: string, state?: boolean) => {
    const listRes = await getDatasourceTableColumnDetailsById(params)
    if (listRes && listRes.length) {
      if (name === 'right_column') {
        listRes.forEach((item: any) => {
          right_column.value.push({
            label: item.column_name,
            value: item.column_name
          })
        })
        if (state) {
          listRes.forEach((item: any) => {
            outOptions.value.push({
              selected: true,
              table_name: params.tableName,
              column_name: item.column_name,
              new_name: ''
            })
            model.select_fields.push({
              selected: true,
              table_name: params.tableName,
              column_name: item.column_name,
              new_name: ''
            })
          })
          console.log(outOptions.value, 'outOptions.value')
          console.log(model.select_fields, 'model.select_fields')
        } else {
          outOptions.value = []
          listRes.forEach((item: any) => {
            let searchResults = false
            model.select_fields.forEach((element: any) => {
              if (element.column_name === item.column_name) {
                searchResults = true
                outOptions.value.push({
                  selected: element.selected,
                  table_name: element.table_name,
                  column_name: element.column_name,
                  new_name: element.new_name
                })
              }
            })
            if (!searchResults) {
              outOptions.value.push({
                selected: false,
                table_name: params.tableName,
                column_name: item.column_name,
                new_name: ''
              })
            }
          })
        }
      }
      if (name === 'left_column') {
        listRes.forEach((item: any) => {
          left_column.value.push({
            label: item.column_name,
            value: item.column_name
          })
        })
      }
    }
  }
  return [
    {
      type: 'select',
      field: 'datasourceId',
      name: t('数据源'),
      span,
      options: optionsSelect,
      props: {
        filterable: true,
        disabled: true // 添加禁用属性
      }
    },
    // 表一选择
    {
      type: 'select',
      field: 'left_table',
      name: t('关联表一'),
      options: left_table,
      span,
      props: {
        clearable: true,
        filterable: true,
        tag: true
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('project.node.left_table_required_tips'))
          }
        }
      }
      // validate: { required: true }
    },
    // 表二选择
    {
      type: 'select',
      field: 'right_table',
      name: t('关联表二'),
      options: right_table,
      span,
      props: {
        clearable: true,
        filterable: true,
        tag: true
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('project.node.right_table_required_tips'))
          }
        }
      }
      // validate: { required: true }
    },
    {
      type: 'custom-parameters',
      field: 'relation',
      name: '关联关系',
      class: 'btn-custom-parameters',
      span,
      children: [
        {
          type: 'select',
          field: 'left_column',
          options: left_column,
          span: 10,
          props: {
            clearable: true,
            placeholder: t('表一字段(必填)')
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.left_column_required_tips'))
              }
            }
          }
        },
        {
          type: 'select',
          field: 'right_column',
          options: right_column,
          span: 10,
          props: {
            clearable: true,
            placeholder: t('表二字段(必填)')
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('project.node.right_column_required_tips'))
              }
            }
          }
        }
      ]
    },
    {
      type: 'select',
      field: 'join_type',
      name: t('关联方式'),
      options: typeList_com,
      span,
      props: { clearable: true },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('project.node.join_type_required_tips'))
          }
        }
      }
      // validate: { required: true }
    },
    {
      type: 'output-field',
      field: 'select_fields',
      name: t('输出字段'),
      span,
      options: outOptions,
      props: {
        clearable: true,
      },
      // validate: { required: true }
    }
  ]
}

