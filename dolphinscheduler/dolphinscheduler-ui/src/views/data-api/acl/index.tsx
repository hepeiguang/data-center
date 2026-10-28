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
  NAlert,
  NButton,
  NDataTable,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NSelect,
  NSpace,
  NTag,
  useMessage
} from 'naive-ui'
import { SearchOutlined } from '@vicons/antd'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user/user'
import Card from '@/components/card'
import Modal from '@/components/modal'
import Search from '@/components/input-search'
import {
  createDataApiAcl,
  deleteDataApiAcl,
  listDataApiAcl,
  listDataApiPublishedApis
} from '@/service/modules/data-api'
import { listUser } from '@/service/modules/users'
import { queryAccessTokenList } from '@/service/modules/token'

type AclRow = {
  id: number
  apiPath: string
  subjectTypeName: string
  subjectId: string
  effectName: string
  remark?: string
}

const emptyForm = () => ({
  apiPath: '',
  subjectType: 'USER',
  subjectId: '',
  effect: 'ALLOW',
  remark: ''
})

const DataApiAcl = defineComponent({
  name: 'DataApiAcl',
  setup() {
    const { t } = useI18n()
    const message = useMessage()
    const userStore = useUserStore()
    const state = reactive({
      loading: false,
      showModal: false,
      confirmLoading: false,
      aclMode: 'optional',
      searchVal: '',
      items: [] as AclRow[],
      apiOptions: [] as { label: string; value: string }[],
      userOptions: [] as { label: string; value: string }[],
      tokenOptions: [] as { label: string; value: string }[],
      form: emptyForm()
    })

    const authHeaders = () => ({
      sessionId: userStore.getSessionId,
      token: userStore.getSessionId
    })

    const loadAcl = async () => {
      state.loading = true
      try {
        const res: any = await listDataApiAcl(
          authHeaders(),
          state.searchVal || undefined
        )
        state.aclMode = res.aclMode || 'optional'
        state.items = res.items || []
      } catch (e: any) {
        message.error(e.message || 'load acl failed')
      } finally {
        state.loading = false
      }
    }

    const loadOptions = async () => {
      try {
        const apis: any = await listDataApiPublishedApis(authHeaders())
        state.apiOptions = (apis.items || []).map((a: any) => ({
          label: `${a.method || 'GET'} ${a.path} (${a.name || ''})`,
          value: a.path
        }))
      } catch {
        /* ignore */
      }
      try {
        const users: any = await listUser()
        const list = Array.isArray(users) ? users : users?.totalList || users || []
        state.userOptions = list.map((u: any) => ({
          label: `${u.userName} (#${u.id})`,
          value: String(u.id)
        }))
      } catch {
        /* ignore */
      }
      try {
        const tokens: any = await queryAccessTokenList({ pageNo: 1, pageSize: 100 })
        const list = tokens?.totalList || []
        state.tokenOptions = list.map((tk: any) => ({
          label: `${tk.userName || ''} ${String(tk.token).slice(0, 8)}…`,
          value: tk.token
        }))
      } catch {
        /* ignore */
      }
    }

    const openCreate = () => {
      state.form = emptyForm()
      state.showModal = true
    }

    const onCancelModal = () => {
      state.showModal = false
      state.form = emptyForm()
    }

    const onConfirmModal = async () => {
      if (!state.form.apiPath || !state.form.subjectId) {
        message.warning(t('data_api.acl.form_required'))
        return
      }
      state.confirmLoading = true
      try {
        await createDataApiAcl(authHeaders(), {
          apiPath: state.form.apiPath,
          subjectType: state.form.subjectType,
          subjectId: state.form.subjectId,
          effect: state.form.effect,
          remark: state.form.remark
        })
        message.success(t('data_api.acl.create_ok'))
        state.showModal = false
        state.form = emptyForm()
        await loadAcl()
      } catch (e: any) {
        message.error(e?.response?.data || e.message || 'create failed')
      } finally {
        state.confirmLoading = false
      }
    }

    const onDelete = async (id: number) => {
      try {
        await deleteDataApiAcl(authHeaders(), id)
        message.success(t('data_api.acl.delete_ok'))
        await loadAcl()
      } catch (e: any) {
        message.error(e.message || 'delete failed')
      }
    }

    const onSearch = () => {
      loadAcl()
    }

    onMounted(async () => {
      await Promise.all([loadAcl(), loadOptions()])
    })

    return {
      t,
      ...toRefs(state),
      loadAcl,
      openCreate,
      onCancelModal,
      onConfirmModal,
      onDelete,
      onSearch
    }
  },
  render() {
    const {
      t,
      items,
      loading,
      aclMode,
      form,
      apiOptions,
      userOptions,
      tokenOptions,
      showModal,
      confirmLoading
    } = this
    const subjectOptions =
      form.subjectType === 'TOKEN'
        ? tokenOptions
        : form.subjectType === 'USER'
          ? userOptions
          : []

    const columns = [
      { title: 'ID', key: 'id', width: 70 },
      { title: t('data_api.acl.api_path'), key: 'apiPath' },
      {
        title: t('data_api.acl.subject_type'),
        key: 'subjectTypeName',
        width: 100,
        render: (row: AclRow) => <NTag size='small'>{row.subjectTypeName}</NTag>
      },
      {
        title: t('data_api.acl.subject_id'),
        key: 'subjectId',
        ellipsis: { tooltip: true }
      },
      {
        title: t('data_api.acl.effect'),
        key: 'effectName',
        width: 90,
        render: (row: AclRow) => (
          <NTag type={row.effectName === 'ALLOW' ? 'success' : 'error'} size='small'>
            {row.effectName}
          </NTag>
        )
      },
      { title: t('data_api.acl.remark'), key: 'remark' },
      {
        title: t('data_api.acl.actions'),
        key: 'actions',
        width: 100,
        render: (row: AclRow) => (
          <NButton size='small' type='error' onClick={() => this.onDelete(row.id)}>
            {t('data_api.acl.delete')}
          </NButton>
        )
      }
    ]

    return (
      <NSpace vertical>
        <NAlert type='warning' title={t('data_api.acl.tip_title')}>
          <div>{t('data_api.acl.tip_body', { mode: aclMode })}</div>
        </NAlert>
        <NSpace vertical>
          <Card>
            <NSpace justify='space-between'>
              <NButton size='small' type='primary' onClick={this.openCreate}>
                {t('data_api.acl.create')}
              </NButton>
              <NSpace>
                <Search
                  v-model:value={this.searchVal}
                  placeholder={t('data_api.acl.search_tips')}
                  onSearch={this.onSearch}
                />
                <NButton size='small' type='primary' onClick={this.onSearch}>
                  <NIcon>
                    <SearchOutlined />
                  </NIcon>
                </NButton>
              </NSpace>
            </NSpace>
          </Card>
          <Card title={t('menu.data_api_acl')}>
            <NDataTable
              loading={loading}
              columns={columns}
              data={items || []}
              row-class-name='items'
            />
          </Card>
        </NSpace>
        <Modal
          show={showModal}
          title={t('data_api.acl.create_title')}
          onCancel={this.onCancelModal}
          onConfirm={this.onConfirmModal}
          confirmLoading={confirmLoading}
        >
          <NForm label-placement='left' label-width={100}>
            <NFormItem label={t('data_api.acl.api_path')} required>
              <NSelect
                filterable
                tag
                clearable
                options={apiOptions}
                v-model:value={form.apiPath}
                placeholder='/smoke/select1 or /smoke/**'
              />
            </NFormItem>
            <NFormItem label={t('data_api.acl.subject_type')} required>
              <NSelect
                options={[
                  { label: 'USER', value: 'USER' },
                  { label: 'TOKEN', value: 'TOKEN' },
                  { label: 'APP (reserved)', value: 'APP', disabled: true }
                ]}
                v-model:value={form.subjectType}
                onUpdateValue={() => {
                  form.subjectId = ''
                }}
              />
            </NFormItem>
            <NFormItem label={t('data_api.acl.subject_id')} required>
              <NSelect
                filterable
                tag
                clearable
                options={subjectOptions}
                v-model:value={form.subjectId}
                placeholder={t('data_api.acl.subject_id_ph')}
              />
            </NFormItem>
            <NFormItem label={t('data_api.acl.effect')} required>
              <NSelect
                options={[
                  { label: 'ALLOW', value: 'ALLOW' },
                  { label: 'DENY', value: 'DENY' }
                ]}
                v-model:value={form.effect}
              />
            </NFormItem>
            <NFormItem label={t('data_api.acl.remark')}>
              <NInput v-model:value={form.remark} />
            </NFormItem>
          </NForm>
        </Modal>
      </NSpace>
    )
  }
})

export default DataApiAcl
