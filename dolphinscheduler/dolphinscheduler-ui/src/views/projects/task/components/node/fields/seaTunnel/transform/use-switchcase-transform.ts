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
export function useSwitchcaseTransform(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
   const dagStore = useDagStore()
      const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))
      const fieldsSpansm = computed(() => (!!model.preTasks[0] ? 12 : 0))
      const contrastStr = dagStore.getContrastStr
      const sortOptions = ref([] as { label: string; value: number; connectionParams?: any }[])
      const fieldOptions = ref([] as { label: string; value: any; connectionParams?: any }[])
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
                stopWatch = watch(model.switch_case_mappings, (newValue, oldValue) => {
                    if (!model.preTasks[0]) {
                        return
                    }
                    if (model.switch_case_mappings) {
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
                            model.switch_case_mappings = []
                            fieldArr.value = []
                        }
                        model.table_name = queryParames.tableName
                        if (pretask && pretask.params && pretask.params.outputFields && pretask.params.outputFields.length) {
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
        //             // fieldOptions.value.push({ label: item.column_name, value: `${item.column_name} :${item.type_name}` })
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
                field: 'switch_field',
                options: fieldOptions,
                span:fieldsSpansm,
                name: t('转换字段'),
                validate: {
                trigger: ['input', 'trigger'],
                required: true,
                message: t('该字段为必填')
                },       
            },  
            {
                type: 'input',
                field: 'switch_target_field',
                span:fieldsSpansm,
                name: t('目标字段'),
                validate: {
                trigger: ['input', 'trigger'],
                required: true,
                message: t('该字段为必填')
                },       
            },
            {
                type: 'select',
                field: 'switch_target_field_type',
                span:fieldsSpansm,
                name: t('目标字段类型'),
                options: [
                  {label: t('数值型'), value: 'num'},
                  {label: t('字符串型'), value: 'string'},
                  {label: t('日期型'), value: 'date'},
                  {label: t('布尔型'), value: 'bool'},
                ],
                validate: {
                trigger: ['input', 'trigger'],
                required: true,
                message: t('该字段为必填')
                },       
            },
          {
            type: 'input',
            field: 'switch_field_value_default',
            name: t('默认值'),
            tooltip: t('其他不符合配置的使用默认值'),
            span:fieldsSpansm,
            props: {
                placeholder: t('请输入默认值'),
                maxLength: 256
              },
          }, 
          { type:'custom-parameters',
            field: 'switch_case_mappings',
            name:t('转换配置'),
            span:fieldsSpan,
            tooltip: t("示例：\n 原值：'1'，\n 目标值：'壹'"),
            children:  [
              {
                type: 'input',
                field: 'switch_case_when',
                span: 11,
                validate: {
                  trigger: ['input', 'trigger'],
                  required: true,
                  message: t('该字段为必填')
                }, 
                props: {
                  placeholder: t('请输入原值'),
                  maxLength: 256
                },
              },
              {
                type: 'input',
                field: 'switch_case_then',
                span: 11,
                validate: {
                  trigger: ['input', 'trigger'],
                  required: true,
                  message: t('该字段为必填')
                }, 
                props: {
                  placeholder: t('请输入目标值'),
                  maxLength: 256
                },
              },
              ]

          } ,  
  ]
}

