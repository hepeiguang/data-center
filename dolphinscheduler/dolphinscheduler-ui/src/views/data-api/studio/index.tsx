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

import {
  computed,
  defineComponent,
  onBeforeUnmount,
  onMounted,
  reactive
} from 'vue'
import {
  NAlert,
  NButton,
  NDropdown,
  NIcon,
  NInput,
  NPopconfirm,
  NSpace,
  NSpin,
  NTabPane,
  NTabs,
  NTag,
  NTree,
  useMessage,
  type TreeOption
} from 'naive-ui'
import {
  ApiOutlined,
  BookOutlined,
  CloudUploadOutlined,
  CodeOutlined,
  DownloadOutlined,
  ExperimentOutlined,
  FileAddOutlined,
  FileSearchOutlined,
  FileTextOutlined,
  FolderAddOutlined,
  FolderOutlined,
  FunctionOutlined,
  HistoryOutlined,
  LockOutlined,
  ReloadOutlined,
  SaveOutlined,
  SearchOutlined,
  UnlockOutlined,
  UploadOutlined
} from '@vicons/antd'
import SectionCard from './section-card'
import cookies from 'js-cookie'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user/user'
import Card from '@/components/card'
import Modal from '@/components/modal'
import BasicPanel from './basic-panel'
import ScriptPanel from './script-panel'
import TestPanel from './test-panel'
import UsagePanel from './usage-panel'
import VersionPanel from './version-panel'
import ReleasePanel from './release-panel'
import styles from './index.module.scss'
import {
  ROOT_GROUP_ID,
  apiPathOf,
  checkMagicSession,
  deleteResource,
  exportResources,
  fetchMagicOptions,
  fetchMagicTree,
  fetchResource,
  flattenGroups,
  importResources,
  lockResource,
  moveResource,
  reloadResources,
  saveGroup,
  saveResource,
  searchScripts,
  unlockResource,
  type GroupInfo,
  type MagicTreeNode,
  type ResourceDetail,
  type ResourceFolder,
  type ResourceNode
} from '@/service/modules/data-api/magic'
import { listDataApiDatasources } from '@/service/modules/data-api'

const METHODS = ['GET', 'POST', 'PUT', 'DELETE', 'HEAD', 'PATCH', 'OPTIONS']

const NODE_MAP: Record<
  string,
  { id: string; folder: ResourceFolder; isGroup: boolean; node: ResourceNode }
> = {}

const messageOf = (error: unknown) =>
  error instanceof Error ? error.message : String(error)

const filterTree = (nodes: TreeOption[], pattern: string) => {
  const keyword = pattern.toLowerCase()
  return nodes.some((node) =>
    String(node.label || '')
      .toLowerCase()
      .includes(keyword)
  )
}

const DataApiStudio = defineComponent({
  name: 'DataApiStudio',
  setup() {
    const { t } = useI18n()
    const message = useMessage()
    const userStore = useUserStore()
    const auth = computed(() => ({
      sessionId:
        userStore.getSessionId || String(cookies.get('sessionId') || '')
    }))
    const state = reactive({
      ready: false,
      treeLoading: false,
      saving: false,
      treeData: [] as TreeOption[],
      groups: [] as GroupInfo[],
      functionGroups: [] as GroupInfo[],
      optionMeta: [] as string[][],
      hints: [] as { key: string; displayName?: string; scriptHint?: string }[],
      maxRows: 0,
      selectedKey: '',
      folder: 'api' as ResourceFolder,
      detail: null as ResourceDetail | null,
      baseline: '',
      revision: 0,
      tab: 'basic',
      pattern: '',
      lastResponse: '',
      serviceError: '',
      sideError: '',
      refreshError: '',
      createModal: {
        show: false,
        mode: 'create' as 'create' | 'edit',
        kind: 'api' as 'api' | 'group' | 'function',
        folder: 'api' as ResourceFolder,
        id: '',
        name: '',
        path: '',
        parentId: ROOT_GROUP_ID
      },
      moveModal: { show: false, src: '', groupId: ROOT_GROUP_ID },
      deleteModal: { show: false, id: '', name: '' },
      searchModal: { show: false, keyword: '', items: [] as any[] }
    })

    const groupsOf = (folder: ResourceFolder) =>
      folder === 'function' ? state.functionGroups : state.groups

    const locked = computed(() => String(state.detail?.lock) === '1')
    const errorTip = (message: string) =>
      message === 'MAGIC_UNAUTHORIZED'
        ? t('data_api.studio.unauthorized_tip')
        : message.startsWith('MAGIC_UNREACHABLE')
        ? t('data_api.studio.service_offline')
        : message
    const dirty = computed(() => {
      if (!state.detail) return false
      return JSON.stringify(buildEntity()) !== state.baseline
    })
    const currentApiPath = computed(() =>
      state.detail ? apiPathOf(state.detail, groupsOf(state.folder)) : ''
    )

    const toOption = (
      item: MagicTreeNode,
      folder: ResourceFolder
    ): TreeOption => {
      const node = item.node
      const isGroup = !!node.type
      const key = `${isGroup ? 'g' : 'f'}:${node.id}`
      NODE_MAP[key] = { id: node.id, folder, isGroup, node }
      const children = isGroup
        ? (item.children || []).map((child) => toOption(child, folder))
        : undefined
      return {
        key,
        label: node.name,
        isLeaf: !isGroup,
        children,
        method: node.method,
        locked: isGroup ? undefined : String(node.lock) === '1'
      } as TreeOption
    }

    const loadTree = async () => {
      state.treeLoading = true
      try {
        const tree = await fetchMagicTree(auth.value)
        Object.keys(NODE_MAP).forEach((key) => delete NODE_MAP[key])
        const emptyRoot = (): MagicTreeNode => ({
          node: { id: ROOT_GROUP_ID, name: 'root' },
          children: []
        })
        const apiRoot = (tree.api as unknown as MagicTreeNode) || emptyRoot()
        const functionRoot =
          (tree.function as unknown as MagicTreeNode) || emptyRoot()
        const options: TreeOption[] = []
        options.push({
          key: 'root:api',
          label: t('data_api.studio.tree_title_api'),
          isLeaf: false,
          children: (apiRoot.children || []).map((child) =>
            toOption(child, 'api')
          )
        } as TreeOption)
        options.push({
          key: 'root:function',
          label: t('data_api.studio.tree_title_function'),
          isLeaf: false,
          children: (functionRoot.children || []).map((child) =>
            toOption(child, 'function')
          )
        } as TreeOption)
        state.treeData = options
        state.groups = flattenGroups(apiRoot, 'api')
        state.functionGroups = flattenGroups(functionRoot, 'function')
        state.serviceError = ''
        state.refreshError = ''
      } catch (error) {
        // a refresh failure must not wipe an already rendered editor, unsaved edits would vanish with it
        if (state.treeData.length > 0) state.refreshError = messageOf(error)
        else state.serviceError = messageOf(error)
      } finally {
        state.treeLoading = false
      }
    }

    const loadSideData = async () => {
      try {
        const [datasources, options] = await Promise.all([
          listDataApiDatasources(auth.value),
          fetchMagicOptions(auth.value)
        ])
        // /data-api answers 4xx with a bare string or its own envelope; neither may look like "no datasources"
        if (
          typeof datasources === 'string' ||
          (datasources?.message && !datasources?.items)
        ) {
          state.sideError = String(datasources.message || datasources)
          return
        }
        state.hints = (datasources?.items || []).map((item: any) => ({
          key: item.key,
          displayName: item.displayName || item.name,
          scriptHint: item.scriptHint
        }))
        state.maxRows = datasources?.maxRows || 0
        state.optionMeta = Array.isArray(options) ? options : []
        state.sideError = ''
      } catch (error) {
        state.sideError = messageOf(error)
      }
    }

    const open = async (id: string, folder: ResourceFolder) => {
      try {
        const detail = await fetchResource(auth.value, id)
        state.folder = folder
        state.detail = detail
        state.baseline = JSON.stringify(buildEntity())
        state.revision += 1
        state.lastResponse = ''
        state.selectedKey = `f:${id}`
      } catch (error) {
        message.error(messageOf(error))
      }
    }

    const refreshDetail = async () => {
      if (state.detail?.id) await open(state.detail.id, state.folder)
    }

    const buildEntity = (): ResourceDetail => {
      const detail = state.detail
      if (!detail) {
        return {
          groupId: ROOT_GROUP_ID,
          name: '',
          path: '',
          script: '',
          parameters: [],
          headers: [],
          paths: [],
          options: []
        }
      }
      const entity: ResourceDetail = {
        groupId: detail.groupId,
        name: detail.name,
        path: detail.path,
        description: detail.description ?? null,
        script: detail.script ?? '',
        parameters: detail.parameters || [],
        headers: detail.headers || [],
        paths: detail.paths || [],
        options: detail.options || []
      }
      if (detail.id) entity.id = detail.id
      if (state.folder === 'api') {
        entity.method = (detail.method || 'GET').toUpperCase()
        entity.requestBody = detail.requestBody ?? ''
        entity.responseBody = detail.responseBody ?? null
        entity.requestBodyDefinition = detail.requestBodyDefinition ?? null
        entity.responseBodyDefinition = detail.responseBodyDefinition ?? null
      } else {
        entity.returnType = detail.returnType ?? null
      }
      return entity
    }

    const save = async (silent = false): Promise<boolean> => {
      if (!state.detail) return false
      if (locked.value) {
        if (!silent) message.warning(t('data_api.studio.locked_readonly'))
        return false
      }
      const entity = buildEntity()
      if (!entity.name || !entity.path) {
        if (!silent) message.error(t('data_api.studio.name_required'))
        return false
      }
      if (JSON.stringify(entity) === state.baseline && entity.id) return true
      state.saving = true
      try {
        const id = await saveResource(auth.value, state.folder, entity)
        if (!id) {
          if (!silent) message.error(t('data_api.studio.save_failed'))
          return false
        }
        if (state.detail) state.detail.id = id
        state.baseline = JSON.stringify(buildEntity())
        if (!silent) message.success(t('data_api.studio.save_ok'))
        await loadTree()
        return true
      } catch (error) {
        message.error(messageOf(error))
        return false
      } finally {
        state.saving = false
      }
    }

    const openCreate = (
      kind: 'api' | 'group' | 'function',
      folder: ResourceFolder
    ) => {
      const parent =
        kind === 'group'
          ? NODE_MAP[state.selectedKey]?.isGroup
            ? NODE_MAP[state.selectedKey].id
            : ROOT_GROUP_ID
          : NODE_MAP[state.selectedKey]?.isGroup
          ? NODE_MAP[state.selectedKey].id
          : groupsOf(folder)[0]?.id || ROOT_GROUP_ID
      state.createModal = {
        show: true,
        mode: 'create',
        kind,
        folder,
        id: '',
        name: '',
        path: kind === 'group' ? '' : '',
        parentId: parent
      }
    }

    const editGroup = (node: ResourceNode) => {
      state.createModal = {
        show: true,
        mode: 'edit',
        kind: 'group',
        folder: (node.type as ResourceFolder) || 'api',
        id: node.id,
        name: node.name,
        path: node.path || '',
        parentId: node.parentId || ROOT_GROUP_ID
      }
    }

    const doCreate = async () => {
      const modal = state.createModal
      if (!modal.name) {
        message.error(t('data_api.studio.name_required'))
        return
      }
      if (modal.kind === 'group' && !modal.path) {
        message.error(t('data_api.studio.path_required'))
        return
      }
      // magic stores files inside a group directory only; the folder root has no group id
      if (
        modal.kind !== 'group' &&
        !groupsOf(modal.folder).some((group) => group.id === modal.parentId)
      ) {
        message.error(t('data_api.studio.group_required'))
        return
      }
      const path = modal.path.startsWith('/') ? modal.path : `/${modal.path}`
      try {
        if (modal.kind === 'group') {
          if (modal.mode === 'edit') {
            const origin = NODE_MAP[`g:${modal.id}`]?.node
            await saveGroup(auth.value, {
              id: modal.id,
              name: modal.name,
              type: modal.folder,
              parentId: modal.parentId,
              path,
              paths: origin?.paths || [],
              options: origin?.options || []
            })
          } else {
            await saveGroup(auth.value, {
              id: `${modal.folder}_${Date.now().toString(36)}`,
              name: modal.name,
              type: modal.folder,
              parentId: modal.parentId,
              path,
              paths: [],
              options: []
            })
          }
          message.success(t('data_api.studio.created'))
          state.createModal.show = false
          await loadTree()
          return
        }
        const entity: ResourceDetail = {
          groupId: modal.parentId,
          name: modal.name,
          path,
          method: modal.kind === 'api' ? 'GET' : undefined,
          description: '',
          script:
            modal.kind === 'api'
              ? '// TODO: write your magic-script here\n'
              : '// TODO: function magic-script\nreturn 1\n',
          parameters: [],
          headers: [],
          paths: [],
          options: []
        }
        const id = await saveResource(auth.value, modal.folder, entity)
        if (!id) {
          message.error(t('data_api.studio.save_failed'))
          return
        }
        message.success(t('data_api.studio.created'))
        state.createModal.show = false
        await loadTree()
        await open(id, modal.folder)
      } catch (error) {
        message.error(messageOf(error))
      }
    }

    const removeNode = async (id: string) => {
      try {
        await deleteResource(auth.value, id)
        message.success(t('data_api.studio.deleted'))
        if (state.detail?.id === id) {
          state.detail = null
          state.selectedKey = ''
        }
        await loadTree()
      } catch (error) {
        message.error(messageOf(error))
      }
    }

    const doMove = async () => {
      try {
        await moveResource(
          auth.value,
          state.moveModal.src,
          state.moveModal.groupId
        )
        message.success(t('data_api.studio.moved'))
        state.moveModal.show = false
        await loadTree()
        await refreshDetail()
      } catch (error) {
        message.error(messageOf(error))
      }
    }

    const toggleLock = async () => {
      const id = state.detail?.id
      if (!id) return
      try {
        if (locked.value) {
          await unlockResource(auth.value, id)
          message.success(t('data_api.studio.unlocked'))
        } else {
          await lockResource(auth.value, id)
          message.success(t('data_api.studio.locked'))
        }
        await refreshDetail()
        await loadTree()
      } catch (error) {
        message.error(messageOf(error))
      }
    }

    const reloadResource = async () => {
      try {
        await reloadResources(auth.value)
        await loadTree()
        message.success(t('data_api.studio.reloaded'))
      } catch (error) {
        message.error(messageOf(error))
      }
    }

    const doSearchScripts = async () => {
      const keyword = state.searchModal.keyword.trim()
      if (!keyword) return
      try {
        const items = await searchScripts(auth.value, keyword)
        state.searchModal.items = items || []
      } catch (error) {
        message.error(messageOf(error))
      }
    }

    const onKeydown = (event: KeyboardEvent) => {
      if ((event.ctrlKey || event.metaKey) && event.key.toLowerCase() === 's') {
        event.preventDefault()
        save()
      }
    }

    const nodeActions = (option: TreeOption) => {
      const meta = NODE_MAP[String(option.key)]
      if (!meta) return []
      const isGroup = meta.isGroup
      const items: { label: string; key: string }[] = []
      if (isGroup) {
        items.push({ label: t('data_api.studio.new_api'), key: 'create-api' })
        items.push({
          label: t('data_api.studio.new_function'),
          key: 'create-function'
        })
        items.push({
          label: t('data_api.studio.new_group'),
          key: 'create-group'
        })
        items.push({ label: t('data_api.studio.rename'), key: 'rename' })
      } else {
        items.push({ label: t('data_api.studio.rename'), key: 'rename-file' })
      }
      items.push({ label: t('data_api.studio.move'), key: 'move' })
      items.push({ label: t('data_api.studio.delete'), key: 'delete' })
      return items
    }

    const onNodeAction = (action: string, option: TreeOption) => {
      const meta = NODE_MAP[String(option.key)]
      if (!meta) return
      if (action === 'create-api') openCreate('api', 'api')
      if (action === 'create-function') openCreate('function', 'function')
      if (action === 'create-group') openCreate('group', meta.folder)
      if (action === 'rename' && meta.isGroup) editGroup(meta.node)
      if (action === 'rename-file') {
        open(meta.id, meta.folder).then(() => (state.tab = 'basic'))
      }
      if (action === 'move') {
        state.moveModal = {
          show: true,
          src: meta.id,
          groupId: meta.node.parentId || ROOT_GROUP_ID
        }
      }
      if (action === 'delete')
        state.deleteModal = { show: true, id: meta.id, name: meta.node.name }
    }

    const exportZip = async () => {
      const groupId = NODE_MAP[state.selectedKey]?.isGroup
        ? NODE_MAP[state.selectedKey].id
        : undefined
      try {
        const { blob, error } = await exportResources(auth.value, groupId)
        if (error) {
          message.error(error)
          return
        }
        const link = document.createElement('a')
        link.href = window.URL.createObjectURL(blob)
        link.download = groupId ? 'magic-api-group.zip' : 'magic-api-all.zip'
        link.click()
        window.URL.revokeObjectURL(link.href)
        message.success(t('data_api.studio.exported'))
      } catch (error) {
        message.error(messageOf(error))
      }
    }

    const importZip = async (event: Event) => {
      const file = (event.target as HTMLInputElement).files?.[0]
      if (!file) return
      try {
        await importResources(auth.value, file, 'full')
        message.success(t('data_api.studio.imported'))
        await refreshDetail()
        await loadTree()
      } catch (error) {
        message.error(messageOf(error))
      } finally {
        ;(event.target as HTMLInputElement).value = ''
      }
    }

    const pickZip = () => {
      const input = document.createElement('input')
      input.type = 'file'
      input.accept = '.zip'
      input.onchange = (changeEvent: Event) => importZip(changeEvent)
      input.click()
    }

    onMounted(async () => {
      if (!auth.value.sessionId) {
        state.serviceError = t('data_api.studio.no_session')
        return
      }
      try {
        await checkMagicSession(auth.value)
        state.ready = true
      } catch (error) {
        state.serviceError = messageOf(error)
      }
      await Promise.all([loadTree(), loadSideData()])
      window.addEventListener('keydown', onKeydown)
    })

    onBeforeUnmount(() => {
      window.removeEventListener('keydown', onKeydown)
    })

    return {
      t,
      state,
      styles,
      auth,
      locked,
      dirty,
      errorTip,
      currentApiPath,
      METHODS,
      nodeActions,
      onNodeAction,
      groupsOf,
      loadTree,
      open,
      save,
      openCreate,
      doCreate,
      doMove,
      removeNode,
      toggleLock,
      reloadResource,
      doSearchScripts,
      refreshDetail,
      exportZip,
      importZip,
      pickZip,
      buildEntity
    }
  },
  render() {
    const { t, state, styles, locked, dirty, METHODS } = this
    if (state.serviceError) {
      return (
        <NSpace vertical>
          <NAlert type='error' title={t('data_api.studio.title')}>
            {this.errorTip(state.serviceError)}
          </NAlert>
        </NSpace>
      )
    }
    const detail = state.detail
    const panelGroups = this.groupsOf(state.folder)
    return (
      <div class={styles.studio}>
        <Card>
          <div class={styles.toolbar}>
            <NSpace size='small'>
              <NInput
                v-model={[state.pattern, 'value']}
                placeholder={t('data_api.studio.search_ph')}
                clearable
                style={{ width: '240px' }}
              >
                {{
                  prefix: () => (
                    <NIcon>
                      <SearchOutlined />
                    </NIcon>
                  )
                }}
              </NInput>
              <NButton
                class='btn-search-script'
                onClick={() => {
                  state.searchModal = {
                    ...state.searchModal,
                    show: true,
                    items: []
                  }
                }}
              >
                {{
                  icon: () => (
                    <NIcon>
                      <FileSearchOutlined />
                    </NIcon>
                  ),
                  default: () => t('data_api.studio.search_script')
                }}
              </NButton>
              <NButton onClick={this.reloadResource}>
                {{
                  icon: () => (
                    <NIcon>
                      <ReloadOutlined />
                    </NIcon>
                  ),
                  default: () => t('data_api.studio.reload_resource')
                }}
              </NButton>
              <NButton class='btn-export-magic' onClick={this.exportZip}>
                {{
                  icon: () => (
                    <NIcon>
                      <DownloadOutlined />
                    </NIcon>
                  ),
                  default: () => t('data_api.studio.export')
                }}
              </NButton>
              <NButton class='btn-import-magic' onClick={this.pickZip}>
                {{
                  icon: () => (
                    <NIcon>
                      <UploadOutlined />
                    </NIcon>
                  ),
                  default: () => t('data_api.studio.import')
                }}
              </NButton>
            </NSpace>
            <NSpace size='small'>
              {detail && (
                <NTag size='small' type={dirty ? 'warning' : 'success'}>
                  {dirty
                    ? t('data_api.studio.unsaved')
                    : t('data_api.studio.saved')}
                </NTag>
              )}
              <NButton
                disabled={!detail || !dirty}
                loading={state.saving}
                class='btn-save-service'
                type='primary'
                onClick={() => this.save()}
              >
                {{
                  icon: () => (
                    <NIcon>
                      <SaveOutlined />
                    </NIcon>
                  ),
                  default: () => t('data_api.studio.save')
                }}
              </NButton>
              {detail && (
                <NPopconfirm onPositiveClick={this.toggleLock}>
                  {{
                    default: () =>
                      locked
                        ? t('data_api.studio.unlock_confirm')
                        : t('data_api.studio.lock_confirm'),
                    trigger: () => (
                      <NButton size='small' quaternary>
                        {{
                          icon: () => (
                            <NIcon>
                              {locked ? <UnlockOutlined /> : <LockOutlined />}
                            </NIcon>
                          ),
                          default: () =>
                            locked
                              ? t('data_api.studio.unlock')
                              : t('data_api.studio.lock')
                        }}
                      </NButton>
                    )
                  }}
                </NPopconfirm>
              )}
              <NButton
                type='primary'
                class='btn-create-api'
                onClick={() => this.openCreate('api', 'api')}
              >
                {{
                  icon: () => (
                    <NIcon>
                      <FileAddOutlined />
                    </NIcon>
                  ),
                  default: () => t('data_api.studio.new_api')
                }}
              </NButton>
              <NButton
                class='btn-create-function'
                onClick={() => this.openCreate('function', 'function')}
              >
                {{
                  icon: () => (
                    <NIcon>
                      <FunctionOutlined />
                    </NIcon>
                  ),
                  default: () => t('data_api.studio.new_function')
                }}
              </NButton>
              <NButton
                class='btn-create-group'
                onClick={() => this.openCreate('group', state.folder)}
              >
                {{
                  icon: () => (
                    <NIcon>
                      <FolderAddOutlined />
                    </NIcon>
                  ),
                  default: () => t('data_api.studio.new_group')
                }}
              </NButton>
            </NSpace>
          </div>
        </Card>
        {state.refreshError && (
          <NAlert type='warning' show-icon={true}>
            {this.errorTip(state.refreshError)}
          </NAlert>
        )}
        {state.sideError && (
          <NAlert type='warning' show-icon={true}>
            {t('data_api.studio.side_error', { message: state.sideError })}
          </NAlert>
        )}
        <div class={styles.body}>
          <SectionCard
            class={styles['tree-panel']}
            icon={FolderOutlined}
            title={t('data_api.studio.tree_title')}
            contentStyle='padding: 10px 12px'
          >
            <NSpin show={state.treeLoading}>
              <NTree
                blockLine
                clickable
                default-expand-all
                data={state.treeData}
                pattern={state.pattern}
                show-irrelevant-nodes={false}
                filter={filterTree}
                renderPrefix={({ option }: { option: TreeOption }) => {
                  const meta = NODE_MAP[String(option.key)]
                  if (!meta) return null
                  return (
                    <NIcon
                      class={styles['node-icon']}
                      component={
                        meta.isGroup ? FolderOutlined : FileTextOutlined
                      }
                    />
                  )
                }}
                key-field='key'
                selectedKeys={state.selectedKey ? [state.selectedKey] : []}
                onUpdate:selectedKeys={(keys: Array<string | number>) => {
                  const key = String(keys[keys.length - 1] || '')
                  const meta = NODE_MAP[key]
                  if (!meta) {
                    if (key === 'root:function') {
                      state.folder = 'function'
                      state.selectedKey = key
                    }
                    if (key === 'root:api') {
                      state.folder = 'api'
                      state.selectedKey = key
                    }
                    return
                  }
                  if (meta.isGroup) {
                    state.selectedKey = key
                    state.folder = meta.folder
                    return
                  }
                  this.open(meta.id, meta.folder)
                }}
                renderSuffix={({ option }: { option: TreeOption }) => {
                  const meta = NODE_MAP[String(option.key)]
                  if (!meta) return null
                  const actions = this.nodeActions(option)
                  return (
                    <span class={styles.node}>
                      {meta.isGroup ? null : (
                        <NTag
                          class={styles['node-method']}
                          size='tiny'
                          type='info'
                          bordered={false}
                        >
                          {String((option as any).method || '')}
                        </NTag>
                      )}
                      <NDropdown
                        trigger='click'
                        options={actions.map((action) => ({
                          label: action.label,
                          key: action.key
                        }))}
                        onSelect={(key: string) =>
                          this.onNodeAction(key, option)
                        }
                      >
                        <NButton size='tiny' quaternary class='btn-node-action'>
                          •••
                        </NButton>
                      </NDropdown>
                    </span>
                  )
                }}
              />
            </NSpin>
          </SectionCard>
          <Card
            class={styles['detail-panel']}
            contentStyle='padding: 0 16px 16px'
          >
            {{
              header: () => (
                <NSpace align='center' size='small'>
                  <span class={styles['card-title']}>
                    <NIcon
                      class={styles['card-icon']}
                      component={ApiOutlined}
                    />
                    {detail ? detail.name : t('data_api.studio.tree_title')}
                  </span>
                  {detail && (
                    <NTag size='tiny' type='info'>
                      {detail.method || state.folder}
                    </NTag>
                  )}
                  {detail && locked && (
                    <NTag size='tiny' type='warning'>
                      {t('data_api.studio.locked')}
                    </NTag>
                  )}
                  {detail && (
                    <span class={styles['section-hint']}>
                      {this.currentApiPath}
                    </span>
                  )}
                </NSpace>
              ),
              default: () =>
                !detail ? (
                  <NSpace vertical>
                    <NAlert
                      type='info'
                      title={t('data_api.studio.empty_title')}
                    >
                      {t('data_api.studio.empty_body')}
                    </NAlert>
                  </NSpace>
                ) : (
                  <NTabs
                    class={styles.tabs}
                    type='line'
                    v-model={[state.tab, 'value']}
                  >
                    <NTabPane
                      name='basic'
                      v-slots={{
                        tab: () => (
                          <span class={styles['tab-label']}>
                            <NIcon component={FileTextOutlined} />
                            {t('data_api.studio.tab_basic')}
                          </span>
                        )
                      }}
                    >
                      <BasicPanel
                        detail={detail}
                        groups={panelGroups}
                        optionMeta={state.optionMeta}
                        methods={METHODS.map((method) => ({
                          label: method,
                          value: method
                        }))}
                        folder={state.folder}
                        locked={locked}
                      />
                    </NTabPane>
                    <NTabPane
                      name='script'
                      v-slots={{
                        tab: () => (
                          <span class={styles['tab-label']}>
                            <NIcon component={CodeOutlined} />
                            {t('data_api.studio.tab_script')}
                          </span>
                        )
                      }}
                    >
                      <ScriptPanel
                        script={detail.script || ''}
                        hints={state.hints}
                        disabled={locked}
                        onUpdate:script={(value: string) => {
                          if (state.detail) state.detail.script = value
                        }}
                      />
                    </NTabPane>
                    <NTabPane
                      name='test'
                      v-slots={{
                        tab: () => (
                          <span class={styles['tab-label']}>
                            <NIcon component={ExperimentOutlined} />
                            {t('data_api.studio.tab_test')}
                          </span>
                        )
                      }}
                    >
                      <TestPanel
                        detail={detail}
                        groups={panelGroups}
                        revision={state.revision}
                        folder={state.folder}
                        ensureSaved={() => this.save(true)}
                        onTested={(body: string) => (state.lastResponse = body)}
                        onSaveSample={(text: string) => {
                          if (state.detail) state.detail.responseBody = text
                        }}
                      />
                    </NTabPane>
                    <NTabPane
                      name='usage'
                      v-slots={{
                        tab: () => (
                          <span class={styles['tab-label']}>
                            <NIcon component={BookOutlined} />
                            {t('data_api.studio.tab_usage')}
                          </span>
                        )
                      }}
                    >
                      <UsagePanel
                        detail={detail}
                        groups={panelGroups}
                        revision={state.revision}
                        maxRows={state.maxRows}
                        folder={state.folder}
                        lastResponse={state.lastResponse}
                      />
                    </NTabPane>
                    <NTabPane
                      name='version'
                      v-slots={{
                        tab: () => (
                          <span class={styles['tab-label']}>
                            <NIcon component={HistoryOutlined} />
                            {t('data_api.studio.tab_version')}
                          </span>
                        )
                      }}
                    >
                      <VersionPanel
                        detail={detail}
                        script={detail.script || ''}
                        revision={state.revision}
                        onRestored={async () => {
                          await this.refreshDetail()
                          await this.loadTree()
                        }}
                      />
                    </NTabPane>
                    <NTabPane
                      name='release'
                      v-slots={{
                        tab: () => (
                          <span class={styles['tab-label']}>
                            <NIcon component={CloudUploadOutlined} />
                            {t('data_api.studio.tab_release')}
                          </span>
                        )
                      }}
                    >
                      <ReleasePanel
                        apiPath={this.currentApiPath}
                        folder={state.folder}
                        revision={state.revision}
                        onChanged={async () => {
                          await this.refreshDetail()
                        }}
                      />
                    </NTabPane>
                  </NTabs>
                )
            }}
          </Card>
        </div>
        <Modal
          show={state.createModal.show}
          title={
            state.createModal.kind === 'group'
              ? state.createModal.mode === 'edit'
                ? t('data_api.studio.rename_group_title')
                : t('data_api.studio.create_group_title')
              : state.createModal.kind === 'function'
              ? t('data_api.studio.create_function_title')
              : t('data_api.studio.create_api_title')
          }
          onCancel={() => (state.createModal.show = false)}
          onConfirm={this.doCreate}
        >
          {{
            default: () => (
              <NSpace vertical size='small'>
                <NInput
                  size='small'
                  v-model={[state.createModal.name, 'value']}
                  placeholder={t('data_api.studio.field_name_ph')}
                />
                <NInput
                  size='small'
                  v-model={[state.createModal.path, 'value']}
                  placeholder={t('data_api.studio.field_path_ph')}
                />
                {state.createModal.kind !== 'group' && (
                  <NTree
                    blockLine
                    clickable
                    data={panelGroups.map((group) => ({
                      key: group.id,
                      label: group.label,
                      isLeaf: true
                    }))}
                    selectedKeys={[state.createModal.parentId]}
                    onUpdate:selectedKeys={(keys: Array<string | number>) => {
                      if (keys.length > 0) {
                        state.createModal.parentId = String(keys[0])
                      }
                    }}
                  />
                )}
                {state.createModal.kind !== 'group' && (
                  <span class={styles['section-hint']}>
                    {t('data_api.studio.field_parent')}:{' '}
                    {state.createModal.parentId}
                  </span>
                )}
              </NSpace>
            )
          }}
        </Modal>
        <Modal
          show={state.moveModal.show}
          title={t('data_api.studio.move_title')}
          onCancel={() => (state.moveModal.show = false)}
          onConfirm={this.doMove}
        >
          {{
            default: () => (
              <NSpace vertical size='small'>
                <span class={styles['section-hint']}>
                  {t('data_api.studio.move_target_group')}
                </span>
                <NTree
                  blockLine
                  clickable
                  data={[
                    {
                      key: ROOT_GROUP_ID,
                      label: t('data_api.studio.group_root'),
                      isLeaf: true
                    },
                    ...panelGroups.map((group) => ({
                      key: group.id,
                      label: group.label,
                      isLeaf: true
                    }))
                  ]}
                  selectedKeys={[state.moveModal.groupId]}
                  onUpdate:selectedKeys={(keys: Array<string | number>) => {
                    if (keys.length > 0) {
                      state.moveModal.groupId = String(keys[0])
                    }
                  }}
                />
              </NSpace>
            )
          }}
        </Modal>
        <Modal
          show={state.deleteModal.show}
          title={t('data_api.studio.delete_title')}
          onCancel={() => (state.deleteModal.show = false)}
          onConfirm={async () => {
            await this.removeNode(state.deleteModal.id)
            state.deleteModal.show = false
          }}
        >
          {{
            default: () => (
              <span>
                {t('data_api.studio.delete_confirm', {
                  name: state.deleteModal.name
                })}
              </span>
            )
          }}
        </Modal>
        <Modal
          show={state.searchModal.show}
          title={t('data_api.studio.search_script_title')}
          confirmShow={false}
          onCancel={() => (state.searchModal.show = false)}
          onConfirm={() => (state.searchModal.show = false)}
        >
          {{
            default: () => (
              <NSpace vertical size='small'>
                <NSpace size='small'>
                  <NInput
                    size='small'
                    v-model={[state.searchModal.keyword, 'value']}
                    placeholder={t('data_api.studio.search_script_ph')}
                    onKeyup={(event: KeyboardEvent) => {
                      if (event.key === 'Enter') this.doSearchScripts()
                    }}
                  />
                  <NButton
                    size='small'
                    type='primary'
                    onClick={this.doSearchScripts}
                  >
                    {t('data_api.studio.search')}
                  </NButton>
                </NSpace>
                <span class={styles['section-hint']}>
                  {t('data_api.studio.search_script_count', {
                    count: state.searchModal.items.length
                  })}
                </span>
                {state.searchModal.items.map((item: any, index: number) => (
                  <div
                    key={`${item.id}-${index}`}
                    style={{ cursor: 'pointer' }}
                    onClick={() => {
                      const meta = NODE_MAP[`f:${item.id}`]
                      state.searchModal.show = false
                      if (meta) this.open(meta.id, meta.folder)
                    }}
                  >
                    {item.id} : {item.line} — {item.text}
                  </div>
                ))}
              </NSpace>
            )
          }}
        </Modal>
      </div>
    )
  }
})

export default DataApiStudio
