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
import { ref, onMounted, onUnmounted, computed, watch, watchEffect, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import type { IJsonItem } from '../../../types'

import { useRoute } from 'vue-router'
import { queryDataSourceListPaging, getDatasourceTableColumnDetailsById } from '@/service/modules/data-source'
import { useDagStore } from '@/store/project/dynamic/dag'

export function useSqlcasewhenTransform(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  const route = useRoute()
  const dagStore = useDagStore()

  const options = ref([] as { label: string; value: number; connectionParams?: any }[])
  
  const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))

  const contrastStr = dagStore.getContrastStr
  let stopWatch: (() => void) | null = null

  const columnNameOptions = ref([] as { label: string; value: number; fieldType: string; }[])
  const compareOptions = [
    { value: '=', label: '=' },
    { value: '!=', label: '!=' },
    { value: '>', label: '>' },
    { value: '>=', label: '>=' },
    { value: '<', label: '<' },
    { value: '<=', label: '<=' }
  ]

  const fieldArr = ref([] as any)
  const getDataSourceList = async () => {
    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        options.value.push({ label: item.name, value: item.id, connectionParams: JSON.parse(item.connectionParams) })
      })
      stopWatch = watch(model.include_fields, (newValue, oldValue) => {
        if (!model.preTasks[0]) {
          return
        }
        if (model.include_fields) {
          model.include_fields.forEach((item: {
            retain: any, field: any
          }) => {
            if (item.retain && item.retain.length > 0 && !!item.retain[0]) {
              fieldArr.value.push(item.field)
            }
          })
          const taskDefinitionList = dagStore.getDagTaskDefinitionList
          const pretask = taskDefinitionList.find((task) => {
            return task.code == model.preTasks[0]
          })
          const queryParames = {
            datasourceId: '',
            database: '',
            tableName: ''
          }
          if (pretask.taskParams) {
            pretask.params = pretask.taskParams
          }
          if (pretask.params.datasourceId && pretask.params.database && pretask.params.table) {
            queryParames.datasourceId = pretask.params.datasourceId
            queryParames.database = pretask.params.database
            queryParames.tableName = pretask.params.table
          } else if (pretask.params.datasourceId && pretask.params.table_path) {
            queryParames.datasourceId = pretask.params.datasourceId
            queryParames.database = pretask.params.table_path.split('.')[0]
            queryParames.tableName = pretask.params.table_path.split('.')[1]
          }
          if (!(contrastStr && JSON.stringify(queryParames) === contrastStr)) {
            dagStore.setContrastStr(JSON.stringify(queryParames))
            model.include_fields = []
            fieldArr.value = []
          }
          if (pretask && pretask.params && pretask.params.outputFields && pretask.params.outputFields.length) {
            pretask.params.outputFields[0].fields.forEach((item: any) => {
                              columnNameOptions.value.push({ label: item, value: item,fieldType: item })
                          })
            if (!queryParames.datasourceId || !queryParames.database || !queryParames.tableName) {
              queryParames.datasourceId = pretask.params.outputFields[0].datasourceId
              if (pretask.params.outputFields[0].tableName) {
                queryParames.database = pretask.params.outputFields[0].tableName.split('.')[0]
                queryParames.tableName = pretask.params.outputFields[0].tableName.split('.')[1]
              }
            }
          }
          getColumnList(Number(queryParames.datasourceId), queryParames.database, queryParames.tableName)
        }

      }, { deep: true, immediate: true })
    }
  }

  const getColumnList = async (datasourceId: number, database: string, tableName: string) => {
    const listRes = await getDatasourceTableColumnDetailsById({datasourceId, database, tableName})
    if (listRes && listRes.length) {
      listRes.forEach((item: any) => {
        for (let i = 0; i < model.include_fields.length; i++) {
          if (item.column_name === model.include_fields[i].field) {
            model.include_fields[i].retain = fieldArr.value.includes(item.column_name) ? [true] : null
            return;
          }
        }
        console.log(222)

        model.include_fields.push({ field: item.column_name, fieldType: item.type_name, fieldDes: item.remarks, retain: fieldArr.value.includes(item.column_name) ? [true] : null, hiddenDel: true })
        // columnNameOptions.value.push({ label: item.column_name, value: item.column_name, fieldType: item.type_name })
      })
      if (stopWatch) stopWatch()
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
      field: 'columnName',
      span: fieldsSpan,
      name: t(`字段名`),
      options: columnNameOptions,
      props: {
        placeholder: t('原数据值'),
        'on-update:value': (value: string, item: object) => {
          model.fieldType = item.fieldType
        }
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('该字段为必填项'))
          }
        }
      }
    },
    //////////////////////////////////////
    {
      type: 'custom-parameters',
      field: 'columns',
      name: t(`数据映射设置`),
      span: fieldsSpan,
      children: [
        (j = 0) => ({
          type: 'select',
          field: 'expression',
          span: 6,
          options: compareOptions,
          value: "=",
          props: {
            placeholder: t('请选择'),
            'on-update:value': (value: string) => {
              // const item = model.columns[j].expression
              // console.log('item', item)
            }
          }
        }),
        (j = 0) => ({
          type: 'input',
          field: 'srcValue',
          span: computed(() => {
            const item = model.columns[j].expression
            return (item=='=') || (item=='!=') ? 8 : 0
          }),
          props: {
            placeholder: t('原数据值'),
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填项'))
              }
            }
          }
        }),
        (j = 0) => ({
          type: 'input-number',
          field: 'srcValue',
          span: computed(() => {
            const item = model.columns[j].expression
            return (item=='=') || (item=='!=') ? 0 : 8
          }),
          props: {
            placeholder: t('原数据值'),
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填项'))
              }
            }
          }
        }),
        (j = 0) => ({
          type: 'input',
          field: 'desValue',
          span: 8,
          props: {
            placeholder: t('映射值'),
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填项'))
              }
            }
          }
        }),
      ]
    },
    //////////////////////////////////////
    {
      type: 'input',
      field: 'defaultValue',
      span: fieldsSpan,
      name: t(`默认映射`),
      props: {
        placeholder: t('默认映射')
      },
    },
    {
      type: 'input',
      field: 'targetField',
      span: fieldsSpan,
      name: t(`映射字段`),
      props: {
        placeholder: t('映射字段')
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('该字段为必填项'))
          }
        }
      }
    },
  ]
}
