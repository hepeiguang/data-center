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

import { defineComponent, onMounted, reactive } from 'vue'
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
import { buildDataComputationSidebarTree } from './seatunnel-compute-utils'

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

    const { t } = useI18n()

    const handleDagMenu = () => {
      getDagMenu().then((res: any) => {
        variables.dataList = res.map((item: any) => {
          return {
            ...item,
            starHover: false,
            type: item.taskType
          }
        })
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
          if (item.taskType === 'SEATUNNEL_COMPUTE' || item.type === 'SEATUNNEL_COMPUTE') {
            return buildDataComputationSidebarTree(item)
          }
          return item
        })
        variables.ml = variables.dataList.filter(
          (item: any) => item.taskCategory === 'MachineLearning'
        )
        variables.other = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Other'
        )
        variables.fav = variables.dataList.filter(
          (item: any) => item.collection === true
        )
      })
    }

    const handleCollection = (item: any) => {
      item.collection
        ? CancelCollection(item.taskType).then(() => {
            handleDagMenu()
          })
        : Collection(item.taskType).then(() => {
            handleDagMenu()
          })
      item.collection = !item.collection
    }

    onMounted(() => {
      handleDagMenu()
    })

    return () => (
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
                                        <em
                                          class={[
                                            styles['sidebar-icon'],
                                            styles['icon-seatunnel_compute']
                                          ]}
                                        />
                                        <span>{task.taskName || task.taskType}</span>
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
                                        {(task.children || []).map(
                                          (stask: any) => (
                                            <div
                                              class={[
                                                styles.draggable,
                                                `task-item-${stask.type}`
                                              ]}
                                              draggable='true'
                                              onDragstart={(e) => {
                                                e.stopPropagation()
                                                context.emit(
                                                  'dragStart',
                                                  e,
                                                  stask.type as TaskType
                                                )
                                              }}
                                            >
                                              <em
                                                class={[
                                                  styles['sidebar-icon'],
                                                  styles['icon-seatunnel_compute']
                                                ]}
                                              />
                                              <span>{stask.taskName}</span>
                                            </div>
                                          )
                                        )}
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
                        <em
                          class={[
                            styles['sidebar-icon'],
                            styles[
                              'icon-' + String(group.type).toLocaleLowerCase()
                            ]
                          ]}
                        />
                        <span>{group.taskType}</span>
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
    )
  }
})
