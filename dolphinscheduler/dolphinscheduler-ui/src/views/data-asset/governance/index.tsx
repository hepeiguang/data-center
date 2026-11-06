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
import { NButton, NIcon, NInput, NSelect, NTag } from 'naive-ui'
import {
  AppstoreOutlined,
  BookOutlined,
  ClusterOutlined,
  PartitionOutlined,
  PlusOutlined,
  TagsOutlined
} from '@vicons/antd'
import {
  createClassification,
  createDataProduct,
  createDomain,
  createGlossaryTerm,
  createTag,
  getGovernanceOverview
} from '@/service/modules/data-asset/api'
import { useAssetTheme } from '../theme'

const formRow = {
  display: 'flex',
  gap: '6px',
  flexWrap: 'wrap' as const
}

const Governance = defineComponent({
  name: 'DataAssetGovernance',
  setup() {
    const state = reactive({
      overview: {
        domains: [],
        dataProducts: [],
        glossaryTerms: [],
        classifications: [],
        tagsByClassification: {}
      } as any,
      classification: { name: '', description: '' },
      tag: { classification: '', name: '', description: '' },
      domain: { name: '', description: '' },
      product: { name: '', description: '', domainFqn: '' },
      term: { glossary: 'AgriSatelliteGlossary', name: '', description: '' }
    })

    const message = (window as any).$message
    const { t } = useAssetTheme()

    const load = async () => {
      try {
        state.overview = await getGovernanceOverview()
        if (!state.tag.classification && state.overview.classifications.length) {
          state.tag.classification = state.overview.classifications[0].name
        }
        if (!state.product.domainFqn && state.overview.domains.length) {
          state.product.domainFqn = state.overview.domains[0].fqn
        }
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '治理目录读取失败')
      }
    }

    const run = async (action: () => Promise<any>, text: string) => {
      try {
        await action()
        message?.success(text)
        await load()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '操作失败')
      }
    }

    const options = (list: any[]) =>
      (list || []).map((item: any) => ({
        label: item.displayName || item.name,
        value: item.name || item.fqn
      }))

    const classOptions = () =>
      (state.overview.classifications || []).map((c: any) => ({
        label: c.name,
        value: c.name
      }))

    const tagsOf = (name: string) =>
      state.overview.tagsByClassification?.[name] || []

    const tagCount = computed(() =>
      (state.overview.classifications || []).reduce(
        (sum: number, c: any) => sum + tagsOf(c.name).length,
        0
      )
    )

    const stats = computed(() => [
      {
        label: '治理域',
        value: (state.overview.domains || []).length,
        sub: `数据产品 ${(state.overview.dataProducts || []).length} 个`,
        tone: 'info',
        icon: ClusterOutlined
      },
      {
        label: '数据产品',
        value: (state.overview.dataProducts || []).length,
        sub: '资产按产品打包共享',
        tone: 'teal',
        icon: AppstoreOutlined
      },
      {
        label: '分类',
        value: (state.overview.classifications || []).length,
        sub: `标签 ${tagCount.value} 个`,
        tone: 'success',
        icon: PartitionOutlined
      },
      {
        label: '标签',
        value: tagCount.value,
        sub: '分层 / 分级 / 认证',
        tone: 'warning',
        icon: TagsOutlined
      },
      {
        label: '术语',
        value: (state.overview.glossaryTerms || []).length,
        sub: '业务口径与同义词',
        tone: 'purple',
        icon: BookOutlined
      }
    ])

    onMounted(load)

    return {
      state,
      load,
      run,
      options,
      classOptions,
      tagsOf,
      stats,
      tagCount,
      t
    }
  },
  render() {
    const { state, stats, t } = this
    const card = t.cardStyle
    const panelTitle = { ...t.sectionTitle, marginBottom: '10px' }
    const dividerRow = {
      ...formRow,
      borderTop: `1px solid ${t.divider}`,
      marginTop: '10px',
      paddingTop: '10px'
    }

    const chip = (item: any, tone: string, key: string) => (
      <NTag
        key={key}
        size='small'
        type='default'
        style={{ ...t.tone(tone), marginRight: '4px', marginBottom: '4px' }}
      >
        {item}
      </NTag>
    )

    const createButton = (text: string, action: () => Promise<any>, done: string) => (
      <NButton
        size='small'
        type='primary'
        onClick={() => this.run(action, done)}
      >
        {{
          icon: () => h(NIcon, { size: 14 }, () => h(PlusOutlined)),
          default: () => text
        }}
      </NButton>
    )

    return (
      <div style={{ height: 'calc(100vh - 130px)', overflow: 'auto' }}>
        <div style={{ ...t.banner, padding: '16px 18px', marginBottom: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px' }}>
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
                <ClusterOutlined />
              </NIcon>
            </div>
            <div style={{ minWidth: 0 }}>
              <div style={{ fontSize: '17px', fontWeight: 700 }}>治理目录</div>
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
                域 / 数据产品 / 分类标签 / 术语表 · 均直接写入 OpenMetadata
              </div>
            </div>
            <span style={{ flex: 1 }} />
            <NButton size='small' onClick={this.load}>
              {{
                icon: () => h(NIcon, { size: 14 }, () => h(PlusOutlined)),
                default: () => '重新读取'
              }}
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
            {stats.map((item: any) => (
              <div
                key={item.label}
                style={{ ...t.tile, padding: '10px 12px', display: 'flex', gap: '10px', alignItems: 'center' }}
              >
                <div
                  style={{
                    width: '30px',
                    height: '30px',
                    minWidth: '30px',
                    borderRadius: '6px',
                    background: t.tone(item.tone).background,
                    border: `1px solid ${t.tone(item.tone).borderColor}`,
                    display: 'flex',
                    alignItems: 'center',
                    justifyContent: 'center'
                  }}
                >
                  <NIcon size={16} color={t.tone(item.tone).color}>{h(item.icon)}</NIcon>
                </div>
                <div style={{ minWidth: 0, flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'baseline', gap: '6px' }}>
                    <span style={{ fontSize: '20px', fontWeight: 700, lineHeight: 1.1, color: t.text }}>
                      {item.value}
                    </span>
                    <span style={{ fontSize: '12px', color: t.textSecondary, whiteSpace: 'nowrap' }}>
                      {item.label}
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
                    {item.sub}
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
          <div style={{ ...card, padding: '12px 14px' }}>
            <div style={panelTitle}>
              域
              <span style={{ color: t.textSecondary, fontWeight: 400, fontSize: '12px', marginLeft: '6px' }}>
                （{state.overview.domains.length}）
              </span>
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap' }}>
              {state.overview.domains.map((d: any) =>
                chip(d.name, 'blue', d.fqn)
              )}
              {!state.overview.domains.length && (
                <span style={{ color: t.textMuted, fontSize: '12px' }}>暂无</span>
              )}
            </div>
            <div style={dividerRow}>
              <NInput
                v-model:value={state.domain.name}
                placeholder='域名称'
                style={{ width: '120px' }}
              />
              <NInput
                v-model:value={state.domain.description}
                placeholder='描述'
                style={{ flex: 1, minWidth: '100px' }}
              />
              {createButton(
                '创建域',
                () => createDomain(state.domain.name, state.domain.description),
                '域已创建'
              )}
            </div>
          </div>

          <div style={{ ...card, padding: '12px 14px' }}>
            <div style={panelTitle}>
              数据产品
              <span style={{ color: t.textSecondary, fontWeight: 400, fontSize: '12px', marginLeft: '6px' }}>
                （{state.overview.dataProducts.length}）
              </span>
            </div>
            <div style={{ display: 'flex', flexWrap: 'wrap' }}>
              {state.overview.dataProducts.map((d: any) =>
                chip(d.displayName || d.name, 'teal', d.fqn)
              )}
              {!state.overview.dataProducts.length && (
                <span style={{ color: t.textMuted, fontSize: '12px' }}>暂无</span>
              )}
            </div>
            <div style={dividerRow}>
              <NInput
                v-model:value={state.product.name}
                placeholder='产品名称'
                style={{ width: '112px' }}
              />
              <NInput
                v-model:value={state.product.description}
                placeholder='描述'
                style={{ flex: 1, minWidth: '90px' }}
              />
              <NSelect
                style={{ width: '118px' }}
                options={this.options(state.overview.domains)}
                value={state.product.domainFqn || null}
                placeholder='所属域'
                onUpdateValue={(v: string) => (state.product.domainFqn = v)}
              />
              {createButton(
                '创建产品',
                () =>
                  createDataProduct(
                    state.product.name,
                    state.product.description,
                    state.product.domainFqn
                  ),
                '数据产品已创建'
              )}
            </div>
          </div>

          <div style={{ ...card, padding: '12px 14px' }}>
            <div style={panelTitle}>
              分类与标签
              <span style={{ color: t.textSecondary, fontWeight: 400, fontSize: '12px', marginLeft: '6px' }}>
                （{state.overview.classifications.length} 个分类 · {this.tagCount} 个标签）
              </span>
            </div>
            <div style={{ maxHeight: '240px', overflow: 'auto' }}>
              {state.overview.classifications.map((c: any) => (
                <div
                  key={c.name}
                  style={{
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '8px',
                    marginBottom: '8px'
                  }}
                >
                  <span
                    style={{
                      fontSize: '12px',
                      color: t.textStrong,
                      background: t.mutedBg,
                      border: `1px solid ${t.cardBorder}`,
                      borderRadius: '3px',
                      padding: '1px 8px',
                      minWidth: '110px'
                    }}
                  >
                    {c.name}
                  </span>
                  <div style={{ display: 'flex', flexWrap: 'wrap' }}>
                    {this.tagsOf(c.name).map((t: any) => chip(t.name, 'gray', t.fqn))}
                    {!this.tagsOf(c.name).length && (
                      <span style={{ color: t.textMuted, fontSize: '12px' }}>暂无标签</span>
                    )}
                  </div>
                </div>
              ))}
            </div>
            <div style={dividerRow}>
              <NInput
                v-model:value={state.classification.name}
                placeholder='新分类名称'
                style={{ width: '120px' }}
              />
              <NInput
                v-model:value={state.classification.description}
                placeholder='描述'
                style={{ flex: 1, minWidth: '90px' }}
              />
              {createButton(
                '建分类',
                () =>
                  createClassification(
                    state.classification.name,
                    state.classification.description
                  ),
                '分类已创建'
              )}
            </div>
            <div style={{ ...formRow, paddingTop: '6px' }}>
              <NSelect
                style={{ width: '128px' }}
                options={this.classOptions()}
                value={state.tag.classification || null}
                placeholder='所属分类'
                onUpdateValue={(v: string) => (state.tag.classification = v)}
              />
              <NInput
                v-model:value={state.tag.name}
                placeholder='标签名称'
                style={{ width: '108px' }}
              />
              <NInput
                v-model:value={state.tag.description}
                placeholder='描述'
                style={{ flex: 1, minWidth: '80px' }}
              />
              {createButton(
                '建标签',
                () => createTag(state.tag.classification, state.tag.name, state.tag.description),
                '标签已创建'
              )}
            </div>
          </div>

          <div style={{ ...card, padding: '12px 14px' }}>
            <div style={panelTitle}>
              术语表
              <span style={{ color: t.textSecondary, fontWeight: 400, fontSize: '12px', marginLeft: '6px' }}>
                （{state.overview.glossaryTerms.length}）
              </span>
            </div>
            <div style={{ maxHeight: '240px', overflow: 'auto', display: 'flex', flexWrap: 'wrap' }}>
              {state.overview.glossaryTerms.map((t: any) => chip(t.name, 'green', t.fqn))}
              {!state.overview.glossaryTerms.length && (
                <span style={{ color: t.textMuted, fontSize: '12px' }}>暂无</span>
              )}
            </div>
            <div style={dividerRow}>
              <NInput
                v-model:value={state.term.glossary}
                placeholder='术语库'
                style={{ width: '124px' }}
              />
              <NInput
                v-model:value={state.term.name}
                placeholder='术语'
                style={{ width: '96px' }}
              />
              <NInput
                v-model:value={state.term.description}
                placeholder='描述'
                style={{ flex: 1, minWidth: '80px' }}
              />
              {createButton(
                '创建术语',
                () =>
                  createGlossaryTerm(
                    state.term.glossary,
                    state.term.name,
                    state.term.description
                  ),
                '术语已创建'
              )}
            </div>
          </div>
        </div>
      </div>
    )
  }
})

export default Governance
