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
import { ref, onMounted, onUnmounted, computed, watch, nextTick } from 'vue'
import { useI18n } from 'vue-i18n'
import { useMessage } from 'naive-ui'
import { useFilterParams } from '../..'
import { useNormalize } from './use-normalize'
import type { IJsonItem } from '../../../types'
import {
  queryDataSourceListPaging,
  getDatasourceTableColumnDetailsById
} from '@/service/modules/data-source'
import { queryDictInfo } from '@/service/modules/dict-info'
import { useDagStore } from '@/store/project/dynamic/dag'
import { useDialogStore } from '@/store/project/dynamic/dialog'
export function useDataNormalizeTransform(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  const message = useMessage()
  const dagStore = useDagStore()
  const dialogStore = useDialogStore()
  const functionOptions = ref([] as any)
  const isCustomAddTable = ref(false)
  functionOptions.value = []
  const options = ref(
    [] as { label: string; value: number; connectionParams?: any }[]
  )
  const fieldsSpan = computed(() =>
    !!model.preTasks[0] ? (model.activeModule === 'first' ? 24 : 0) : 0
  )
  // const moduleSpan = computed(() => (model.activeModule === 'first' ? 0 : (model.include_fields.length > 0 ? 24 : 0)))
  const moduleSpan = computed(() => (model.activeModule === 'first' ? 0 : ((model.include_fields.length > 0 || isCustomAddTable.value) ? 24 : 0)))
  const nextBtnSpan = computed(() => (((model.include_fields.length > 0 || isCustomAddTable.value) && model.activeModule !== 'first') ? 2 : 0))
  // const nextBtnSpan = computed(() => ((model.activeModule !== 'first') ? 2 : 0))
  // const dictCodeArr = computed(() => (Array.isArray(functionOptions.value) && functionOptions.value.find((item:any) => item.value === 'NAMESPACE')?.children ))
  // const typeCodeArr:any = computed(() => (Array.isArray(functionOptions.value) && functionOptions.value.map((item:any) => {return {'label': item.label, 'value': item.value, 'apiUrl': item.apiUrl }} ) ))
  const contrastStr = dagStore.getContrastStr
  const fieldArr = ref([] as any)
  const queryParames = {
    datasourceId: '',
    database: '',
    tableName: ''
  }
  let stopWatch: (() => void) | null = null;
  watch(
    () => dialogStore.step,
    (newValue, oldValue) => {
      if (newValue === 1) {
        if (model.activeModule === 'first') {
          // const dictName = model.dictType !== 'NAMESPACE' ? (typeCodeArr.value.find((ele:any) => ele.value === model.dictType)?.label) : getCodeZh(model.dictCode);
          const dictName = getCodeZh(model.dictId, 'label')
          const api_url = getCodeZh(model.dictId, 'apiUrl')
          // const dictCode = model.dictType !== 'NAMESPACE' ? '' : model.dictCode
          // model.api_url = typeCodeArr.value.find((ele:any) => ele.value === model.dictType)?.apiUrl
          // console.log(model.api_url, model, 'model.api_url')
          model.include_fields.forEach((item: any) => {
            if (item.retain && item.retain.length > 0 && !!item.retain[0]) {
              // model.api_url = typeCodeArr.value.find((ele:any) => ele.value === model.dictType)?.apiUrl
              model.columns.push({
                table_name: queryParames.tableName,
                column_name: item.field,
                dict_code: model.dictId,
                dict_name: dictName,
                dict_id: model.id,
                min_num: model.min_num,
                max_num: model.max_num,
                time_type: model.time_type,
                num_accuracy: model.num_accuracy,
                dict_type: model.dictType,
                api_url: api_url
              })
            }
          })
          // 去重
          model.columns = model.columns.filter(
            (item: any, index: any, self: any) => {
              return (
                index ===
                self.findIndex(
                  (t: any) =>
                    t.dict_id === item.dict_id &&
                    t.column_name === item.column_name
                )
              ) && item.column_name
            }
          )
   
          // 
          model.activeModule = 'second'
          
          // 如果修改值 需要清楚之前的选择
          nextTick(() => {
            if (isCustomAddTable.value) {
              model.dictType = ''
              model.min_num = ''
              model.max_num = ''
              model.time_type = ''
              model.num_accuracy = ''
              model.dictId = ''
              model.dictCode = ''
              model.include_fields = []
            }
          })
          console.log(model)

          dialogStore.setDialogCanNext(false)
        } 
        // 在这里处理变化
      } else {
        model.activeModule = 'first'
        dialogStore.setDialogCanNext(true)
      }
    }
  )
  // 查找對應的选择中文名称
  const getCodeZh = (value: any, filId:any) => {
    let newData: any = []
    const InfinityData = (data: any) => {
      console.log("datat", data);
      data.forEach((item: any) => {
        newData.push(item)
        if (item.children && item.children.length) {
          InfinityData(item.children)
        }
      })
    }
    InfinityData(functionOptions.value)
    return newData.find((item: any) => item.id === value)[filId]
  }
  const getDataSourceList = async () => {
    functionOptions.value = await queryDictInfo()
    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (model.columns.length > 0) {
      model.activeModule = 'second'
      dialogStore.setStep(1)
    }
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        options.value.push({
          label: item.name,
          value: item.id,
          connectionParams: JSON.parse(item.connectionParams)

        })
      })


      stopWatch = watch(
        model.include_fields,
        (newValue, oldValue) => {
          if (!model.preTasks[0]) {
            return
          }
          if (model.include_fields) {
            model.include_fields.forEach(
              (item: { retain: any; field: any }) => {
                if (item.retain && item.retain.length > 0 && !!item.retain[0]) {
                  fieldArr.value.push(item.field)
                }
              }
            )
            const taskDefinitionList = dagStore.getDagTaskDefinitionList
            
            const pretask = taskDefinitionList.find((task) => {
              return task.code == model.preTasks[0]
            })
           
            if (pretask.taskParams) {
              pretask.params = pretask.taskParams
            }
            if (
              pretask.params.datasourceId &&
              pretask.params.database &&
              pretask.params.table
            ) {
              queryParames.datasourceId = pretask.params.datasourceId
              queryParames.database = pretask.params.database
              queryParames.tableName = pretask.params.table
            } else if (
              pretask.params.datasourceId &&
              pretask.params.table_path
            ) {
              queryParames.datasourceId = pretask.params.datasourceId
              queryParames.database = pretask.params.table_path.split('.')[0]
              queryParames.tableName = pretask.params.table_path.split('.')[1]
            }
            if (
              !(contrastStr && JSON.stringify(queryParames) === contrastStr)
            ) {
              dagStore.setContrastStr(JSON.stringify(queryParames))
              model.include_fields = []
              fieldArr.value = []
            }
            // 列举需要走获取列表的接口
            let typeFtechType = ['JDBC_SOURCE','MYSQL_SOURCE','GAUSSDB_SOURCE','POSTGRESQL_SOURCE','DAMENG_SOURCE','DORIS_SOURCE']
            console.log(pretask, 'pretask')
            if (!typeFtechType.includes(pretask.taskType)) {
              isCustomAddTable.value = true
              queryParames.tableName = pretask.name
              return
            }

            getColumnList(
              Number(queryParames.datasourceId),
              queryParames.database,
              queryParames.tableName
            )
          }
        },
        { deep: true, immediate: true }
      )
    }
  }

  const getColumnList = async (
    datasourceId: number,
    database: string,
    tableName: string
  ) => {
    const listRes = await getDatasourceTableColumnDetailsById(
      {
        datasourceId,
        database,
        tableName
      }
    )
    if (listRes && listRes.length) {
      listRes.forEach((item: any) => {
        for (let i = 0; i < model.include_fields.length; i++) {
          if (item.column_name === model.include_fields[i].field) {
            model.include_fields[i].retain = fieldArr.value.includes(
              item.column_name
            )
              ? [true]
              : null
            return
          }
        }
        model.include_fields.push({
          field: item.column_name,
          fieldType: item.type_name,
          fieldDes: item.column_name,
          retain: fieldArr.value.includes(item.column_name) ? [true] : null,
          hiddenDel: true
        })
      })
      if (stopWatch) stopWatch()
    }
  }
  // resetData()
  onMounted(() => {
    getDataSourceList()
  })

  onUnmounted(() => {
    if (stopWatch) stopWatch()
  })
  return [
    ...useNormalize(model, fieldsSpan, functionOptions),
    ...useFilterParams({
      model,
      field: 'include_fields',
      isSimple: true,
      name: '原表字段',
      span: fieldsSpan
    }),
    {
      type: 'button',
      field: 'nextButton',
      name: '',
      span: nextBtnSpan,
      offset: 21,
      props: {
        type: 'primary',
        onClick: () => {
          dialogStore.setStep(0)
          model.include_fields = model.include_fields.map((item:any) => {
            item.retain = null
            return item
          })
        }
      },
      slots: {
        default: () => '新增',
      }
    },
    {
      type: 'dict-transformers',
      field: 'columns',
      name: t('字典配置列表'),
      span: moduleSpan
    }
  ]
}
