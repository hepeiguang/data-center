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
import { remove, cloneDeep } from 'lodash'
import { useMessage } from 'naive-ui'
import { TaskType } from '@/store/project/types'
import { formatParams } from '@/views/projects/task/components/node/format-data'
import { useCellUpdate } from './dag-hooks'
import type { Ref, watch } from 'vue'
import type { Graph } from '@antv/x6'
import { useDagStore } from '@/store/project/dynamic/dag'
import { useDialogStore } from '@/store/project/dynamic/dialog'
import type {
  Coordinate,
  NodeData,
  WorkflowDefinition,
  EditWorkflowDefinition
} from './types'

interface Options {
  graph: Ref<Graph | undefined>
  definition: Ref<WorkflowDefinition | undefined>
}

const dialogStore = useDialogStore()
/**
 * Edit task configuration when dbclick
 * @param {Options} options
 * @returns
 */
export function useTaskEdit(options: Options) {
  const { graph, definition } = options
  const message = useMessage()
  const {
    addNode,
    removeNode,
    getSources,
    getTargets,
    setNodeName,
    setNodeMode,
    setNodeFillColor,
    setNodeEdge
  } = useCellUpdate({
    graph
  })
  const whiteTypes = ["SQLFUNCTION_TRANSFORM","DATANORMALIZE_TRANSFORM"]


  const processDefinition = ref(
    definition || {
      processDefinition: {},
      processTaskRelationList: [],
      taskDefinitionList: []
    }
  ) as Ref<EditWorkflowDefinition>

  watch(processDefinition, () => {
    useDagStore().setDagTaskDefinitionList(processDefinition.value.taskDefinitionList)
  }, {
    deep: true
  })

  const currTask = ref<NodeData>({
    taskType: 'SHELL',
    code: 0,
    name: ''
  })
  const taskModalVisible = ref(false)
  const unitaryModeVisible = ref(false)

  /**
   * Append a new task
   */
  function appendTask(code: number, type: TaskType, coordinate: Coordinate, name: string) {
    const node = addNode(code + '', type, '', 'YES', coordinate)
    if (node) {
      // debugger
      // console.log(node)
      // let name = type
      // if (name.indexOf('_') > 0) {
      //   name = name.split('_')[0]
      // }
      processDefinition.value.taskDefinitionList.push({
        code,
        taskType: type,
        name
      })
      openTaskModal({ code, taskType: type, name })
    }
  }

  /**
   * Copy a task
   */
  function copyTask(
    name: string,
    code: number,
    targetCode: number,
    type: TaskType,
    flag: string,
    coordinate: Coordinate
  ) {
    addNode(code + '', type, name, flag, coordinate)
    const definition = processDefinition.value.taskDefinitionList.find(
      (t) => t.code === targetCode
    )

    const newDefinition = {
      ...cloneDeep(definition),
      code,
      name
    } as NodeData

    processDefinition.value.taskDefinitionList.push(newDefinition)
  }

  /**
   * Remove task
   * @param {number} codes
   */
  function removeTasks(codes: number[], cells: any[]) {
    processDefinition.value.taskDefinitionList =
      processDefinition.value.taskDefinitionList.filter(
        (task) => !codes.includes(task.code)
      )
    codes.forEach((code: number) => {
      remove(
        processDefinition.value.processTaskRelationList,
        (process) =>
          process.postTaskCode === code || process.preTaskCode === code
      )
    })
    cells?.forEach((cell) => {
      if (cell.isEdge()) {
        const preTaskCode = cell.getSourceCellId()
        const postTaskCode = cell.getTargetCellId()
        remove(
          processDefinition.value.processTaskRelationList,
          (process) =>
            String(process.postTaskCode) === postTaskCode &&
            String(process.preTaskCode) === preTaskCode
        )
      }
    })
  }

  function openTaskModal(task: NodeData) {
    currTask.value = task
    // if (currTask.value.taskType === "SQLFUNCTION_TRANSFORM"||currTask.value.taskType === "DATANORMALIZE_TRANSFORM") {
    //   dialogStore.setStep(1)
    //   dialogStore.setDialogCanNext(false)
    // }
    if (whiteTypes.includes(currTask.value.taskType)) {
      dialogStore.setStep(1)
      dialogStore.setDialogCanNext(false)
    }
    taskModalVisible.value = true
  }

  /**
   * Edit task
   * @param {number} code
   */
  function editTask(code: number) {
    if (whiteTypes.includes(currTask.value.taskType)) {
      dialogStore.setStep(1)
      dialogStore.setDialogCanNext(false)
    }
    const definition = processDefinition.value.taskDefinitionList.find(
      (t) => t.code === code
    )
    if (definition) {
      currTask.value = definition
    }
    updatePreTasks(getSources(String(code)), code)
    updatePostTasks(code)
    taskModalVisible.value = true
  }

  /**
   * The confirm event in task config modal
   * @param formRef
   * @param from
   */
  function taskConfirm({ data }: any) {
    if (dialogStore.canNextStep) {
      // if (data.taskType === 'SQLFUNCTION_TRANSFORM' && dialogStore.step !== 1) {
      //   dialogStore.setStep(dialogStore.step + 1)
      //   return
      // }
      if (whiteTypes.includes(data.taskType) && dialogStore.step !== 1) {
        dialogStore.setStep(dialogStore.step + 1)
        return
      }
    } else {
      dialogStore.setStep(1)
      dialogStore.setDialogCanNext(false)
    }
    data = formatFilterTask(data)
    if (data.taskType === 'SQLFUNCTION_TRANSFORM' && data.columns.length) {
    // if (whiteTypes.includes(data.taskType) && data.columns.length) {
      for (let i = 0; i < data.columns.length; i++) {
        if (data.columns[i].function_param) {
          for (const k in data.columns[i].function_param) {
            if (data.columns[i].function_param.hasOwnProperty(k)) {
              const value = data.columns[i].function_param[k]
              if (!value && value !== 0) {
                message.warning('需要确保属性值都已配置完成')
                // 暂时放行后期需要处理
                return
              }
            }
          }
        } else if (data.columns[i].hasParam ){
          message.warning('需要确保属性值都已配置完成')
          // 暂时放行后期需要处理
          return
        }
      }
    }
    const taskDef = formatParams(data).taskDefinitionJsonObj as NodeData

    // override target config
    processDefinition.value.taskDefinitionList =
      processDefinition.value.taskDefinitionList.map((task) => {
        if (task.taskType === "SEATUNNEL" && task.children && task.children.length > 0) {
          task.children = task.children.map((ctask: any) => {
            ctask = formatFilterTask(ctask, data)
            console.log('ctask: ', ctask);
            return ctask
          })
        }
        if (task.code === currTask.value?.code) {
          setNodeName(task.code + '', taskDef.name)
          setNodeMode(task.code + '', data.scriptType)
          let fillColor = '#ffffff'
          if (taskDef.flag === 'NO') {
            fillColor = 'var(--custom-disable-bg)'
          }

          setNodeFillColor(task.code + '', fillColor)

          setNodeEdge(String(task.code), data.preTasks)
          console.log('data.preTasks: ', data.preTasks);
          updatePreTasks(data.preTasks, task.code)
          return {
            ...taskDef,
            version: task.version,
            code: task.code,
            taskType: currTask.value.taskType,
            id: task.id
          }
        }
        return task
      })
    taskModalVisible.value = false
    // data.taskExecuteType = 'a,b'
    if (data.taskExecuteType.split(',').length > 1) {
      // unitaryModeVisible.value = true
    }
  }

  /**
   * The cancel event in task config modal
   */
  function taskCancel() {
    if (dialogStore.step === 0 && dialogStore.canNextStep) {
      dialogStore.setStep(1)
      return
    } else {
      dialogStore.setStep(0)
    }
    dialogStore.setDialogCanNext(false)
    taskModalVisible.value = false
    if (!currTask.value.name) {
      removeNode(String(currTask.value.code))
      remove(
        processDefinition.value.taskDefinitionList,
        (task) => task.code === currTask.value.code
      )
    }
  }

  function modeConfirm({ data, mode }: any) {
    unitaryModeVisible.value = false
  }

  function modeCancel() {
    unitaryModeVisible.value = false
  }

  function formatFilterTask(data: any, mergedData?: any) {
    if (data.taskType === "FILTER_TRANSFORM") {
      if (data.include_fields) {
        data.include_fields = data.include_fields.filter((field: any) => {
          return field.retain && field.retain.length > 0 && !!field.retain[0]
        })
      }
      if (data.taskParams?.include_fields && mergedData.include_fields && data.code === mergedData.code) {
        data.taskParams.include_fields = mergedData.include_fields
      }
    }

    return data
  }

  function updatePreTasks(preTasks: number[], code: number) {
    if (processDefinition.value?.processTaskRelationList?.length) {
      remove(
        processDefinition.value.processTaskRelationList,
        (process) => process.postTaskCode === code
      )
    }
    if (!preTasks?.length) return
    preTasks.forEach((task) => {
      processDefinition.value?.processTaskRelationList.push({
        postTaskCode: code,
        preTaskCode: task,
        name: '',
        preTaskVersion: 1,
        postTaskVersion: 1,
        conditionType: 'NONE',
        conditionParams: {}
      })
    })
  }

  function updatePostTasks(code: number) {
    const targets = getTargets(String(code))
    targets.forEach((target: number) => {
      if (
        !processDefinition.value?.processTaskRelationList.find(
          (relation) =>
            relation.postTaskCode === target && relation.preTaskCode === code
        )
      ) {
        processDefinition.value?.processTaskRelationList.push({
          postTaskCode: target,
          preTaskCode: code,
          name: '',
          preTaskVersion: 1,
          postTaskVersion: 1,
          conditionType: 'NONE',
          conditionParams: {}
        })
      }
    })
  }

  onMounted(() => {
    if (graph.value) {
      graph.value.on('cell:dblclick', ({ cell }) => {
        const code = Number(cell.id)
        editTask(code)
      })
    }
  })

  watch(() => definition, () => {
    if (definition.value) processDefinition.value = definition.value
  })

  return {
    currTask,
    taskModalVisible,
    unitaryModeVisible,
    processDefinition,
    taskConfirm,
    taskCancel,
    modeConfirm,
    modeCancel,
    appendTask,
    editTask,
    copyTask,
    removeTasks
  }
}
