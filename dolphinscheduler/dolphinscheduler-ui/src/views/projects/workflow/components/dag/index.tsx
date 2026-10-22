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
import UnitaryModeModal from '@/views/projects/task/components/node/unitary-mode-modal'
import StartModal from '@/views/projects/workflow/definition/components/start-modal'
import LogModal from '@/components/log-modal'
import './x6-style.scss'
import { queryLog } from '@/service/modules/log'
import { useAsyncState } from '@vueuse/core'
import utils from '@/utils'
import { useUISettingStore } from '@/store/ui-setting/ui-setting'
import { executeTask } from '@/service/modules/executors'
import { removeTaskInstanceCache } from '@/service/modules/task-instances'
import DependenciesModal from '@/views/projects/components/dependencies/dependencies-modal'
import { useDagStore } from '@/store/project/dynamic/dag'

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
  },
  externalParam: {
    type: String as PropType<string>,
    default: ''
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

    const uiSettingStore = useUISettingStore()
    const logTimer = uiSettingStore.getLogTimer

    // Whether the graph can be operated
    provide('readonly', toRef(props, 'readonly'))

    const graph = ref<Graph>()
    provide('graph', graph)
    context.expose(graph)
    useDagStore().setGraph(graph)
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
    // llxl 回显2



    // llxl 回显

    const definition1 = toRef(props, 'definition')
    let newConnects  = []
    let newTask = []
    if (definition1.value) {
      const tasks = definition1.value.taskDefinitionList
      const connects = definition1.value.processTaskRelationList
      const taskTypeMap = {} as { [key in string]: TaskType }
      
      let relation = {pre: {}, next: {}}
      let subTaskIds = []
      let parentTaskIds = []
      let parent2Sub = {}
  
      newConnects = [ ...connects ]
  
      let tasksObj = {}
  
  
      let addConnectFlag = false
  
  
      tasks.forEach((item: any) => {
        if (item.taskType == 'SEATUNNEL' && item.taskParams.useCustom) {
          parentTaskIds.push(item.code)
          tasksObj[item.code] = item
          // const location = locations.find((l) => l.taskCode == item.code) || {}
          // const node = buildNode(
          //   item.code + '',
          //   item.taskType,
          //   item.name,
          //   item.flag,
          //   {
          //     x: location.x,
          //     y: location.y
          //   },
          //   item.taskParams.scriptType,
          //   item.taskParams.useCustom
          // )
          // node.children = []
          // nodes.push(node)
          item.children = []
          newTask.push(item)
  
          taskTypeMap[String(item.code)] = item.taskType
          if (item.taskParams.rawScript) {
            parent2Sub[item.code] = []
            try {
              const subTasks = JSON.parse(item.taskParams.rawScript)
              subTasks.forEach((subItem: any, i: number) => {
                subTaskIds.push(subItem.code)
                subItem.code = Number(subItem.code)
                parent2Sub[item.code].push(subItem.code)
                if (subItem.postTaskcode) {
                  relation.next[subItem.code] = subItem.postTaskcode
                }
                if (subItem.preTaskcode) {
                  relation.pre[subItem.code] = subItem.preTaskcode
                }
                tasksObj[subItem.code] = subItem
                subItem.taskType = subItem.type?`${subItem.pluginName}_${subItem.type}`: subItem.pluginName
  
                if (subItem.outputFields) {
                  subItem.params.outputFields = subItem.outputFields
                }
                subItem.taskParams = subItem.params
                item.children.push(subItem)
  
                newTask.push(subItem)
  
                // node.children.push(snode)
                taskTypeMap[String(subItem.code)] = subItem.type?`${subItem.pluginName}_${subItem.type}`: subItem.pluginName
              })

              if (!addConnectFlag && item.taskParams.relation) {
                let outConntent = JSON.parse(item.taskParams.relation)
                const base = newConnects.length ? newConnects[0]: {id: 10}
                outConntent.forEach((connect: any, i: number) => {
                  newConnects.push({...base, ...connect, id: base.id * 10 + i})
                })
                addConnectFlag = true
              }
            } catch (error) {
            }
          }
        }else{
          tasksObj[item.code] = item
          newTask.push(item)
          taskTypeMap[String(item.code)] = item.taskType
        }
      })
  
  
      // let processDefinitionCode = 0
      // if (connects && connects.length) {
      //   processDefinitionCode = connects[0].processDefinitionCode
      // }
      // tasks.forEach((item: any) => {
      //   if (item.taskType == 'SEATUNNEL' && item.taskParams.useCustom) {
      //     if (item.taskParams.rawScript) {
      //       try {
      //         const subTasks = JSON.parse(item.taskParams.rawScript)
      //         subTasks.forEach((subItem: any) => {
      //           let newCon = {conditionParams :  {},  conditionType :  "NONE", createTim  :  "", id :  new Date().getTime(), name :  "",  operateTime :  "", operator:  1, postTaskCode :  0, postTaskVersion :  3, preTaskCode:  0, preTaskVersion :  0, processDefinitionCode :  processDefinitionCode,processDefinitionVersion :  3, projectCode :  item.projectCode, updateTime :  ""}
    
      //           if (subItem.postTaskcode) {
      //             newCon.postTaskcode = subItem.code
      //           }
      //           if (subItem.preTaskcode) {
      //             newCon.preTaskcode = relation.pre[subItem.code]
      //             newCon.postTaskCode = subItem.code
      //           }
      //           newConnects.push(newCon)
      //         })
      //       } catch (error) {
      //       }
      //     }
      //   }
      // })
  
      console.log('newConnects', newConnects)
    }

    // newConnects
    //   .filter((r) => !!r.preTaskCode)
    //   .forEach((c) => {
    //     const isStream =
    //       TASK_TYPES_MAP[taskTypeMap[c.preTaskCode]].taskExecuteType ==
    //       'STREAM' ||
    //       TASK_TYPES_MAP[taskTypeMap[c.postTaskCode]].taskExecuteType ==
    //       'STREAM'
    //     const edge = buildEdge(
    //       c.preTaskCode + '',
    //       c.postTaskCode + '',
    //       c.name,
    //       isStream
    //     )
    //     edges.push(edge)
    //   })
    const {
      taskConfirm,
      taskModalVisible,
      unitaryModeVisible,
      currTask,
      taskCancel,
      modeConfirm,
      modeCancel,
      appendTask,
      editTask,
      copyTask,
      processDefinition,
      removeTasks
    } = useTaskEdit({ graph, definition: definition1.value? { ...definition1.value, taskDefinitionList: newTask, processTaskRelationList:  newConnects }: definition1.value})
    // Right click cell
    const { nodeVariables, menuHide, menuStart, viewLog } = useNodeMenu({
      graph
    })

    // start button in the dag node menu
    const startDisplay = computed(() => {
      if (props.definition) {
        return (
          route.name === 'workflow-definition-detail' &&
          props.definition!.processDefinition.releaseState === 'ONLINE'
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
          props.instance.state === 'WAITING_THREAD' ||
          props.instance.state === 'SUCCESS' ||
          props.instance.state === 'PAUSE' ||
          props.instance.state === 'FAILURE' ||
          props.instance.state === 'STOP'
        )
      } else if (props.definition) {
        return props.definition!.processDefinition.releaseState === 'OFFLINE'
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
          console.log('taskCode', taskCode)
          console.log('taskList.value: ', taskList.value);
          currentTaskInstance.value = taskList.value.find(
            (task: any) => task.taskCode === taskCode
          )
        }
      }
    )

    watch(
      () => currentTaskInstance.value,
      (val) => {
        console.log('currentTaskInstance', val)
      },
      {deep: true}
    )

    const statusTimerRef = ref()
    const { taskList, refreshTaskStatus } = useNodeStatus({ graph })

    const { onDragStart, onDrop, onGroupSet } = useDagDragAndDrop({
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
        processDefinition.value.taskDefinitionList as any
      )
      // llxl 上送处理
      let relation = {pre: {}, next: {}}
      let subTaskIds = []
      let parentTaskIds = []
      let parent2Sub = {}
      console.log('nodes: ', nodes);
      console.log('processDefinition.value.taskDefinitionList: ', processDefinition.value.taskDefinitionList);
      if (nodes && nodes.length) {
        nodes.forEach((item: any) => {
          if (item.shape == 'dag-task' && item.children && item.children.length) {
            parentTaskIds.push(item.id)
            parent2Sub[item.id] = []
            item.children.forEach((subItem: any) => {
              if (subItem.shape == 'dag-task'){
                subTaskIds.push(subItem.id)
                parent2Sub[item.id].push(subItem.id)
              // }else if (subItem.shape == 'dag-edge') {
              //   relation.pre[subItem?.source?.cell] = subItem?.target?.cell
              //   relation.next[subItem?.target?.cell] = subItem?.source?.cell
              }
            })
          }
        })
      }

      let newConnects = []
      let outConnects = []
      if (connects && connects.length) {
        connects.forEach((item: any) => {
          if (subTaskIds.indexOf(item.preTaskVersion + '') < 0 && subTaskIds.indexOf(item.postTaskCode + '') < 0) {
            newConnects.push(item)
          }else{
            if (subTaskIds.indexOf(item.preTaskVersion + '') >= 0 || subTaskIds.indexOf(item.postTaskCode + '') >= 0) {
              relation.pre[item.preTaskCode] = item.postTaskCode
              relation.next[item.postTaskCode] = item.preTaskCode
            }
            outConnects.push(item)
          }
        })
      }


      let newTaskDefinitions = []
      let taskDefinitionObj = {}
      let hasConnectedFlag = false
      if (processDefinition.value.taskDefinitionList && processDefinition.value.taskDefinitionList.length) {
        processDefinition.value.taskDefinitionList.forEach((item: any) => {
          taskDefinitionObj[item.code] = item
        })
        processDefinition.value.taskDefinitionList.forEach((item: any) => {
          if (parentTaskIds.indexOf(item.code + '') >= 0) {
            let res = { ...item }
            let rawScript = []
            parent2Sub[item.code].forEach((subId: string, i: number) => {
              const subItem = { ...taskDefinitionObj[subId] }
              if (subItem.taskType) {
                let types = subItem.taskType.split('_')
                const {localParams, resourceList, ...params} = { ...subItem.taskParams }
                let subItemR = {type: types.length >= 2? types[1]: '', pluginName: types.length >= 1? types[0]: '', code: subId, params: { ...params, outputFields: null, localParams: localParams && localParams.length ? localParams: null}, name: subItem.name, outputFields: params.outputFields}
                
                if (relation.next[subId]) {
                  subItemR.preTaskcode = relation.next[subId]
                }
                if (relation.pre[subId]) {
                  subItemR.postTaskcode = relation.pre[subId]
                }
                rawScript.push(subItemR)
              }
            })
            res.taskParams.rawScript = JSON.stringify(rawScript)
            if (!hasConnectedFlag) {
              hasConnectedFlag = true
              res.taskParams.relation = JSON.stringify(outConnects)
            }
            newTaskDefinitions.push(res)
          }else if (subTaskIds.indexOf(item.code + '') < 0) {
            newTaskDefinitions.push(item)
          }
        })
      }


      console.log(' processDefinition.value.taskDefinitionList: ',  processDefinition.value.taskDefinitionList);
      console.log('connects: ', connects);
      const locations = getLocations(nodes)
      context.emit('save', {
        taskDefinitions: newTaskDefinitions,
        // taskDefinitions: processDefinition.value.taskDefinitionList,
        saveForm,
        connects: newConnects,
        locations
      })
      saveModelToggle(false)
    }

    const handleViewLog = (taskId: number, taskType: string) => {
      taskModalVisible.value = false
      viewLog(taskId, taskType)

      getLogs(logTimer)
    }

    let getLogsID: number

    const getLogs = (logTimer: number) => {
      const { state } = useAsyncState(
        queryLog({
          taskInstanceId: nodeVariables.logTaskId,
          limit: nodeVariables.limit,
          skipLineNum: nodeVariables.skipLineNum
        }).then((res: any) => {
          nodeVariables.logRef += res.message || ''
          if (res && res.message !== '') {
            nodeVariables.limit += 1000
            nodeVariables.skipLineNum += res.lineNum
            getLogs(logTimer)
          } else {
            nodeVariables.logLoadingRef = false
            if (logTimer !== 0) {
              if (typeof getLogsID === 'number') {
                clearTimeout(getLogsID)
              }
              getLogsID = setTimeout(() => {
                nodeVariables.limit += 1000
                nodeVariables.skipLineNum += 1000
                getLogs(logTimer)
              }, logTimer * 1000)
            }
          }
        }),
        {}
      )

      return state
    }

    const refreshLogs = (logTimer: number) => {
      nodeVariables.logRef = ''
      nodeVariables.limit = 1000
      nodeVariables.skipLineNum = 0
      getLogs(logTimer)
    }

    const handleExecuteTask = (
      startNodeList: number,
      taskDependType: string
    ) => {
      executeTask(
        {
          processInstanceId: Number(route.params.id),
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

    const handleRemoveTaskInstanceCache = (taskId: number) => {
      removeTaskInstanceCache(props.projectCode, taskId).then(() => {
        window.$message.success(t('project.workflow.success'))
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
          onGroupSet={onGroupSet}
          v-model:dependenciesData={dependenciesData}
        />
        <div class={Styles.content}>
          <DagSidebar onDragStart={onDragStart} externalParam={props.externalParam} />
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
            v-model:row={props.definition.processDefinition}
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
          processInstance={props.instance}
          taskInstance={currentTaskInstance.value}
          onViewLog={handleViewLog}
          data={currTask.value as any}
          definition={processDefinition}
          onSubmit={taskConfirm}
          onCancel={taskCancel}
        />
        <UnitaryModeModal readonly={props.readonly}
          projectCode={props.projectCode}
          processInstance={props.instance}
          taskInstance={currentTaskInstance.value}
          onViewLog={handleViewLog}
          data={currTask.value as any}
          definition={processDefinition}
          onSubmit={modeConfirm} 
          onCancel={modeCancel} 
          show={unitaryModeVisible.value}
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
          onRemoveTaskInstanceCache={handleRemoveTaskInstanceCache}
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
            v-model:row={props.definition.processDefinition}
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
