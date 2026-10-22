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
import {useI18n} from 'vue-i18n'
import {useCustomRange} from '../..'
import type {IJsonItem} from '../../../types'
import {useUdfs} from '../../use-udfs'
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { queryDataSourceListPaging, getDatasourceTableColumnDetailsById } from '@/service/modules/data-source'
import { useDagStore } from '@/store/project/dynamic/dag'

export function useIncreasesequenceTransform(model: { [field: string]: any }): IJsonItem[] {
  const {t} = useI18n()
   const dagStore = useDagStore()
      const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))
      const fieldsSpansm = computed(() => (!!model.preTasks[0] ? 8 : 0))
      const contrastStr = dagStore.getContrastStr
      const sortOptions = ref([] as { label: string; value: number; connectionParams?: any }[])
      const fieldOptions = ref([] as { label: string; value: number; connectionParams?: any }[])
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
                stopWatch = watch(fieldsSpan, (newValue, oldValue) => {
                    if (!model.preTasks[0]) {
                        return
                    }
                    if (fieldsSpan) {
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
                            fieldArr.value = []
                        }
                        model.table_name = queryParames.tableName
                        pretask.params.outputFields[0].fields.forEach((item: any) => {
                            fieldOptions.value.push({ label: item, value: item })
                        })
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
    
        onMounted(() => {
            getDataSourceList()
        })
    
        onUnmounted(() => {
            if (stopWatch) stopWatch()
        })
  return [
    {
      type: 'select',
      field: 'field_name',
      name: t('列名'),
      options: fieldOptions,
      value: '',
      span: fieldsSpan,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('该字段为必填项'))
          }
        },
      },
    },
    {
      type: 'input-number',
      field: 'start_value',
      name: t('初始值'),
      span: fieldsSpansm,
      props: {
        step: 1,       
        placeholder: t('初始值'),

    },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: number) {
          if (!value && value !== 0)  {
            return new Error(t('该字段为必填项'))
          }
         const boo =  model.min_number ==  value && model.max_number ?model.max_number< value:false
          if(boo){
            return new Error(t('不得大于最大值'))
          }
        },
      },
    },
    {
      type: 'input-number',
      field: 'add_value',
      name: t('增长根据'),
      span: fieldsSpansm,
      props: {
        step: 1,      
        placeholder: t('累加值(需为正整数)'),

    },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: number) {
          if (!value && value !== 0) {
            return new Error(t('该字段为必填项'))
          }
        }
      },
    },
    {
      type: 'input-number',
      field: 'max_value',
      name: t('最大值'),
      value: '',
      span: fieldsSpansm,
      props: {
        step: 1,      
        placeholder: t('最大值'),

    },
      validate: {
        validator(validate: any, value: number) {
          const boo =   model.max_number ==  value && model.min_number ?model.min_number> value:false
          if (boo) {
            return new Error(t('不得小于初始值'))
          }
        },
      },
    },
  ]
}

