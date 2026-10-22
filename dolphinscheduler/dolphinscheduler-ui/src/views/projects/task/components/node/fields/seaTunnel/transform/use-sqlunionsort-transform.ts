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
import { ref, reactive, onMounted, onUnmounted, computed, watch, watchEffect, nextTick, h } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
import { useUdfs } from '../../use-udfs'
import { useDagStore } from '@/store/project/dynamic/dag'
import {
  queryDataSourceListPaging,
  getDatasourceTablesById,
  getDatasourceTableColumnDetailsById,
  queryDataSource
} from '@/service/modules/data-source'
export function useSqlunionsortTransform(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  const dagStore = useDagStore()
  const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))
  const contrastStr = dagStore.getContrastStr
  const sortOptions = ref([] as { label: string; value: number; connectionParams?: any }[])
  const fieldOptions = ref([] as { label: string; value: any; }[])
  
  const options = ref(
    [] as { label: string; value: number; connectionParams?: any }[]
  )
  const optionslist = ref({})
  const tableOptions = ref([] as { label: string; value: string }[])
  const tableFieldOptions = ref([] as { label: string; value: string }[])
  const showColumns = computed(() => {
    let temp = []
    tableFieldOptions.value.forEach(item => {
        temp.push({label:  item.label,value: item.value, disabled: fieldsSelectedColumns.value.includes(item.value)})
    })
    return temp
  })

  const fieldsSelectedColumns = computed(() => {
    let temp = []
    model.fields.forEach(item => {
        temp.push(item.fieldName)
    })
    return temp
  })

  let stopWatch: (() => void) | null = null
  const fieldArr = ref([] as any)
   const filterOptions = [
    {
      label: '',
      value: true
    }
  ]

  const getDataSourceList = async () => {

    const listRes = await queryDataSourceListPaging({
      pageNo: 1,
      pageSize: 100
    })
    if (listRes.totalList && listRes.totalList.length) {
      listRes.totalList.forEach((item: any) => {
        if (item.type == 'KAFKA') {
          return
        }
        options.value.push({
          label: item.name,
          value: item.id,
          connectionParams: JSON.parse(item.connectionParams)
        })
      })
    }
    console.log('options>>>', options)
    const taskDefinitionList = dagStore.getDagTaskDefinitionList
    const pretask = taskDefinitionList.find((task) => {
      return task.code == model.preTasks[0]
    })
    if(pretask){
      const activeSource = options.value.find(
        (source) => source.value === pretask.taskParams.datasourceId
      )
      model.datasourceId = pretask.taskParams.datasourceId
        console.log('model>>>',model.datasourceId)
        await List()
      const database = activeSource?.connectionParams.database
      getTableList(pretask.taskParams.datasourceId, database)
    }
  }
  watch(
      () => model.tableName,
      (value,ord) => {
        if (value) {
          const newValue = value.split('.')
          const data = {
            database: optionslist.value.database,
            datasourceId: optionslist.value.id,
            tableName: newValue[1]
          }
          if(ord){
          model.fields = []
          model.sortFields = []}
          getTablefields(data)
        }
      }
    )
  const getTablefields = async (params: any) => { 
    if(params.database && params.datasourceId && params.tableName){
    const listRes = await getDatasourceTableColumnDetailsById(params)
     if (listRes && listRes.length) {
      console.log(11111111)
      tableFieldOptions.value = []
      listRes.forEach((item: any) => {
         tableFieldOptions.value.push({
            label: item.column_name,
            value: item.column_name
          })
        })
     }}
  }
  const List = async () => {
      if (!model.datasourceId) {
        return
      }
      const listRes = await queryDataSource(model.datasourceId)
      if (listRes) {
        optionslist.value = listRes
        const newValue = model.tableName.split('.')
         const data = {
            database: optionslist.value.database,
            datasourceId: optionslist.value.id,
            tableName: newValue[1]
          }
          getTablefields(data)

      }}
  const getTableList = async (datasourceId: number, database: string) => {
    const listRes = await getDatasourceTablesById(datasourceId, database)
    if (listRes && listRes.length) {
      tableOptions.value = []
      listRes.forEach((item: any) => {
        tableOptions.value.push({
          label: item.label,
          value: database + '.' + item.value
        })
      })
    }
  }

  onMounted(() => {
    getDataSourceList()
  })

  return [
    {
      type: 'select',
      field: 'tableName',
      name: t('表名'),
      options: tableOptions,
      span: fieldsSpan,
      props: {
        placeholder: t('请选择表名'),
        'on-update:value': (value: string) => {
          model.tableName = value
        }
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
      type: 'custom-parameters',
      field: 'fields',
      name: t('合并字段'),
      class: 'btn-custom-parameters',
      span:fieldsSpan,
      children: [
        
         {
          type: 'select',
          field: 'fieldName',
          span: 7,
          options: showColumns,
          value: '',
          props: {
            // disabled: isSimple
            placeholder: t('表字段'),
          },
           validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填'))
              }
               const duplicates = model.fields.filter((item:any) => item.fieldName === value);
              if (duplicates.length >= 2) {
                  return new Error(t('字段已存在'));
              }
            }
          }
        },
        {
          type: 'input',
          field: 'aliasName',
          span: 7,
          class: 'input-param-key',
          props: {
            placeholder: t('字段别名'),
            maxLength: 256
          },
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (!value) {
                return new Error(t('该字段为必填'))
              }
               const duplicates = model.fields.filter((item:any) => item.aliasName === value);
              if (duplicates.length >= 2) {
                  return new Error(t('字段已存在'));
              }
            }
            
          }
        },
        {
          type:'select',
          field: 'isSelect',
          value:true,
          options:
           [{label: '选择字段',  value: true}
            ,{label: '不选择字段',  value: false}
           ] ,
           props: {
            placeholder: t('字段是否选择'),
            maxLength: 256
          },
          span: 7,
          validate: {
            trigger: ['input', 'blur'],
            required: true,
            validator(validate: any, value: string) {
              if (value !== true && value !== false) {
                return new Error(t('该字段为必填'))
              }
            }
          }
        },
        // {
        //   type: 'checkbox',
        //   field: 'isSelect',
        //   value: [true],
        //   options: filterOptions,
        //   single: true,
        //   span: 2,
        // },
        
      ]
    },
     {
            type: 'custom-parameters',
            field: 'sortFields',
            name: t('排序规则'),
            class: 'btn-custom-parameters',
            span:fieldsSpan,
            children: [
               {
                    type: 'select',
                    field: 'fieldName',
                    span: 7,
                    options: tableFieldOptions,
                    value: '',
                    props: {
                        // disabled: isSimple
                        placeholder: t('表字段'),
                    },
                    validate: {
                        trigger: ['input', 'blur'],
                        required: true,
                        validator(validate: any, value: string) {
                        if (!value) {
                            return new Error(t('该字段为必填'))
                        }
                         const duplicates = model.sortFields.filter((item:any) => item.fieldName === value);
                          if (duplicates.length >= 2) {
                              return new Error(t('字段已存在'));
                          }
                        }
                    }
                },
                {
                    type: 'select',
                    field: 'sortType',
                    span: 7,
                    props: {
                        clearable: true,
                        placeholder: t('排序方式')
                    },
                    options: [
                        { value: 'ASC', label: t('升序') },
                        { value: 'DESC', label: t('降序') }
                    ],
                    validate: {
                        trigger: ['input', 'blur'],
                        required: true,
                        validator(validate: any, value: string) {
                        if (!value) {
                            return new Error(t('该字段为必填'))
                        }
                        }
                    }
                }
            ]
            }

  ]
}
