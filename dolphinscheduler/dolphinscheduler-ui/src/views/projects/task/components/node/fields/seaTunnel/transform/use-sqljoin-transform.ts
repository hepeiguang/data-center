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
import { useSqljoinParams } from '../../use-sqljoin-params'
import type { IJsonItem } from '../../../types'
import { useRoute } from 'vue-router'
import {
  queryDataSourceListPaging,
  getDatasourceTableColumnDetailsById
} from '@/service/modules/data-source'
import { useDagStore } from '@/store/project/dynamic/dag'

export function useSqljoinTransform(model: { [field: string]: any }): IJsonItem[] {
  console.log('model', model)
  const { t } = useI18n()
  const route = useRoute()
  const dagStore = useDagStore()
  const options = ref(
    [] as { label: string; value: number; connectionParams?: any }[]
  )
  const fieldsSpan = computed(() => (!!model.preTasks[0] ? 24 : 0))
  const contrastStr = dagStore.getContrastStr
  let stopWatch: (() => void) | null = null
  const fieldArr = ref([] as any)
  const Fields = ref([])
  const taskDefinitionList = dagStore.getDagTaskDefinitionList
  watch(
    () => model,
    () => {
      if (!model.preTasks[0]) {
        return
      }
      // const taskDefinitionList = dagStore.getDagTaskDefinitionList
      const pretask = taskDefinitionList.find((task) => {
        return task.code == model.preTasks[0]
      })
      model.datasourceId = pretask.taskParams.datasourceId
      Fields.value = pretask.taskParams.outputFields
    },
    {
      deep: true
    }
  )
  onMounted(() => {
    // getDataSourceList()
  })
  return [
    ...useSqljoinParams({
      model,
      Fields,
      span: fieldsSpan,
    })
  ]
}
