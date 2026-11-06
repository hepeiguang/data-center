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

import { defineComponent, PropType } from 'vue'
import { NButton, NIcon, NList, NListItem, NThing, useMessage } from 'naive-ui'
import { CopyOutlined, DatabaseOutlined } from '@vicons/antd'
import { useI18n } from 'vue-i18n'
import MonacoEditor from '@/components/monaco-editor'
import SectionCard from './section-card'
import styles from './index.module.scss'

type DsHint = {
  key: string
  displayName?: string
  scriptHint?: string
  type?: string
}

const props = {
  script: { type: String as PropType<string>, default: '' },
  hints: { type: Array as PropType<DsHint[]>, default: () => [] },
  disabled: { type: Boolean, default: false }
}

export default defineComponent({
  name: 'ScriptPanel',
  props,
  emits: ['update:script'],
  setup(props, ctx) {
    const { t } = useI18n()
    const message = useMessage()
    const copy = async (text: string) => {
      try {
        await navigator.clipboard.writeText(text)
        message.success(t('data_api.studio.copied'))
      } catch (error) {
        message.error(String(error))
      }
    }
    const update = (value: string) => ctx.emit('update:script', value)
    return { t, copy, update, props }
  },
  render() {
    const { t, hints, script, disabled, copy, update } = this
    return (
      <div class={styles['script-layout']}>
        <div class={styles['script-editor']}>
          <MonacoEditor
            value={script}
            height='100%'
            onUpdate:value={update}
            options={{
              readOnly: disabled,
              language: 'javascript',
              minimap: { enabled: false },
              automaticLayout: true,
              scrollBeyondLastLine: false,
              fontSize: 13
            }}
          />
        </div>
        <div class={styles['script-side']}>
          <SectionCard
            title={t('data_api.studio.datasource_list')}
            icon={DatabaseOutlined}
          >
            <NList size='small'>
              {hints.length === 0 && (
                <NListItem>
                  <span class={styles['section-hint']}>
                    {t('data_api.studio.datasource_empty')}
                  </span>
                </NListItem>
              )}
              {hints.map((hint) => (
                <NListItem key={hint.key}>
                  <NThing
                    title={hint.displayName || hint.key}
                    description={`db.${hint.key}`}
                  />
                  <div class={styles['script-actions']}>
                    <NButton
                      size='tiny'
                      quaternary
                      onClick={() => copy(`db.${hint.key}`)}
                    >
                      {{
                        icon: () => (
                          <NIcon>
                            <CopyOutlined />
                          </NIcon>
                        ),
                        default: () => t('data_api.studio.copy')
                      }}
                    </NButton>
                    {hint.scriptHint && (
                      <NButton
                        size='tiny'
                        quaternary
                        onClick={() => copy(hint.scriptHint || '')}
                      >
                        {{ default: () => t('data_api.studio.copy_snippet') }}
                      </NButton>
                    )}
                  </div>
                </NListItem>
              ))}
            </NList>
            <div class={styles['section-hint']}>
              {t('data_api.studio.script_variable_hint')}
            </div>
          </SectionCard>
        </div>
      </div>
    )
  }
})
