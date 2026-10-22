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
import { useCustomParams, useResources } from '../../'
import { useUdfs } from '../../use-udfs'
import type { IJsonItem } from '../../../types'
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { queryDataSourceListPaging, getDatasourceTableColumnDetailsById } from '@/service/modules/data-source'
import { useDagStore } from '@/store/project/dynamic/dag'
export function useSqlTransform(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  const dagStore = useDagStore()
    const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))
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
              stopWatch = watch(model.query, (newValue, oldValue) => {
                  if (!model.preTasks[0]) {
                      return
                  }
                    console.log(model.query)
                  if (model.query) {
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
                          model.query = []
                          fieldArr.value = []
                      }
                      model.table_name = queryParames.tableName
                      pretask.params.outputFields[0].fields.forEach((item: any) => {
                            fieldOptions.value.push({ label: item, value: item })
                        })
                    //   getColumnList(Number(queryParames.datasourceId), queryParames.database, queryParames.tableName)
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
            type: 'mirror',
            field: 'query',
            span: fieldsSpan,
            name: t('project.node.sql_statement'),
            validate: {
              trigger: ['input', 'trigger'],
              required: true,
              message: t('project.node.sql_empty_tips')
            },
            props: {
              language: 'sql',
              placeholder: t('请输入sql语句'),
            }
          },
        //   useUdfs(model),
        //   ...useCustomParams({ model, field: 'localParams', span: fieldsSpan,isSimple: false }),
  ]
}

