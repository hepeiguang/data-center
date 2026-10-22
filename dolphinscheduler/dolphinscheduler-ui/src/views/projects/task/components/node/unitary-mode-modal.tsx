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
import { formatModel } from './format-data'
import {
    HistoryOutlined,
    ProfileOutlined,
    QuestionCircleTwotone,
    BranchesOutlined
} from '@vicons/antd'
import { NIcon, NForm, NFormItem, NSelect } from 'naive-ui'
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
import { queryProjectPreferenceByProjectCode } from '@/service/modules/projects-preference'
import { INodeData } from './types'

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

interface Option {
    label: string
    value: string
  }

const NodeUnitaryModeModal = defineComponent({
    name: 'NodeUnitaryModeModal',
    props,
    emits: ['cancel', 'submit', 'viewLog'],
    setup(props, { emit }) {
        const { t, locale } = useI18n()
        const router: Router = useRouter()
        const taskStore = useTaskNodeStore()
        const modeOptions = ref<Option[]>([])
        const rules = {
            mode: {
              required: true,
              message: '请选择一种job.mode',
              trigger: ['blur', 'change'],
            }
          }
        const renderIcon = (icon: any) => {
            return () => h(NIcon, null, { default: () => h(icon) })
        }
        const detailRef = ref()

        const onConfirm = async () => {
            console.log('selectedMode.value: ', selectedMode.value);
            console.log('detailRef.value: ', detailRef.value);
            emit('submit', { data: detailRef.value, mode:  selectedMode.value})
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

        const selectedMode = ref(''); 

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
                console.log('props: ', props);
                if (!props.show) return
                initHeaderLinks(props.processInstance, props.data.taskType)
                taskStore.init()
                const nodeData = formatModel(props.data)
                await nextTick()
                restructureNodeData(nodeData)
                console.log('nodeData: ', nodeData);
                console.log('props.definition: ', props.definition?.value.taskDefinitionList);
                const data =  props.definition?.value.taskDefinitionList.find((task)=>{
                    return task.code === nodeData.code
                })
                console.log('data.taskExecuteType', data.taskExecuteType);
                // data.taskExecuteType = 'a,b'
                modeOptions.value = data.taskExecuteType.split(',').map(
                    v => ({
                        label: v,
                        value: v
                    })
                )
                detailRef.value =nodeData
            }
        )

        return () => (
            <Modal
                show={props.show}
                title={
                    `${'需要设置单一job.mode'}`
                }
                onConfirm={onConfirm}
                onCancel={onCancel}
                confirmLoading={props.saving}
                confirmDisabled={props.readonly}
                headerLinks={headerLinks}
            >
                {{
                    default: () => (
                        <NForm
                            rules={rules}
                            ref='detailFormRef'
                            require-mark-placement='left'
                            label-align='left'
                        >
                            <NFormItem
                                label={t('job.mode')}
                                path='mode'
                                show-require-mark
                            >
                                <NSelect
                                options={modeOptions.value}
                                placeholder={t('请选择一种job.mode')}
                                v-model:value={selectedMode.value}
                                />
                            </NFormItem>
                        </NForm>
                    )
                }}
            </Modal>
        )
    }
})

export default NodeUnitaryModeModal
