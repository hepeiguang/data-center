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

import { ref, onMounted, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { listServiceTemplate } from '@/service/modules/process-definition'
import type { IEnvironmentNameOption, IJsonItem } from '../types'

export function useTemplateSelect(
  model: { [field: string]: any },
  isCreate: boolean
): IJsonItem {
  const { t } = useI18n()

  let environmentList = [] as IEnvironmentNameOption[]
  const options = ref([] as IEnvironmentNameOption[])
  const loading = ref(false)
  let mounted = false

  const getEnvironmentList = async () => {
    if (loading.value) return
    loading.value = true
    const res = await listServiceTemplate()
    const { value } = res
    environmentList = value.map(
      (item: { id: string; name: string; serviceApi: any }) => ({
        label: item.name,
        value: item.id,
        serviceApi: item.serviceApi
      })
    )
    options.value = environmentList
  }

  watch(
    () => model.templateId,
    () => {
        if (!environmentList.length) {
          return
        }
        let obj = environmentList.find(item => {
          if (item.value === model.templateId) {
            return item
          }
        })
        const { serviceApi }:any = obj || {};
        model.url = serviceApi && serviceApi.url || ''
        model.httpMethod = serviceApi && serviceApi.method || ''
        model.httpBody = serviceApi && serviceApi.requestBody || ''
        // model.httpParams
        const paramsJsonArray = JSON.parse(serviceApi.paramsJson)
        model.httpParams = paramsJsonArray.map((item: any) => {
          return {
            httpParametersType: item.type,
            prop: item.name,
            value: item.paramValue,
          }
        })
        // console.log(JSON.parse(serviceApi.paramsJson), "serviceApi.paramsJson")
        // console.log(serviceApi,  model.httpParams, 'serviceApi')
        model.otherParams = [
          {
            prop: 'templateId',
            value: model.templateId
          }
        ]
      
    }
  )

  onMounted(async () => {
    await getEnvironmentList()
    mounted = true
  })

  return {
    type: 'select',
    field: 'templateId',
    class: 'env-select',
    // span: 12,
    name: t('project.node.template_type'),
    props: {
      // loading: loading,
      clearable: true
    },
    
    options: options
  }
}
