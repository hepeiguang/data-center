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

import { computed, h } from 'vue'
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../types'
import SeatunnelComputeEditor from './seatunnel-compute/SeatunnelComputeEditor'

export function useSeaTunnelCompute(model: {
  [field: string]: any
}): IJsonItem[] {
  const { t } = useI18n()
  const customEditorSpan = computed(() =>
    model.scriptType === 'custom' ? 24 : 0
  )
  const visualizeTipSpan = computed(() =>
    model.scriptType === 'visualize' ? 24 : 0
  )

  return [
    {
      type: 'select',
      field: 'startupScript',
      span: 12,
      name: t('project.node.startup_script'),
      options: [
        { label: 'seatunnel.sh', value: 'seatunnel.sh' },
        {
          label: 'start-seatunnel-spark-3-connector-v2.sh',
          value: 'start-seatunnel-spark-3-connector-v2.sh'
        },
        {
          label: 'start-seatunnel-flink-15-connector-v2.sh',
          value: 'start-seatunnel-flink-15-connector-v2.sh'
        }
      ],
      value: model.startupScript || 'seatunnel.sh',
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        message: t('project.node.startup_script_tips')
      }
    },
    {
      type: 'radio',
      field: 'scriptType',
      name: t('project.node.script_type'),
      span: 24,
      options: [
        {
          label: t('project.node.st_compute_mode_visualize'),
          value: 'visualize'
        },
        {
          label: t('project.node.st_compute_mode_script'),
          value: 'custom'
        }
      ],
      props: {
        'on-update:value': (value: string) => {
          model.scriptType = value
          model.useCustom = true
        }
      }
    },
    {
      type: 'custom',
      field: 'seatunnelComputeVisualTip',
      name: ' ',
      span: visualizeTipSpan,
      widget: h(
        'div',
        { style: 'color: #666; line-height: 1.6; padding: 4px 0 12px;' },
        t('project.node.st_compute_visualize_tip')
      )
    },
    {
      type: 'custom',
      field: 'seatunnelComputeEditor',
      name: t('project.node.st_compute_editor'),
      span: customEditorSpan,
      widget: h(SeatunnelComputeEditor, { model })
    }
  ]
}
