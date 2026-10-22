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
  PropType,
  ref,
  watch,
  nextTick,
  provide,
  computed,
  h,
  Ref,
  onMounted
} from 'vue'
import { useI18n } from 'vue-i18n'
import Modal from '@/components/modal'
import Detail from './detail'
import { formatModel, formatParams } from './format-data'
import {
  HistoryOutlined,
  ProfileOutlined,
  QuestionCircleTwotone,
  BranchesOutlined
} from '@vicons/antd'
import { NIcon } from 'naive-ui'
import { TASK_TYPES_MAP } from '../../constants/task-type'
import { Router, useRouter } from 'vue-router'
import { querySubProcessInstanceByTaskCode } from '@/service/modules/process-instances'
import { useTaskNodeStore } from '@/store/project/task-node'
import type {
  ITaskData,
  ITaskType,
  EditWorkflowDefinition,
  IWorkflowTaskInstance,
  WorkflowInstance
} from './types'
import { queryProjectPreferenceByProjectCode, getOutputFields } from '@/service/modules/projects-preference'
import { INodeData } from './types'
import { useDagStore } from '@/store/project/dynamic/dag'

const props = {
  show: {
    type: Boolean as PropType<boolean>,
    default: false
  },
  data: {
    type: Object as PropType<ITaskData>,
    default: { code: 0, taskType: 'SHELL', name: '' }
  },
  projectCode: {
    type: Number as PropType<number>,
    required: true,
    default: 0
  },
  readonly: {
    type: Boolean as PropType<boolean>,
    default: false
  },
  from: {
    type: Number as PropType<number>,
    default: 0
  },
  definition: {
    type: Object as PropType<Ref<EditWorkflowDefinition>>
  },
  processInstance: {
    type: Object as PropType<WorkflowInstance>
  },
  taskInstance: {
    type: Object as PropType<IWorkflowTaskInstance>
  },
  saving: {
    type: Boolean,
    default: false
  }
}

const NodeDetailModal = defineComponent({
  name: 'NodeDetailModal',
  props,
  emits: ['cancel', 'submit', 'viewLog'],
  setup(props, { emit }) {
    const { t, locale } = useI18n()
    const router: Router = useRouter()
    const taskStore = useTaskNodeStore()
    const dagStore = useDagStore()


    const renderIcon = (icon: any) => {
      return () => h(NIcon, null, { default: () => h(icon) })
    }
    const detailRef = ref()

    const onConfirm = async (flag: boolean = false) => {
      // console.log('detailRef.value: ', detailRef.value);
      await detailRef.value.value.validate()
      const data = detailRef.value.value.getValues()
      if (data && data.taskType && (data.taskType.indexOf('_SOURCE') > 0 || data.taskType.indexOf('_SINK') > 0 || data.taskType.indexOf('_TRANSFORM') > 0)) {
        const taskDefinitionList = dagStore.getDagTaskDefinitionList
        const pretasks = taskDefinitionList.filter((task) => {
          return data.preTasks.indexOf(task.code) >= 0
        })
        // console.log(pretasks)
        let inputFields = []
        const specials = {
            'JDBC_SOURCE': {
              flagKey: 'is_custom_sql',
              flagValue: false,
              tableName: 'table_path',
              outputKey: 'selectedColumns',
              datasourceName: 'datasourceId'
            },
            'MYSQL_SOURCE': {
              flagKey: 'is_custom_sql',
              flagValue: false,
              tableName: 'table_path',
              outputKey: 'selectedColumns',
              datasourceName: 'datasourceId'
            },
            'DAMENG_SOURCE': {
              flagKey: 'is_custom_sql',
              flagValue: false,
              tableName: 'table_path',
              outputKey: 'selectedColumns',
              datasourceName: 'datasourceId'
            },
            'POSTGRESQL_SOURCE': {
              flagKey: 'is_custom_sql',
              flagValue: false,
              tableName: 'table_path',
              outputKey: 'selectedColumns',
              datasourceName: 'datasourceId'
            },
            'GAUSSDB_SOURCE': {
              flagKey: 'is_custom_sql',
              flagValue: false,
              tableName: 'table_path',
              outputKey: 'selectedColumns',
              datasourceName: 'datasourceId'
            }

          }
        const {taskType, code, params, name} = { ...data }
          let realParams = params
          if (!realParams) {
          }
          realParams = formatParams(data)
          if (realParams.taskDefinitionJsonObj && realParams.taskDefinitionJsonObj.taskParams) {
            realParams = realParams.taskDefinitionJsonObj.taskParams
          }
          // llxl 处理jdbc sql 请求未返回
        if (data.taskType == 'JDBC_SOURCE' && !flag && (!realParams[specials[data.taskType].outputKey] || realParams[specials[data.taskType].outputKey].length == 0)) {
          setTimeout(() => {
            onConfirm(true)
          }, 1000)
          return
        }
        if (data.taskType == 'JDBC_SOURCE' || data.taskType == 'MYSQL_SOURCE'|| data.taskType == 'DAMENG_SOURCE' || data.taskType == 'POSTGRESQL_SOURCE'||  data.taskType == 'GAUSSDB_SOURCE') {
          let outputFields = {fields: realParams.outputFields, type: 'OTHER', taskName: data.name, tableName: '', datasourceId: ''}
          if (realParams.outputFields && realParams.outputFields.length && realParams.outputFields[0].fields && realParams.outputFields[0].type) {
            outputFields = realParams.outputFields[0]
          }
          const special = specials[data.taskType]
          if (special) {
            outputFields.fields = realParams[special.outputKey]
            if (special.flagValue == realParams[special.flagKey]) {
              // 是列表
              outputFields.type = 'TABLE'
            }
            outputFields.datasourceId = realParams[special.datasourceName] || ''
            outputFields.tableName = realParams[special.tableName]
          }
          data.outputFields = [outputFields]

        }else{
          if (pretasks.length) {
            const item = pretasks[0]
            if (item.taskParams.outputFields && item.taskParams.outputFields.length && item.taskParams.outputFields[0].fields && item.taskParams.outputFields[0].type) {
              inputFields = item.taskParams.outputFields
            }else{
              let input = {fields: item.taskParams.outputFields, type: 'OTHER', taskName: item.name, tableName: '', datasourceId: ''}
              const special = specials[item.taskType]
              if (special) {
                input.fields = item.taskParams[special.outputKey]
                if (special.flagValue == item.taskParams[special.flagKey]) {
                  // 是列表
                  input.type = 'TABLE'
                }
                input.datasourceId = item.taskParams[special.datasourceName] || ''
                input.tableName = item.taskParams[special.tableName] || ''
              }
              inputFields.push(input)
            }
          }
          // pretasks.forEach(item => {
          //   let input = {fields: item.taskParams.outputFields, type: 'OTHER', taskName: item.name, tableName: ''}
          //   const special = specials[item.taskType]
          //   if (special) {
          //     input.fields = item.taskParams[special.outputKey]
          //     if (special.flagValue == item.taskParams[special.flagKey]) {
          //       // 是列表
          //       input.type == 'TABLE'
          //       input.tableName = item.taskParams[special.tableName]
          //     }
          //   }
          //   inputFields.push(input)
          // })
          
          if (taskType && taskType.indexOf('_') > 0) {
            let types = taskType.split('_')
            let reqObj = {code, params: realParams, name, type: types.length >= 2? types[1]: '', pluginName: types.length >= 1? types[0]: '', inputFields, preTaskcode: pretasks.length ? pretasks[0].code: ''}
            // llxl 请求outputFields接口
            // console.log(reqObj)
            const res = await getOutputFields(reqObj)
            if (res) {
              data.outputFields = res
            }
            // console.log(res)
            // // data.outputFields = ['3333']
          }

        }
        emit('submit', { data: data })
      }else{
        emit('submit', { data: data })
      }
    }
    const onCancel = () => {
      emit('cancel')
    }

    const headerLinks = ref([] as any)
    const projectPreferences = ref({} as any)

    const handleViewLog = () => {
      if (props.taskInstance) {
        emit('viewLog', props.taskInstance.id, props.taskInstance.taskType)
      }
    }

    const initProjectPreferences = (projectCode: number) => {
      queryProjectPreferenceByProjectCode(projectCode).then((result: any) => {
        if (result?.preferences && result.state === 1) {
          projectPreferences.value = JSON.parse(result.preferences)
        }
      })
    }

    const restructureNodeData = (data: INodeData) => {
      if (!data?.id) {
        for (const item in projectPreferences.value) {
          if (projectPreferences.value[item] !== null && item in data) {
            Object.assign(data, { item: projectPreferences.value[item] })
          }
        }
      }
    }

    const initHeaderLinks = (processInstance: any, taskType?: ITaskType) => {
      headerLinks.value = [
        {
          text: t('project.node.instructions'),
          show: !!(taskType && !TASK_TYPES_MAP[taskType]?.helperLinkDisable),
          action: () => {
            let linkedTaskType = taskType?.toLowerCase().replace('_', '-')
            if (taskType === 'PROCEDURE') linkedTaskType = 'stored-procedure'
            const helpUrl =
              'https://dolphinscheduler.apache.org/' +
              locale.value.toLowerCase().replace('_', '-') +
              '/docs/latest/user_doc/guide/task/' +
              linkedTaskType +
              '.html'
            window.open(helpUrl)
          },
          icon: renderIcon(QuestionCircleTwotone)
        },
        {
          text: t('project.node.view_history'),
          show: !!props.taskInstance,
          action: () => {
            router.push({
              name: 'task-instance',
              query: {
                taskCode: props.data.code
              }
            })
          },
          icon: renderIcon(HistoryOutlined)
        },
        {
          text: t('project.node.view_log'),
          show: !!props.taskInstance,
          action: () => {
            handleViewLog()
          },
          icon: renderIcon(ProfileOutlined)
        },
        {
          text: t('project.node.enter_this_child_node'),
          show:
            props.data.taskType === 'SUB_PROCESS' ||
            props.data.taskType === 'DYNAMIC',
          disabled:
            !props.data.id ||
            (router.currentRoute.value.name === 'workflow-instance-detail' &&
              !props.taskInstance),
          action: () => {
            if (router.currentRoute.value.name === 'workflow-instance-detail') {
              querySubProcessInstanceByTaskCode(
                { taskId: props.taskInstance?.id },
                { projectCode: props.projectCode }
              ).then((res: any) => {
                router.push({
                  name: 'workflow-instance-detail',
                  params: { id: res.subProcessInstanceId },
                  query: { code: props.data.taskParams?.processDefinitionCode }
                })
              })
            } else {
              router.push({
                name: 'workflow-definition-detail',
                params: { code: props.data.taskParams?.processDefinitionCode }
              })
            }
          },
          icon: renderIcon(BranchesOutlined)
        }
      ]
    }

    const onTaskTypeChange = (taskType: ITaskType) => {
      // eslint-disable-next-line vue/no-mutating-props
      props.data.taskType = taskType
      initHeaderLinks(props.processInstance, props.data.taskType)
    }

    provide(
      'data',
      computed(() => ({
        projectCode: props.projectCode,
        data: props.data,
        from: props.from,
        readonly: props.readonly,
        definition: props.definition
      }))
    )

    onMounted(() => {
      initProjectPreferences(props.projectCode)
    })

    watch(
      () => [props.show, props.data],
      async () => {
        if (!props.show) return
        initHeaderLinks(props.processInstance, props.data.taskType)
        taskStore.init()
        const nodeData = formatModel(props.data)
        await nextTick()
        restructureNodeData(nodeData)
        detailRef.value.value.setValues(nodeData)
      }
    )

    return () => (
      <Modal
        show={props.show}
        title={
          props.from === 1
            ? `${t('project.task.current_task_settings')}`
            : `${t('project.node.current_node_settings')}`
        }
        onConfirm={onConfirm}
        confirmLoading={props.saving}
        confirmDisabled={props.readonly}
        onCancel={onCancel}
        headerLinks={headerLinks}
      >
        <Detail
          ref={detailRef}
          onTaskTypeChange={onTaskTypeChange}
          key={props.data.taskType}
        />
      </Modal>
    )
  }
})

export default NodeDetailModal
