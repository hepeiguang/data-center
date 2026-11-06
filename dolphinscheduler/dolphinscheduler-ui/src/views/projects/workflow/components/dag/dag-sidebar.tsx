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

import {
  defineComponent,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref
} from 'vue'
import styles from './dag.module.scss'
import type { TaskType } from './types'
import { NCollapse, NCollapseItem, NIcon } from 'naive-ui'
import { StarFilled, StarOutlined } from '@vicons/antd'
import {
  CancelCollection,
  Collection,
  getDagMenu
} from '@/service/modules/dag-menu'
import { useI18n } from 'vue-i18n'
import {
  buildDataComputationSidebarTree,
  isSeaTunnelChildType,
  taskIconCssVars
} from './seatunnel-compute-utils'

const SIDEBAR_WIDTH_KEY = 'ds-dag-sidebar-width'
const SIDEBAR_DEFAULT_WIDTH = 280
const SIDEBAR_MIN_WIDTH = 200
const SIDEBAR_MAX_WIDTH = 480
const ST_CONNECTOR_FAV_KEY = 'ds-st-compute-favs'

function readSidebarWidth(): number {
  const raw = Number(localStorage.getItem(SIDEBAR_WIDTH_KEY))
  if (!Number.isFinite(raw)) return SIDEBAR_DEFAULT_WIDTH
  return Math.min(SIDEBAR_MAX_WIDTH, Math.max(SIDEBAR_MIN_WIDTH, raw))
}

function readLocalStFavs(): Set<string> {
  try {
    const raw = JSON.parse(localStorage.getItem(ST_CONNECTOR_FAV_KEY) || '[]')
    return new Set(Array.isArray(raw) ? raw.map(String) : [])
  } catch {
    return new Set()
  }
}

function writeLocalStFavs(set: Set<string>) {
  localStorage.setItem(ST_CONNECTOR_FAV_KEY, JSON.stringify([...set]))
}

export default defineComponent({
  name: 'workflow-dag-sidebar',
  emits: ['dragStart'],
  setup(props, context) {
    const variables = reactive({
      dataList: [],
      universal: [],
      cloud: [],
      logic: [],
      di: [],
      dc: [],
      ml: [],
      other: [],
      fav: []
    })

    const sidebarWidth = ref(readSidebarWidth())
    const resizing = ref(false)
    let resizeStartX = 0
    let resizeStartWidth = SIDEBAR_DEFAULT_WIDTH

    const onResizeMove = (e: MouseEvent) => {
      if (!resizing.value) return
      const next = Math.min(
        SIDEBAR_MAX_WIDTH,
        Math.max(
          SIDEBAR_MIN_WIDTH,
          resizeStartWidth + (e.clientX - resizeStartX)
        )
      )
      sidebarWidth.value = next
    }

    const stopResize = () => {
      if (!resizing.value) return
      resizing.value = false
      document.body.style.cursor = ''
      document.body.style.userSelect = ''
      localStorage.setItem(SIDEBAR_WIDTH_KEY, String(sidebarWidth.value))
      window.removeEventListener('mousemove', onResizeMove)
      window.removeEventListener('mouseup', stopResize)
    }

    const startResize = (e: MouseEvent) => {
      e.preventDefault()
      e.stopPropagation()
      resizing.value = true
      resizeStartX = e.clientX
      resizeStartWidth = sidebarWidth.value
      document.body.style.cursor = 'col-resize'
      document.body.style.userSelect = 'none'
      window.addEventListener('mousemove', onResizeMove)
      window.addEventListener('mouseup', stopResize)
    }

    const { t } = useI18n()

    const handleDagMenu = () => {
      getDagMenu().then((res: any) => {
        variables.dataList = res.map((item: any) => {
          const taskType = item.taskType
          const isChild = isSeaTunnelChildType(taskType)
          return {
            ...item,
            starHover: false,
            type: taskType,
            taskName: isChild
              ? String(taskType).replace(/_(SOURCE|SINK|TRANSFORM)$/, '')
              : item.taskName || taskType
          }
        })
        const favSet = new Set<string>([
          ...variables.dataList
            .filter((item: any) => item.collection)
            .map((item: any) => item.taskType as string),
          ...readLocalStFavs()
        ])
        variables.universal = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Universal'
        )
        variables.cloud = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Cloud'
        )
        variables.logic = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Logic'
        )
        variables.di = variables.dataList.filter(
          (item: any) => item.taskCategory === 'DataIntegration'
        )
        const dcRaw = variables.dataList.filter(
          (item: any) => item.taskCategory === 'DataComputation'
        )
        // Nest SeaTunnel Compute connectors like ds_ui_code seatunnel_group.
        variables.dc = dcRaw.map((item: any) => {
          if (
            item.taskType === 'SEATUNNEL_COMPUTE' ||
            item.type === 'SEATUNNEL_COMPUTE'
          ) {
            return buildDataComputationSidebarTree(item, favSet)
          }
          return item
        })
        variables.ml = variables.dataList.filter(
          (item: any) => item.taskCategory === 'MachineLearning'
        )
        variables.other = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Other'
        )
        const apiFav = variables.dataList.filter(
          (item: any) => item.collection === true
        )
        const apiFavTypes = new Set(apiFav.map((i: any) => i.taskType))
        // Local-only connector favs (before/without API listing them)
        const localExtra = [...favSet]
          .filter((t) => !apiFavTypes.has(t) && isSeaTunnelChildType(t))
          .map((taskType) => ({
            taskType,
            type: taskType,
            taskName: taskType.replace(/_(SOURCE|SINK|TRANSFORM)$/, ''),
            taskCategory: taskType.endsWith('_SOURCE')
              ? 'Source'
              : taskType.endsWith('_SINK')
                ? 'Sink'
                : 'Transform',
            collection: true,
            starHover: false
          }))
        variables.fav = [...apiFav, ...localExtra]
      })
    }

    const handleCollection = (item: any, e?: MouseEvent) => {
      e?.stopPropagation()
      e?.preventDefault()
      const taskType = item.taskType || item.type
      const next = !item.collection
      item.collection = next
      if (isSeaTunnelChildType(taskType) || taskType === 'SEATUNNEL_COMPUTE') {
        const local = readLocalStFavs()
        if (next) local.add(taskType)
        else local.delete(taskType)
        writeLocalStFavs(local)
      }
      const req = next ? Collection(taskType) : CancelCollection(taskType)
      req.then(() => handleDagMenu()).catch(() => handleDagMenu())
    }

    const renderStar = (task: any) => (
      <div
        class={styles.stars}
        onMouseenter={() => {
          task.starHover = true
        }}
        onMouseleave={() => {
          task.starHover = false
        }}
        onClick={(e: MouseEvent) => handleCollection(task, e)}
        onMousedown={(e: MouseEvent) => e.stopPropagation()}
      >
        <NIcon
          size='20'
          color={task.collection || task.starHover ? '#288FFF' : '#ccc'}
        >
          {task.collection ? <StarFilled /> : <StarOutlined />}
        </NIcon>
      </div>
    )

    const renderSidebarIcon = (type: string) => {
      if (
        type === 'SEATUNNEL_COMPUTE' ||
        isSeaTunnelChildType(type) ||
        type === 'SEATUNNEL'
      ) {
        return (
          <em class={styles['sidebar-icon']} style={taskIconCssVars(type)} />
        )
      }
      return (
        <em
          class={[
            styles['sidebar-icon'],
            styles['icon-' + String(type).toLocaleLowerCase()]
          ]}
        />
      )
    }

    onMounted(() => {
      handleDagMenu()
    })

    onBeforeUnmount(() => {
      stopResize()
    })

    return () => (
      <div
        class={styles['sidebar-wrap']}
        style={{ width: `${sidebarWidth.value}px` }}
      >
        <div class={styles.sidebar}>
        <NCollapse default-expanded-names='1' accordion>
          {variables.fav.length > 0 && (
            <NCollapseItem
              title={t('project.menu.fav')}
              name='0'
              class='task-cate-fav'
              v-slots={{
                default: () => {
                  return variables.fav.map((task: any) => (
                    <div
                      class={[styles.draggable, `task-item-${task.type}`]}
                      draggable='true'
                      onDragstart={(e) => {
                        context.emit('dragStart', e, task.type as TaskType)
                      }}
                    >
                      {renderSidebarIcon(String(task.type || task.taskType))}
                      <span>{task.taskName || task.taskType}</span>
                      {renderStar(task)}
                    </div>
                  ))
                }
              }}
            ></NCollapseItem>
          )}
          {variables.universal.length > 0 && (
            <NCollapseItem
              title={t('project.menu.universal')}
              name='1'
              class='task-cate-universal'
              v-slots={{
                default: () => {
                  return variables.universal.map((task: any) => (
                    <div
                      class={[styles.draggable, `task-item-${task.type}`]}
                      draggable='true'
                      onDragstart={(e) => {
                        context.emit('dragStart', e, task.type as TaskType)
                      }}
                    >
                      <em
                        class={[
                          styles['sidebar-icon'],
                          styles['icon-' + task.type.toLocaleLowerCase()]
                        ]}
                      />
                      <span>{task.taskType}</span>
                      <div
                        class={styles.stars}
                        onMouseenter={() => {
                          task.starHover = true
                        }}
                        onMouseleave={() => {
                          task.starHover = false
                        }}
                        onClick={() => handleCollection(task)}
                      >
                        <NIcon
                          size='20'
                          color={
                            task.collection || task.starHover
                              ? '#288FFF'
                              : '#ccc'
                          }
                        >
                          {task.collection ? <StarFilled /> : <StarOutlined />}
                        </NIcon>
                      </div>
                    </div>
                  ))
                }
              }}
            ></NCollapseItem>
          )}
          {variables.cloud.length > 0 && (
            <NCollapseItem
              title={t('project.menu.cloud')}
              name='2'
              class='task-cate-cloud'
              v-slots={{
                default: () => {
                  return variables.cloud.map((task: any) => (
                    <div
                      class={[styles.draggable, `task-item-${task.type}`]}
                      draggable='true'
                      onDragstart={(e) => {
                        context.emit('dragStart', e, task.type as TaskType)
                      }}
                    >
                      <em
                        class={[
                          styles['sidebar-icon'],
                          styles['icon-' + task.type.toLocaleLowerCase()]
                        ]}
                      />
                      <span>{task.taskType}</span>
                      <div
                        class={styles.stars}
                        onMouseenter={() => {
                          task.starHover = true
                        }}
                        onMouseleave={() => {
                          task.starHover = false
                        }}
                        onClick={() => handleCollection(task)}
                      >
                        <NIcon
                          size='20'
                          color={
                            task.collection || task.starHover
                              ? '#288FFF'
                              : '#ccc'
                          }
                        >
                          {task.collection ? <StarFilled /> : <StarOutlined />}
                        </NIcon>
                      </div>
                    </div>
                  ))
                }
              }}
            ></NCollapseItem>
          )}
          {variables.logic.length > 0 && (
            <NCollapseItem
              title={t('project.menu.logic')}
              name='3'
              class='task-cate-logic'
              v-slots={{
                default: () => {
                  return variables.logic.map((task: any) => (
                    <div
                      class={[styles.draggable, `task-item-${task.type}`]}
                      draggable='true'
                      onDragstart={(e) => {
                        context.emit('dragStart', e, task.type as TaskType)
                      }}
                    >
                      <em
                        class={[
                          styles['sidebar-icon'],
                          styles['icon-' + task.type.toLocaleLowerCase()]
                        ]}
                      />
                      <span>{task.taskType}</span>
                      <div
                        class={styles.stars}
                        onMouseenter={() => {
                          task.starHover = true
                        }}
                        onMouseleave={() => {
                          task.starHover = false
                        }}
                        onClick={() => handleCollection(task)}
                      >
                        <NIcon
                          size='20'
                          color={
                            task.collection || task.starHover
                              ? '#288FFF'
                              : '#ccc'
                          }
                        >
                          {task.collection ? <StarFilled /> : <StarOutlined />}
                        </NIcon>
                      </div>
                    </div>
                  ))
                }
              }}
            ></NCollapseItem>
          )}
          {variables.di.length > 0 && (
            <NCollapseItem
              title={t('project.menu.di')}
              name='4'
              class='task-cate-di'
              v-slots={{
                default: () => {
                  return variables.di.map((task: any) => (
                    <div
                      class={[styles.draggable, `task-item-${task.type}`]}
                      draggable='true'
                      onDragstart={(e) => {
                        context.emit('dragStart', e, task.type as TaskType)
                      }}
                    >
                      <em
                        class={[
                          styles['sidebar-icon'],
                          styles['icon-' + task.type.toLocaleLowerCase()]
                        ]}
                      />
                      <span>{task.taskType}</span>
                      <div
                        class={styles.stars}
                        onMouseenter={() => {
                          task.starHover = true
                        }}
                        onMouseleave={() => {
                          task.starHover = false
                        }}
                        onClick={() => handleCollection(task)}
                      >
                        <NIcon
                          size='20'
                          color={
                            task.collection || task.starHover
                              ? '#288FFF'
                              : '#ccc'
                          }
                        >
                          {task.collection ? <StarFilled /> : <StarOutlined />}
                        </NIcon>
                      </div>
                    </div>
                  ))
                }
              }}
            ></NCollapseItem>
          )}
          {variables.dc.length > 0 && (
            <NCollapseItem
              title={t('project.menu.dc')}
              name='5'
              class='task-cate-dc'
              v-slots={{
                default: () => {
                  return variables.dc.map((group: any) => {
                    if (group.taskType === 'seatunnel_compute_group') {
                      return (
                        <NCollapse default-expanded-names={['SEATUNNEL_COMPUTE']}>
                          <NCollapseItem
                            title={group.taskName}
                            name='SEATUNNEL_COMPUTE'
                            v-slots={{
                              default: () =>
                                (group.children || []).map((task: any) => {
                                  if (task.leaf && task.type === 'SEATUNNEL_COMPUTE') {
                                    return (
                                      <div
                                        class={[
                                          styles.draggable,
                                          `task-item-${task.type}`
                                        ]}
                                        draggable='true'
                                        onDragstart={(e) => {
                                          context.emit(
                                            'dragStart',
                                            e,
                                            task.type as TaskType
                                          )
                                        }}
                                      >
                                        {renderSidebarIcon('SEATUNNEL_COMPUTE')}
                                        <span>{task.taskName || task.taskType}</span>
                                        {renderStar(task)}
                                      </div>
                                    )
                                  }
                                  return (
                                    <NCollapse>
                                      <NCollapseItem
                                        title={task.taskName}
                                        name={task.taskType}
                                        style='margin-left: 8px; border-top: 1px solid rgba(0,0,0,0.08); padding: 4px 0;'
                                      >
                                        <div class={styles.stConnectorGrid}>
                                          {(task.children || []).map(
                                            (stask: any) => (
                                              <div
                                                class={[
                                                  styles.draggable,
                                                  `task-item-${stask.type}`
                                                ]}
                                                draggable='true'
                                                title={stask.taskName}
                                                onDragstart={(e) => {
                                                  e.stopPropagation()
                                                  context.emit(
                                                    'dragStart',
                                                    e,
                                                    stask.type as TaskType
                                                  )
                                                }}
                                              >
                                                {renderSidebarIcon(
                                                  String(stask.type)
                                                )}
                                                <span>{stask.taskName}</span>
                                                {renderStar(stask)}
                                              </div>
                                            )
                                          )}
                                        </div>
                                      </NCollapseItem>
                                    </NCollapse>
                                  )
                                })
                            }}
                          />
                        </NCollapse>
                      )
                    }
                    return (
                      <div
                        class={[styles.draggable, `task-item-${group.type}`]}
                        draggable='true'
                        onDragstart={(e) => {
                          context.emit('dragStart', e, group.type as TaskType)
                        }}
                      >
                        {renderSidebarIcon(String(group.type || group.taskType))}
                        <span>{group.taskType}</span>
                        {renderStar(group)}
                      </div>
                    )
                  })
                }
              }}
            ></NCollapseItem>
          )}
          {variables.ml.length > 0 && (
            <NCollapseItem
              title={t('project.menu.ml')}
              name='6'
              class='task-cate-ml'
              v-slots={{
                default: () => {
                  return variables.ml.map((task: any) => (
                    <div
                      class={[styles.draggable, `task-item-${task.type}`]}
                      draggable='true'
                      onDragstart={(e) => {
                        context.emit('dragStart', e, task.type as TaskType)
                      }}
                    >
                      <em
                        class={[
                          styles['sidebar-icon'],
                          styles['icon-' + task.type.toLocaleLowerCase()]
                        ]}
                      />
                      <span>{task.taskType}</span>
                      <div
                        class={styles.stars}
                        onMouseenter={() => {
                          task.starHover = true
                        }}
                        onMouseleave={() => {
                          task.starHover = false
                        }}
                        onClick={() => handleCollection(task)}
                      >
                        <NIcon
                          size='20'
                          color={
                            task.collection || task.starHover
                              ? '#288FFF'
                              : '#ccc'
                          }
                        >
                          {task.collection ? <StarFilled /> : <StarOutlined />}
                        </NIcon>
                      </div>
                    </div>
                  ))
                }
              }}
            ></NCollapseItem>
          )}
          {variables.other.length > 0 && (
            <NCollapseItem
              title={t('project.menu.other')}
              name='7'
              class='task-cate-other'
              v-slots={{
                default: () => {
                  return variables.other.map((task: any) => (
                    <div
                      class={[styles.draggable, `task-item-${task.type}`]}
                      draggable='true'
                      onDragstart={(e) => {
                        context.emit('dragStart', e, task.type as TaskType)
                      }}
                    >
                      <em
                        class={[
                          styles['sidebar-icon'],
                          styles['icon-' + task.type.toLocaleLowerCase()]
                        ]}
                      />
                      <span>{task.taskType}</span>
                      <div
                        class={styles.stars}
                        onMouseenter={() => {
                          task.starHover = true
                        }}
                        onMouseleave={() => {
                          task.starHover = false
                        }}
                        onClick={() => handleCollection(task)}
                      >
                        <NIcon
                          size='20'
                          color={
                            task.collection || task.starHover
                              ? '#288FFF'
                              : '#ccc'
                          }
                        >
                          {task.collection ? <StarFilled /> : <StarOutlined />}
                        </NIcon>
                      </div>
                    </div>
                  ))
                }
              }}
            ></NCollapseItem>
          )}
        </NCollapse>
        </div>
        <div
          class={[
            styles['sidebar-resizer'],
            resizing.value ? styles['sidebar-resizer-active'] : null
          ]}
          title='拖拽调整宽度'
          onMousedown={startResize}
        />
      </div>
    )
  }
})
