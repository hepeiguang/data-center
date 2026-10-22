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
import {useCustomSqlfilter} from '../../'
import type {IJsonItem} from '../../../types'
import {useUdfs} from '../../use-udfs'
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { queryDataSourceListPaging, getDatasourceTableColumnDetailsById } from '@/service/modules/data-source'
import { useDagStore } from '@/store/project/dynamic/dag'

export function useSqlfilterTransform(model: { [field: string]: any }): IJsonItem[] {
  const {t} = useI18n()
   const dagStore = useDagStore()
      const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))
      const fieldsSpansm = computed(() => (!!model.preTasks[0] ? 6 : 0))
      const fieldsSpansm1 = computed(() => (!!model.preTasks[0] ? 4 : 0))
      const contrastStr = dagStore.getContrastStr
      const sortOptions = ref([] as { label: string; value: number; connectionParams?: any }[])
      const fieldOptions = ref([] as { label: string; value: number; connectionParams?: any }[])

      const showColumns = computed(() => {
        let temp = []
        fieldOptions.value.forEach(item => {
            temp.push({label:  item.label,value: item.value, disabled: sqlfiltermapSelectedColumns.value.includes(item.value)})
        })
        return temp
      })

      const sqlfiltermapSelectedColumns = computed(() => {
        let temp = []
        model.sqlfiltermap.forEach(item => {
          temp.push(item.prop)
        })
        if (model.prop_init) {
          temp.push(model.prop_init)
        }
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
                stopWatch = watch(model.sqlfiltermap, (newValue, oldValue) => {
                    if (!model.preTasks[0]) {
                        return
                    }
                    if (model.sqlfiltermap) {
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
                            model.sqlfiltermap = []
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
    
        onMounted(() => {
            getDataSourceList()
        })
    
        onUnmounted(() => {
            if (stopWatch) stopWatch()
        })
  const compareOptions = [
    {label: t('='), value: '='},
    {label: t('!='), value: '!='},
    {label: t('>'), value: '>'},
    {label: t('>='), value: '>='},
    {label: t('<'), value: '<'},
    {label: t('<='), value: '<='},
    // {label: t('是null'), value: 'is null'},
    // {label: t('不是null'), value: 'is not null'},
  ]


  return [
    // {
    //   type: 'select',
    //   field: 'datasourceId',
    //   name: '条件参数',
    //   span: 24,
    //   props: {
    //     hidden: true,
    //   },
    // },
    {
      type: 'select',
      field: 'prop_init',
      // name:'条件参数',
      span: fieldsSpansm,
      options:fieldOptions,
      class: 'input-param-key',
      props: {
        placeholder: t('字段名'),
        maxLength: 256
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
    {
      type: 'select',
      field: 'status_init',
      // name:'    ',
      span: fieldsSpansm,
      options: compareOptions,
      value: '=',
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
    {
      type: 'input',
      field: 'value_init',
      // name:'    ',
      span: fieldsSpansm,
      class: 'input-param-value',
      // value: computed(() =>
      //   ['is null', 'is not null'].includes(model.status_init)
      //     ? 'null'
      //     : ''
      // ),
      props: {
        placeholder: t('字段值'),
        // disabled: computed(() => ['is null', 'is not null'].includes(model.status_init)),
        maxLength: 256
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
    {
      type: 'select',
      field: 'type_init',
      // name:'    ',
      span: fieldsSpansm,
      options: [
        {label: t('数值型'), value: 'num'},
        {label: t('字符串型'), value: 'string'},
        {label: t('日期型'), value: 'date'},
        {label: t('布尔型'), value: 'bool'},
      ],
      // value: computed(() =>
      //   ['is null', 'is not null'].includes(model.status_init)
      //     ? 'null'
      //     : ''
      // ),
      props: {
        placeholder: t('字段值类型'),
        // disabled: computed(() => ['is null', 'is not null'].includes(model.status_init)),
      },
      validate: {
        trigger: ['blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('该字段为必填项'))
          }
        }
      }
    },
    ...useCustomSqlfilter({
      model,
      name: 'custom_parameters',
      field: 'sqlfiltermap',
      span: fieldsSpan,
      options: showColumns
    })


  ]
}
