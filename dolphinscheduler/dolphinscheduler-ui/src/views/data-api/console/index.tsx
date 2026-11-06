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

import { computed, defineComponent, h, onMounted, reactive } from 'vue'
import {
  NAlert,
  NButton,
  NDataTable,
  NEllipsis,
  NEmpty,
  NSpin,
  NTag,
  NSelect,
  NSpace,
  NTooltip,
  useMessage
} from 'naive-ui'
import type { DataTableColumns } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user/user'
import Card from '@/components/card'
import MonacoEditor from '@/components/monaco-editor'
import {
  cancelConsoleQuery,
  executeConsoleSql,
  exportConsoleCsv,
  listConsoleDatasources,
  listConsoleHistory
} from '@/service/modules/data-api'
import styles from './index.module.scss'

type DsOption = {
  id: number
  name?: string
  type?: string
  database?: string
  maxRows?: number
}

type HistoryItem = {
  id: number
  datasourceId: number
  sqlExcerpt: string
  sqlSha256: string
  rowCount: number
  latencyMs: number
  resultCode: number
  rejectReason?: string
  createTime: string
}

type ColumnMeta = { name: string; type: string; nullable: boolean }

const DataApiConsole = defineComponent({
  name: 'DataApiConsole',
  setup() {
    const { t } = useI18n()
    const message = useMessage()
    const userStore = useUserStore()
    const state = reactive({
      loadingDs: false,
      running: false,
      exporting: false,
      datasources: [] as DsOption[],
      datasourceId: null as number | null,
      sql: '',
      columns: [] as ColumnMeta[],
      rows: [] as any[][],
      rowCount: 0,
      truncated: false,
      elapsedMs: 0,
      queryId: '' as string,
      error: '',
      history: [] as HistoryItem[],
      historyLoading: false
    })

    const authHeaders = () => ({
      sessionId: userStore.getSessionId,
      token: userStore.getSessionId
    })

    // data-api answers rejections with {code, message}; anything else is the success body
    const failure = (res: any): string => {
      if (
        res &&
        typeof res === 'object' &&
        'code' in res &&
        !('columns' in res)
      ) {
        return res.message || `error ${res.code}`
      }
      if (typeof res === 'string') {
        return res
      }
      if (
        res &&
        typeof res === 'object' &&
        'status' in res &&
        res.status >= 400
      ) {
        return res.error || `HTTP ${res.status}`
      }
      return ''
    }

    const dsOptions = computed(() =>
      state.datasources.map((d) => ({
        label: `${d.name || d.id}${d.type ? ' · ' + d.type : ''}${
          d.database ? ' · ' + d.database : ''
        }`,
        value: d.id
      }))
    )

    const currentDs = computed(
      () => state.datasources.find((d) => d.id === state.datasourceId) || null
    )

    const loadDatasources = async () => {
      state.loadingDs = true
      try {
        const res: any = await listConsoleDatasources(authHeaders())
        const fail = failure(res)
        if (fail) {
          state.error = fail
          state.datasources = []
          return
        }
        state.error = ''
        state.datasources = res.items || []
        if (!state.datasourceId && state.datasources.length > 0) {
          state.datasourceId = state.datasources[0].id
        }
      } catch (e: any) {
        state.error = e.message || 'load failed'
      } finally {
        state.loadingDs = false
      }
    }

    const loadHistory = async () => {
      state.historyLoading = true
      try {
        const res: any = await listConsoleHistory(authHeaders(), 50)
        state.history = failure(res) ? [] : res.items || []
      } catch (e: any) {
        message.error(e.message || 'history failed')
      } finally {
        state.historyLoading = false
      }
    }

    const validate = (): boolean => {
      if (!state.datasourceId) {
        state.error = t('data_api.console.pick_datasource')
        return false
      }
      if (!state.sql.trim()) {
        state.error = t('data_api.console.empty_sql')
        return false
      }
      return true
    }

    const onRun = async () => {
      state.error = ''
      if (!validate()) {
        return
      }
      state.running = true
      try {
        const res: any = await executeConsoleSql(authHeaders(), {
          datasourceId: state.datasourceId as number,
          sql: state.sql
        })
        const fail = failure(res)
        if (fail) {
          state.error = fail
          state.columns = []
          state.rows = []
        } else {
          state.columns = res.columns || []
          state.rows = res.rows || []
          state.rowCount = res.rowCount || 0
          state.truncated = !!res.truncated
          state.elapsedMs = res.elapsedMs || 0
          state.queryId = res.queryId || ''
        }
      } catch (e: any) {
        state.error = e.message || 'execute failed'
      } finally {
        state.running = false
        loadHistory()
      }
    }

    const onCancel = async () => {
      if (!state.queryId) {
        return
      }
      try {
        const res: any = await cancelConsoleQuery(authHeaders(), state.queryId)
        if (res?.cancelled) {
          message.info(t('data_api.console.cancel_ok'))
        } else {
          message.info(t('data_api.console.cancel_miss'))
        }
      } catch (e: any) {
        message.error(e.message || 'cancel failed')
      }
    }

    const onExport = async () => {
      state.error = ''
      if (!validate()) {
        return
      }
      state.exporting = true
      try {
        const res: any = await exportConsoleCsv(authHeaders(), {
          datasourceId: state.datasourceId as number,
          sql: state.sql
        })
        if (res instanceof Blob) {
          const url = window.URL.createObjectURL(res)
          const a = document.createElement('a')
          a.href = url
          a.download = `data-api-query-${Date.now()}.csv`
          a.click()
          window.URL.revokeObjectURL(url)
          message.success(t('data_api.console.export_ok'))
        } else {
          state.error = res.message || 'export failed'
        }
      } catch (e: any) {
        state.error = e.message || 'export failed'
      } finally {
        state.exporting = false
        loadHistory()
      }
    }

    const useHistory = (item: HistoryItem) => {
      state.sql = item.sqlExcerpt || ''
      state.datasourceId = item.datasourceId
    }

    const resultColumns = computed<DataTableColumns<any[]>>(() =>
      state.columns.map((c, i) => ({
        title: c.name,
        key: i,
        ellipsis: { tooltip: false },
        render: (row: any[]) => {
          const v = row[i]
          if (v === null || v === undefined) {
            return h('span', { class: styles.null }, 'NULL')
          }
          return String(v)
        },
        sorter: (a: any[], b: any[]) =>
          String(a[i] ?? '').localeCompare(String(b[i] ?? ''), undefined, {
            numeric: true
          })
      }))
    )

    onMounted(() => {
      loadDatasources()
      loadHistory()
    })

    return {
      t,
      state,
      dsOptions,
      currentDs,
      resultColumns,
      onKeyup: (e: KeyboardEvent) => {
        if ((e.ctrlKey || e.metaKey) && e.key === 'Enter') {
          onRun()
        }
      },
      onRun,
      onCancel,
      onExport,
      useHistory,
      loadDatasources,
      loadHistory
    }
  },
  render() {
    const { t, state } = this
    return (
      <div class={styles.console}>
        <Card
          title={t('data_api.console.title')}
          content-style={{ padding: '12px' }}
        >
          <NSpace vertical size={12}>
            <NSpace align='center' wrap>
              <NSelect
                value={state.datasourceId}
                options={this.dsOptions}
                loading={state.loadingDs}
                style='min-width: 320px'
                placeholder={t('data_api.console.pick_datasource')}
                onUpdateValue={(v: number) => (state.datasourceId = v)}
              />
              <NTooltip>
                {{
                  trigger: () => (
                    <NButton
                      type='primary'
                      loading={state.running}
                      disabled={!state.datasourceId}
                      onClick={this.onRun}
                    >
                      {t('data_api.console.run')}
                    </NButton>
                  ),
                  default: () => t('data_api.console.run_tips')
                }}
              </NTooltip>
              <NButton
                quaternary
                disabled={!state.running && !state.queryId}
                onClick={this.onCancel}
              >
                {t('data_api.console.cancel')}
              </NButton>
              <NButton
                secondary
                loading={state.exporting}
                disabled={!state.datasourceId}
                onClick={this.onExport}
              >
                {t('data_api.console.export')}
              </NButton>
              {this.currentDs?.maxRows && (
                <NTag size='small' type='info'>
                  {t('data_api.console.max_rows')}: {this.currentDs.maxRows}
                </NTag>
              )}
            </NSpace>
            {state.error && (
              <NAlert type='error' closable onClose={() => (state.error = '')}>
                {state.error}
              </NAlert>
            )}
            <div class={styles.editor} onKeyup={this.onKeyup}>
              <MonacoEditor
                value={state.sql}
                height='100%'
                onUpdate:value={(v: string) => (state.sql = v)}
                options={{
                  language: 'sql',
                  minimap: { enabled: false },
                  automaticLayout: true,
                  scrollBeyondLastLine: false,
                  fontSize: 13,
                  lineNumbersMinChars: 4
                }}
              />
            </div>
            <NSpace align='center' size={12}>
              <span class={styles.status}>
                {state.rowCount > 0 &&
                  `${t('data_api.console.rows')}: ${state.rowCount}`}
                {state.elapsedMs > 0 &&
                  ` · ${t('data_api.console.elapsed')}: ${state.elapsedMs} ms`}
              </span>
              {state.truncated && (
                <NTag size='small' type='warning'>
                  {t('data_api.console.truncated')}
                </NTag>
              )}
            </NSpace>
            <div class={styles.result}>
              {state.columns.length === 0 ? (
                <NEmpty description={t('data_api.console.no_result')} />
              ) : (
                <NDataTable
                  size='small'
                  striped
                  columns={this.resultColumns as any}
                  data={state.rows}
                  max-height={420}
                  pagination={{ pageSize: 200 }}
                  scrollX={1000}
                />
              )}
            </div>
          </NSpace>
        </Card>
        <Card
          title={t('data_api.console.history')}
          class={styles.history_card}
          content-style={{ padding: '8px' }}
        >
          <NSpin show={state.historyLoading}>
            <div class={styles.history_list}>
              {state.history.length === 0 && (
                <NEmpty
                  size='small'
                  description={t('data_api.console.history_empty')}
                />
              )}
              {state.history.map((item) => (
                <div
                  key={item.id}
                  class={styles.history_item}
                  onClick={() => this.useHistory(item)}
                >
                  <NSpace justify='space-between' align='center' size={4}>
                    <span class={styles.history_time}>{item.createTime}</span>
                    <NTag
                      size='tiny'
                      type={item.resultCode === 200 ? 'success' : 'error'}
                    >
                      {item.resultCode}
                    </NTag>
                  </NSpace>
                  <NEllipsis
                    class={styles.history_sql}
                    line-clamp={2}
                    expand-on-click={false}
                  >
                    {item.sqlExcerpt || item.rejectReason || '-'}
                  </NEllipsis>
                  {item.rejectReason && (
                    <span class={styles.history_reject}>
                      {item.rejectReason}
                    </span>
                  )}
                </div>
              ))}
            </div>
          </NSpin>
        </Card>
      </div>
    )
  }
})

export default DataApiConsole
