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
  NInput,
  NPopconfirm,
  NSpace,
  NTable,
  NTag,
  useMessage
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user/user'
import SectionCard from './section-card'
import {
  listDataApiReleases,
  offlineDataApiRelease,
  rollbackDataApiRelease,
  snapshotDataApiRelease
} from '@/service/modules/data-api'
import { CloudUploadOutlined } from '@vicons/antd'
import styles from './index.module.scss'

type ReleaseRow = Record<string, any>

const props = {
  apiPath: {
    type: String as PropType<string>,
    required: true as const
  },
  folder: { type: String as PropType<'api' | 'function'>, default: 'api' },
  revision: { type: Number, default: 0 }
}

export default defineComponent({
  name: 'ReleasePanel',
  props,
  emits: ['changed'],
  setup(props, ctx) {
    const { t } = useI18n()
    const message = useMessage()
    const userStore = useUserStore()
    const state = reactive({
      loading: false,
      items: [] as ReleaseRow[],
      remark: '',
      error: ''
    })
    const auth = () => ({ sessionId: userStore.getSessionId || '' })

    const load = async () => {
      if (!props.apiPath) return
      state.loading = true
      state.error = ''
      try {
        const result = await listDataApiReleases(auth(), props.apiPath)
        if (typeof result === 'string') {
          state.error = result
          state.items = []
        } else {
          state.items = (result?.items || []) as ReleaseRow[]
        }
      } catch (error: any) {
        state.error = error?.message || String(error)
        state.items = []
      } finally {
        state.loading = false
      }
    }

    watch(() => [props.revision, props.apiPath], load, { immediate: true })

    const handle = async (
      action: (headers: any, id: number) => Promise<any>,
      id: number
    ) => {
      try {
        const result = await action(auth(), id)
        if (typeof result === 'string') {
          message.error(result)
        } else {
          message.success(t('data_api.studio.release_action_ok'))
          ctx.emit('changed')
          load()
        }
      } catch (error: any) {
        message.error(error?.message || String(error))
      }
    }

    const snapshot = async () => {
      state.loading = true
      try {
        const result = await snapshotDataApiRelease(auth(), state.remark)
        if (typeof result === 'string') {
          message.error(result)
        } else {
          message.success(
            t('data_api.studio.release_snapshot_ok', {
              count: result?.snapshotCount ?? 0
            })
          )
          state.remark = ''
          ctx.emit('changed')
          load()
        }
      } catch (error: any) {
        message.error(error?.message || String(error))
      } finally {
        state.loading = false
      }
    }

    return { t, state, load, snapshot, handle }
  },
  render() {
    const { t, state, load, snapshot, handle } = this
    if (this.folder === 'function') {
      return (
        <NAlert type='info'>
          {t('data_api.studio.release_function_note')}
        </NAlert>
      )
    }
    return (
      <NSpace vertical size='large'>
        <NAlert type='info' title={t('data_api.studio.release_tip_title')}>
          {t('data_api.studio.release_tip_body')}
        </NAlert>
        {state.error && <NAlert type='error'>{state.error}</NAlert>}
        <SectionCard
          title={t('data_api.studio.release_list')}
          icon={CloudUploadOutlined}
        >
          {{
            extra: () => (
              <NSpace size='small'>
                <NInput
                  size='tiny'
                  v-model={[state.remark, 'value']}
                  style={{ width: '200px' }}
                  placeholder={t('data_api.studio.release_snapshot_remark_ph')}
                />
                <NButton
                  size='tiny'
                  type='primary'
                  loading={state.loading}
                  class='btn-release-snapshot'
                  onClick={snapshot}
                >
                  {t('data_api.studio.release_snapshot')}
                </NButton>
                <NButton size='tiny' onClick={load}>
                  {t('data_api.studio.refresh')}
                </NButton>
              </NSpace>
            ),
            default: () => (
              <NTable size='small' singleLine class={styles['readonly-table']}>
                <thead>
                  <tr>
                    <th>{t('data_api.studio.release_col_version')}</th>
                    <th>{t('data_api.studio.release_col_method')}</th>
                    <th>{t('data_api.studio.release_col_path')}</th>
                    <th>{t('data_api.studio.release_col_state')}</th>
                    <th>{t('data_api.studio.release_col_remark')}</th>
                    <th>{t('data_api.studio.release_col_by')}</th>
                    <th>{t('data_api.studio.version_col_time')}</th>
                    <th style={{ width: '170px' }}>
                      {t('data_api.studio.version_col_action')}
                    </th>
                  </tr>
                </thead>
                <tbody>
                  {state.items.length === 0 && (
                    <tr>
                      <td colspan={8}>
                        <span class={styles['section-hint']}>
                          {state.loading
                            ? t('data_api.studio.loading')
                            : t('data_api.studio.release_empty')}
                        </span>
                      </td>
                    </tr>
                  )}
                  {state.items.map((row) => (
                    <tr key={row.id}>
                      <td>v{row.version}</td>
                      <td>{row.method}</td>
                      <td>{row.apiPath}</td>
                      <td>
                        <NTag
                          size='small'
                          type={row.state === 'ONLINE' ? 'success' : 'warning'}
                        >
                          {row.state === 'ONLINE'
                            ? t('data_api.studio.release_state_online')
                            : t('data_api.studio.release_state_offline')}
                        </NTag>
                      </td>
                      <td>{row.remark || '-'}</td>
                      <td>{row.createBy || '-'}</td>
                      <td>{row.createTime || '-'}</td>
                      <td>
                        <NSpace size='small'>
                          <NPopconfirm
                            onPositiveClick={() =>
                              handle(rollbackDataApiRelease, row.id)
                            }
                          >
                            {{
                              default: () =>
                                t('data_api.studio.release_rollback_confirm', {
                                  version: `v${row.version}`
                                }),
                              trigger: () => (
                                <NButton size='tiny' type='primary'>
                                  {t('data_api.studio.release_rollback')}
                                </NButton>
                              )
                            }}
                          </NPopconfirm>
                          <NPopconfirm
                            onPositiveClick={() =>
                              handle(offlineDataApiRelease, row.id)
                            }
                          >
                            {{
                              default: () =>
                                t('data_api.studio.release_offline_confirm'),
                              trigger: () => (
                                <NButton size='tiny' type='warning'>
                                  {t('data_api.studio.release_offline')}
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
      </NSpace>
    )
  }
})
