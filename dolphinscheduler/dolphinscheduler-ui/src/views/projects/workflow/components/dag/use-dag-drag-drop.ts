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

import { ref } from 'vue'
import type { Ref } from 'vue'
import type { Graph } from '@antv/x6'
import { genTaskCodeList } from '@/service/modules/task-definition'
import { Coordinate, Dragged } from './types'
import { TaskType } from '@/store/project/types'
import { useRoute } from 'vue-router'
import { useCustomCellBuilder } from './dag-hooks'

interface Options {
  readonly: Ref<boolean>
  graph: Ref<Graph | undefined>
  appendTask: (code: number, type: TaskType, coor: Coordinate, name: string) => void
}

/**
 * Sidebar item drag && drop in canvas
 */
export function useDagDragAndDrop(options: Options) {
  const { readonly, graph, appendTask } = options

  const route = useRoute()
  const projectCode = Number(route.params.projectCode)

  // The element currently being dragged up
  const dragged = ref<Dragged>({
    x: 0,
    y: 0,
    type: 'SHELL',
    name: ''
  })

  function onDragStart(e: DragEvent, type: TaskType, name: string) {
    if (readonly.value) {
      e.preventDefault()
      return
    }
    dragged.value = {
      x: e.offsetX,
      y: e.offsetY,
      type: type,
      name
    }
  }

  function onDrop(e: DragEvent) {
    e.stopPropagation()
    e.preventDefault()
    if (readonly.value) {
      return
    }
    if (dragged.value && graph.value && projectCode) {
      const { type, x: eX, y: eY, name } = dragged.value
      const { x, y } = graph.value.clientToLocal(e.clientX, e.clientY)
      const genNums = 1
      genTaskCodeList(genNums, projectCode).then((res) => {
        const [code] = res
        appendTask(code, type, { x: x - eX, y: y - eY }, name)
      })
    }
  }

  const onGroupSet = (x, y, type: TaskType) => {
    console.log('x: ', x)
    console.log('y: ', y)
    console.log('type: ', type)
    if (readonly.value) {
      return
    }
    dragged.value = {
      x,
      y,
      type
    }
    if (dragged.value && graph.value && projectCode) {
      console.log('dragged.value: ', dragged.value);
      const genNums = 1
      const { buildNode } = useCustomCellBuilder()
      genTaskCodeList(genNums, projectCode).then((res) => {
        const [code] = res
        const node = buildNode(code + '', type, '', 'YES', {
          x: x - 50,
          y: y - 50
        })
        console.log('node: ', node)
        node.attrs.title.text = '分组'
        graph.value?.addNode({
          attrs: node.attrs,
          shape: 'dag-task',
          x: 200,
          y: 80,
          width: 280,
          height: 280,
          zIndex: -1,
          label: 'Parent',
          data: {
            parent: true,
            taskType: type
          }
        })
        console.log('Cells: ', graph.value.getCells())
        // appendTask(code, type, { x: x - 50, y: y - 50 })
      })
    }
  }

  const preventDefault = (e: DragEvent) => {
    e.preventDefault()
  }

  return {
    onDragStart,
    onDrop,
    onGroupSet,
    onDragenter: preventDefault,
    onDragover: preventDefault,
    onDragleave: preventDefault
  }
}
