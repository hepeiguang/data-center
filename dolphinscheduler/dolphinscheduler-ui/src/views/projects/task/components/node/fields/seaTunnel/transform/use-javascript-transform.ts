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
import {useUdfs} from '../../use-udfs'
import type {IJsonItem} from '../../../types'
import { ref, onMounted, onUnmounted, computed, watch } from 'vue'
import { queryDataSourceListPaging, getDatasourceTableColumnDetailsById } from '@/service/modules/data-source'
import { useDagStore } from '@/store/project/dynamic/dag'

export function useJavascriptTransform(model: { [field: string]: any }): IJsonItem[] {
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
                stopWatch = watch(model.source_code, (newValue, oldValue) => {
                    if (!model.preTasks[0]) {
                        return
                    }
                    if (model.source_code) {
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
                        // if (!(contrastStr && JSON.stringify(queryParames) === contrastStr)) {
                        //     dagStore.setContrastStr(JSON.stringify(queryParames))
                        //     model.source_code = []
                        //     fieldArr.value = []
                        // }

                        model.table_name = queryParames.tableName
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
//   const customSpan = computed(() => (model.compile_pattern=='ABSOLUTE_PATH'? 24 : 0))
//   const unCustomSpan = computed(() => (model.compile_pattern=='SOURCE_CODE' ? 24 : 0))
//   watch(
//     () => model.compile_pattern,
//     (newVal) => {
//       if (newVal === 'ABSOLUTE_PATH') {
//         model.source_code = ''
//       } else {
//         model.absolute_path = ''
//       }
//     }
//   )


  return [
    //   {
    //     type: 'select',
    //     field: 'compile_language',
    //     name: t('编译语句'),
    //     span:12,
    //     options: [
    //         { value: 'JAVASCRIPT', label: 'JAVASCRIPT' },
    //     ],
    //     validate: {
    //       trigger: ['input', 'trigger'],
    //       required: true,
    //       message: t('该字段为必填')
    //     },
    //   },
    //   {
    //     type: 'select',
    //     field: 'compile_pattern',
    //     span:12,
    //     value:'SOURCE_CODE',
    //     options: [
    //         { value: 'ABSOLUTE_PATH', label: '文件路径' },
    //         { value: 'SOURCE_CODE', label: '源代码' },
    //     ],
    //     name: t('编译模式'),
    //     validate: {
    //       trigger: ['input', 'trigger'],
    //       required: true,
    //       message: t('该字段为必填')
    //     },
    //   },
    {
      type: 'mirror',
      field: 'source_code',
      name: t('源代码'),
      span: fieldsSpan,
      value: model.compile_pattern == 'ABSOLUTE_PATH' ? '' : model.source_code,
      // span: unCustomSpan,
      validate: {
        trigger: ['input', 'trigger'],
        required: true,
        message: t('该字段为必填')
      },
      props: {
        language: 'javascript',
        placeholder: t(
          `// 填写示例：
var calculator = {
  // 返回输出字段数组
  getInlineOutputColumns: function () {
    return ['filed1', 'filed2'];
  },
  // 返回value数组，长度和字段数组长度一致，val1，val2代表字段的值
  getInlineOutputFieldValues: function (val1, val2) {
    var arr = [];
    if (val1 == 9) {
      arr[0] = 1;
    } else {
      arr[0] = val1;
    }
    arr[1] = val2;
    return arr;
  }
};
                `),
      },
    },
    //   {
    //     type: 'input',
    //     field: 'absolute_path',
    //     name: t('文件路径'),
    //     value:model.compile_pattern=='SOURCE_CODE'? '': model.absolute_path ,
    //     tooltip: t('文件路径："""/tmp/GroovyFile"""'),
    //     span: customSpan,
    //     validate: {
    //       trigger: ['input', 'trigger'],
    //       required: true,
    //       message: t('该字段为必填')
    //     },
    //   },
  ]
}

