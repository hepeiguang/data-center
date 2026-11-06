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
import { TaskType } from '@/store/project/types'
import { formatParams } from '@/views/projects/task/components/node/format-data'
import { useCellUpdate } from './dag-hooks'
import type { Ref } from 'vue'
import type { Graph } from '@antv/x6'
import type {
  Coordinate,
  NodeData,
  WorkflowDefinition,
  EditWorkflowDefinition
} from './types'
import {
  buildDefaultConnects,
  defaultConnectorParams,
  isSeaTunnelChildType,
  isSeaTunnelComputeParent,
  parseSeaTunnelChildType,
  parseStComputeGraph,
  taskDefFromStNode
} from './seatunnel-compute-utils'

interface Options {
  graph: Ref<Graph | undefined>
  definition: Ref<WorkflowDefinition | undefined>
}

function expandDefinition(
  def: WorkflowDefinition | undefined
): EditWorkflowDefinition {
  const base = (def || {
    workflowDefinition: {},
    workflowTaskRelationList: [],
    taskDefinitionList: []
  }) as EditWorkflowDefinition

  const tasks = [...(base.taskDefinitionList || [])]
  const relations = [...(base.workflowTaskRelationList || [])]
  const codes = new Set(tasks.map((t: any) => Number(t.code)))

  tasks.forEach((task: any) => {
    if (!isSeaTunnelComputeParent(task.taskType)) return
    const params = task.taskParams || {}
    if (params.scriptType === 'custom') return
    const graph = parseStComputeGraph(params)
    graph.nodes.forEach((node) => {
      if (codes.has(Number(node.code))) return
      codes.add(Number(node.code))
      tasks.push(taskDefFromStNode(node))
    })
    let childRels: any[] = []
    if (params.relation) {
      try {
        const rel = JSON.parse(params.relation)
        if (Array.isArray(rel)) {
          childRels = rel.filter(
            (r: any) => Number(r.preTaskCode) > 0 && Number(r.postTaskCode) > 0
          )
        }
      } catch {
        /* ignore */
      }
    }
    if (!childRels.length && graph.nodes.length > 1) {
      childRels = buildDefaultConnects(graph.nodes)
    }
    childRels.forEach((r: any) => {
      if (
        !relations.find(
          (x: any) =>
            Number(x.preTaskCode) === Number(r.preTaskCode) &&
            Number(x.postTaskCode) === Number(r.postTaskCode)
        )
      ) {
        relations.push(r)
      }
    })
  })

  return {
    ...base,
    taskDefinitionList: tasks,
    workflowTaskRelationList: relations
  }
}

export function useTaskEdit(options: Options) {
  const { graph, definition } = options
  const {
    addNode,
    removeNode,
    getSources,
    getTargets,
    setNodeName,
    setNodeFillColor,
    setNodeEdge
  } = useCellUpdate({
    graph
  })
  const workflowDefinition = ref(
    expandDefinition(definition?.value)
  ) as Ref<EditWorkflowDefinition>

  const currTask = ref<NodeData>({
    taskType: 'SHELL',
    code: 0,
    name: ''
  })
  const taskModalVisible = ref(false)

  function appendTask(code: number, type: TaskType, coordinate: Coordinate) {
    const isParent = isSeaTunnelComputeParent(String(type))
    const isChild = isSeaTunnelChildType(String(type))
    const node = addNode(
      code + '',
      type,
      '',
      'YES',
      coordinate,
      isParent ? 'visualize' : undefined,
      isParent ? true : false
    )
    if (!node && isChild) {
      return
    }

    const parsed = isChild
      ? parseSeaTunnelChildType(String(type))
      : null
    const taskParams = isParent
      ? {
          scriptType: 'visualize',
          useCustom: true,
          startupScript: 'seatunnel.sh',
          deployMode: 'local',
          stComputeGraph: JSON.stringify({
            env: { 'job.mode': 'BATCH', parallelism: 2 },
            nodes: []
          }),
          rawScript: JSON.stringify({
            env: { 'job.mode': 'BATCH', parallelism: 2 },
            source: [],
            sink: []
          })
        }
      : isChild && parsed
        ? {
            pluginName: parsed.pluginName,
            connectorType: parsed.kind,
            ...defaultConnectorParams(parsed.pluginName, parsed.kind),
            localParams: [],
            resourceList: []
          }
        : undefined

    workflowDefinition.value.taskDefinitionList.push({
      code,
      taskType: type,
      name: '',
      ...(taskParams ? { taskParams } : {})
    } as any)
    openTaskModal({
      code,
      taskType: type,
      name: '',
      ...(taskParams ? { taskParams } : {})
    } as any)
  }

  function copyTask(
    name: string,
    code: number,
    targetCode: number,
    type: TaskType,
    flag: string,
    coordinate: Coordinate
  ) {
    addNode(code + '', type, name, flag, coordinate)
    const definitionItem = workflowDefinition.value.taskDefinitionList.find(
      (t) => t.code === targetCode
    )

    const newDefinition = {
      ...cloneDeep(definitionItem),
      code,
      name
    } as NodeData

    workflowDefinition.value.taskDefinitionList.push(newDefinition)
  }

  function removeTasks(codes: number[], cells: any[]) {
    workflowDefinition.value.taskDefinitionList =
      workflowDefinition.value.taskDefinitionList.filter(
        (task) => !codes.includes(task.code)
      )
    codes.forEach((code: number) => {
      remove(
        workflowDefinition.value.workflowTaskRelationList,
        (workflow) =>
          workflow.postTaskCode === code || workflow.preTaskCode === code
      )
    })
    cells?.forEach((cell) => {
      if (cell.isEdge()) {
        const preTaskCode = cell.getSourceCellId()
        const postTaskCode = cell.getTargetCellId()
        remove(
          workflowDefinition.value.workflowTaskRelationList,
          (workflow) =>
            String(workflow.postTaskCode) === postTaskCode &&
            String(workflow.preTaskCode) === preTaskCode
        )
      }
    })
  }

  function openTaskModal(task: NodeData) {
    currTask.value = task
    taskModalVisible.value = true
  }

  function editTask(code: number) {
    const definitionItem = workflowDefinition.value.taskDefinitionList.find(
      (t) => t.code === code
    )
    if (definitionItem) {
      currTask.value = definitionItem
    }
    updatePreTasks(getSources(String(code)), code)
    updatePostTasks(code)
    taskModalVisible.value = true
  }

  function taskConfirm({ data }: any) {
    const taskDef = formatParams(data).taskDefinitionJsonObj as NodeData

    workflowDefinition.value.taskDefinitionList =
      workflowDefinition.value.taskDefinitionList.map((task) => {
        if (task.code === currTask.value?.code) {
          setNodeName(task.code + '', taskDef.name)
          let fillColor = '#ffffff'
          if (taskDef.flag === 'NO') {
            fillColor = 'var(--custom-disable-bg)'
          }

          setNodeFillColor(task.code + '', fillColor)

          setNodeEdge(String(task.code), data.preTasks)
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
  }

  function taskCancel() {
    taskModalVisible.value = false
    if (!currTask.value.name) {
      removeNode(String(currTask.value.code))
      remove(
        workflowDefinition.value.taskDefinitionList,
        (task) => task.code === currTask.value.code
      )
    }
  }

  function updatePreTasks(preTasks: number[], code: number) {
    if (workflowDefinition.value?.workflowTaskRelationList?.length) {
      remove(
        workflowDefinition.value.workflowTaskRelationList,
        (workflow) => workflow.postTaskCode === code
      )
    }
    if (!preTasks?.length) return
    preTasks.forEach((task) => {
      workflowDefinition.value?.workflowTaskRelationList.push({
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
        !workflowDefinition.value?.workflowTaskRelationList.find(
          (relation) =>
            relation.postTaskCode === target && relation.preTaskCode === code
        )
      ) {
        workflowDefinition.value?.workflowTaskRelationList.push({
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

  watch(definition, () => {
    if (definition.value) workflowDefinition.value = expandDefinition(definition.value)
  })

  return {
    currTask,
    taskModalVisible,
    workflowDefinition,
    taskConfirm,
    taskCancel,
    appendTask,
    editTask,
    copyTask,
    removeTasks
  }
}
