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
import { ref, onMounted, onUnmounted,reactive, computed, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../../../types'
import { useUdfs } from '../../use-udfs'
import { useDagStore } from '@/store/project/dynamic/dag'

export function useSplitcolumnsTransform(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  const dagStore = useDagStore()
  const fieldOptions = ref([] as { label: string; value: number; connectionParams?: any }[])
  const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))
  const fieldsSpansm = computed(() => (!!model.preTasks[0] ? 12 : 0))

  
  let stopWatch: (() => void) | null = null
  
  onMounted(() => {
    stopWatch = watch(model.output_field, (newValue, oldValue) => {
      if (!model.preTasks[0]) {
          return
      }
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
      model.table_name = queryParames.tableName
      if (pretask.params && pretask.params.outputFields && pretask.params.outputFields.length && fieldOptions.value.length ==0) {
        pretask.params.outputFields[0].fields.forEach((item: any) => {
            fieldOptions.value.push({ label: item, value: item })
        })
        if (stopWatch) stopWatch()
      }
  
    }, { deep: true, immediate: true })

  })
  onUnmounted(() => {
    if (stopWatch) stopWatch()
  })

  return [
    {
      type: 'select',
      field: 'splitField',
      span:fieldsSpan,
      name: t(`要分拆的字段`),
      options: fieldOptions,
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
      type: 'input',
      field: 'separator',
      span: fieldsSpansm,
      name: t(`分隔符`),
      props: {
        placeholder: t(';'),
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
      type: 'checkbox',
      field: 'regex',
      name: t('分隔符是一个正则表达式'),
      span: fieldsSpansm
    },
    {
      type: 'input',
      field: 'newField',
      span:fieldsSpan,
      name: t(`新字段名`),
      props: {
        placeholder: t('新字段名'),
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
    // //////////////////////////////////
    useUdfs(model)
  ]
}
