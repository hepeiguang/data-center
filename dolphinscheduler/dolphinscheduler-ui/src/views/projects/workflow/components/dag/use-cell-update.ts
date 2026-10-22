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

import type { Ref } from 'vue'
import type { Graph } from '@antv/x6'
import type { TaskType } from '@/store/project/types'
import type { Coordinate } from './types'
import { TASK_TYPES_MAP } from '@/store/project/task-type'
import { useCustomCellBuilder } from './dag-hooks'
import utils from '@/utils'
import type { Edge } from '@antv/x6'

interface Options {
  graph: Ref<Graph | undefined>
}

/**
 * Expose some cell query
 * @param {Options} options
 */
export function useCellUpdate(options: Options) {
  const { graph } = options

  const { buildNode, buildEdge } = useCustomCellBuilder()

  /**
   * Set node name by id
   * @param {string} id
   * @param {string} name
   */
  function setNodeName(id: string, newName: string) {
    const node = graph.value?.getCellById(id)
    if (node) {
      const truncation = utils.truncateText(newName, 18)
      node.attr('title/text', truncation)
      node.setData({ taskName: newName })
    }
  }

  /**
 * Set node mode by id
 * @param {string} id
 * @param {string} mode
 */
  function setNodeMode(id: string, mode: string) {
    console.log('mode: ', mode);
    const node = graph.value?.getCellById(id)
    if (node) {
      if (mode !== 'visualize') {
        node.resize(220, 48);
        node.setZIndex(1);
        node.setData({ parent: false })
      } else {
        node.resize(550, 180);
        node.setZIndex(-1);
        node.setData({ parent: true })
      }
    }
  }

  /**
   * Set the node's fill color by id
   * @param {string} id
   * @param {string} color
   */
  function setNodeFillColor(id: string, color: string) {
    const node = graph.value?.getCellById(id)
    if (!node) {
      return false
    }
    node.attr('rect/fill', color)
  }

  /**
   * Add a node to the graph
   * @param {string} id
   * @param {string} taskType
   * @param {Coordinate} coordinate Default is { x: 100, y: 100 }
   */
  function addNode(
    id: string,
    type: TaskType,
    name: string,
    flag: string,
    coordinate: Coordinate = { x: 100, y: 100 }
  ) {
    if (!TASK_TYPES_MAP[type as TaskType]) {
      return null
    }
    const isSeaTunnelChild = type.indexOf('_SOURCE') > 0 || type.indexOf('_SINK') > 0 || type.indexOf('_TRANSFORM') > 0
    let parentNode = null
    // llxl 注入分组
    if (isSeaTunnelChild) {
      let addChildFlag = false
      const nodes = graph.value?.getNodes() || []
      console.log('nodes: ', nodes);
      if (nodes && nodes.length) {
        nodes.forEach((item: any) => {
          if (!addChildFlag && item.data && item.data.taskType == 'SEATUNNEL' && (item.data.useCustom || item.data.parent)) {
            let { x, y } = item.getPosition()
            let { width, height } = item.getSize()
            if (coordinate.x > x - 60 && coordinate.x < x + width && coordinate.y > y && coordinate.y < y + height) {
              // node.parent = item
              // if (!item.children) {
              //   item.children = []
              // }
              // item.children.push(node)
              // item.addChild(child)
              let flag = false
              if (coordinate.x + 220 > x + width) {
                width += 220
                flag = true
              }
              if (coordinate.y + 48 > y + height) {
                height += 48
                flag = true
              }
              if (coordinate.x - 180 < x) {
                x -= 180
                width += 180
                flag = true
              }
              // if (coordinate.y - 48 < y) {
              //   y -= 48
              //   flag = true
              // }
              if (flag) {
                item.setPosition({x, y})
                item.setSize({width, height})
              }
              console.log('注入分组')
              parentNode = item
              addChildFlag = true
            }
          }
        })
      }
      if (!parentNode) {
        window.$message.error('请把seaTunnel的子算子，拖入seaTunnel分组')
        return null
      }
    }
    const node = buildNode(id, type, name, flag, coordinate)
    const child = graph.value?.addNode(node)
    if (isSeaTunnelChild && parentNode) {
      parentNode.addChild(child)
    }
    return child
  }

  function removeNode(id: string) {
    graph.value?.removeNode(id)
  }

  const getNodeEdge = (id: string): Edge[] => {
    const node = graph.value?.getCellById(id)
    if (!node) return []
    const edges = graph.value?.getConnectedEdges(node)
    return edges || []
  }

  const setNodeEdge = (id: string, preTaskCode: number[]) => {
    const edges = getNodeEdge(id)
    if (edges?.length) {
      edges.forEach((edge) => {
        if (edge.getTargetNode()?.id === id) {
          graph.value?.removeEdge(edge)
        }
      })
    }
    preTaskCode.forEach((task) => {
      graph.value?.addEdge(buildEdge(String(task), id))
    })
  }

  const getSources = (id: string): number[] => {
    const edges = getNodeEdge(id)
    if (!edges.length) return []
    const sources = [] as number[]
    edges.forEach((edge) => {
      const sourceNode = edge.getSourceNode()
      if (sourceNode && sourceNode.id !== id) {
        sources.push(Number(sourceNode.id))
      }
    })
    return sources
  }

  const getTargets = (id: string): number[] => {
    const edges = getNodeEdge(id)
    if (!edges.length) return []
    const targets = [] as number[]
    edges.forEach((edge) => {
      const targetNode = edge.getTargetNode()
      if (targetNode && targetNode.id !== id) {
        targets.push(Number(targetNode.id))
      }
    })
    return targets
  }

  return {
    setNodeName,
    setNodeMode,
    setNodeFillColor,
    setNodeEdge,
    addNode,
    removeNode,
    getSources,
    getTargets
  }
}
