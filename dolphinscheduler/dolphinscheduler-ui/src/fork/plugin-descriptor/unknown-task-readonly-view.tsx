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

import { defineComponent, ref, computed, h } from 'vue'
import { useMessage, NAlert, NCard, NCode, NSpace, NTag, NText } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import type { INodeData, ITaskData } from '../types'

/**
 * Read-only view for task types that have no descriptor registered.
 * Replaces the dangerous SHELL fallback that could silently overwrite taskParams.
 *
 * This component:
 * 1. Displays the raw taskParams as formatted JSON
 * 2. Shows a prominent warning that editing is disabled
 * 3. Prevents any save operation
 */
export default defineComponent({
  name: 'UnknownTaskReadOnlyView',
  components: {
    NAlert,
    NCard,
    NCode,
    NSpace,
    NTag,
    NText
  },
  props: {
    data: {
      type: Object as () => ITaskData,
      required: true
    },
    taskType: {
      type: String,
      required: true
    }
  },
  setup(props) {
    const { t } = useI18n()
    const message = useMessage()

    const formattedParams = computed(() => {
      try {
        const params = props.data.taskParams
        if (typeof params === 'string') {
          return JSON.stringify(JSON.parse(params), null, 2)
        }
        return JSON.stringify(params, null, 2)
      } catch {
        return String(props.data.taskParams || '{}')
      }
    })

    const hasDescriptor = computed(() => {
      return false // Will be overridden by descriptor cache lookup in the future
    })

    return {
      t,
      formattedParams,
      hasDescriptor
    }
  },
  render() {
    return h(NSpace, { vertical: true, size: 'large' }, {
      default: () => [
        // Warning alert
        h(NAlert, {
          type: 'warning',
          title: this.t('project.unknown_task_warning_title', 'Task Type Not Registered'),
          style: 'margin-bottom: 16px'
        }, {
          default: () => [
            h('p', {}, [
              this.t('project.unknown_task_warning_body',
                'The task type "{type}" is registered on the backend but has no frontend descriptor. ' +
                'Editing this node may cause parameter loss. The form is displayed as read-only.',
                { type: this.taskType })
            ]),
            h(NTag, { type: 'warning', size: 'small', bordered: false }, {
              default: () => this.t('project.read_only', 'Read Only')
            })
          ]
        }),

        // Raw taskParams display
        h(NCard, {
          title: this.t('project.task_params_raw', 'Raw Task Parameters'),
          size: 'small'
        }, {
          default: () => [
            h(NCode, {
              code: this.formattedParams,
              language: 'json',
              wordWrap: true
            })
          ]
        }),

        // Info note
        h(NAlert, {
          type: 'info',
          style: 'margin-top: 16px'
        }, {
          default: () => [
            h(NText, { depth: 3 }, {
              default: () => this.t('project.unknown_task_info',
                'To enable editing, the plugin developer needs to add a ds-plugin.yaml descriptor ' +
                'to the plugin JAR, or the platform admin needs to register a descriptor for this task type.')
            })
          ]
        })
      ]
    })
  }
})
