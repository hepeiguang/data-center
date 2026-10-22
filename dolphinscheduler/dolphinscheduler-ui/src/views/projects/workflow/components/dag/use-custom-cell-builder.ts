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

export function useCustomCellBuilder() {
  /**
   * Convert locationStr to JSON
   * @param {string} locationStr
   * @returns
   */
  function parseLocationStr(locationStr: string) {
    let locations = null
    if (!locationStr) return locations
    locations = JSON.parse(locationStr)
    return Array.isArray(locations) ? locations : null
  }

  /**
   * Build edge metadata
   * @param {string} sourceId
   * @param {string} targetId
   * @param {string} label
   */
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

  /**
   * Build node metadata
   * @param {string} id
   * @param {string} taskType
   * @param {Coordinate} coordinate Default is { x: 100, y: 100 }
   */
  function buildNode(
    id: string,
    type: TaskType,
    taskName: string,
    flag: string,
    coordinate: Coordinate = { x: 100, y: 100 },
    mode: string,
    useCustom: boolean = false
  ): Node.Metadata {
    const truncation = taskName ? utils.truncateText(taskName, 18) : id
    console.log('truncation: ', truncation);
    console.log('type: ', type);
    const isGroup = ['SEATUNNEL'].includes(type) && mode === 'visualize' && useCustom
    console.log('isGroup: ', isGroup);
    let node = {
      id: id,
      shape: X6_NODE_NAME,
      x: coordinate.x,
      y: coordinate.y,
      data: {
        taskType: type,
        taskName: taskName || id,
        flag: flag,
        taskExecuteType: TASK_TYPES_MAP[type].taskExecuteType
      },
      attrs: {
        image: {
          // Use href instead of xlink:href, you may lose the icon when downloadPNG
          'xlink:href': `${import.meta.env.BASE_URL
            }images/task-icons/${(type !== ('FLINK_STREAM' as TaskType)
              ? type
              : 'FLINK'
            ).toLocaleLowerCase()}.png`
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
      node.width = 550;
      node.height = 280;
      node.zIndex = -1;
      node.data.parent = true;
      node.attrs.body = {
        strokeDasharray: '5 5'
      }
    }
    return node
  }

  /**
   * Build graph JSON
   * @param {WorkflowDefinition} definition
   * @returns
   */
  function buildGraph(definition: WorkflowDefinition) {
    const nodes: Node.Metadata[] = []
    const edges: Edge.Metadata[] = []

    const locations =
      parseLocationStr(definition.processDefinition.locations) || []
    const tasks = definition.taskDefinitionList
    const connects = definition.processTaskRelationList
    const taskTypeMap = {} as { [key in string]: TaskType }
    // llxl 回显

    let relation = { pre: {}, next: {} }
    let subTaskIds = []
    let parentTaskIds = []
    let parent2Sub = {}

    let newConnects = [...connects]

    let tasksObj = {}
    let addConnectFlag = false
    let outConntentss = []


    tasks.forEach((item: any) => {
      if (item.taskType == 'SEATUNNEL' && (item.taskParams.useCustom || item.taskParams.parent)) {
        parentTaskIds.push(item.code)
        tasksObj[item.code] = item
        const location = locations.find((l) => l.taskCode == item.code) || {}
        const node = buildNode(
          item.code + '',
          item.taskType,
          item.name,
          item.flag,
          {
            x: location.x,
            y: location.y
          },
          item.taskParams.scriptType,
          item.taskParams.useCustom
        )
        node.children = []
        node.width = location.width
        node.height = location.height
        nodes.push(node)
        taskTypeMap[String(item.code)] = item.taskType
        if (item.taskParams.rawScript) {
          parent2Sub[item.code] = []
          try {
            const subTasks = JSON.parse(item.taskParams.rawScript)
            subTasks.forEach((subItem: any) => {
              subTaskIds.push(subItem.code)
              parent2Sub[item.code].push(subItem.code)
              if (subItem.postTaskcode) {
                relation.next[subItem.code] = subItem.postTaskcode
              }
              if (subItem.preTaskcode) {
                relation.pre[subItem.code] = subItem.preTaskcode
              }

              tasksObj[subItem.code] = subItem

              const location = locations.find((l) => l.taskCode == subItem.code) || {}
              const snode = buildNode(
                subItem.code + '',
                subItem.type ? `${subItem.pluginName}_${subItem.type}` : subItem.pluginName,
                subItem.name || subItem.code,
                subItem.flag || 'YES',
                {
                  x: location.x,
                  y: location.y
                },
                // subItem.params.scriptType
              )
              snode.parent = node
              nodes.push(snode)
              node.children.push(snode)
              taskTypeMap[String(subItem.code)] = subItem.type ? `${subItem.pluginName}_${subItem.type}` : subItem.pluginName
            })

            if (!addConnectFlag && item.taskParams.relation) {
              let outConntent = JSON.parse(item.taskParams.relation)
              const base = newConnects.length ? newConnects[0] : { id: 10 }
              outConntent.forEach((connect: any, i: number) => {
                const tempConn = { ...base, ...connect, id: base.id * 10 + i }
                newConnects.push(tempConn)
                outConntentss.push(tempConn)
              })
              addConnectFlag = true
            }

            const childSet = new Set<any>(node.children)
            outConntentss.forEach((tempConn: any) => {
              if (parent2Sub[item.code].indexOf(tempConn.preTaskCode + '') > 0 || parent2Sub[item.code].indexOf(tempConn.postTaskCode + '') > 0) {
                childSet.add(tempConn)
              }
            })
            node.children = [...childSet]
          } catch (error) {
          }
        }
      }
      else {
        tasksObj[item.code] = item

        const location = locations.find((l) => l.taskCode == item.code) || {}
        const node = buildNode(
          item.code + '',
          item.taskType,
          item.name,
          item.flag,
          {
            x: location.x,
            y: location.y
          },
        )
        nodes.push(node)
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
    //      try {
    //        const subTasks = JSON.parse(item.taskParams.rawScript)
    //        subTasks.forEach((subItem: any) => {
    //          let newCon = {conditionParams :  {},  conditionType :  "NONE", createTim  :  "", id :  new Date().getTime(), name :  "",  operateTime :  "", operator:  1, postTaskCode :  0, postTaskVersion :  3, preTaskCode:  0, preTaskVersion :  0, processDefinitionCode :  processDefinitionCode,processDefinitionVersion :  3, projectCode :  item.projectCode, updateTime :  ""}

    //          if (subItem.postTaskcode) {
    //            newCon.postTaskcode = subItem.code
    //          }
    //          if (subItem.preTaskcode) {
    //            newCon.preTaskcode = relation.pre[subItem.code]
    //            newCon.postTaskCode = subItem.code
    //          }
    //          newConnects.push(newCon)
    //        })
    //      } catch (error) {
    //      }
    //     }
    //   }
    // })

    // console.log('newConnects', newConnects)

    newConnects
      .filter((r) => !!r.preTaskCode)
      .forEach((c) => {
        const isStream =
          TASK_TYPES_MAP[taskTypeMap[c.preTaskCode]].taskExecuteType ==
          'STREAM' ||
          TASK_TYPES_MAP[taskTypeMap[c.postTaskCode]].taskExecuteType ==
          'STREAM'
        const edge = buildEdge(
          c.preTaskCode + '',
          c.postTaskCode + '',
          c.name,
          isStream
        )
        edges.push(edge)
      })
    /* 
      
      tasks.forEach((task) => {
        const location = locations.find((l) => l.taskCode === task.code) || {}
        const node = buildNode(
          task.code + '',
          task.taskType,
          task.name,
          task.flag,
          {
            x: location.x,
            y: location.y
          },
          task.taskParams.scriptType
        )
        nodes.push(node)
        taskTypeMap[String(task.code)] = task.taskType
      })
  
      connects
        .filter((r) => !!r.preTaskCode)
        .forEach((c) => {
          const isStream =
            TASK_TYPES_MAP[taskTypeMap[c.preTaskCode]].taskExecuteType ===
              'STREAM' ||
            TASK_TYPES_MAP[taskTypeMap[c.postTaskCode]].taskExecuteType ===
              'STREAM'
          const edge = buildEdge(
            c.preTaskCode + '',
            c.postTaskCode + '',
            c.name,
            isStream
          )
          edges.push(edge)
        })
      */


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
