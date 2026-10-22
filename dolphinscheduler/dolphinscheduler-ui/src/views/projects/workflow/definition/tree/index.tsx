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

import Card from '@/components/card'
import { ArrowLeftOutlined } from '@vicons/antd'
import {
  NButton,
  NFormItem,
  NIcon,
  NSelect,
  NSpace,
  NImage,
  NTooltip
} from 'naive-ui'
import {
  defineComponent,
  onMounted,
  Ref,
  ref,
  watch,
  h,
  toRefs,
  reactive,
  getCurrentInstance
} from 'vue'
import { useI18n } from 'vue-i18n'
import styles from './index.module.scss'
import UseD3Tree from '@/views/projects/workflow/definition/tree/use-d3-tree'
import Tree from '@/views/projects/workflow/definition/tree/use-d3-tree/tree'
import { IChartDataItem } from '@/components/chart/modules/types'
import { Router, useRouter } from 'vue-router'
import { viewTree } from '@/service/modules/process-definition'
import { SelectMixedOption } from 'naive-ui/lib/select/src/interface'
import { tasksState, uuid } from '@/common/common'
import type { ITaskTypeNodeOption } from './types'
import { cloneDeep, map } from 'lodash'

export default defineComponent({
  name: 'WorkflowDefinitionTree',
  setup() {
    const router: Router = useRouter()
    const { t, locale } = useI18n()
    const options: Ref<Array<SelectMixedOption>> = ref([
      { label: '25', value: 25 },
      { label: '50', value: 50 },
      { label: '75', value: 75 },
      { label: '100', value: 100 }
    ])

    const projectCode = ref(
      Number(router.currentRoute.value.params.projectCode)
    )
    const definitionCode = ref(
      Number(router.currentRoute.value.params.definitionCode)
    )

    const chartData: Ref<Array<IChartDataItem>> = ref([] as IChartDataItem[])
    const taskStateMap = ref()

    const taskTypeNodeOptions: Ref<Array<ITaskTypeNodeOption>> = ref([
      {
        taskType: 'SHELL',
        color: '#646464',
        image: `${import.meta.env.BASE_URL}images/task-icons/shell.png`
      },
      {
        taskType: 'SUB_PROCESS',
        color: '#4295DA',
        image: `${import.meta.env.BASE_URL}images/task-icons/sub_process.png`
      },
      {
        taskType: 'PROCEDURE',
        color: '#545CC6',
        image: `${import.meta.env.BASE_URL}images/task-icons/procedure.png`
      },
      {
        taskType: 'SQL',
        color: '#8097A0',
        image: `${import.meta.env.BASE_URL}images/task-icons/sql.png`
      },
      {
        taskType: 'SPARK',
        color: '#a16435',
        image: `${import.meta.env.BASE_URL}images/task-icons/spark.png`
      },
      {
        taskType: 'FLINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/flink.png`
      },
      {
        taskType: 'MR',
        color: '#A1A5C9',
        image: `${import.meta.env.BASE_URL}images/task-icons/mr.png`
      },
      {
        taskType: 'PYTHON',
        color: '#60BCD5',
        image: `${import.meta.env.BASE_URL}images/task-icons/python.png`
      },
      {
        taskType: 'DEPENDENT',
        color: '#60BCD5',
        image: `${import.meta.env.BASE_URL}images/task-icons/dependent.png`
      },
      {
        taskType: 'HTTP',
        color: '#7f3903',
        image: `${import.meta.env.BASE_URL}images/task-icons/http.png`
      },
      {
        taskType: 'ASYNCSTATE',
        color: '#75CC71',
        image: `${import.meta.env.BASE_URL}images/task-icons/asyncstate.png`
      },
      {
        taskType: 'WEBHOOK',
        color: '#75CC71',
        image: `${import.meta.env.BASE_URL}images/task-icons/webhook.png`
      },
      {
        taskType: 'DATAX',
        color: '#75CC71',
        image: `${import.meta.env.BASE_URL}images/task-icons/datax.png`
      },
      {
        taskType: 'PIGEON',
        color: '#5EC459',
        image: `${import.meta.env.BASE_URL}images/task-icons/pigeon.png`
      },
      {
        taskType: 'SQOOP',
        color: '#f98b3d',
        image: `${import.meta.env.BASE_URL}images/task-icons/sqoop.png`
      },
      {
        taskType: 'CONDITIONS',
        color: '#b99376',
        image: `${import.meta.env.BASE_URL}images/task-icons/conditions.png`
      },
      {
        taskType: 'SWITCH',
        color: '#ff6f00',
        image: `${import.meta.env.BASE_URL}images/task-icons/switch.png`
      },
      {
        taskType: 'SEATUNNEL',
        color: '#8c8c8f',
        image: `${import.meta.env.BASE_URL}images/task-icons/seatunnel.png`
      },
      {
        taskType: 'DINKY',
        color: '#d69f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/dinky.png`
      },
      { taskType: 'DAG', color: '#bbdde9' },
      {
        taskType: 'FLINK_STREAM',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/flink.png`
      },
      {
        taskType: 'JDBC_SOURCE',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/jdbc_source.png`
      },
      {
        taskType: 'JDBC_SINK',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/jdbc_sink.png`
      },
      {
        taskType: 'FILTER_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/filter_transform.png`
      },
      {
        taskType: 'JSONPATH_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/jsonpath_transform.png`
      },
      {
        taskType: 'RANGE_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/range_transform.png`
      },
      {
        taskType: 'SQL_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/sql_transform.png`
      },
      {
        taskType: 'SQLFILTER_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/sqlfilter_transform.png`
      },
      {
        taskType: 'ENCRYPT_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/encrypt_transform.png`
      },

      {
        taskType: 'SQLCASEWHEN_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/sqlcasewhen_transform.png`
      },
      
      {
        taskType: 'SPLITCOLUMNS_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/splitcolumns_transform.png`
      },

      {
        taskType: 'DESENSITIZE_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/desensitize_transform.png`
      },
      {
        taskType: 'DECRYPT_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/decrypt_transform.png`
      },
      {
        taskType: 'SPLIT_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/split_transform.png`
      },
      {
        taskType: 'SWITCHCASE_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/switchcase_transform.png`
      },
      {
        taskType: 'JAVA_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/java_transform.png`
      },
      {
        taskType: 'JAVASCRIPT_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/javascript_transform.png`
      },
      {
        taskType: 'RANDOMNUMBER_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/randomnumber_transform.png`
      },
      {
        taskType: 'SEQUENCE_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/sequence_transform.png`
      },
      {
        taskType: 'JOIN_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/join_transform.png`
      },
      {
        taskType: 'SQLFUNCTION_TRANSFORM',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/sqlfunction_transform.png`
      },
      {
        taskType: 'CONSTREFERENCE_TRANSFORM',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/constreference_transform.png`
      },
      {
        taskType: 'SORT_TRANSFORM',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/sort_transform.png`
      },
      {
        taskType: 'DATADISTINCT_TRANSFORM',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/datadistinct_transform.png`
      },
      {
        taskType: 'GROUP_TRANSFORM',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/group_transform.png`
      },
      {
        taskType: 'KAFKA_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/kafka_source.png`
      },
      {
        taskType: 'KAFKA_SINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/kafka_sink.png`
      },
      {
        taskType: 'MYSQL_SINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/mysql_sink.png`
      },
       {
        taskType: 'SQLJOIN_TRANSFORM',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/sqljoin_transform.png`
      },
      {
        taskType: 'MYSQL_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/mysql_source.png`
      },
      {
        taskType: 'POSTGRESQL_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/postgresql_source.png`
      },
      {
        taskType: 'POSTGRESQL_SINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/postgresql_sink.png`
      },
      {
        taskType: 'DAMENG_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/dameng_source.png`
      },
      {
        taskType: 'DAMENG_SINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/dameng_sink.png`
      },
      {
        taskType: 'GAUSSDB_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/gaussdb_source.png`
      },
      {
        taskType: 'GAUSSDB_SINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/gaussdb_sink.png`
      },
      {
        taskType: 'UDP_SINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/udp_sink.png`
      },
      {
        taskType: 'UDP_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/udp_source.png`
      },
      {
        taskType: 'HTTP_SOURCE',
        color: '#7f3903',
        image: `${import.meta.env.BASE_URL}images/task-icons/http_source.png`
      },
      {
        taskType: 'HTTP_SINK',
        color: '#7f3903',
        image: `${import.meta.env.BASE_URL}images/task-icons/http_sink.png`
      },
      {
        taskType: 'LOCALFILE_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/localfile_source.png`
      },
      {
        taskType: 'LOCALFILE_SINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/localfile_sink.png`
      },
      {
        taskType: 'SOCKETSERVER_SOURCE',
        color: '#7f3903',
        image: `${import.meta.env.BASE_URL}images/task-icons/socketserver_source.png`
      },
      {
        taskType: 'HTTPSERVER_SOURCE',
        color: '#7f3903',
        image: `${import.meta.env.BASE_URL}images/task-icons/httpserver_source.png`
      },
      {
        taskType: 'FAKESOURCE_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/fakesource_source.png`
      },
      {
        taskType: 'UDPMULTICAST_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/udpmulticast_source.png`
      },
      {
        taskType: 'UDPMULTICAST_SINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/udpmulticast_sink.png`
      },
      {
        taskType: 'FTPFILE_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/ftpfile_source.png`
      },
      {
        taskType: 'SFTPFILE_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/sftpfile_source.png`
      },
      {
        taskType: 'HDFSFILE_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL
          }images/task-icons/hdfsfile_source.png`
      },
      {
        taskType: 'HDFSFILE_SINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/hdfsfile_sink.png`
      },
      {
        taskType: 'MINIOFILE_SOURCE',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/miniofile_source.png`
      },
      {
        taskType: 'MINIOFILE_SINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/miniofile_sink.png`
      },
      {
        taskType: 'HIVE_SINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/hive_sink.png`
      },

      {
        taskType: 'SQLROWTOCOLUMN_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/sqlrowtocolumn_transform.png`
      },
      {
        taskType: 'SQLCOLUMNTOROW_TRANSFORM',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/sqlcolumntorow_transform.png`
      },
      {
        taskType: 'DORIS_SOURCE',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/doris_source.png`
      },
      {
        taskType: 'DORIS_SINK',
        color: '#d68f5b',
        
        image: `${import.meta.env.BASE_URL}images/task-icons/doris_sink.png`
      },
      {
        taskType: 'DORIS_SINK',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/unarchive.png`
      },
      {
        taskType: 'DATANORMALIZE_TRANSFORM',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/datanormalize_transform.png`
      },
      {
        taskType: 'EXTRACT',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/extract.png`
      },
      {
        taskType: 'MINIO',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/minio.png`
      },

      {
        taskType: 'SQLUNION_TRANSFORM',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/sqlunion_transform.png`
      },
      {
        taskType: 'SQLUNIONSORT_TRANSFORM',
        color: '#d68f5b',
        image: `${import.meta.env.BASE_URL}images/task-icons/sqlunionsort_transform.png`
      },
      {
        taskType: 'FLEXIBLE_HTTP',
        color: '#7f3903',
        image: `${import.meta.env.BASE_URL}images/task-icons/http.png`
      },
      {
        taskType: 'FLEXIBLE_API',
        color: '#7f3903',
        image: `${import.meta.env.BASE_URL}images/task-icons/http.png`
      },
    ])

    const showTooltip = ref(false)
    const tooltipText = ref('')
    const tooltipProps = reactive({
      x: 0,
      y: 0
    })

    const changeTooltip = (options: any) => {
      tooltipProps.x = options.x
      tooltipProps.y = options.y - 20
    }

    const initTaskStateMap = () => {
      taskStateMap.value = Object.entries(tasksState(t)).map(([key, item]) => ({
        state: key,
        value: item.desc,
        color: item.color
      }))
    }

    const currentInstance = getCurrentInstance()

    const getWorkflowTreeData = async (limit: number) => {
      if (projectCode.value && definitionCode) {
        Tree.reset()

        const res = await viewTree(projectCode.value, definitionCode.value, {
          limit: limit
        })

        const treeData = cloneDeep(res)
        if (!treeData?.children) return

        const recursiveChildren = (children: any) => {
          if (children.length) {
            map(children, (v) => {
              v.uuid = `${uuid('uuid_')}${uuid('') + uuid('')}`
              if (v.children.length) {
                recursiveChildren(v.children)
              }
            })
          }
        }

        recursiveChildren(treeData.children)

        Tree.init({
          data: cloneDeep(treeData),
          limit: limit,
          selfTree: currentInstance,
          taskTypeNodeOptions: taskTypeNodeOptions.value,
          tasksStateObj: tasksState(t)
        })
      }
    }

    const onSelectChange = (value: number) => {
      if (value) {
        getWorkflowTreeData(value)
      }
    }

    const initData = () => {
      initTaskStateMap()
      getWorkflowTreeData(25)
    }

    onMounted(() => {
      initData()
    })

    watch(
      () => locale.value,
      () => {
        initData()
      }
    )

    return {
      chartData,
      options,
      onSelectChange,
      taskTypeNodeOptions,
      showTooltip,
      tooltipText,
      changeTooltip,
      ...toRefs(tooltipProps)
    }
  },
  render() {
    const { options, onSelectChange, taskTypeNodeOptions } = this
    const { t } = useI18n()
    const router: Router = useRouter()

    return (
      <div class={styles.content}>
        <Card class={styles.card}>
          <div class={styles.header}>
            <NButton type='primary' onClick={() => router.go(-1)}>
              <NIcon>
                <ArrowLeftOutlined />
              </NIcon>
            </NButton>
            <NFormItem
              size={'small'}
              class={styles.right}
              showFeedback={false}
              labelPlacement={'left'}
              label={t('project.workflow.tree_limit')}
            >
              <NSelect
                size='small'
                defaultValue={25}
                onUpdateValue={onSelectChange}
                options={options}
              />
            </NFormItem>
          </div>
        </Card>
        <Card title={t('project.workflow.tree_view')}>
          <NSpace align='center'>
            {taskTypeNodeOptions
              .filter((option: any) => option.image)
              .map((option: any, unused: number) => (
                <NButton text size='tiny' color={option.color}>
                  <NImage width='20' src={option.image} />
                  {option.taskType}
                </NButton>
              ))}
          </NSpace>
        </Card>
        {h(
          NTooltip,
          {
            show: this.showTooltip,
            placement: 'top',
            x: this.x,
            y: this.y,
            duration: 10,
            'show-arrow': false
          },
          {
            default: () => <div innerHTML={this.tooltipText}></div>,
            trigger: () => ''
          }
        )}
        <Card>
          <UseD3Tree />
        </Card>
      </div>
    )
  }
})
