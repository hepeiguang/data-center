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
  NDrawer,
  NDrawerContent,
  NEmpty,
  NIcon,
  NInput,
  NSelect,
  NTabPane,
  NTabs,
  NTag
} from 'naive-ui'
import {
  AppstoreOutlined,
  CheckOutlined,
  ClusterOutlined,
  DatabaseOutlined,
  PartitionOutlined,
  SearchOutlined
} from '@vicons/antd'
import { useUserStore } from '@/store/user/user'
import {
  addTag,
  attachDataProduct,
  attachDomain,
  attachGlossaryTerm,
  createEntry,
  getAssetGovernance,
  getColumns,
  getDetail,
  getGovernanceAssets,
  getGovernanceOverview,
  getRows,
  removeTag,
  searchAssets,
  setClassificationTag
} from '@/service/modules/data-asset/api'
import { useAssetTheme } from '../theme'

const Catalog = defineComponent({
  name: 'DataAssetCatalog',
  setup() {
    const userStore = useUserStore()
    const state = reactive({
      q: '',
      assets: [] as any[],
      govAssets: [] as any[],
      overview: {
        domains: [],
        dataProducts: [],
        glossaryTerms: [],
        classifications: [],
        tagsByClassification: {}
      } as any,
      filter: null as null | { type: string; value: string; label: string },
      selected: null as any,
      detail: null as any,
      gov: null as any,
      columns: [] as any[],
      rowColumns: [] as string[],
      rows: [] as any[][],
      page: 1,
      size: 100,
      hasMore: false,
      browseAllowed: false,
      entryId: '',
      notice: '',
      showDetail: false,
      form: {
        layer: '',
        level: '',
        cert: '',
        freeTag: '',
        domain: '',
        product: '',
        term: ''
      }
    })

    const message = (window as any).$message
    const { t } = useAssetTheme()

    const tagsOf = (classification: string): any[] =>
      state.overview.tagsByClassification?.[classification] || []

    const glossaryFqns = computed(
      () => new Set((state.overview.glossaryTerms || []).map((t: any) => t.fqn))
    )
    const currentTags = computed(() =>
      (state.gov?.tags || []).filter((t: string) => !glossaryFqns.value.has(t))
    )
    const currentTerms = computed(() =>
      (state.gov?.tags || []).filter((t: string) => glossaryFqns.value.has(t))
    )
    const currentLayer = computed(
      () => currentTags.value.find((t: string) => t.startsWith('DataLayer.')) || ''
    )
    const currentLevel = computed(
      () =>
        currentTags.value.find((t: string) =>
          t.startsWith('AgriDataSensitivity.')
        ) || ''
    )

    const options = (list: any[]) =>
      (list || []).map((item: any) => ({
        label: item.displayName || item.name,
        value: item.fqn
      }))
    const tagOptions = (classification: string) =>
      tagsOf(classification).map((t: any) => ({ label: t.name, value: t.fqn }))

    const otherTagOptions = computed(() => {
      const skip = new Set([
        'DataLayer',
        'AgriDataSensitivity',
        'Certification',
        'Tier'
      ])
      const result: any[] = []
      Object.keys(state.overview.tagsByClassification || {}).forEach((name) => {
        if (!skip.has(name)) {
          tagsOf(name).forEach((t: any) =>
            result.push({ label: `${name}.${t.name}`, value: t.fqn })
          )
        }
      })
      return result
    })

    const govMap = computed(() => {
      const map: Record<string, any> = {}
      state.govAssets.forEach((item) => {
        map[item.fqn] = item
      })
      return map
    })

    const visibleAssets = computed(() => {
      if (!state.filter) return state.assets
      return state.assets.filter((asset) => {
        const gov = govMap.value[asset.fqn]
        if (!gov) return false
        if (state.filter!.type === 'domain') {
          return (gov.domains || []).includes(state.filter!.value)
        }
        if (state.filter!.type === 'product') {
          return (gov.dataProducts || []).includes(state.filter!.value)
        }
        if (state.filter!.type === 'cert') {
          return gov.certification === state.filter!.value
        }
        return (gov.tags || []).includes(state.filter!.value)
      })
    })

    const stats = computed(() => {
      const total = state.assets.length
      const layered = state.assets.filter((a: any) => !!layerOf(a.fqn)).length
      const certified = state.assets.filter((a: any) => !!certOf(a.fqn)).length
      const linked = state.overview.dataProducts.reduce(
        (sum: number, p: any) => sum + productAssetCount(p.fqn),
        0
      )
      return [
        {
          label: '数据资产',
          value: total,
          sub: `已分层 ${layered}/${total}`,
          tone: 'info',
          icon: DatabaseOutlined
        },
        {
          label: '数据产品',
          value: state.overview.dataProducts.length,
          sub: `关联资产 ${linked}`,
          tone: 'teal',
          icon: AppstoreOutlined
        },
        {
          label: '治理域',
          value: state.overview.domains.length,
          sub: `分类 ${state.overview.classifications.length} · 术语 ${state.overview.glossaryTerms.length}`,
          tone: 'success',
          icon: PartitionOutlined
        },
        {
          label: '已认证',
          value: certified,
          sub: `分层标签 ${tagsOf('DataLayer').length} 个`,
          tone: 'warning',
          icon: ClusterOutlined
        }
      ]
    })

    const layerOf = (fqn: string) => {
      const gov = govMap.value[fqn]
      const tag = (gov?.tags || []).find((t: string) => t.startsWith('DataLayer.'))
      return tag ? tag.split('.')[1] : ''
    }
    const levelOf = (fqn: string) => {
      const gov = govMap.value[fqn]
      const tag = (gov?.tags || []).find((t: string) =>
        t.startsWith('AgriDataSensitivity.')
      )
      return tag ? tag.split('.')[1] : ''
    }
    const tierOf = (fqn: string) => {
      const gov = govMap.value[fqn]
      const tag = (gov?.tags || []).find((t: string) => t.startsWith('Tier.'))
      return tag ? tag.split('.')[1] : ''
    }
    const certOf = (fqn: string) => govMap.value[fqn]?.certification || ''

    const productAssetCount = (fqn: string) =>
      state.govAssets.filter((item: any) =>
        (item.dataProducts || []).includes(fqn)
      ).length

    const loadAssets = async () => {
      try {
        const assets = await searchAssets(state.q || '*', 100)
        state.assets = assets.items || []
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '资产搜索失败')
      }
      getGovernanceAssets()
        .then((govAssets) => {
          state.govAssets = govAssets || []
        })
        .catch(() => {
          state.govAssets = []
        })
    }

    const loadOverview = async () => {
      try {
        state.overview = await getGovernanceOverview()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '治理目录读取失败')
      }
    }

    const loadGovernance = async (fqn: string) => {
      try {
        state.gov = await getAssetGovernance(fqn)
        state.form.layer = currentLayer.value
        state.form.level = currentLevel.value
        state.form.cert = state.gov.certification || ''
        state.form.domain = ''
        state.form.product = ''
        state.form.term = ''
        state.form.freeTag = ''
      } catch (e: any) {
        state.gov = null
      }
    }

    const loadRows = async () => {
      if (!state.selected) return
      try {
        const result = await getRows(state.selected.fqn, state.page, state.size)
        state.rowColumns = result.columns || []
        state.rows = result.rows || []
        state.hasMore = !!result.hasMore
        state.entryId = result.entryId || ''
      } catch (e: any) {
        state.notice = e?.response?.data?.message || '数据读取失败'
        state.rows = []
      }
    }

    const select = async (asset: any) => {
      state.selected = asset
      state.notice = ''
      state.page = 1
      state.rows = []
      state.rowColumns = []
      state.showDetail = true
      try {
        const detail = await getDetail(asset.fqn)
        state.detail = detail.entity
        state.browseAllowed = !!detail.browseAllowed
        state.entryId = detail.entryId || ''
        state.columns = await getColumns(asset.fqn)
        await loadGovernance(asset.fqn)
        if (state.browseAllowed) {
          await loadRows()
        }
      } catch (e: any) {
        state.notice = e?.response?.data?.message || '资产详情读取失败'
      }
    }

    const refreshGovernance = async () => {
      if (!state.selected) return
      await Promise.all([loadGovernance(state.selected.fqn), loadAssets()])
    }

    const applyLayer = async () => {
      try {
        await setClassificationTag(state.selected.fqn, 'DataLayer', state.form.layer)
        message?.success('分层已更新')
        await refreshGovernance()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '更新失败')
      }
    }
    const applyLevel = async () => {
      try {
        await setClassificationTag(
          state.selected.fqn,
          'AgriDataSensitivity',
          state.form.level
        )
        message?.success('分级已更新')
        await refreshGovernance()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '更新失败')
      }
    }
    const applyCertification = async () => {
      try {
        await setClassificationTag(
          state.selected.fqn,
          'Certification',
          state.form.cert
        )
        message?.success('认证已更新')
        await refreshGovernance()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '更新失败')
      }
    }
    const addFreeTag = async () => {
      if (!state.form.freeTag) return
      try {
        await addTag(state.selected.fqn, state.form.freeTag)
        message?.success('标签已添加')
        await refreshGovernance()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '添加失败')
      }
    }
    const dropTag = async (tagFqn: string) => {
      try {
        await removeTag(state.selected.fqn, tagFqn)
        await refreshGovernance()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '移除失败')
      }
    }
    const attach = async (type: string, action: string) => {
      const key =
        type === 'domain' ? 'domain' : type === 'product' ? 'product' : 'term'
      const value = (state.form as any)[key]
      if (!value) return
      try {
        let result: any
        if (type === 'domain') {
          result = await attachDomain(state.selected.fqn, value, action)
        } else if (type === 'product') {
          result = await attachDataProduct(state.selected.fqn, value, action)
        } else {
          result = await attachGlossaryTerm(state.selected.fqn, value, action)
        }
        message?.success(
          result?.status === 'inherited'
            ? '该项为继承值，无需重复添加'
            : result?.status === 'exists'
              ? '该项已存在'
              : '已更新'
        )
        await refreshGovernance()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '操作失败')
      }
    }

    const applyAccess = async () => {
      if (!state.selected) return
      const parts = String(state.selected.fqn).split('.')
      if (parts.length !== 4) return
      try {
        await createEntry({
          datasource: parts[0],
          database: parts[1],
          schema: parts[2],
          tables: [parts[3]],
          subjects: {
            users: [(userStore.getUserInfo as any)?.userName || 'admin'],
            roles: [],
            projects: []
          },
          pageSize: 100,
          maxRows: 5000,
          exportEnabled: false
        })
        message?.success('已提交授权申请，待审批')
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '申请失败')
      }
    }

    const dataColumns = computed(() =>
      state.rowColumns.map((name) => ({
        title: name,
        key: name,
        width: 160,
        ellipsis: { tooltip: true }
      }))
    )
    const dataRows = computed(() =>
      state.rows.map((row) => {
        const item: Record<string, any> = {}
        state.rowColumns.forEach((name, index) => {
          item[name] = row[index]
        })
        return item
      })
    )
    const columnTableColumns = [
      { title: '字段', key: 'name', width: 180 },
      { title: '类型', key: 'dataType', width: 100 },
      { title: '约束', key: 'constraint', width: 120 },
      { title: '说明', key: 'description', ellipsis: { tooltip: true } },
      { title: '可见', key: 'visible', width: 80 },
      { title: '脱敏', key: 'masked', width: 80 }
    ]

    const chooseFilter = (type: string, value: string, label: string) => {
      if (
        state.filter &&
        state.filter.type === type &&
        state.filter.value === value
      ) {
        state.filter = null
      } else {
        state.filter = { type, value, label }
      }
    }

    onMounted(async () => {
      await loadOverview()
      await loadAssets()
    })

    return {
      state,
      options,
      tagOptions,
      otherTagOptions,
      currentTags,
      currentTerms,
      visibleAssets,
      stats,
      govMap,
      layerOf,
      levelOf,
      tierOf,
      certOf,
      productAssetCount,
      loadAssets,
      loadRows,
      select,
      applyLayer,
      applyLevel,
      applyCertification,
      addFreeTag,
      dropTag,
      attach,
      applyAccess,
      chooseFilter,
      dataColumns,
      dataRows,
      columnTableColumns,
      tagsOf,
      t
    }
  },
  render() {
    const {
      state,
      dataColumns,
      dataRows,
      columnTableColumns,
      currentTags,
      currentTerms,
      visibleAssets,
      stats,
      t
    } = this

    const filterGroups = [
      { title: '分层', type: 'tag', items: this.tagsOf('DataLayer') },
      { title: '分级', type: 'tag', items: this.tagsOf('AgriDataSensitivity') },
      { title: '认证', type: 'cert', items: this.tagsOf('Certification') },
      { title: '域', type: 'domain', items: state.overview.domains },
      { title: '数据产品', type: 'product', items: state.overview.dataProducts }
    ]

    const detail = (
      <div style={{ display: 'flex', flexDirection: 'column', height: '100%' }}>
        {state.notice && (
          <NAlert type='warning' style={{ marginBottom: '8px' }}>
            {state.notice}
          </NAlert>
        )}
        <NTabs type='line' animated>
          <NTabPane name='data' tab='数据'>
            {!state.browseAllowed ? (
              <NAlert type='info' title='该表尚未开放数据浏览'>
                <div style={{ marginBottom: '8px' }}>
                  可以申请授权，审批通过后即可在资产目录中查看数据。
                </div>
                <NButton size='small' type='primary' onClick={this.applyAccess}>
                  申请授权
                </NButton>
              </NAlert>
            ) : (
              <div>
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                  <span style={{ color: t.textSecondary, fontSize: '12px' }}>
                    条目 {state.entryId}
                  </span>
                  <span style={{ flex: 1 }} />
                  <NButton
                    size='small'
                    disabled={state.page <= 1}
                    onClick={() => {
                      state.page -= 1
                      this.loadRows()
                    }}
                  >
                    上一页
                  </NButton>
                  <span>第 {state.page} 页</span>
                  <NButton
                    size='small'
                    disabled={!state.hasMore}
                    onClick={() => {
                      state.page += 1
                      this.loadRows()
                    }}
                  >
                    下一页
                  </NButton>
                </div>
                <NDataTable
                  size='small'
                  bordered
                  columns={dataColumns}
                  data={dataRows}
                  max-height={460}
                  scroll-x={1100}
                />
              </div>
            )}
          </NTabPane>

          <NTabPane name='columns' tab='字段'>
            <NDataTable
              size='small'
              bordered
              columns={columnTableColumns}
              data={state.columns}
              max-height={520}
            />
          </NTabPane>

          <NTabPane name='governance' tab='治理'>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              <div>
                <div style={{ fontWeight: 600, marginBottom: '4px' }}>当前标签</div>
                {currentTags.map((tag: string) => (
                  <NTag
                    key={tag}
                    size='small'
                    closable
                    style={{ marginRight: '6px', marginBottom: '4px' }}
                    onClose={() => this.dropTag(tag)}
                  >
                    {tag}
                  </NTag>
                ))}
                {state.gov?.certification && (
                  <NTag size='small' type='success' style={{ marginRight: '6px' }}>
                    {state.gov.certification}
                  </NTag>
                )}
                {!currentTags.length && !state.gov?.certification && (
                  <span style={{ color: t.textMuted }}>暂无标签</span>
                )}
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ width: '70px' }}>分层</span>
                <NSelect
                  style={{ width: '220px' }}
                  options={this.tagOptions('DataLayer')}
                  value={state.form.layer || null}
                  onUpdateValue={(v: string) => (state.form.layer = v)}
                />
                <NButton size='small' onClick={this.applyLayer}>应用</NButton>
                <span style={{ color: t.textSecondary, fontSize: '12px' }}>
                  当前：{currentTags.find((t: string) => t.startsWith('DataLayer.')) || '未设置'}
                </span>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ width: '70px' }}>分级</span>
                <NSelect
                  style={{ width: '220px' }}
                  options={this.tagOptions('AgriDataSensitivity')}
                  value={state.form.level || null}
                  onUpdateValue={(v: string) => (state.form.level = v)}
                />
                <NButton size='small' onClick={this.applyLevel}>应用</NButton>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ width: '70px' }}>认证</span>
                <NSelect
                  style={{ width: '220px' }}
                  clearable
                  options={this.tagOptions('Certification')}
                  value={state.form.cert || null}
                  onUpdateValue={(v: string) => (state.form.cert = v || '')}
                />
                <NButton size='small' onClick={this.applyCertification}>应用</NButton>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ width: '70px' }}>分类</span>
                <NSelect
                  style={{ width: '260px' }}
                  options={this.otherTagOptions}
                  value={state.form.freeTag || null}
                  onUpdateValue={(v: string) => (state.form.freeTag = v)}
                />
                <NButton size='small' onClick={this.addFreeTag}>添加</NButton>
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ width: '70px' }}>域</span>
                <NSelect
                  style={{ width: '220px' }}
                  options={this.options(state.overview.domains)}
                  value={state.form.domain || null}
                  onUpdateValue={(v: string) => (state.form.domain = v)}
                />
                <NButton size='small' onClick={() => this.attach('domain', 'add')}>添加</NButton>
                {(state.gov?.domains || []).map((d: string) => {
                  const inherited = (state.gov?.inheritedDomains || []).includes(d)
                  return (
                    <NTag
                      key={d}
                      size='small'
                      type={inherited ? 'info' : 'default'}
                      closable={!inherited}
                      onClose={() => {
                        state.form.domain = d
                        this.attach('domain', 'remove')
                      }}
                    >
                      {d}
                      {inherited ? '（继承）' : ''}
                    </NTag>
                  )
                })}
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ width: '70px' }}>数据产品</span>
                <NSelect
                  style={{ width: '220px' }}
                  options={this.options(state.overview.dataProducts)}
                  value={state.form.product || null}
                  onUpdateValue={(v: string) => (state.form.product = v)}
                />
                <NButton size='small' onClick={() => this.attach('product', 'add')}>添加</NButton>
                {(state.gov?.dataProducts || []).map((d: string) => {
                  const inherited = (state.gov?.inheritedDataProducts || []).includes(d)
                  return (
                    <NTag
                      key={d}
                      size='small'
                      type={inherited ? 'info' : 'default'}
                      closable={!inherited}
                      onClose={() => {
                        state.form.product = d
                        this.attach('product', 'remove')
                      }}
                    >
                      {d}
                      {inherited ? '（继承）' : ''}
                    </NTag>
                  )
                })}
              </div>

              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <span style={{ width: '70px' }}>术语</span>
                <NSelect
                  style={{ width: '260px' }}
                  options={this.options(state.overview.glossaryTerms)}
                  value={state.form.term || null}
                  onUpdateValue={(v: string) => (state.form.term = v)}
                />
                <NButton size='small' onClick={() => this.attach('term', 'add')}>添加</NButton>
                {currentTerms.map((t: string) => (
                  <NTag
                    key={t}
                    size='small'
                    closable
                    onClose={() => {
                      state.form.term = t
                      this.attach('term', 'remove')
                    }}
                  >
                    {t}
                  </NTag>
                ))}
              </div>

              <div style={{ color: t.textSecondary, fontSize: '12px' }}>
                所有者：{(state.gov?.owners || []).join(', ') || '未设置'}；治理变更实时写入
                OpenMetadata。
              </div>
            </div>
          </NTabPane>
        </NTabs>
      </div>
    )

    return (
      <div style={{ paddingBottom: '24px' }}>
        <div style={{ ...t.banner, padding: '16px 18px', marginBottom: '12px' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              flexWrap: 'nowrap'
            }}
          >
            <div
              style={{
                width: '38px',
                height: '38px',
                minWidth: '38px',
                borderRadius: '8px',
                background: t.tone('info').background,
                border: `1px solid ${t.tone('info').borderColor}`,
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center'
              }}
            >
              <NIcon size={20} color={t.primary}>
                <DatabaseOutlined />
              </NIcon>
            </div>
            <div style={{ minWidth: 0 }}>
              <div style={{ fontSize: '17px', fontWeight: 700 }}>数据资产</div>
              <div
                style={{
                  fontSize: '12px',
                  color: t.textSecondary,
                  marginTop: '2px',
                  whiteSpace: 'nowrap',
                  overflow: 'hidden',
                  textOverflow: 'ellipsis'
                }}
              >
                分层 / 分级 / 认证统一管理 · 元数据与血缘来自 OpenMetadata
              </div>
            </div>
            <span style={{ flex: 1 }} />
            <NInput
              style={{ width: '300px', maxWidth: '100%', flexShrink: 0 }}
              value={state.q}
              placeholder='搜索表 / 字段，回车检索'
              onUpdateValue={(v: string) => (state.q = v)}
              onKeydown={(e: KeyboardEvent) => e.key === 'Enter' && this.loadAssets()}
              v-slots={{
                suffix: () => (
                  <NIcon
                    size={18}
                    color={t.primary}
                    style={{ cursor: 'pointer' }}
                    onClick={this.loadAssets}
                  >
                    <SearchOutlined />
                  </NIcon>
                )
              }}
            />
            <NButton size='small' type='primary' onClick={this.loadAssets}>
              检索
            </NButton>
          </div>

          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit, minmax(172px, 1fr))',
              gap: '10px',
              marginTop: '14px'
            }}
          >
            {stats.map((stat: any) => (
              <div
                key={stat.label}
                style={{ ...t.tile, padding: '10px 12px', display: 'flex', gap: '10px', alignItems: 'center' }}
              >
                <div
                  style={{
                    width: '30px',
                    height: '30px',
                    minWidth: '30px',
                    borderRadius: '6px',
                    background: t.tone(stat.tone).background,
                    border: `1px solid ${t.tone(stat.tone).borderColor}`,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                >
                  <NIcon size={16} color={t.tone(stat.tone).color}>{h(stat.icon)}</NIcon>
                </div>
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: '6px' }}>
                    <span style={{ fontSize: '20px', fontWeight: 700, lineHeight: 1.1, color: t.text }}>
                      {stat.value}
                    </span>
                    <span style={{ fontSize: '12px', color: t.textSecondary, whiteSpace: 'nowrap' }}>
                      {stat.label}
                    </span>
                  </div>
                  <div
                    style={{
                      fontSize: '11px',
                      color: t.textMuted,
                      marginTop: '2px',
                      whiteSpace: 'nowrap',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis'
                    }}
                  >
                    {stat.sub}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fit, minmax(230px, 1fr))',
            gap: '10px',
            marginBottom: '14px'
          }}
        >
          {filterGroups.map((group) => {
            const groupActive = state.filter?.type === group.type
            return (
              <div key={group.title} style={{ ...t.cardStyle, padding: '10px 12px' }}>
                <div style={{ display: 'flex', alignItems: 'center', gap: '6px', marginBottom: '8px' }}>
                  <span style={{ fontWeight: 600, fontSize: '12px', color: t.text }}>
                    {group.title}
                  </span>
                  <span
                    style={{
                      fontSize: '11px',
                      lineHeight: '16px',
                      padding: '0 6px',
                      borderRadius: '8px',
                      background: t.tone('info').background,
                      border: `1px solid ${t.tone('info').borderColor}`,
                      color: t.tone('info').color
                    }}
                  >
                    {group.items.length}
                  </span>
                  <span style={{ flex: 1 }} />
                  {groupActive && state.filter && (
                    <span
                      onClick={() => (state.filter = null)}
                      style={{ fontSize: '11px', color: t.tone('info').color, cursor: 'pointer' }}
                    >
                      清除
                    </span>
                  )}
                </div>
                <div style={{ display: 'flex', flexWrap: 'wrap', gap: '6px' }}>
                  {group.items.map((item: any) => {
                    const active = groupActive && state.filter?.value === item.fqn
                    return (
                      <span
                        key={item.fqn}
                        onClick={() => this.chooseFilter(group.type, item.fqn, item.name)}
                        style={{
                          display: 'inline-flex',
                          alignItems: 'center',
                          gap: '4px',
                          padding: '2px 8px',
                          borderRadius: '4px',
                          fontSize: '12px',
                          cursor: 'pointer',
                          background: active ? t.primary : t.card,
                          border: `1px solid ${active ? t.primary : t.borderSoft}`,
                          color: active ? t.onPrimary : t.textStrong
                        }}
                      >
                        {active && (
                          <NIcon size={11} color={t.onPrimary}>
                            <CheckOutlined />
                          </NIcon>
                        )}
                        {item.name}
                      </span>
                    )
                  })}
                  {!group.items.length && (
                    <span style={{ color: t.textMuted, fontSize: '12px' }}>暂无</span>
                  )}
                </div>
              </div>
            )
          })}
        </div>

        {state.overview.dataProducts.length > 0 && (
          <div style={{ marginBottom: '14px' }}>
            <div style={{ fontWeight: 600, marginBottom: '8px', borderLeft: `3px solid ${t.primary}`, paddingLeft: '8px' }}>
              数据产品
            </div>
            <div
              style={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
                gap: '12px'
              }}
            >
              {state.overview.dataProducts.map((product: any) => (
                <div
                  key={product.fqn}
                  onClick={() =>
                    this.chooseFilter('product', product.fqn, product.displayName || product.name)
                  }
                  style={{
                    background: t.card,
                    border: `1px solid ${t.cardBorder}`,
                    borderRadius: '4px',
                    padding: '14px',
                    cursor: 'pointer'
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                    <div
                      style={{
                        width: '34px',
                        height: '34px',
                        borderRadius: '3px',
                        background: t.tone('teal').background,
                        border: `1px solid ${t.tone('teal').borderColor}`,
                        display: 'flex',
                        alignItems: 'center',
                        justifyContent: 'center'
                      }}
                    >
                      <NIcon size={18} color={t.tone('teal').color}><AppstoreOutlined /></NIcon>
                    </div>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 600 }}>
                        {product.displayName || product.name}
                      </div>
                      <div style={{ color: t.textSecondary, fontSize: '12px' }}>
                        {product.name}
                      </div>
                    </div>
                    <NTag size='small' type='default' style={{ background: t.card, borderColor: t.borderChip }}>
                      {this.productAssetCount(product.fqn)} 个资产
                    </NTag>
                  </div>
                  <div style={{ color: t.textSecondary, fontSize: '12px', marginTop: '8px' }}>
                    {product.description || '暂无描述'}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        <div style={{ fontWeight: 600, marginBottom: '8px', borderLeft: `3px solid ${t.primary}`, paddingLeft: '8px' }}>
          资产 <span style={{ color: t.textSecondary, fontWeight: 400 }}>（{visibleAssets.length}）</span>
        </div>
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill, minmax(320px, 1fr))',
            gap: '12px'
          }}
        >
          {visibleAssets.map((asset: any) => (
            <div
              key={asset.fqn}
              onClick={() => this.select(asset)}
              style={{
                background: t.card,
                border: `1px solid ${t.cardBorder}`,
                borderRadius: '4px',
                padding: '14px',
                cursor: 'pointer',
                display: 'flex',
                flexDirection: 'column',
                gap: '8px'
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '10px' }}>
                <div
                  style={{
                    width: '34px',
                    height: '34px',
                    borderRadius: '3px',
                    background: t.tone('info').background,
                    border: `1px solid ${t.tone('info').borderColor}`,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                >
                  <NIcon size={18} color={t.primary}><DatabaseOutlined /></NIcon>
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontWeight: 600, overflow: 'hidden', textOverflow: 'ellipsis' }}>
                    {asset.displayName || asset.name}
                  </div>
                  <div
                    style={{
                      color: t.textSecondary,
                      fontSize: '11px',
                      overflow: 'hidden',
                      textOverflow: 'ellipsis',
                      whiteSpace: 'nowrap'
                    }}
                  >
                    {asset.fqn}
                  </div>
                </div>
                {this.layerOf(asset.fqn) && (
                  <NTag size='small' type='default' style={t.tone('info')}>
                    {this.layerOf(asset.fqn)}
                  </NTag>
                )}
              </div>
              <div
                style={{
                  color: t.textSecondary,
                  fontSize: '12px',
                  minHeight: '34px',
                  overflow: 'hidden'
                }}
              >
                {asset.description || '暂无描述'}
              </div>
              <div style={{ display: 'flex', gap: '6px', flexWrap: 'wrap' }}>
                {this.levelOf(asset.fqn) && (
                  <NTag size='tiny' type='default' style={t.tone('warning')}>
                    {this.levelOf(asset.fqn)}
                  </NTag>
                )}
                {this.tierOf(asset.fqn) && (
                  <NTag size='tiny' type='default' style={t.tone('neutral')}>
                    {this.tierOf(asset.fqn)}
                  </NTag>
                )}
                {this.certOf(asset.fqn) && (
                  <NTag size='tiny' type='default' style={t.tone('success')}>
                    {this.certOf(asset.fqn).split('.').pop()}
                  </NTag>
                )}
              </div>
            </div>
          ))}
        </div>

        <NDrawer v-model:show={state.showDetail} width={980} placement='right'>
          <NDrawerContent closable>
            <div style={{ display: 'flex', alignItems: 'center', gap: '14px', marginBottom: '10px' }}>
              <div
                style={{
                  width: '46px',
                  height: '46px',
                  borderRadius: '50%',
                  background: t.tone('info').background,
                  border: `1px solid ${t.tone('info').borderColor}`,
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center'
                }}
              >
                <NIcon size={22} color={t.primary}><DatabaseOutlined /></NIcon>
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: '18px', fontWeight: 700 }}>
                  {state.selected?.displayName || state.selected?.name}
                </div>
                <div style={{ color: t.textSecondary, fontSize: '12px' }}>
                  {state.selected?.fqn}
                </div>
              </div>
              <NTag
                type='default'
                style={
                  state.browseAllowed
                    ? t.tone('success')
                    : t.tone('warning')
                }
              >
                {state.browseAllowed ? '已开放浏览' : '未开放浏览'}
              </NTag>
              {state.gov?.certification && (
                <NTag type='default' style={t.tone('success')}>
                  {state.gov.certification}
                </NTag>
              )}
            </div>
            {detail}
          </NDrawerContent>
        </NDrawer>
      </div>
    )
  }
})

export default Catalog
