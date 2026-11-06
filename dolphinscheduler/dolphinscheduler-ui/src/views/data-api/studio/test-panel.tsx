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

import { computed, defineComponent, PropType, reactive, watch } from 'vue'
import {
  NAlert,
  NButton,
  NCode,
  NIcon,
  NSpace,
  NTable,
  NTag,
  useMessage
} from 'naive-ui'
import {
  ClearOutlined,
  CopyOutlined,
  HistoryOutlined,
  LineChartOutlined,
  SendOutlined,
  ThunderboltOutlined
} from '@vicons/antd'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user/user'
import SectionCard from './section-card'
import MonacoEditor from '@/components/monaco-editor'
import {
  apiPathOf,
  callGateway,
  gatewayUrlOf,
  type GatewayResult,
  type GroupInfo,
  type ResourceDetail
} from '@/service/modules/data-api/magic'
import DefinitionTable from './definition-table'
import styles from './index.module.scss'

type Row = Record<string, any>

const BODY_METHODS = ['POST', 'PUT', 'PATCH']

const props = {
  detail: {
    type: Object as PropType<ResourceDetail>,
    required: true as const
  },
  groups: { type: Array as PropType<GroupInfo[]>, default: () => [] },
  revision: { type: Number, default: 0 },
  folder: { type: String as PropType<'api' | 'function'>, default: 'api' },
  ensureSaved: {
    type: Function as PropType<() => Promise<boolean>>,
    required: true as const
  }
}

const pathVarNames = (path: string) => {
  const names = new Set<string>()
  const braces = /\{(\w+)\}/g
  const colons = /:([A-Za-z0-9_]+)/g
  let match = braces.exec(path)
  while (match) {
    names.add(match[1])
    match = braces.exec(path)
  }
  let colonMatch = colons.exec(path)
  while (colonMatch) {
    names.add(colonMatch[1])
    colonMatch = colons.exec(path)
  }
  return [...names]
}

const toMap = (rows: Row[]) =>
  rows
    .filter((row) => row.name && row.value !== '' && row.value != null)
    .reduce((acc: Record<string, string>, row) => {
      acc[String(row.name)] = String(row.value)
      return acc
    }, {})

export default defineComponent({
  name: 'TestPanel',
  props,
  emits: ['tested', 'saveSample'],
  setup(props, ctx) {
    const { t } = useI18n()
    const message = useMessage()
    const userStore = useUserStore()
    const state = reactive({
      query: [] as Row[],
      pathVars: [] as Row[],
      headers: [] as Row[],
      body: '',
      sending: false,
      result: null as GatewayResult | null,
      history: [] as Row[]
    })

    const sync = () => {
      const detail = props.detail
      state.query = (detail.parameters || []).map((param) => ({
        name: param.name,
        value: param.defaultValue || ''
      }))
      const declared = (detail.paths || []).map((param) =>
        String(param.name || '')
      )
      const scanned = pathVarNames(detail.path || '')
      state.pathVars = [...new Set([...declared, ...scanned])]
        .filter((name) => name)
        .map((name) => ({ name, value: '' }))
      state.headers = (detail.headers || []).map((header) => ({
        name: header.name,
        value: header.value == null ? '' : String(header.value)
      }))
      state.body = detail.requestBody || ''
      state.result = null
    }

    watch(() => props.revision, sync, { immediate: true })

    const requestPath = computed(() => {
      const detail = props.detail
      let path = apiPathOf(detail, props.groups)
      state.pathVars.forEach((row) => {
        if (!row.name) return
        const value = encodeURIComponent(String(row.value ?? ''))
        path = path
          .replace(new RegExp(`\\{${row.name}\\}`, 'g'), value)
          .replace(new RegExp(`:${row.name}`, 'g'), value)
      })
      return path
    })

    const fullUrl = computed(
      () =>
        `${window.location.origin}${gatewayUrlOf(props.detail, props.groups)}`
    )

    const prettyBody = computed(() => {
      const raw = state.result?.body || ''
      try {
        return JSON.stringify(JSON.parse(raw), null, 2)
      } catch (error) {
        return raw
      }
    })

    const send = async () => {
      state.sending = true
      try {
        const saved = await props.ensureSaved()
        if (!saved) {
          message.error(t('data_api.studio.test_save_failed'))
          return
        }
        if (!props.detail.id) {
          message.error(t('data_api.studio.test_need_id'))
          return
        }
        const method = (props.detail.method || 'GET').toUpperCase()
        const withBody = BODY_METHODS.includes(method)
        const result = await callGateway({
          auth: { sessionId: userStore.getSessionId || '' },
          method,
          path: requestPath.value,
          query: withBody ? undefined : toMap(state.query),
          headers: toMap(state.headers),
          body: withBody ? state.body : undefined,
          scriptId: props.detail.id
        })
        state.result = result
        state.history = [
          {
            time: new Date().toLocaleTimeString(),
            method,
            path: requestPath.value,
            status: result.status,
            durationMs: result.durationMs,
            result
          },
          ...state.history
        ].slice(0, 10)
        ctx.emit('tested', result.body)
        if (result.error) message.error(result.error)
      } finally {
        state.sending = false
      }
    }

    const copyUrl = async () => {
      try {
        await navigator.clipboard.writeText(fullUrl.value)
        message.success(t('data_api.studio.copied'))
      } catch (error) {
        message.error(String(error))
      }
    }

    const copyBody = async () => {
      try {
        await navigator.clipboard.writeText(prettyBody.value)
        message.success(t('data_api.studio.copied'))
      } catch (error) {
        message.error(String(error))
      }
    }

    const saveAsSample = () => {
      if (!state.result?.body) return
      ctx.emit('saveSample', prettyBody.value)
      message.success(t('data_api.studio.test_sample_saved'))
    }

    return {
      t,
      state,
      send,
      copyUrl,
      copyBody,
      saveAsSample,
      fullUrl,
      prettyBody,
      requestPath
    }
  },
  render() {
    const {
      t,
      state,
      send,
      copyUrl,
      copyBody,
      saveAsSample,
      fullUrl,
      prettyBody
    } = this
    if (this.folder === 'function') {
      return (
        <NAlert type='info'>{t('data_api.studio.test_function_note')}</NAlert>
      )
    }
    const method = (this.detail.method || 'GET').toUpperCase()
    const withBody = BODY_METHODS.includes(method)
    const statusType =
      state.result == null
        ? 'default'
        : state.result.status >= 200 && state.result.status < 300
        ? 'success'
        : 'error'
    return (
      <NSpace vertical size='large'>
        <NAlert type='info' title={t('data_api.studio.test_tip')}>
          {t('data_api.studio.test_body_tip')}
        </NAlert>
        <SectionCard
          title={t('data_api.studio.test_request')}
          icon={ThunderboltOutlined}
        >
          <div class={styles['path-line']}>
            <NTag size='small' type='info'>
              {method}
            </NTag>
            <span>{fullUrl}</span>
            <NButton size='tiny' quaternary onClick={copyUrl}>
              {{
                icon: () => (
                  <NIcon>
                    <CopyOutlined />
                  </NIcon>
                ),
                default: () => t('data_api.studio.copy')
              }}
            </NButton>
          </div>
          <NSpace vertical size='medium' style={{ marginTop: '12px' }}>
            <div>
              <div class={styles['section-title']}>
                {t('data_api.studio.test_query')}
              </div>
              <DefinitionTable
                columns={[
                  {
                    key: 'name',
                    label: t('data_api.studio.col_name'),
                    width: '40%'
                  },
                  { key: 'value', label: t('data_api.studio.col_value') }
                ]}
                v-model={[state.query, 'value']}
                addLabel={t('data_api.studio.add_row')}
                emptyText={t('data_api.studio.no_rows')}
              />
            </div>
            {state.pathVars.length > 0 && (
              <div>
                <div class={styles['section-title']}>
                  {t('data_api.studio.test_path_vars')}
                </div>
                <DefinitionTable
                  columns={[
                    {
                      key: 'name',
                      label: t('data_api.studio.col_name'),
                      width: '40%'
                    },
                    { key: 'value', label: t('data_api.studio.col_value') }
                  ]}
                  v-model={[state.pathVars, 'value']}
                />
              </div>
            )}
            <div>
              <div class={styles['section-title']}>
                {t('data_api.studio.test_headers')}
              </div>
              <DefinitionTable
                columns={[
                  {
                    key: 'name',
                    label: t('data_api.studio.col_name'),
                    width: '40%'
                  },
                  { key: 'value', label: t('data_api.studio.col_value') }
                ]}
                v-model={[state.headers, 'value']}
                addLabel={t('data_api.studio.add_row')}
                emptyText={t('data_api.studio.test_header_empty')}
              />
            </div>
            {withBody && (
              <div>
                <div class={styles['section-title']}>
                  {t('data_api.studio.test_body')}
                </div>
                <MonacoEditor
                  v-model={[state.body, 'value']}
                  height='160px'
                  options={{
                    language: 'json',
                    minimap: { enabled: false },
                    scrollBeyondLastLine: false,
                    lineNumbers: 'on'
                  }}
                />
              </div>
            )}
            <NSpace>
              <NButton
                type='primary'
                size='small'
                loading={state.sending}
                class='btn-send-api-test'
                onClick={send}
              >
                {{
                  icon: () => (
                    <NIcon>
                      <SendOutlined />
                    </NIcon>
                  ),
                  default: () => t('data_api.studio.test_send')
                }}
              </NButton>
              {state.history.length > 0 && (
                <NButton
                  size='small'
                  quaternary
                  class='btn-clear-api-test-history'
                  onClick={() => (state.history = [])}
                >
                  {{
                    icon: () => (
                      <NIcon>
                        <ClearOutlined />
                      </NIcon>
                    ),
                    default: () => t('data_api.studio.test_clear')
                  }}
                </NButton>
              )}
            </NSpace>
          </NSpace>
        </SectionCard>
        <SectionCard
          title={t('data_api.studio.test_response')}
          icon={LineChartOutlined}
        >
          {state.result == null ? (
            <span class={styles['section-hint']}>
              {t('data_api.studio.test_none')}
            </span>
          ) : (
            <>
              <div class={styles['meta-list']}>
                <span class={styles['meta-item']}>
                  {t('data_api.studio.test_status')}:{' '}
                  <NTag size='small' type={statusType}>
                    {state.result.status || 'ERR'}
                  </NTag>
                </span>
                <span class={styles['meta-item']}>
                  {t('data_api.studio.test_duration')}:{' '}
                  {state.result.durationMs} ms
                </span>
                <span class={styles['meta-item']}>
                  {t('data_api.studio.test_size')}: {state.result.size} B
                </span>
                {state.result.contentType && (
                  <span class={styles['meta-item']}>
                    Content-Type: {state.result.contentType}
                  </span>
                )}
              </div>
              {state.result.error && (
                <NAlert type='error' title={t('data_api.studio.test_failed')}>
                  {state.result.error}
                </NAlert>
              )}
              <NSpace size='small' style={{ marginBottom: '8px' }}>
                <NButton size='tiny' onClick={copyBody}>
                  {t('data_api.studio.copy')}
                </NButton>
                <NButton size='tiny' onClick={saveAsSample}>
                  {t('data_api.studio.test_save_as_sample')}
                </NButton>
              </NSpace>
              <div class={styles['response-box']}>
                <NCode word-wrap code={prettyBody} />
              </div>
            </>
          )}
        </SectionCard>
        {state.history.length > 0 && (
          <SectionCard
            title={t('data_api.studio.test_history')}
            icon={HistoryOutlined}
          >
            <NTable size='small' singleLine class={styles['readonly-table']}>
              <thead>
                <tr>
                  <th>{t('data_api.studio.version_col_time')}</th>
                  <th>Method</th>
                  <th>Path</th>
                  <th>{t('data_api.studio.test_status')}</th>
                  <th>{t('data_api.studio.test_duration')}</th>
                </tr>
              </thead>
              <tbody>
                {state.history.map((row, index) => (
                  <tr
                    key={index}
                    style={{ cursor: 'pointer' }}
                    onClick={() => (state.result = row.result as GatewayResult)}
                  >
                    <td>{row.time}</td>
                    <td>{row.method}</td>
                    <td>{row.path}</td>
                    <td>{row.status}</td>
                    <td>{row.durationMs} ms</td>
                  </tr>
                ))}
              </tbody>
            </NTable>
          </SectionCard>
        )}
      </NSpace>
    )
  }
})
