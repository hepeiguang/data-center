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

import { defineComponent, onMounted, ref, defineAsyncComponent } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useThemeStore } from '@/store/theme/theme'
import { useMessage } from 'naive-ui'
import { useI18n } from 'vue-i18n'
// import Dag from '../../components/dag'
import {
  queryProcessDefinitionByCode,
  updateProcessDefinition,
  jgUpdateProcessDefinition,
  getDetailByDsWdCode
} from '@/service/modules/process-definition'
import {
  WorkflowDefinition,
  SaveForm,
  TaskDefinition,
  Connect,
  Location
} from '../../components/dag/types'
import Styles from './index.module.scss'
import { useGraphAutoLayout } from '../../components/dag/use-graph-auto-layout'

interface SaveData {
  saveForm: SaveForm
  taskDefinitions: TaskDefinition[]
  connects: Connect[]
  locations: Location[]
}

export default defineComponent({
  name: 'WorkflowDefinitionDetails',
  setup(props, context) {
    const Dag = defineAsyncComponent(() => 
      import('@/views/projects/workflow/components/dag')
    )
    const theme = useThemeStore()
    const route = useRoute()
    const router = useRouter()
    const message = useMessage()
    const { t } = useI18n()
    const projectCode = Number(route.params.projectCode)
    const code = Number(route.params.code)

    const definition = ref<WorkflowDefinition>()
    const readonly = ref(false)
    const isLoading = ref(true)
    const dagRef = ref()
     // 增加给基改使用内容
    // 查看路由跳转是否有类型参数
    const queryParamsType = route.query.type
    const jgQueryId = route.query.id
    const refresh = () => {
      isLoading.value = true
      if (queryParamsType) {
        getDetailByDsWdCode(code, projectCode).then((resp: any) => {
          const res = resp.value;
          readonly.value = res.processDefinition.releaseState === 'ONLINE'
          res.processDefinition.taskType = res.policy.taskType
          res.processDefinition.templateId = res.policy.dsWdCode
          definition.value = res
          isLoading.value = false
          if (!res.processDefinition.locations) {
            setTimeout(() => {
              const graph = dagRef.value
              const { submit } = useGraphAutoLayout({ graph })
              submit()
            }, 1000)
          }
        })
      } else {
         queryProcessDefinitionByCode(code, projectCode).then((res: any) => {
          readonly.value = res.processDefinition.releaseState === 'ONLINE'
          definition.value = res
          isLoading.value = false
          if (!res.processDefinition.locations) {
            setTimeout(() => {
              const graph = dagRef.value
              const { submit } = useGraphAutoLayout({ graph })
              submit()
            }, 1000)
          }
        })
      }
    }
   
    console.log(route, 'route')
    const save = ({
      taskDefinitions,
      saveForm,
      connects,
      locations
    }: SaveData) => {
      const globalParams = saveForm.globalParams.map((p) => {
        return {
          prop: p.key,
          value: p.value,
          direct: p.direct,
          type: 'VARCHAR'
        }
      })
      const params:any = {
        taskDefinitionJson: JSON.stringify(taskDefinitions),
        taskRelationJson: JSON.stringify(connects),
        locations: JSON.stringify(locations),
        name: saveForm.name,
        executionType: saveForm.executionType,
        description: saveForm.description,
        globalParams: JSON.stringify(globalParams),
        timeout: saveForm.timeoutFlag ? saveForm.timeout : 0,
        releaseState: saveForm.release ? 'ONLINE' : 'OFFLINE',
        projectCode,
        code,
      }
      if (queryParamsType) {
        // 处理下数据
        let jgparams:any = {
          "id": jgQueryId,
          "policyName": saveForm.name,
          "taskType": saveForm.taskType,
          "description": saveForm.description,
          "type": queryParamsType == "stategy" ? 0 : 1,
          "workflow": params
        }
        jgUpdateProcessDefinition(
          jgparams
        ).then((ignored: any) => {
          message.success(t('project.dag.success'))
          
        }).finally(() => {
          window.parent.postMessage('clone', '*')
        })
        return
      }
      updateProcessDefinition(
        params,
        code,
        projectCode
      ).then((ignored: any) => {
        message.success(t('project.dag.success'))
        router.push({ path: `/projects/${projectCode}/workflow-definition` })
      })
    }

    context.expose(dagRef)
    onMounted(() => {
      console.log('llxl 详情页面挂载')
      if (!code || !projectCode) return
      refresh()
    })

    return () => (
      <div
        class={[
          Styles.container,
          theme.darkTheme ? Styles['dark'] : Styles['light']
        ]}
      >
        {!isLoading.value && (
          <Dag
            externalParam={queryParamsType}
            ref={dagRef}
            definition={definition.value}
            onRefresh={refresh}
            projectCode={projectCode}
            onSave={save}
            readonly={readonly.value}
          />
        )}
      </div>
    )
  }
})
