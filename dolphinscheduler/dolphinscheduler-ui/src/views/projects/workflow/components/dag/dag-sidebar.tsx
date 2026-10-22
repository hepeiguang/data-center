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

import { defineComponent, onMounted, reactive, PropType } from 'vue'
import styles from './dag.module.scss'
import type { TaskType } from './types'
import { NCollapse, NCollapseItem, NIcon } from 'naive-ui'
import { StarFilled, StarOutlined } from '@vicons/antd'
import {
  CancelCollection,
  Collection,
  getDagMenu
} from '@/service/modules/dag-menu'
import { getTaskTypes } from '@/service/modules/process-definition'
import { useI18n } from 'vue-i18n'
const props = {
  externalParam: {
    type: String as PropType<string>,
    default: ''
  }
}
export default defineComponent({
  name: 'workflow-dag-sidebar',
  emits: ['dragStart'],
  props,
  setup(props, context) {
    const variables = reactive({
      dataList: [],
      universal: [],
      cloud: [],
      logic: [],
      di: [],
      dq: [],
      ml: [],
      other: [],
      fav: [],
      SEATUNNEL: [],
      DATAX: [],
      SQOOP: [],
      SOURCE: [],
      SINK: [],
      TRANSFORM: [],
      minio: [],
    })
    const resData = reactive([])
    const { t } = useI18n()
    // 配合业管使用
    const handleDagMenu = async () => {
    // 业管使用 需要过滤掉现在的显示内容
      
      getDagMenu().then((res: any) => {
        const temp = setDataType(res)
        if (props.externalParam) {
         handlerYgShowSide(temp)
          console.log(resData, 'resData')
          return
        }
        temp.forEach((item: any) => {
          // 分组需要特殊处理
          if (item.taskType == 'seatunnel_group') {
            const children = item.children && item.children.length ? item.children.map(sitem => {
              sitem.taskType = sitem.taskType.toUpperCase()
              return sitem.children && sitem.children.length ? { ...sitem, children: handleSeaTunnel(sitem.children, sitem.taskType.toUpperCase())}: sitem
            }) : null
            resData.push({ ...item, children})
          }else {
            resData.push(item)
          }


        })
        // console.log(resData)
        return
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
        variables.dq = variables.dataList.filter(
          (item: any) => item.taskCategory === 'DataQuality'
        )
        variables.ml = variables.dataList.filter(
          (item: any) => item.taskCategory === 'MachineLearning'
        )
        variables.other = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Other'
        )
        // llxl 增加测试
        variables.minio = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Metadata'
        )
        variables.fav = variables.dataList.filter(
          (item: any) => item.collection === true
        )
        variables.SEATUNNEL = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Seatunnel'
        )
        variables.DATAX = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Datax'
        )
        variables.SQOOP = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Sqoop'
        )

        variables.SOURCE = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Source'
        )
        variables.SINK = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Sink'
        )
        variables.TRANSFORM = variables.dataList.filter(
          (item: any) => item.taskCategory === 'Transform'
        )

        variables.SOURCE.map((item: any) => {
          item.type = `${item.taskType.toUpperCase()}_${item.taskCategory.toUpperCase()}`
          return item
        })

        variables.SINK.map((item: any) => {
          item.type = `${item.taskType.toUpperCase()}_${item.taskCategory.toUpperCase()}`
          return item
        })

        variables.TRANSFORM.map((item: any) => {
          item.type = `${item.taskType.toUpperCase()}_${item.taskCategory.toUpperCase()}`
          return item
        })
      })
    }
    // 处理业管数据
    const handlerYgShowSide = (temp:any) => {
      const ygWhite = ['logic', 'universal'];
      const ygWhiteSub = {
        'universal': ['FLEXIBLE_HTTP']
      };
      const ygSubKeys = Object.keys(ygWhiteSub)
      return temp.filter((item:any) => {
          if (ygWhite.includes(item.taskType) && !ygSubKeys.includes(item.taskType)) {
            resData.push(item)
          } else if (ygWhite.includes(item.taskType) && ygSubKeys.includes(item.taskType)) {
            if (item.children.length) {
              const children = item.children.filter((ele:any) => {
                if (ygWhiteSub[item.taskType].includes(ele.taskType)) {
                  return ele
                }
              })
            
              resData.push({ ...item, children})
            }
          }
      })



    }


    const handleSeaTunnel = (data: any, category='') => {
      if (data && data.length) {
        return data.map((item: any) => {
          item.type = `${item.taskType.toUpperCase()}_${category}`
          return item
        })
      } else {
        return null
      }
    }

    const setDataType = (data) => {
      if (data && data.length) {
        return data.map(item => {
          item.type = item.taskType
          if (item.children && item.children.length) {
            item.children = setDataType(item.children)
          }
          if (item.collection) {
            variables.fav.push(item)
          }
          return item
        })
      }else {
        return null
      }
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
      console.log(resData)
    })

    return () => (
      <div class={styles.sidebar}>
        <NCollapse default-expanded-names={['SEATUNNEL', 'DATAX', 'SQOOP']}>
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
                        context.emit('dragStart', e, task.type as TaskType, task.taskName)
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
        { resData.length > 0 && resData.map((group, i) => {
            const { taskName, taskType, children } = { ...group }
            if (taskType == 'seatunnel_group') {
              return (
                <NCollapseItem
                  title={taskName}
                  name={taskType}
                  v-slots={{
                    default: () => {
                      return children.map((task: any) => {
                        if (task.taskType == 'SEATUNNEL') {
                          return (
                            <div
                            class={[styles.draggable, `task-item-${task.type}`]}
                            draggable='true'
                            onDragstart={(e) => {
                              context.emit('dragStart', e, task.type as TaskType, task.taskName)
                            }}
                          >
                            <em
                              class={[
                                styles['sidebar-icon'],
                                styles['icon-' + task.type.toLocaleLowerCase()]
                              ]}
                            />
                            <span>{task.taskName}</span>
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
                          )
                        } else {
                          return (
                            <NCollapse>
                                <NCollapseItem
                                  title={task.taskName}
                                  name={task.taskType}
                                  class='task-cate-logic'
                                  style="margin-left: 0"
                                >
                                  {
                                    task.children.length > 0 && task.children.map((stask: any) => (
                                      <div
                                        class={[styles.draggable, `task-item-${stask.type}`]}
                                        draggable='true'
                                        onDragstart={(e) => {
                                          e.stopPropagation();
                                          context.emit('dragStart', e, stask.type as TaskType, stask.taskName);
                                        }}
                                      >
                                        <em
                                          class={[
                                            styles['sidebar-icon'],
                                            styles['icon-' + stask.type.toLocaleLowerCase()]
                                          ]}
                                        />
                                        <span>{stask.taskName}</span>
                                        <div
                                          class={styles.stars}
                                          onMouseenter={() => {
                                            stask.starHover = true
                                          }}
                                          onMouseleave={() => {
                                            stask.starHover = false
                                          }}
                                          onClick={() => handleCollection(stask)}
                                        >
                                          <NIcon
                                            size='20'
                                            color={
                                              stask.collection || stask.starHover
                                                ? '#288FFF'
                                                : '#ccc'
                                            }
                                          >
                                            {stask.collection ? <StarFilled /> : <StarOutlined />}
                                          </NIcon>
                                        </div>
                                      </div>
                                    ))
                                  }
                                </NCollapseItem>
                            </NCollapse>
                          )
                        }
                    })
                    }
                  }}
                ></NCollapseItem>
              )
            } else {
              return (
                <NCollapseItem
                  title={taskName}
                  name={taskType}
                  v-slots={{
                    default: () => {
                      return children.map((task: any) => (
                        <div
                          class={[styles.draggable, `task-item-${task.type}`]}
                          draggable='true'
                          onDragstart={(e) => {
                            context.emit('dragStart', e, task.type as TaskType, task.taskName)
                          }}
                        >
                          <em
                            class={[
                              styles['sidebar-icon'],
                              styles['icon-' + task.type.toLocaleLowerCase()]
                            ]}
                          />
                          <span>{task.taskName}</span>
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
              )
            }
          })}

        </NCollapse>
      </div>
    )
  }
})
