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
import { ref, onMounted, watchEffect, computed } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import type { IJsonItem } from '../../../types'
import {
    viewProcessDefinitionVariables
} from '@/service/modules/process-definition'
import { useDagStore } from '@/store/project/dynamic/dag'

export function useConstReferenceTransform(model: { [field: string]: any }): IJsonItem[] {
    const { t } = useI18n()
    const dagStore = useDagStore()
    const route = useRoute()
    const projectCode = Number(route.params.projectCode)
    const code = !route.params.code ? Number(route.query.code) : Number(route.params.code)
    const contrastStr = dagStore.getContrastStr
    const fieldOptions = ref([] as { label: string; value: number; connectionParams?: any }[])
    const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))

    watchEffect(() => {
        if (typeof model.variable === 'object') {
            model.variable = JSON.stringify(model.variable)
        }

        if (!model.preTasks[0]) {
            return
        }
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
            queryParames.tableName = pretask.params.table
        } else if (pretask.params.datasourceId && pretask.params.table_path) {
            queryParames.tableName = pretask.params.table_path.split('.')[1]
        }
        if (!(contrastStr && JSON.stringify(queryParames) === contrastStr)) {
            dagStore.setContrastStr(JSON.stringify(queryParames))
        }
        model.table_name = queryParames.tableName
        if (pretask.params && pretask.params.outputFields && pretask.params.outputFields.length && fieldOptions.value.length ==0) {
            pretask.params.outputFields[0].fields.forEach((item: any) => {
                fieldOptions.value.push({ label: item, value: item })
            })
        }
    },
        {
            flush: 'post'
        })

    const constOptions = ref([
        {
            label: '全局变量',
            value: 'global',
            children: []
        },
        {
            label: '局部变量',
            value: 'local',
            children: []
        },
        {
            label: '内置变量',
            value: 'builtIn',
            children: [
                {
                    label: '前一天',
                    value: JSON.stringify({ type: 3, name: "前一天", value: "system.biz.date" })
                },
                {
                    label: '当天',
                    value: JSON.stringify({ type: 3, name: "当天", value: "system.biz.curdate" })
                },
                {
                    label: '当天（精确到秒）',
                    value: JSON.stringify({ type: 3, name: "当天（精确到秒）", value: "system.datetime" })
                },
            ]
        },
    ] as any)

    const getProcessListByCode = async () => {
        if (!projectCode || !code) return
        const res = await viewProcessDefinitionVariables(projectCode, code)
        constOptions.value[0].children = res.globalParams.map((item: any) => {
            return {
                label: item.prop,
                value: JSON.stringify({ type: 1, name: item.prop, value: item.value })
            }
        })
        const KeyList = Object.keys(res.localParams)
        const DefinitionList = dagStore.getDagTaskDefinitionList
        const graph = dagStore.getGraph
        const parentId = graph.getCellById(model.code)._parent.id
        const parentNode = DefinitionList.find((task) => {
            return task.code == parentId
        })
        KeyList.forEach((k) => {
            if (k !== parentNode.name) return
            constOptions.value[1].children = res.localParams[k].localParamsList.map((item: any) => {
                return {
                    label: item.prop,
                    value: JSON.stringify({ type: 2, name: item.prop, value: item.value })
                }
            })
        })
    }

    onMounted(() => {
        getProcessListByCode()
    })

    return [
        {
            type: 'select',
            field: 'field_name',
            name: t('字段名称'),
            props: {
                placeholder: t('请输入字段名称'),
            },
            span: fieldsSpan,
            options: fieldOptions,
            validate: {
                required: true
            }
        },
        {
            type: 'tree-select',
            field: 'variable',
            name: t('变量选择'),
            span: fieldsSpan,
            options: constOptions,
            props: {
                overrideDefaultNodeClickBehavior: true,
                keyField: 'value',
                labelField: 'label',
            },
            validate: {
                trigger: ['input', 'blur'],
                required: true,
                validator(validate: any, value: string) {
                    if (!value) {
                        return new Error(t('请选择变量'))
                    }
                }
            }
        },
    ]
}

