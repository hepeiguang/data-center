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

import { defineComponent, defineAsyncComponent } from 'vue'
import { useMessage } from 'naive-ui'
// import Dag from '../../components/dag'
import { DynamicDag } from '@/views/projects/workflow/components/dynamic-dag'
import { useThemeStore } from '@/store/theme/theme'
import { useRoute, useRouter } from 'vue-router'
import {
  SaveForm,
  TaskDefinition,
  Connect,
  Location
} from '../../components/dag/types'
import { createProcessDefinition, jgCreateProcessDefinition } from '@/service/modules/process-definition'
import { useI18n } from 'vue-i18n'
import Styles from './index.module.scss'

interface SaveData {
  saveForm: SaveForm
  taskDefinitions: TaskDefinition[]
  connects: Connect[]
  locations: Location[]
}

export default defineComponent({
  name: 'WorkflowDefinitionCreate',
  setup() {
    const Dag = defineAsyncComponent(() => 
      import('@/views/projects/workflow/components/dag')
    )
    const theme = useThemeStore()
    const message = useMessage()
    const { t } = useI18n()
    const route = useRoute()
    const router = useRouter()
    const projectCode = Number(route.params.projectCode)

    // 增加给基改使用内容
    // 查看路由跳转是否有类型参数
    const queryParamsType = route.query.type
    const onSave = ({
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
      let params = {
        taskDefinitionJson: JSON.stringify(taskDefinitions),
        taskRelationJson: JSON.stringify(connects),
        locations: JSON.stringify(locations),
        name: saveForm.name,
        executionType: saveForm.executionType,
        description: saveForm.description,
        globalParams: JSON.stringify(globalParams),
        timeout: saveForm.timeoutFlag ? saveForm.timeout : 0,
        projectCode
      }
      // 基改项目需要走的上传接口  如果存在类型 则
      if (queryParamsType) {
        // 处理下数据
        // params.projectCode = projectCode
        let jgparams: any = {
          "policyName": saveForm.name,
          "taskType": saveForm.taskType,
          "description": saveForm.description,
          "type": queryParamsType == "stategy" ? 0 : 1,
          "workflow": params
        }

        jgCreateProcessDefinition(
          jgparams
        ).then((ignored: any) => {
          message.success(t('project.dag.success'))
        }).finally(() => {
          window.parent.postMessage('clone', '*')
        })
        return
      }
      // 原始数据 上传
      createProcessDefinition(
        params,
        projectCode
      ).then((ignored: any) => {
        message.success(t('project.dag.success'))
        router.push({ path: `/projects/${projectCode}/workflow-definition` })
      })
    }

    return () => (
      <div
        class={[
          Styles.container,
          theme.darkTheme ? Styles['dark'] : Styles['light']
        ]}
      >
        {route.query.dynamic === 'true' ? (
          <DynamicDag />
        ) : (
          <Dag projectCode={projectCode} externalParam={queryParamsType} onSave={onSave} />
        )}
      </div>
    )
  }
})
