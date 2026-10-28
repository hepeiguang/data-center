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

import { defineComponent, onMounted, reactive, toRefs } from 'vue'
import {
  NButton,
  NDataTable,
  NSpace,
  NTag,
  NAlert,
  NCode,
  useMessage
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user/user'
import Card from '@/components/card'
import {
  listDataApiDatasources,
  reloadDataApiDatasources,
  pingDataApiDatasource
} from '@/service/modules/data-api'

type DsItem = {
  key: string
  name?: string
  displayName?: string
  type?: string
  id?: number
  database?: string
  scriptHint?: string
}

const DataApiDatasources = defineComponent({
  name: 'DataApiDatasources',
  setup() {
    const { t } = useI18n()
    const message = useMessage()
    const userStore = useUserStore()
    const state = reactive({
      loading: false,
      items: [] as DsItem[],
      loadDbTypes: [] as string[],
      maxRows: 0,
      pingResult: '' as string
    })

    const authHeaders = () => ({
      sessionId: userStore.getSessionId,
      token: userStore.getSessionId
    })

    const applyRes = (res: any) => {
      state.items = res.items || (res.keys || []).map((key: string) => ({ key }))
      state.loadDbTypes = res.loadDbTypes || []
      state.maxRows = res.maxRows || 0
    }

    const load = async () => {
      state.loading = true
      try {
        const res: any = await listDataApiDatasources(authHeaders())
        applyRes(res)
      } catch (e: any) {
        message.error(e.message || 'load failed')
      } finally {
        state.loading = false
      }
    }

    const onReload = async () => {
      state.loading = true
      try {
        const res: any = await reloadDataApiDatasources(authHeaders())
        applyRes(res)
        message.success(t('data_api.datasources.reload_ok'))
      } catch (e: any) {
        message.error(e.message || 'reload failed')
      } finally {
        state.loading = false
      }
    }

    const onPing = async (key: string) => {
      try {
        const res: any = await pingDataApiDatasource(key, authHeaders())
        state.pingResult = `${key}: ${JSON.stringify(res)}`
        if (res.ok) {
          message.success(`${key} OK`)
        } else {
          message.warning(res.error || `${key} failed`)
        }
      } catch (e: any) {
        state.pingResult = `${key}: ${e.message || 'error'}`
        message.error(e.message || 'ping failed')
      }
    }

    onMounted(load)

    return {
      t,
      ...toRefs(state),
      load,
      onReload,
      onPing
    }
  },
  render() {
    const { t, items, loadDbTypes, maxRows, loading, pingResult } = this
    const columns = [
      {
        title: t('data_api.datasources.key'),
        key: 'key',
        render: (row: DsItem) => <NTag type='info'>{row.key}</NTag>
      },
      {
        title: t('data_api.datasources.name'),
        key: 'name'
      },
      {
        title: t('data_api.datasources.type'),
        key: 'type',
        width: 100
      },
      {
        title: t('data_api.datasources.ds_id'),
        key: 'id',
        width: 80
      },
      {
        title: t('data_api.datasources.database'),
        key: 'database'
      },
      {
        title: t('data_api.datasources.script'),
        key: 'scriptHint',
        render: (row: DsItem) => (
          <NCode code={row.scriptHint || `db.${row.key}.select('select 1')`} language='js' />
        )
      },
      {
        title: t('data_api.datasources.actions'),
        key: 'actions',
        width: 100,
        render: (row: DsItem) => (
          <NButton size='tiny' onClick={() => this.onPing(row.key)}>
            Ping
          </NButton>
        )
      }
    ]

    return (
      <NSpace vertical>
        <NAlert type='info'>{t('data_api.datasources.tip')}</NAlert>
        <Card title={t('menu.data_api_datasources')}>
          <NSpace vertical>
            <NSpace>
              <NButton type='primary' loading={loading} onClick={this.onReload}>
                {t('data_api.datasources.reload')}
              </NButton>
              <NButton onClick={this.load}>{t('data_api.datasources.refresh')}</NButton>
              <NTag>maxRows={maxRows}</NTag>
              {(loadDbTypes || []).map((tp: string) => (
                <NTag key={tp} type='success'>
                  {tp}
                </NTag>
              ))}
            </NSpace>
            <NDataTable loading={loading} columns={columns} data={items || []} />
            {pingResult ? <NAlert type='default'>{pingResult}</NAlert> : null}
          </NSpace>
        </Card>
      </NSpace>
    )
  }
})

export default DataApiDatasources
