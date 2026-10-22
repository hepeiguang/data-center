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
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
import { queryDataSourceListPaging, getDatasourceTableColumnDetailsById } from '@/service/modules/data-source'
import { useDagStore } from '@/store/project/dynamic/dag'
export function useDataDistinctTransform(model: { [field: string]: any }): IJsonItem[] {
    const { t } = useI18n()
    const dagStore = useDagStore()

    const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))
    const contrastStr = dagStore.getContrastStr
    const sortOptions = ref([] as { label: string; value: number; connectionParams?: any }[])
    const fieldOptions = ref([] as { label: string; value: number; connectionParams?: any }[])
    let stopWatch: (() => void) | null = null
    const paramsSpan = computed(() => (model.distinct_fields.length > 0 ? 24 : 0))
    const textSpan = (index: number) => {
        console.log('textSpan', model)
        if (model.cond_type == 'keywords' || model.cond_type == 'regex' ) {
            return 15
        }
        return 0
    }
    const textPlacehoder = (index: number) => {
        if (model.cond_type == 'keywords') {
            return '请输入关键字'
        } else if (model.cond_type == 'regex') {
            return '请输入正则表达式'
        }
        return ''
    }
    const showColumns = computed(() => {
        let temp = []
        model.distinct_fields.forEach(item => {
            temp.push({label:  item,value: item, disabled: condsSelectedColumns.value.includes(item)})
        })
        return temp
    })
// llxl
    const condsSelectedColumns = computed(() => {
        let temp = []
        model.conds.forEach(item => {
            temp.push(item.column)
        })
        return temp
    })

    const sourceList = ref([] as {id: string, type: string}[])

    const fieldArr = ref([] as any)
    const getDataSourceList = async () => {
        const listRes = await queryDataSourceListPaging({
            pageNo: 1,
            pageSize: 100
        })
        if (listRes.totalList && listRes.totalList.length) {
            listRes.totalList.forEach((item: any) => {
                sortOptions.value.push({ label: item.name, value: item.id, connectionParams: JSON.parse(item.connectionParams) })
                sourceList.value.push({id: item.id, type: item.type})
            })
            stopWatch = watch(model.distinct_fields, (newValue, oldValue) => {
                if (!model.preTasks[0]) {
                    return
                }
                if (model.distinct_fields) {
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
                        model.datasourceId = pretask.params.datasourceId
                    } else if (pretask.params.datasourceId && pretask.params.table_path) {
                        queryParames.datasourceId = pretask.params.datasourceId
                        queryParames.database = pretask.params.table_path.split('.')[0]
                        queryParames.tableName = pretask.params.table_path.split('.')[1]
                        model.datasourceId = pretask.params.datasourceId
                    }
                    if (!(contrastStr && JSON.stringify(queryParames) === contrastStr)) {
                        dagStore.setContrastStr(JSON.stringify(queryParames))
                        model.distinct_fields = []
                        fieldArr.value = []
                    }
                    if (queryParames.datasourceId) {
                        sourceList.value.forEach(item => {
                            if (item.id == queryParames.datasourceId) {
                                model.db_type = item.type
                            }
                        })
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
        // {
        //     type: 'custom-parameters',
        //     field: 'distinct_fields',
        //     name: '字段去重',
        //     class: 'btn-custom-parameters',
        //     span: fieldsSpan,
        //     children: [
        //         {
        //             type: 'select',
        //             field: 'field_name',
        //             span: 22,
        //             options: fieldOptions,
        //             value: '',
        //         }
        //     ]
        // },
        {
            type: 'select',
            field: 'distinct_fields',
            span: fieldsSpan,
            name: '字段去重',
            options: fieldOptions,
            value: '',
            props: {
                multiple: true
            },
        },
        {
            type: 'select',
            field: 'cond_type',
            name: '过滤规则',
            span: paramsSpan,
            options: TYPES,
            value: '',
        },
        {
            type: 'custom-parameters',
            field: 'conds',
            name: '过滤参数',
            class: 'btn-custom-parameters',
            span: paramsSpan,
            children: [
                
                {
                    type: 'select',
                    field: 'column',
                    span: 7,
                    options: showColumns,
                    value: '',
                },
                (i = 0) => (
                    {
                        type: 'input',
                        field: 'value',
                        span: textSpan(i),
                        props: {
                            placeholder: textPlacehoder(i)
                        }
                    }
                )
            ]
        },
    ]
}

export const TYPES = [
    {
      label: '关键字一致',
      value: 'keywords'
    },
    {
      label: '完全匹配',
      value: 'complateMatch'
    },
    {
      label: '正则表达式',
      value: 'regex'
    },
    {
      label: '非空',
      value: 'NOTNULL'
    }
  ]
