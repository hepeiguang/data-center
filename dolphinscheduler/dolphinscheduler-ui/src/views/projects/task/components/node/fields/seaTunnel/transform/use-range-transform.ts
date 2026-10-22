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
import {useCustomRange} from '../../'
import type {IJsonItem} from '../../../types'
import {useUdfs} from '../../use-udfs'
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { queryDataSourceListPaging, getDatasourceTableColumnDetailsById } from '@/service/modules/data-source'
import { useDagStore } from '@/store/project/dynamic/dag'

export function useRangeTransform(model: { [field: string]: any }): IJsonItem[] {
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
                stopWatch = watch(model.range_field, (newValue, oldValue) => {
                    if (!model.preTasks[0]) {
                        return
                    }
                    if (model.range_field) {
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
                            model.range_field = []
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
      type: 'input',
      field: 'range_field',
      name: t('映射字段'),
      value: '',
      span: fieldsSpansm,
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
      type: 'select',
      field: 'range_type',
      name: t('规则'),
      span: fieldsSpansm,
      options: [
        {label: t('左闭右开'), value: 'greater_than_or_equal'},
        {label: t('左开右闭'), value: 'less_than_or_equal'},
      ],
      value: 'greater_than_or_equal',
    },
    {
      type: 'input',
      field: 'range_value_default',
      name: t('默认值'),
      tooltip: t('其他不符合配置的使用默认值'),
      value: '',
      span: fieldsSpansm,
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('该字段为必填项'))
          }
           const fieldType = model.range_to_field_type
          if (fieldType === 'DOUBLE') {
            if (isNaN(Number(value))) {
              return new Error(t('数字类型必须输入有效数值'))
            }
          }
          if (fieldType === 'LONG') {
              if (!Number.isInteger(Number(value)) || value.indexOf('.')!==-1) {
                 return new Error(t('整数类型必须输入有效整数'))
              }
            }
          if (fieldType === 'BOOLEAN') {
            if (!['true', 'false'].includes(value.toLowerCase())) {
              return new Error(t('布尔类型必须为 true 或 false'))
            }
          }
        },
      },
    },
    {
      type: 'input',
      field: 'range_to_field',
      name: t('目标字段'),
      value: '',
      span: fieldsSpansm,
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
      type: 'select',
      field: 'range_to_field_type',
      name: t('目标字段类型'),
      span: fieldsSpansm,
      options: [
        { label: '字符串型', value: 'STRING' },
        { label: '整数型', value: 'LONG' },
        { label: '浮点型', value: 'DOUBLE' },
        { label: '布尔型', value: 'BOOLEAN' }  
      ],
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

    useUdfs(model),
    // ...useCustomRange({model, field: 'range_mappings',}),
    {
        type: 'custom-parameters',
        field: 'range_mappings',
        name: t(`映射配置`),
        tooltip:t(`示例：\n 最小值：0 \n 最大值：18 \n 值：child`),
        class: 'btn-custom-parameters',
        span:fieldsSpan,
        children: [
        {
            type: 'input-number',
            field: 'range_start',
            span: 7,
            props: {
                step: 1,      
                precision: 2 ,
                placeholder: t('初始值'),

            },
            validate: {
              trigger: ['input', 'blur'],
              required: true,
              validator(validate: any, value: number) {
                if (!value && value !== 0) {
                  return new Error(t('该字段为必填项'))
                }
                const com = model.range_mappings
                console.log(com,111)
                const comItems = model.range_mappings.filter(
                  (item: { range_start: number ,range_end:number}) =>item.range_start ==  value && item.range_end ?item.range_end< value:false
                )
                if (comItems.length > 0) {
                  return new Error(t('不得大于最大值'))
                }
                const sameItems = model.range_mappings.filter(
                  (item: { range_start: number ,range_end:number}) =>item.range_start < value && item.range_end > value
                )
                if (sameItems.length > 0) {
                  return new Error(t('范围值已存在'))
                }
              }
            }
        },
        {
            type: 'input-number',
            field: 'range_end',
            span: 7,
            props: {
                step: 1,     
                placeholder: t('最大值'), 
                precision: 2     
            },
            validate: {
              trigger: ['input', 'blur'],
              required: true,
              validator(validate: any, value: number) {
                if (!value && value !== 0) {
                  return new Error(t('该字段为必填项'))
                }
                const comItems = model.range_mappings.filter(
                  (item: { range_start: number ,range_end:number}) =>item.range_end == value && item.range_start? item.range_start > value : false  
                )
                if (comItems.length > 0) {
                  return new Error(t('不得小于最小值'))
                }
                const sameItems = model.range_mappings.filter(
                  (item: { range_start: number ,range_end:number}) =>item.range_start <  value && item.range_end > value
                )
                if (sameItems.length > 0) {
                  return new Error(t('范围值已存在'))
                }
              }
            }
        },
        {
            type: 'input',
            field: 'range_value',
            value: '',
            span: 7,
            props: {
                placeholder: t('值'),
            },
            validate: {
                trigger: ['input', 'blur'],
                required: true,
                validator(validate: any, value: string) {
                  if (!value) {
                    return new Error(t('该字段为必填项'))
                  }
                  const fieldType = model.range_to_field_type
                  if (fieldType === 'DOUBLE') {
                    if (isNaN(Number(value))) {
                      return new Error(t('数字类型必须输入有效数值'))
                    }
                  }
                  if (fieldType === 'LONG') {
                    if (!Number.isInteger(Number(value)) || value.indexOf('.')!==-1) {
                      return new Error(t('整数类型必须输入有效整数'))
                    }
                  }

                  if (fieldType === 'BOOLEAN') {
                    if (!['true', 'false'].includes(value.toLowerCase())) {
                      console.log(value.indexOf('.'))
                      return new Error(t('布尔类型必须为 true 或 false'))
                    }
                  }
                }
              }
        },]}
  ]
}

