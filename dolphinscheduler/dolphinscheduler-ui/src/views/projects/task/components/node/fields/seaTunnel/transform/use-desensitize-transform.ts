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
import { useI18n } from 'vue-i18n'
import { useCustomParams, useResources } from '../..'
import { useUdfs } from '../../use-udfs'
import type { IJsonItem } from '../../../types'
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { queryDataSourceListPaging, getDatasourceTableColumnDetailsById } from '@/service/modules/data-source'
import { useDagStore } from '@/store/project/dynamic/dag'
import {getAlgorithmSecret,getAlgorithmTree,getTableStoreEncrypt} from '@/service/modules/data-transform'
export function useDesensitizeTransform(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
   const dagStore = useDagStore()
      const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))
      const fieldsSpansm = computed(() => (!!model.preTasks[0] ? 8 : 0))
      const contrastStr = dagStore.getContrastStr
      const sortOptions = ref([] as { label: string; value: number; connectionParams?: any }[])
      const fieldOptions = ref([] as { label: string; value: number; connectionParams?: any }[])
      const showColumns = computed(() => {
        let temp = []
        fieldOptions.value.forEach(item => {
            temp.push({label:  item.label,value: item.value, disabled: custom_listSelectedColumns.value.includes(item.value)})
        })
        return temp
      })

      const custom_listSelectedColumns = computed(() => {
        let temp = []
        model.custom_list.forEach(item => {
            temp.push(item.desensitize_field)
        })
        return temp
      })
      let stopWatch: (() => void) | null = null
    
        const fieldArr = ref([] as any)
        const getDataSourceList = async () => {
            const listRes = await queryDataSourceListPaging({
                pageNo: 1,
                pageSize: 100
            })
            if (listRes.totalList && listRes.totalList.length) {
              console.log(111)
                listRes.totalList.forEach((item: any) => {
                    sortOptions.value.push({ label: item.name, value: item.id, connectionParams: JSON.parse(item.connectionParams) })
                })
                stopWatch = watch(model.custom_list, (newValue, oldValue) => {
                    if (!model.preTasks[0]) {
                        return
                    }
                    if (model.custom_list) {
                      console.log(222)
                        const taskDefinitionList = dagStore.getDagTaskDefinitionList
                        const pretask = taskDefinitionList.find((task) => {
                            return task.code == model.preTasks[0]
                        })
                        const queryParames = {
                            datasourceId: '',
                            database: '',
                            tableName: ''
                        }
                        console.log(pretask.taskParams)
                        if (pretask.taskParams) {
                            pretask.params = pretask.taskParams
                        }
                        console.log(pretask.params.datasourceId , pretask.params.database , pretask.params.table)
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
                            model.custom_list = []
                            fieldArr.value = []
                        }
                        model.table_name = queryParames.tableName
                        if (pretask.params && pretask.params.outputFields && pretask.params.outputFields.length && fieldOptions.value.length ==0) {
                          pretask.params.outputFields[0].fields.forEach((item: any) => {
                             fieldOptions.value.push({ label: item, value: item })
                          })
                          if (stopWatch) stopWatch()
                        }
                        // getColumnList(Number(queryParames.datasourceId), queryParames.database, queryParames.tableName)
                    }
    
                }, { deep: true, immediate: true })
            }
        }
    
        // const getColumnList = async (datasourceId: number, database: string, tableName: string) => {
        //     const listRes = await getDatasourceTableColumnDetailsById({ datasourceId, database, tableName })
        //     if (listRes && listRes.length) {
        //         listRes.forEach((item: any) => {
        //             fieldOptions.value.push({ label: item.column_name, value: item.column_name })
        //         })
        //         if (stopWatch) stopWatch()
        //     }
        // }
    
    
        onUnmounted(() => {
            if (stopWatch) stopWatch()
        })
    const optionstm = ref<Array<{
        label: string
        key: string
        children: Array<{ label: string; value: string; key: string }>
      }>>([])
    const getList = async () => {
          const { data } = await getAlgorithmTree()
          optionstm.value = convertToTree(data.DST.children)
        }
        const convertToTree = (data: Record<string, any>): any[] => {
        return Object.entries(data).map(([_, node],i) => ({
          label: node.ruleName,
          value: node.rule, 
          key: node.code,
          children: node.children &&   Object.keys(node.children).length > 0 ? convertToTree(node.children) : undefined
        }))
      } 
      onMounted(() => {
        getList()
        getDataSourceList()
      })

  return [
        {type:'custom-parameters',
          field: 'custom_list',
          name: t('脱敏配置'),
          span: fieldsSpan,
          children:[
            {
              type: 'select',
              field: 'desensitize_field',
              options: showColumns,
              span: 11,
              props: {
                placeholder: t('请输入脱敏字段')},
              validate: {
                trigger: ['input', 'trigger'],
                required: true,
                message: t('该字段为必填')
              },       
            }, 
            {
              type: 'tree-select',
              field: 'desensitize_type',
              span: 11,
              options: optionstm,
              props: {
                placeholder: t('请选择脱敏类型'),
                overrideDefaultNodeClickBehavior:"override", 
              },
              validate: {
                trigger: ['input', 'trigger'],
                required: true,
                message: t('该字段为必填')
              },       
            },       
          ]
        },
          
  ]
}

