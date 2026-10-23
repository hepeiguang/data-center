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

import { computed, defineComponent, PropType } from 'vue'
import { useI18n } from 'vue-i18n'
import Editor from '@/components/monaco-editor'
import StConnectorParamGuide from './StConnectorParamGuide'
import StUdpUdfPanel, { isUdpPlugin } from './StUdpUdfPanel'
import styles from './seatunnel-compute.module.scss'

const props = {
  model: {
    type: Object as PropType<Record<string, any>>,
    required: true
  },
  readonly: {
    type: Boolean,
    default: false
  }
}

const StConnectorParamPanel = defineComponent({
  name: 'StConnectorParamPanel',
  props,
  setup(props) {
    const { t } = useI18n()
    const showUdpUdf = computed(() =>
      isUdpPlugin(
        props.model.pluginName,
        props.model.taskType || props.model.type || props.model.connectorType
      )
    )

    return () => (
      <div class={styles.paramSplit}>
        <div class={styles.paramSplitLeft}>
          <div class={styles.paramSplitTitle}>
            {t('project.node.st_param_guide')}
          </div>
          <StConnectorParamGuide model={props.model} />
        </div>
        <div class={styles.paramSplitRight}>
          {showUdpUdf.value && (
            <StUdpUdfPanel
              model={props.model}
              paramsField='connectorParams'
              readonly={props.readonly}
            />
          )}
          <div class={styles.paramSplitTitle}>
            {t('project.node.st_compute_node_config')}
          </div>
          <Editor
            value={props.model.connectorParams || '{}'}
            height={showUdpUdf.value ? '360px' : '520px'}
            options={{
              language: 'json',
              readOnly: props.readonly,
              automaticLayout: true,
              minimap: { enabled: false },
              wordWrap: 'on'
            }}
            onUpdateValue={(v: string) => {
              props.model.connectorParams = v
            }}
          />
        </div>
      </div>
    )
  }
})

export default StConnectorParamPanel
