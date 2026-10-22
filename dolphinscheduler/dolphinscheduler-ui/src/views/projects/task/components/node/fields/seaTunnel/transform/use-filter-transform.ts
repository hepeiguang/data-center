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
import { ref, onMounted, onUnmounted, computed, watch, watchEffect } from 'vue'
import { useI18n } from 'vue-i18n'
import { useFilterParams } from '../..'
import type { IJsonItem } from '../../../types'
import { useRoute } from 'vue-router'
import { queryDataSourceListPaging, getDatasourceTableColumnDetailsById } from '@/service/modules/data-source'
import { useDagStore } from '@/store/project/dynamic/dag'
export function useFilterTransform(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  const route = useRoute()
  const dagStore = useDagStore()

  const options = ref([] as { label: string; value: number; connectionParams?: any }[])
  const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))
  const contrastStr = dagStore.getContrastStr
  let stopWatch: (() => void) | null = null

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
      stopWatch = watch(model.include_fields_base, (newValue, oldValue) => {
        console.log(newValue)
        if (!model.preTasks[0]) {
          return
        }
        if (model.include_fields_base) {
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
          // if (!(contrastStr && JSON.stringify(queryParames) === contrastStr)) {
          //   dagStore.setContrastStr(JSON.stringify(queryParames))
          //   model.include_fields_base = []
          //   fieldArr.value = []
          // }
          if ((!model.include_fields_base || model.include_fields_base.length == 0) && pretask && pretask.params && pretask.params.outputFields && pretask.params.outputFields.length) {
            if (stopWatch) stopWatch()
              let temp = []
            pretask.params.outputFields[0].fields.forEach((item: any) => {
              temp.push({
                field: item,
                fieldType: item,
                // fieldDes: item,
                retain: fieldArr.value.includes(item) ? [true] : null,
                hiddenDel: true
              })
            })
            model.include_fields_base = [ ...temp ]
            if (!queryParames.datasourceId || !queryParames.database || !queryParames.tableName) {
              queryParames.datasourceId = pretask.params.outputFields[0].datasourceId
              if (pretask.params.outputFields[0].tableName) {
                queryParames.database = pretask.params.outputFields[0].tableName.split('.')[0]
                queryParames.tableName = pretask.params.outputFields[0].tableName.split('.')[1]
              }
            }
          }
          if (pretask && pretask.params && pretask.params.outputFields && pretask.params.outputFields.length) {
            if (stopWatch) stopWatch()
              let temp = []
            pretask.params.outputFields[0].fields.forEach((item: any) => {
              // for (let i = 0; i < model.include_fields.length; i++) {
              //   if (item === model.include_fields[i].field) {
              //     model.include_fields[i].retain = fieldArr.value.includes(item) ? [true] : null
              //     return;
              //   }
              // }
              temp.push({
                field: item,
                fieldType: item,
                // fieldDes: item,
                retain: fieldArr.value.includes(item) ? [true] : null,
                hiddenDel: true
              })
            })
            model.include_fields = [ ...temp ]
            // if (stopWatch) stopWatch()
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

   watchEffect(() => {
      if (model.include_fields_base && model.include_fields_base.length) {
        let temp1 = []
        model.include_fields_base.forEach((item: {
          retain: any, field: any
        }) => {
          if (item.retain && item.retain.length > 0 && !!item.retain[0]) {
            temp1.push(item)
          }
        })
        model.include_fields = temp1
        console.log('temp1', temp1)
        console.log('model.include_fields', model.include_fields)
      }
    },
    {
        flush: 'post'
    })
  

  let flag = false

  const getColumnList = async (datasourceId: number, database: string, tableName: string) => {
    if (flag || !datasourceId || !database || !tableName) {
      return
    }
    const listRes = await getDatasourceTableColumnDetailsById({datasourceId, database, tableName})
    flag = true
    if (listRes && listRes.length) {
      listRes.forEach((item: any) => {
        for (let i = 0; i < model.include_fields_base.length; i++) {
          if (item.column_name === model.include_fields_base[i].field) {
            model.include_fields_base[i].retain = fieldArr.value.includes(item.column_name) ? [true] : null
            model.include_fields_base[i].fieldType = item.type_name
            model.include_fields_base[i].fieldDes = model.include_fields_base[i].fieldDes || item.remarks
            return;
          }
        }
        // console.log(333)
        // model.include_fields_base.push({ field: item.column_name, fieldType: item.type_name, fieldDes: item.remarks, retain: fieldArr.value.includes(item.column_name) ? [true] : null, hiddenDel: true })
      })
      // if (stopWatch) stopWatch()
    }
  }

  onMounted(() => {
    getDataSourceList()
  })

  onUnmounted(() => {
    if (stopWatch) stopWatch()
  })
  return [
    ...useFilterParams({ model, field: 'include_fields_base', isSimple: true, name: '原表字段', span: fieldsSpan })
  ]
}

