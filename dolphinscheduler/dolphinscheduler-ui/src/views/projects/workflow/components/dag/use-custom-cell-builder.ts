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

import type { Node, Edge } from '@antv/x6'
import { X6_NODE_NAME, X6_EDGE_NAME } from './dag-config'
import utils from '@/utils'
import { TaskType } from '@/store/project/types'
import { TASK_TYPES_MAP } from '@/store/project/task-type'
import { WorkflowDefinition, Coordinate } from './types'
import {
  buildDefaultConnects,
  isSeaTunnelChildType,
  isSeaTunnelComputeParent,
  parseStComputeGraph,
  resolveTaskIconKey,
  ST_COMPUTE_GROUP_DEFAULT_HEIGHT,
  ST_COMPUTE_GROUP_DEFAULT_WIDTH,
  toSeaTunnelChildType
} from './seatunnel-compute-utils'

export function useCustomCellBuilder() {
  function parseLocationStr(locationStr: string) {
    let locations = null
    if (!locationStr) return locations
    locations = JSON.parse(locationStr)
    return Array.isArray(locations) ? locations : null
  }

  function buildEdge(
    sourceId: string,
    targetId: string,
    label = '',
    isStream = false
  ): Edge.Metadata {
    return {
      shape: X6_EDGE_NAME,
      source: {
        cell: sourceId
      },
      target: {
        cell: targetId
      },
      labels: label ? [label] : undefined,
      attrs: {
        line: {
          strokeDasharray: isStream ? '5 5' : 'none'
        }
      }
    }
  }

  function iconType(type: TaskType | string) {
    if (type === ('FLINK_STREAM' as TaskType)) return 'flink'
    return resolveTaskIconKey(String(type))
  }

  function buildNode(
    id: string,
    type: TaskType,
    taskName: string,
    flag: string,
    coordinate: Coordinate = { x: 100, y: 100 },
    scriptType?: string,
    useCustom: boolean = false
  ): Node.Metadata {
    const truncation = taskName ? utils.truncateText(taskName, 18) : id
    const isGroup =
      isSeaTunnelComputeParent(type) &&
      (scriptType === 'visualize' || !scriptType) &&
      useCustom

    const node: Node.Metadata = {
      id: id,
      shape: X6_NODE_NAME,
      x: coordinate.x,
      y: coordinate.y,
      data: {
        taskType: type,
        taskName: taskName || id,
        flag: flag,
        taskExecuteType:
          TASK_TYPES_MAP[type]?.taskExecuteType ||
          (type === 'SEATUNNEL_COMPUTE' ? 'BATCH' : undefined),
        useCustom,
        scriptType
      },
      attrs: {
        image: {
          'xlink:href': `${
            import.meta.env.BASE_URL
          }images/task-icons/${iconType(type)}.png`
        },
        title: {
          text: truncation
        },
        rect: {
          fill: flag === 'NO' ? 'var(--custom-disable-bg)' : '#ffffff'
        }
      }
    }
    if (isGroup) {
      node.width = ST_COMPUTE_GROUP_DEFAULT_WIDTH
      node.height = ST_COMPUTE_GROUP_DEFAULT_HEIGHT
      node.zIndex = -1
      ;(node.data as any).parent = true
      node.attrs = {
        ...node.attrs,
        body: {
          strokeDasharray: '5 5'
        }
      }
    }
    return node
  }

  function buildGraph(definition: WorkflowDefinition) {
    const nodes: Node.Metadata[] = []
    const edges: Edge.Metadata[] = []

    const locations =
      parseLocationStr(definition.workflowDefinition.locations) || []
    const tasks = definition.taskDefinitionList
    const connects = [...(definition.workflowTaskRelationList || [])]
    const taskTypeMap = {} as { [key in string]: TaskType }
    const relationSet = new Set<string>()

    tasks.forEach((task: any) => {
      const location: any =
        locations.find((l: any) => l.taskCode === task.code) || {}
      const params = task.taskParams || {}
      const isVisualParent =
        isSeaTunnelComputeParent(task.taskType) &&
        (params.scriptType === 'visualize' ||
          (!params.scriptType && params.useCustom))

      if (isVisualParent) {
        const node = buildNode(
          task.code + '',
          task.taskType,
          task.name,
          task.flag,
          { x: location.x, y: location.y },
          params.scriptType || 'visualize',
          true
        )
        node.children = []
        if (location.width) node.width = location.width
        if (location.height) node.height = location.height
        const graph = parseStComputeGraph(params)
        if (node.data) {
          node.data.taskExecuteType = task.taskExecuteType || 'BATCH'
        }
        nodes.push(node)
        taskTypeMap[String(task.code)] = task.taskType
        graph.nodes.forEach((subItem) => {
          const subLoc: any =
            locations.find((l: any) => l.taskCode === Number(subItem.code)) ||
            {}
          const childType = toSeaTunnelChildType(
            subItem.pluginName,
            subItem.type
          ) as TaskType
          const snode = buildNode(
            String(subItem.code),
            childType,
            subItem.name || subItem.pluginName,
            'YES',
            {
              x: subLoc.x ?? (location.x || 0) + 40,
              y: subLoc.y ?? (location.y || 0) + 60
            }
          )
          snode.parent = node.id
          nodes.push(snode)
          ;(node.children as any[]).push(snode)
          taskTypeMap[String(subItem.code)] = childType
        })

        // Child edges: prefer saved relation; otherwise synthesize Source→Transform→Sink
        let childConnects: any[] = []
        if (params.relation) {
          try {
            const rel = JSON.parse(params.relation)
            if (Array.isArray(rel)) {
              childConnects = rel.filter(
                (c: any) => Number(c.preTaskCode) > 0 && Number(c.postTaskCode) > 0
              )
            }
          } catch {
            /* ignore */
          }
        }
        if (!childConnects.length && graph.nodes.length > 1) {
          childConnects = buildDefaultConnects(graph.nodes)
        }
        childConnects.forEach((connect: any, i: number) => {
          const key = `${connect.preTaskCode}_${connect.postTaskCode}`
          if (!relationSet.has(key) && Number(connect.preTaskCode) > 0) {
            relationSet.add(key)
            connects.push({
              ...connect,
              id: (connect.id || Date.now()) + i
            })
          }
        })
      } else if (!isSeaTunnelChildType(task.taskType)) {
        const node = buildNode(
          task.code + '',
          task.taskType,
          task.name,
          task.flag,
          { x: location.x, y: location.y },
          params.scriptType,
          !!params.useCustom
        )
        nodes.push(node)
        taskTypeMap[String(task.code)] = task.taskType
      }
    })

    const streamCodes = new Set(
      tasks
        .filter((t: any) => t.taskExecuteType === 'STREAM')
        .map((t: any) => String(t.code))
    )
    connects
      .filter((r) => !!r.preTaskCode)
      .forEach((c) => {
        const isStream =
          streamCodes.has(String(c.preTaskCode)) ||
          streamCodes.has(String(c.postTaskCode)) ||
          TASK_TYPES_MAP[taskTypeMap[c.preTaskCode]]?.taskExecuteType ===
            'STREAM' ||
          TASK_TYPES_MAP[taskTypeMap[c.postTaskCode]]?.taskExecuteType ===
            'STREAM'
        const edge = buildEdge(
          c.preTaskCode + '',
          c.postTaskCode + '',
          c.name,
          isStream
        )
        edges.push(edge)
      })
    return {
      nodes,
      edges
    }
  }

  return {
    buildNode,
    buildEdge,
    buildGraph
  }
}
