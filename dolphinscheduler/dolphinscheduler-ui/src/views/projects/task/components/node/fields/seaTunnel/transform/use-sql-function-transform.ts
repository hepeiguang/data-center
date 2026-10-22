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
import { useMessage } from 'naive-ui'
import { useFilterParams } from '../..'
import type { IJsonItem } from '../../../types'
import {
  queryDataSourceListPaging,
  getDatasourceTableColumnDetailsById
} from '@/service/modules/data-source'
import { queryFunctionInfo } from '@/service/modules/sql-function'
import { useDagStore } from '@/store/project/dynamic/dag'
import { useDialogStore } from '@/store/project/dynamic/dialog'
export function useSqlFunctionTransform(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  const message = useMessage()
  const dagStore = useDagStore()
  const dialogStore = useDialogStore()
  const functionOptions = ref([] as any)
  functionOptions.value = []
  const options = ref(
    [] as { label: string; value: number; connectionParams?: any }[]
  )
  const fieldsSpan = computed(() =>
    !!model.preTasks[0] ? (model.activeModule === 'first' ? 24 : 0) : 0
  )
  const moduleSpan = computed(() => (model.activeModule === 'first' ? 0 : (model.include_fields.length > 0 ? 24 : 0)))
  const nextBtnSpan = computed(() => ((model.include_fields.length > 0 && model.activeModule !== 'first') ? 2 : 0))
  const contrastStr = dagStore.getContrastStr
  const fieldArr = ref([] as any)
  const queryParames = {
    datasourceId: '',
    database: '',
    tableName: ''
  }
  let stopWatch: (() => void) | null = null

  watch(
    () => dialogStore.step,
    (newValue, oldValue) => {
      if (newValue === 1) {
        // if (model.activeModule === 'first' && !model.useFunction) {
        //   message.warning('请选择函数')
        //   return
        // }
        // const selectedArr = model.include_fields.filter((item: any) => {
        //   return item.retain && item.retain.length > 0 && !!item.retain[0]
        // })
        // if (model.activeModule === 'first' && selectedArr.length === 0) {
        //   message.warning('请至少选择一个字段')
        //   return
        // }

        if (model.activeModule === 'first') {
          try {
            const foundItem = findItemByFunctionCode(functionOptions.value, model.useFunction);
            const hasParam = foundItem?.hasParam ?? true; // 默认true处理

            // 1. 使用reduce替代forEach+push，直接生成带去重的新数组
            const uniqueColumnsMap = model.include_fields.reduce((acc:any, item:any) => {
              if (item.retain?.[0]) {
                const key = `${item.field}_${model.useFunction}`; // 创建唯一键
                if (!acc.has(key)) {
                  acc.set(key, {
                    table_name: queryParames.tableName,
                    column_name: item.field,
                    function_code: model.useFunction,
                    new_name: item.fieldDes,
                    hasParam
                  });
                }
              }
              return acc;
            }, new Map<string, any>()); // 使用Map存储唯一值

            // 2. 合并新旧列（Map直接转数组）
            model.columns = [...model.columns, ...uniqueColumnsMap.values()];

            // 3. 最终去重（应对可能的历史重复数据）
            const finalUniqueKeys = new Set();
            model.columns = model.columns.filter((item:any) => {
              const key = `${item.column_name}_${item.function_code}`;
              if (!finalUniqueKeys.has(key)) {
                finalUniqueKeys.add(key);
                return true;
              }
              return false;
            });

            model.activeModule = 'second';
            dialogStore.setDialogCanNext(false);

          } catch (error) {
            console.error('处理函数时出错:', error);
            // 错误处理逻辑...
          }
        }
        // 在这里处理变化
      } else {
        model.activeModule = 'first'
        dialogStore.setDialogCanNext(true)
      }
    }
  )


  interface FunctionItem {
    id?: number;
    functionType?: any;
    typeName: string;
    functionName: string;
    functionCode: string;
    // 其他可能的属性...
    [key: string]: any; // 如果还有其他不确定的属性
  }

  interface FunctionGroup {
    label: string;
    value: string;
    children?: FunctionItem[];
  }

  const findItemByFunctionCode = (data: FunctionGroup[], targetCode: string): FunctionItem | undefined => {
    return data.flatMap((group: FunctionGroup) => group.children || [])
      .find((item: FunctionItem) => item.functionCode === targetCode);
  };


  const getDataSourceList = async () => {
    functionOptions.value = await queryFunctionInfo()
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
            // if (
            //   !(contrastStr && JSON.stringify(queryParames) === contrastStr)
            // ) {
            //   dagStore.setContrastStr(JSON.stringify(queryParames))
            //   model.include_fields = []
            //   fieldArr.value = []
            // }
            // llxl llxl
            // getColumnList(
            //   Number(queryParames.datasourceId),
            //   queryParames.database,
            //   queryParames.tableName 
            // )
            if (pretask && pretask.params && pretask.params.outputFields && pretask.params.outputFields.length) {
              pretask.params.outputFields[0].fields.forEach((item: any) => {
                // fieldOptions.value.push({ label: item, value: item })
                for (let i = 0; i < model.include_fields.length; i++) {
                  if (item === model.include_fields[i].field) {
                    model.include_fields[i].retain = fieldArr.value.includes(
                      item
                    )
                      ? [true]
                      : null
                    return
                  }
                }
                model.include_fields.push({
                  field: item,
                  fieldType: item,
                  fieldDes: item,
                  retain: fieldArr.value.includes(item) ? [true] : null,
                  hiddenDel: true
                })
              })
              if (stopWatch) stopWatch()
            }
          }
        },
        { deep: true, immediate: true }
      )
    }
  }

  // const getColumnList = async (
  //   datasourceId: number,
  //   database: string,
  //   tableName: string
  // ) => {
  //   const listRes = await getDatasourceTableColumnDetailsById(
  //     { datasourceId,
  //     database,
  //     tableName }
  //   )
  //   if (listRes && listRes.length) {
  //     listRes.forEach((item: any) => {
  //       for (let i = 0; i < model.include_fields.length; i++) {
  //         if (item.column_name === model.include_fields[i].field) {
  //           model.include_fields[i].retain = fieldArr.value.includes(
  //             item.column_name
  //           )
  //             ? [true]
  //             : null
  //           return
  //         }
  //       }
  //       console.log(1111)
  //       model.include_fields.push({
  //         field: item.column_name,
  //         fieldType: item.type_name,
  //         fieldDes: item.column_name,
  //         retain: fieldArr.value.includes(item.column_name) ? [true] : null,
  //         hiddenDel: true
  //       })
  //     })
  //     if (stopWatch) stopWatch()
  //   }
  // }

  onMounted(() => {
    getDataSourceList()
  })

  onUnmounted(() => {
    if (stopWatch) stopWatch()
  })
  return [
    {
      type: 'tree-select',
      field: 'useFunction',
      name: t('函数选择'),
      span: fieldsSpan,
      options: functionOptions,
      props: {
        overrideDefaultNodeClickBehavior: true,
        keyField: 'value',
        labelField: 'label'
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(validate: any, value: string) {
          if (!value) {
            return new Error(t('请选择函数'))
          }
        }
      }
    },
    ...useFilterParams({
      model,
      field: 'include_fields',
      isSimple: true,
      name: '原表字段',
      span: fieldsSpan,
      hideType: true
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
        default: () => '新增'
      }
    },
    {
      type: 'sql-transformers',
      field: 'columns',
      name: t('函数配置列表'),
      span: moduleSpan
    }
  ]
}
