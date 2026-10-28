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

import type { Cell, Graph } from '@antv/x6'
import {
  defineComponent,
  ref,
  provide,
  PropType,
  toRef,
  watch,
  onBeforeUnmount,
  computed,
  reactive
} from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import DagToolbar from './dag-toolbar'
import DagCanvas from './dag-canvas'
import DagSidebar from './dag-sidebar'
import Styles from './dag.module.scss'
import DagAutoLayoutModal from './dag-auto-layout-modal'
import {
  useGraphAutoLayout,
  useGraphBackfill,
  useDagDragAndDrop,
  useTaskEdit,
  useBusinessMapper,
  useNodeMenu,
  useNodeStatus
} from './dag-hooks'
import { useThemeStore } from '@/store/theme/theme'
import VersionModal from '../../definition/components/version-modal'
import { WorkflowDefinition, WorkflowInstance } from './types'
import DagSaveModal from './dag-save-modal'
import ContextMenuItem from './dag-context-menu'
import TaskModal from '@/views/projects/task/components/node/detail-modal'
import StartModal from '@/views/projects/workflow/definition/components/start-modal'
import LogModal from '@/components/log-modal'
import './x6-style.scss'
import { queryLog } from '@/service/modules/log'
import { useAsyncState } from '@vueuse/core'
import utils from '@/utils'
import { executeTask } from '@/service/modules/executors'
import DependenciesModal from '@/views/projects/components/dependencies/dependencies-modal'
import {
  assembleStockSeaTunnelConfig,
  buildDefaultConnects,
  isSeaTunnelChildType,
  isSeaTunnelComputeParent,
  parseSeaTunnelChildType,
  type StComputeNode
} from './seatunnel-compute-utils'

const props = {
  // If this prop is passed, it means from definition detail
  instance: {
    type: Object as PropType<WorkflowInstance>,
    default: undefined
  },
  definition: {
    type: Object as PropType<WorkflowDefinition>,
    default: undefined
  },
  readonly: {
    type: Boolean as PropType<boolean>,
    default: false
  },
  projectCode: {
    type: Number as PropType<number>,
    default: 0
  }
}

export default defineComponent({
  name: 'workflow-dag',
  props,
  emits: ['refresh', 'save'],
  setup(props, context) {
    const { t } = useI18n()
    const route = useRoute()
    const theme = useThemeStore()

    // Whether the graph can be operated
    provide('readonly', toRef(props, 'readonly'))

    const graph = ref<Graph>()
    provide('graph', graph)
    context.expose(graph)

    // Auto layout modal
    const {
      visible: layoutVisible,
      toggle: layoutToggle,
      formValue,
      formRef,
      submit,
      cancel
    } = useGraphAutoLayout({ graph })

    // Edit task
    const {
      taskConfirm,
      taskModalVisible,
      currTask,
      taskCancel,
      appendTask,
      editTask,
      copyTask,
      workflowDefinition,
      removeTasks
    } = useTaskEdit({ graph, definition: toRef(props, 'definition') })

    // Right click cell
    const { nodeVariables, menuHide, menuStart, viewLog } = useNodeMenu({
      graph
    })

    // start button in the dag node menu
    const startDisplay = computed(() => {
      if (props.definition) {
        return (
          route.name === 'workflow-definition-detail' &&
          props.definition!.workflowDefinition.releaseState === 'ONLINE'
        )
      } else {
        return false
      }
    })

    // execute task buttons in the dag node menu
    const executeTaskDisplay = computed(() => {
      return route.name === 'workflow-instance-detail'
    })

    // other button in the dag node menu
    const menuDisplay = computed(() => {
      if (props.instance) {
        return (
          props.instance.state === 'SUCCESS' ||
          props.instance.state === 'PAUSE' ||
          props.instance.state === 'FAILURE' ||
          props.instance.state === 'STOP'
        )
      } else if (props.definition) {
        return props.definition!.workflowDefinition.releaseState === 'OFFLINE'
      } else {
        return false
      }
    })

    const taskInstance = computed(() => {
      if (nodeVariables.menuCell) {
        const taskCode = Number(nodeVariables.menuCell!.id)
        return taskList.value.find((task: any) => task.taskCode === taskCode)
      } else {
        return undefined
      }
    })

    const currentTaskInstance = ref()

    watch(
      () => taskModalVisible.value,
      () => {
        if (props.instance && taskModalVisible.value) {
          const taskCode = currTask.value.code
          currentTaskInstance.value = taskList.value.find(
            (task: any) => task.taskCode === taskCode
          )
        }
      }
    )

    const statusTimerRef = ref()
    const { taskList, refreshTaskStatus } = useNodeStatus({ graph })

    const { onDragStart, onDrop } = useDagDragAndDrop({
      graph,
      readonly: toRef(props, 'readonly'),
      appendTask
    })

    // backfill
    useGraphBackfill({ graph, definition: toRef(props, 'definition') })

    // version modal
    const versionModalShow = ref(false)
    const versionToggle = (bool: boolean) => {
      if (typeof bool === 'boolean') {
        versionModalShow.value = bool
      } else {
        versionModalShow.value = !versionModalShow.value
      }
    }
    const refreshDetail = () => {
      context.emit('refresh')
      versionModalShow.value = false
    }

    // Save modal
    const saveModalShow = ref(false)
    const saveModelToggle = (bool: boolean) => {
      if (typeof bool === 'boolean') {
        saveModalShow.value = bool
      } else {
        saveModalShow.value = !versionModalShow.value
      }
    }
    const { getConnects, getLocations } = useBusinessMapper()
    const onSave = (saveForm: any) => {
      const edges = graph.value?.getEdges() || []
      const nodes = graph.value?.getNodes() || []
      if (!nodes.length) {
        window.$message.error(t('project.dag.node_not_created'))
        saveModelToggle(false)
        return
      }
      const connects = getConnects(
        nodes,
        edges,
        workflowDefinition.value.taskDefinitionList as any
      )

      const parentTaskIds: string[] = []
      const subTaskIds: string[] = []
      const parent2Sub: Record<
        string,
        { subIds: string[]; connects: any[] }
      > = {}
      const sub2Parent: Record<string, string> = {}

      nodes.forEach((item: any) => {
        if (
          item.shape === 'dag-task' &&
          item.children &&
          item.children.length &&
          isSeaTunnelComputeParent(item.data?.taskType)
        ) {
          parentTaskIds.push(item.id)
          parent2Sub[item.id] = { subIds: [], connects: [] }
          item.children.forEach((subItem: any) => {
            if (subItem.shape === 'dag-task') {
              subTaskIds.push(subItem.id)
              parent2Sub[item.id].subIds.push(subItem.id)
              sub2Parent[subItem.id] = item.id
            }
          })
        }
      })

      const newConnects: any[] = []
      connects.forEach((item: any) => {
        const pre = String(item.preTaskCode)
        const post = String(item.postTaskCode)
        const preNum = Number(item.preTaskCode)
        const postNum = Number(item.postTaskCode)
        if (subTaskIds.indexOf(pre) < 0 && subTaskIds.indexOf(post) < 0) {
          newConnects.push(item)
        } else {
          // Skip DS placeholder edges (preTaskCode=0) — they break SeaTunnel chaining
          if (!(preNum > 0 && postNum > 0)) return
          const pId = sub2Parent[pre] || sub2Parent[post]
          if (pId && parent2Sub[pId]) {
            parent2Sub[pId].connects.push(item)
          }
        }
      })

      const taskDefinitionObj: Record<string, any> = {}
      workflowDefinition.value.taskDefinitionList.forEach((item: any) => {
        taskDefinitionObj[String(item.code)] = item
      })

      const newTaskDefinitions: any[] = []
      workflowDefinition.value.taskDefinitionList.forEach((item: any) => {
        if (isSeaTunnelComputeParent(item.taskType)) {
          if (parentTaskIds.indexOf(String(item.code)) >= 0) {
            const res = { ...item }
            const nodesGraph: StComputeNode[] = []
            let env: Record<string, any> = {
              'job.mode': 'BATCH',
              parallelism: 2
            }
            try {
              if (res.taskParams?.stComputeGraph) {
                const g = JSON.parse(res.taskParams.stComputeGraph)
                env = { ...env, ...(g.env || {}) }
              }
            } catch {
              /* keep default env */
            }

            let groupConnects =
              parent2Sub[String(item.code)].connects || []
            parent2Sub[String(item.code)].subIds.forEach((subId: string) => {
              const subItem = taskDefinitionObj[subId]
              if (!subItem?.taskType) return
              const { pluginName, kind } = parseSeaTunnelChildType(
                subItem.taskType
              )
              const {
                localParams,
                resourceList,
                pluginName: _pn,
                connectorType: _ct,
                ...params
              } = { ...(subItem.taskParams || {}) }
              nodesGraph.push({
                type: kind,
                pluginName,
                code: Number(subId),
                params: { ...params },
                name: subItem.name
              })
            })
            // Persist chain edges so reload shows Source→Transform→Sink links
            if (
              !groupConnects.some(
                (c: any) => Number(c.preTaskCode) > 0 && Number(c.postTaskCode) > 0
              )
            ) {
              groupConnects = buildDefaultConnects(nodesGraph)
            }

            if (!res.taskParams) res.taskParams = {}
            res.taskParams.scriptType = res.taskParams.scriptType || 'visualize'
            res.taskParams.useCustom = true
            res.taskParams.stComputeGraph = JSON.stringify({
              env,
              nodes: nodesGraph
            })
            res.taskParams.rawScript = JSON.stringify(
              assembleStockSeaTunnelConfig(env, nodesGraph, groupConnects),
              null,
              2
            )
            res.taskParams.relation = JSON.stringify(groupConnects)
            // Preserve user-configured BATCH / STREAM; default BATCH
            res.taskExecuteType = res.taskExecuteType || 'BATCH'
            newTaskDefinitions.push(res)
          } else {
            const res = { ...item }
            if (!res.taskParams) res.taskParams = {}
            if (res.taskParams.scriptType !== 'custom') {
              res.taskParams.stComputeGraph =
                res.taskParams.stComputeGraph ||
                JSON.stringify({
                  env: { 'job.mode': 'BATCH', parallelism: 2 },
                  nodes: []
                })
              res.taskParams.relation = JSON.stringify([])
            }
            newTaskDefinitions.push(res)
          }
        } else if (
          subTaskIds.indexOf(String(item.code)) < 0 &&
          !isSeaTunnelChildType(item.taskType)
        ) {
          newTaskDefinitions.push(item)
        }
      })

      const locations = getLocations(nodes)
      context.emit('save', {
        taskDefinitions: newTaskDefinitions,
        saveForm,
        connects: newConnects,
        locations
      })
      saveModelToggle(false)
    }

    const handleViewLog = (taskId: number, taskType: string) => {
      taskModalVisible.value = false
      viewLog(taskId, taskType)

      getLogs()
    }

    const getLogs = () => {
      const { state } = useAsyncState(
        queryLog({
          taskInstanceId: nodeVariables.logTaskId,
          limit: nodeVariables.limit,
          skipLineNum: nodeVariables.skipLineNum
        }).then((res: any) => {
          nodeVariables.logRef += res.message || ''
          if (res && res.message !== '') {
            nodeVariables.skipLineNum += res.lineNum
            getLogs()
          } else {
            nodeVariables.logLoadingRef = false
          }
        }),
        {}
      )

      return state
    }

    const refreshLogs = () => {
      nodeVariables.logRef = ''
      nodeVariables.limit = 1000
      nodeVariables.skipLineNum = 0
      getLogs()
    }

    const handleExecuteTask = (
      startNodeList: number,
      taskDependType: string
    ) => {
      executeTask(
        {
          workflowInstanceId: Number(route.params.id),
          startNodeList: startNodeList,
          taskDependType: taskDependType
        },
        props.projectCode
      ).then(() => {
        window.$message.success(t('project.workflow.success'))
        setTimeout(() => {
          window.location.reload()
        }, 1000)
      })
    }

    const downloadLogs = () => {
      utils.downloadFile('log/download-log', {
        taskInstanceId: nodeVariables.logTaskId
      })
    }

    const onConfirmModal = () => {
      nodeVariables.showModalRef = false
    }

    const layoutSubmit = () => {
      submit()

      // Refresh task status in workflow instance
      if (props.instance) {
        refreshTaskStatus()
      }
    }

    const dependenciesData = reactive({
      showRef: ref(false),
      taskLinks: ref([]),
      required: ref(false),
      tip: ref(''),
      action: () => {}
    })

    watch(
      () => props.definition,
      () => {
        if (props.instance) {
          refreshTaskStatus()
          statusTimerRef.value = setInterval(() => refreshTaskStatus(), 90000)
        }
      }
    )

    watch(
      () => nodeVariables.showModalRef,
      () => {
        if (!nodeVariables.showModalRef) {
          nodeVariables.row = {}
          nodeVariables.logRef = ''
          nodeVariables.logLoadingRef = true
          nodeVariables.skipLineNum = 0
          nodeVariables.limit = 1000
        }
      }
    )

    onBeforeUnmount(() => clearInterval(statusTimerRef.value))

    return () => (
      <div
        class={[
          Styles.dag,
          Styles[`dag-${theme.darkTheme ? 'dark' : 'light'}`]
        ]}
      >
        <DagToolbar
          layoutToggle={layoutToggle}
          instance={props.instance}
          definition={props.definition}
          onVersionToggle={versionToggle}
          onSaveModelToggle={saveModelToggle}
          onRemoveTasks={removeTasks}
          onRefresh={refreshTaskStatus}
          v-model:dependenciesData={dependenciesData}
        />
        <div class={Styles.content}>
          <DagSidebar onDragStart={onDragStart} />
          <DagCanvas onDrop={onDrop} />
        </div>
        <DagAutoLayoutModal
          visible={layoutVisible.value}
          submit={layoutSubmit}
          cancel={cancel}
          formValue={formValue}
          formRef={formRef}
        />
        {!!props.definition && (
          <VersionModal
            isInstance={!!props.instance}
            v-model:row={props.definition.workflowDefinition}
            v-model:show={versionModalShow.value}
            onUpdateList={refreshDetail}
          />
        )}
        <DagSaveModal
          v-model:show={saveModalShow.value}
          onSave={onSave}
          definition={props.definition}
          instance={props.instance}
        />
        <TaskModal
          readonly={props.readonly}
          show={taskModalVisible.value}
          projectCode={props.projectCode}
          workflowInstance={props.instance}
          taskInstance={currentTaskInstance.value}
          onViewLog={handleViewLog}
          data={currTask.value as any}
          definition={workflowDefinition}
          onSubmit={taskConfirm}
          onCancel={taskCancel}
        />
        <ContextMenuItem
          startDisplay={startDisplay.value}
          executeTaskDisplay={executeTaskDisplay.value}
          menuDisplay={menuDisplay.value}
          taskInstance={taskInstance.value}
          cell={nodeVariables.menuCell as Cell}
          visible={nodeVariables.menuVisible}
          left={nodeVariables.pageX}
          top={nodeVariables.pageY}
          onHide={menuHide}
          onStart={menuStart}
          onEdit={editTask}
          onCopyTask={copyTask}
          onRemoveTasks={removeTasks}
          onViewLog={handleViewLog}
          onExecuteTask={handleExecuteTask}
          v-model:dependenciesData={dependenciesData}
        />
        <DependenciesModal
          v-model:show={dependenciesData.showRef}
          v-model:taskLinks={dependenciesData.taskLinks}
          required={dependenciesData.required}
          content={dependenciesData.tip}
          onConfirm={dependenciesData.action}
        />
        {!!props.definition && (
          <StartModal
            v-model:row={props.definition.workflowDefinition}
            v-model:show={nodeVariables.startModalShow}
            taskCode={nodeVariables.taskCode}
          />
        )}
        {!!props.instance && (
          <LogModal
            showModalRef={nodeVariables.showModalRef}
            logRef={nodeVariables.logRef}
            row={nodeVariables.row}
            showDownloadLog={true}
            logLoadingRef={nodeVariables.logLoadingRef}
            onConfirmModal={onConfirmModal}
            onRefreshLogs={refreshLogs}
            onDownloadLogs={downloadLogs}
          />
        )}
      </div>
    )
  }
})
