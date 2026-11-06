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

import type { Markup, Node } from '@antv/x6'
import { ref, onMounted, Ref } from 'vue'
import { Graph } from '@antv/x6'
import { NODE, EDGE, X6_NODE_NAME, X6_EDGE_NAME } from './dag-config'
import { debounce } from 'lodash'
import { useResizeObserver } from '@vueuse/core'
import ContextMenuTool from './dag-context-menu'
import { isSeaTunnelChildType } from './seatunnel-compute-utils'

interface Options {
  readonly: Ref<boolean>
  graph: Ref<Graph | undefined>
}

/**
 * Canvas Init
 * 1. Bind the graph to the dom
 * 2. Redraw when the page is resized
 * 3. Register custom graphics
 */
export function useCanvasInit(options: Options) {
  // Whether the graph can be operated
  const { readonly, graph } = options

  const paper = ref<HTMLElement>() // The graph mount HTMLElement
  const minimap = ref<HTMLElement>() // The minimap mount HTMLElement
  const container = ref<HTMLElement>() // The container of paper and minimap

  /**
   * Graph Init, bind graph to the dom
   */
  function graphInit() {
    Graph.registerNodeTool('contextmenu', ContextMenuTool, true)

    return new Graph({
      container: paper.value,
      selecting: {
        enabled: true,
        multiple: true,
        rubberband: true,
        rubberEdge: true,
        movable: true,
        showNodeSelectionBox: false
      },
      scaling: {
        min: 0.2,
        max: 2
      },
      // Only SEATUNNEL_COMPUTE visualize group frames can be drag-resized
      resizing: {
        enabled(this: Graph, node: Node) {
          if (readonly.value) return false
          return !!node.getData?.()?.parent
        },
        minWidth: 400,
        minHeight: 220,
        orthogonal: false,
        restrict: false,
        preserveAspectRatio: false
      },
      mousewheel: {
        enabled: true,
        modifiers: ['ctrl', 'meta']
      },
      scroller: true,
      grid: {
        size: 10,
        visible: true
      },
      embedding: {
        enabled: true,
        findParent({ node }) {
          if (!isSeaTunnelChildType(node?.getData?.()?.taskType)) {
            return []
          }
          const bbox = node.getBBox()
          return this.getNodes().filter((n) => {
            const data = n.getData()
            if (data?.parent) {
              return bbox.isIntersectWithRect(n.getBBox())
            }
            return false
          })
        }
      },
      snapline: true,
      minimap: {
        enabled: true,
        container: minimap.value,
        scalable: true,
        width: 250,
        height: 150
      },
      interacting: {
        edgeLabelMovable: false,
        nodeMovable: !readonly.value,
        magnetConnectable: !readonly.value
      },
      connecting: {
        // Whether multiple edges can be created between the same start node and end
        allowMulti: false,
        // Whether a point is allowed to connect to a blank position on the canvas
        allowBlank: false,
        // The start node and the end node are the same node
        allowLoop: false,
        // Whether an edge is allowed to link to another edge
        allowEdge: false,
        // Whether edges are allowed to link to nodes
        allowNode: true,
        // Whether to allow edge links to ports
        allowPort: false,
        // Whether all available ports or nodes are highlighted when you drag the edge
        highlight: true,
        createEdge() {
          return graph.value?.createEdge({
            shape: X6_EDGE_NAME
          })
        },
        validateConnection(data) {
          const { sourceCell, targetCell } = data

          if (
            sourceCell &&
            targetCell &&
            sourceCell.isNode() &&
            targetCell.isNode()
          ) {
            const sourceData = sourceCell.getData()
            if (!sourceData) return true
            if (sourceData.taskType !== 'CONDITIONS') return true
            const edges = graph.value?.getConnectedEdges(sourceCell)
            if (!edges || edges.length < 2) return true
            let len = 0
            return !edges.some((edge) => {
              if (edge.getSourceCellId() === sourceCell.id) {
                len++
              }
              return len > 2
            })
          }

          return true
        },
        validateEdge({ edge }) {
          const sourceData = edge.getSourceNode()?.getData()
          const targetData = edge.getTargetNode()?.getData()
          edge?.setAttrs({
            line: {
              strokeDasharray:
                sourceData.taskExecuteType === 'STREAM' ||
                targetData.taskExecuteType === 'STREAM'
                  ? '5 5'
                  : 'none'
            }
          })
          return true
        }
      },
      highlighting: {
        nodeAvailable: {
          name: 'className',
          args: {
            className: 'available'
          }
        },
        magnetAvailable: {
          name: 'className',
          args: {
            className: 'available'
          }
        },
        magnetAdsorbed: {
          name: 'className',
          args: {
            className: 'adsorbed'
          }
        }
      }
    })
  }

  onMounted(() => {
    graph.value = graphInit()
    // Make sure the edge starts with node, not port
    graph.value.on('edge:connected', ({ isNew, edge }) => {
      if (isNew) {
        const sourceNode = edge.getSourceNode() as Node
        edge.setSource(sourceNode)
      }
    })

    // Add a node tool when the mouse entering
    graph.value.on('node:mouseenter', ({ node }) => {
      const nodeName = node.getData().taskName
      const markup = node.getMarkup() as Markup.JSONMarkup[]
      const fo = markup.filter((m) => m.tagName === 'foreignObject')[0]

      node.addTools({
        name: 'button',
        args: {
          markup: [
            {
              tagName: 'text',
              textContent: nodeName,
              attrs: {
                fill: '#868686',
                'font-size': 16,
                'text-anchor': 'center'
              }
            }
          ],
          x: 0,
          y: 0,
          offset: { x: 0, y: fo ? -28 : -10 }
        }
      })
    })

    // Remove all tools when the mouse leaving
    graph.value.on('node:mouseleave', ({ node }) => {
      node.removeTool('button')
    })

    // Keep group origin size when user drag-resizes the frame
    graph.value.on('node:change:size', ({ node, options }) => {
      if (options.skipParentHandler) return
      const children = node.getChildren()
      if (children?.length) {
        node.prop('originSize', node.getSize())
      }
    })

    // Auto-expand parent group when children move near the edge
    const embedPadding = 20
    let ctrlPressed = false
    graph.value.on('node:embedding', ({ e }: { e: MouseEvent }) => {
      ctrlPressed = e.metaKey || e.ctrlKey
    })
    graph.value.on('node:embedded', () => {
      ctrlPressed = false
    })
    graph.value.on('node:change:position', ({ node, options }) => {
      if (options.skipParentHandler || ctrlPressed) return

      const children = node.getChildren()
      if (children?.length) {
        node.prop('originPosition', node.getPosition())
      }

      const parent = node.getParent()
      if (!(parent && parent.isNode())) return

      let originSize = parent.prop('originSize')
      if (originSize == null) {
        originSize = parent.getSize()
        parent.prop('originSize', originSize)
      }
      let originPosition = parent.prop('originPosition')
      if (originPosition == null) {
        originPosition = parent.getPosition()
        parent.prop('originPosition', originPosition)
      }

      let x = originPosition.x
      let y = originPosition.y
      let cornerX = originPosition.x + originSize.width
      let cornerY = originPosition.y + originSize.height
      let hasChange = false

      parent.getChildren()?.forEach((child) => {
        const bbox = child.getBBox().inflate(embedPadding)
        const corner = bbox.getCorner()
        if (bbox.x < x) {
          x = bbox.x
          hasChange = true
        }
        if (bbox.y < y) {
          y = bbox.y
          hasChange = true
        }
        if (corner.x > cornerX) {
          cornerX = corner.x
          hasChange = true
        }
        if (corner.y > cornerY) {
          cornerY = corner.y
          hasChange = true
        }
      })

      if (hasChange) {
        parent.prop(
          {
            position: { x, y },
            size: { width: cornerX - x, height: cornerY - y }
          },
          { skipParentHandler: true }
        )
      }
    })
  })

  /**
   * Redraw when the page is resized
   */
  const resize = debounce(() => {
    if (container.value && true) {
      const w = container.value.offsetWidth
      const h = container.value.offsetHeight
      graph.value?.resize(w, h)
    }
  }, 200)
  useResizeObserver(container, resize)

  /**
   * Register custom cells
   */
  function registerCustomCells() {
    Graph.unregisterNode(X6_NODE_NAME)
    Graph.unregisterEdge(X6_EDGE_NAME)
    Graph.registerNode(X6_NODE_NAME, { ...NODE })
    Graph.registerEdge(X6_EDGE_NAME, { ...EDGE })
  }
  onMounted(() => {
    registerCustomCells()
  })

  return {
    graph,
    paper,
    minimap,
    container
  }
}
