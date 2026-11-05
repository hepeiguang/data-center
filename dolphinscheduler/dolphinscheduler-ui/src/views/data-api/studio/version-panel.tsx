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

import { defineComponent, PropType, reactive, watch } from 'vue'
import {
  NAlert,
  NButton,
  NPopconfirm,
  NSpace,
  NTable,
  useMessage
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user/user'
import SectionCard from './section-card'
import Modal from '@/components/modal'
import MonacoEditor from '@/components/monaco-editor'
import {
  backupEverything,
  fetchBackupScript,
  fetchBackups,
  rollbackBackup,
  type BackupItem,
  type ResourceDetail
} from '@/service/modules/data-api/magic'
import { HistoryOutlined } from '@vicons/antd'
import styles from './index.module.scss'

const props = {
  detail: {
    type: Object as PropType<ResourceDetail>,
    required: true as const
  },
  script: { type: String as PropType<string>, default: '' },
  revision: { type: Number, default: 0 }
}

export default defineComponent({
  name: 'VersionPanel',
  props,
  emits: ['restored'],
  setup(props, ctx) {
    const { t } = useI18n()
    const message = useMessage()
    const userStore = useUserStore()
    const state = reactive({
      loading: false,
      items: [] as BackupItem[],
      showModal: false,
      diffOld: '',
      diffNew: '',
      current: null as BackupItem | null,
      error: ''
    })
    const auth = () => ({ sessionId: userStore.getSessionId || '' })

    const load = async () => {
      if (!props.detail.id) {
        state.items = []
        return
      }
      state.loading = true
      state.error = ''
      try {
        state.items = (await fetchBackups(auth(), props.detail.id)) || []
      } catch (error: any) {
        state.error = error?.message || String(error)
        state.items = []
      } finally {
        state.loading = false
      }
    }

    watch(() => [props.revision, props.detail.id], load, { immediate: true })

    const view = async (item: BackupItem) => {
      if (!props.detail.id) return
      state.current = item
      state.diffNew = props.script || ''
      state.showModal = true
      try {
        state.diffOld =
          (await fetchBackupScript(auth(), props.detail.id, item.createDate)) ||
          ''
      } catch (error: any) {
        state.diffOld = ''
        message.error(error?.message || String(error))
      }
    }

    const rollback = async (item: BackupItem) => {
      if (!props.detail.id) return
      try {
        await rollbackBackup(auth(), props.detail.id, item.createDate)
        message.success(t('data_api.studio.version_rolled_back'))
        ctx.emit('restored')
      } catch (error: any) {
        message.error(error?.message || String(error))
      }
    }

    const fullBackup = async () => {
      try {
        await backupEverything(auth())
        message.success(t('data_api.studio.version_full_backup_ok'))
        load()
      } catch (error: any) {
        message.error(error?.message || String(error))
      }
    }

    return { t, state, auth, load, view, rollback, fullBackup }
  },
  render() {
    const { t, state, view, rollback, fullBackup, load } = this
    return (
      <NSpace vertical size='large'>
        {state.error && (
          <NAlert type='error' title={t('data_api.studio.version_error')}>
            {state.error}
          </NAlert>
        )}
        <SectionCard
          title={t('data_api.studio.version_list')}
          icon={HistoryOutlined}
        >
          {{
            extra: () => (
              <NSpace size='small'>
                <NButton size='tiny' onClick={load}>
                  {t('data_api.studio.refresh')}
                </NButton>
                <NPopconfirm onPositiveClick={fullBackup}>
                  {{
                    default: () =>
                      t('data_api.studio.version_full_backup_confirm'),
                    trigger: () => (
                      <NButton size='tiny' class='btn-full-backup'>
                        {t('data_api.studio.version_full_backup')}
                      </NButton>
                    )
                  }}
                </NPopconfirm>
              </NSpace>
            ),
            default: () => (
              <NTable size='small' singleLine class={styles['readonly-table']}>
                <thead>
                  <tr>
                    <th>{t('data_api.studio.version_col_time')}</th>
                    <th>{t('data_api.studio.version_col_type')}</th>
                    <th>{t('data_api.studio.version_col_tag')}</th>
                    <th>{t('data_api.studio.version_col_by')}</th>
                    <th style={{ width: '180px' }}>
                      {t('data_api.studio.version_col_action')}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {state.items.length === 0 && (
                    <tr>
                      <td colspan={5}>
                        <span class={styles['section-hint']}>
                          {state.loading
                            ? t('data_api.studio.loading')
                            : t('data_api.studio.version_empty')}
                        </span>
                      </td>
                    </tr>
                  )}
                  {state.items.map((item) => (
                    <tr key={`${item.id}-${item.createDate}`}>
                      <td>{new Date(item.createDate).toLocaleString()}</td>
                      <td>{item.type}</td>
                      <td>{item.tag || item.name || '-'}</td>
                      <td>{item.createBy || '-'}</td>
                      <td>
                        <NSpace size='small'>
                          <NButton size='tiny' onClick={() => view(item)}>
                            {t('data_api.studio.version_view')}
                          </NButton>
                          <NPopconfirm onPositiveClick={() => rollback(item)}>
                            {{
                              default: () =>
                                t('data_api.studio.version_rollback_confirm', {
                                  time: new Date(
                                    item.createDate
                                  ).toLocaleString()
                                }),
                              trigger: () => (
                                <NButton size='tiny' type='warning'>
                                  {t('data_api.studio.version_rollback')}
                                </NButton>
                              )
                            }}
                          </NPopconfirm>
                        </NSpace>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </NTable>
            )
          }}
        </SectionCard>
        <Modal
          show={state.showModal}
          title={t('data_api.studio.version_modal_title')}
          wide
          confirmShow={false}
          onCancel={() => (state.showModal = false)}
          onConfirm={() => (state.showModal = false)}
        >
          <div class={styles['diff-grid']}>
            <div>
              <div class={styles['section-title']}>
                {t('data_api.studio.version_history_content')}
              </div>
              <MonacoEditor
                value={state.diffOld}
                height='420px'
                options={{
                  readOnly: true,
                  language: 'javascript',
                  minimap: { enabled: false },
                  scrollBeyondLastLine: false
                }}
              />
            </div>
            <div>
              <div class={styles['section-title']}>
                {t('data_api.studio.version_current')}
              </div>
              <MonacoEditor
                value={state.diffNew}
                height='420px'
                options={{
                  readOnly: true,
                  language: 'javascript',
                  minimap: { enabled: false },
                  scrollBeyondLastLine: false
                }}
              />
            </div>
          </div>
        </Modal>
      </NSpace>
    )
  }
})
