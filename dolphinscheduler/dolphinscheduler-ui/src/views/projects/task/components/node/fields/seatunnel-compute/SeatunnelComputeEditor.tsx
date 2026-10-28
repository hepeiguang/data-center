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

import { defineComponent, onMounted, PropType, reactive, ref, watch } from 'vue'
import {
  NButton,
  NCollapse,
  NCollapseItem,
  NInput,
  NSelect,
  NSpace,
  NTag,
  NEmpty
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { getSeaTunnelConnectors } from '@/service/modules/seatunnel'
import {
  assembleStockSeaTunnelConfig,
  defaultConnectorParams,
  normalizeSeaTunnelEnv,
  type StComputeNode
} from '@/views/projects/workflow/components/dag/seatunnel-compute-utils'
import catalogFallback from './connectors-catalog.json'
import StConnectorParamGuide from './StConnectorParamGuide'
import StUdpUdfPanel, { isUdpPlugin } from './StUdpUdfPanel'
import styles from './seatunnel-compute.module.scss'

export type ComputeNodeType = 'SOURCE' | 'TRANSFORM' | 'SINK'

export interface ComputeNode {
  code: number
  name: string
  type: ComputeNodeType
  pluginName: string
  params: Record<string, any>
  paramsText: string
}

interface Catalog {
  sources: string[]
  sinks: string[]
  transforms: string[]
}

const genCode = () => Date.now() + Math.floor(Math.random() * 1000)

const nodeFromGraph = (n: any): ComputeNode => ({
  code: n.code || genCode(),
  name: n.name || n.pluginName,
  type: (n.type || 'SOURCE').toUpperCase(),
  pluginName: n.pluginName,
  params: n.params || {},
  paramsText: JSON.stringify(n.params || {}, null, 2)
})

const parseGraph = (
  relation: string | undefined,
  raw: string | undefined
): { env: Record<string, any>; nodes: ComputeNode[] } => {
  const env = { 'job.mode': 'BATCH', parallelism: 2 }
  // Prefer relation (editable graph). Fallback: reconstruct from SeaTunnel config.
  if (relation) {
    try {
      const parsed = JSON.parse(relation)
      return {
        env: { ...env, ...(parsed.env || {}) },
        nodes: (parsed.nodes || []).map(nodeFromGraph)
      }
    } catch {
      /* fall through */
    }
  }
  if (!raw) return { env, nodes: [] }
  try {
    const parsed = JSON.parse(raw)
    if (parsed.nodes) {
      return {
        env: { ...env, ...(parsed.env || {}) },
        nodes: (parsed.nodes || []).map(nodeFromGraph)
      }
    }
    const nodes: ComputeNode[] = []
    ;(parsed.source || []).forEach((s: any) => {
      const { plugin_name, ...params } = s
      nodes.push(
        nodeFromGraph({
          type: 'SOURCE',
          pluginName: plugin_name,
          name: s.plugin_output || plugin_name,
          params
        })
      )
    })
    ;(parsed.transform || []).forEach((s: any) => {
      const { plugin_name, ...params } = s
      nodes.push(
        nodeFromGraph({
          type: 'TRANSFORM',
          pluginName: plugin_name,
          name: s.plugin_output || plugin_name,
          params
        })
      )
    })
    ;(parsed.sink || []).forEach((s: any) => {
      const { plugin_name, ...params } = s
      nodes.push(
        nodeFromGraph({
          type: 'SINK',
          pluginName: plugin_name,
          name: s.plugin_output || plugin_name,
          params
        })
      )
    })
    return { env: { ...env, ...(parsed.env || {}) }, nodes }
  } catch {
    return { env, nodes: [] }
  }
}

export default defineComponent({
  name: 'SeatunnelComputeEditor',
  props: {
    model: {
      type: Object as PropType<Record<string, any>>,
      required: true
    }
  },
  setup(props) {
    const { t } = useI18n()
    const catalog = reactive<Catalog>({
      sources: [],
      sinks: [],
      transforms: []
    })
    const envText = ref('')
    const nodes = ref<ComputeNode[]>([])
    const selectedCode = ref<number | null>(null)
    const filter = ref('')

    const syncToModel = () => {
      const env = normalizeSeaTunnelEnv((() => {
        try {
          return JSON.parse(envText.value || '{}')
        } catch {
          return { 'job.mode': 'BATCH', parallelism: 1 }
        }
      })())
      // Keep editable graph in stComputeGraph; rawScript is final SeaTunnel JSON for stock runtime.
      const graphNodes: StComputeNode[] = nodes.value.map((n) => {
        let params = n.params || {}
        try {
          params = JSON.parse(n.paramsText || '{}')
        } catch {
          /* keep previous */
        }
        n.params = params
        return {
          code: n.code,
          name: n.name,
          type: n.type,
          pluginName: n.pluginName,
          params
        }
      })
      const seatunnelConfig = assembleStockSeaTunnelConfig(env, graphNodes)
      props.model.stComputeGraph = JSON.stringify({ env, nodes: graphNodes })
      props.model.rawScript = JSON.stringify(seatunnelConfig, null, 2)
      props.model.scriptType = 'custom'
      props.model.useCustom = true
    }

    const addNode = (type: ComputeNodeType, pluginName: string) => {
      const code = genCode()
      const defaults = defaultConnectorParams(pluginName, type)
      const node: ComputeNode = {
        code,
        name: `${pluginName}_${code % 100000}`,
        type,
        pluginName,
        params: { ...defaults },
        paramsText: JSON.stringify(defaults, null, 2)
      }
      nodes.value.push(node)
      selectedCode.value = code
      syncToModel()
    }

    const removeNode = (code: number) => {
      nodes.value = nodes.value.filter((n) => n.code !== code)
      if (selectedCode.value === code) {
        selectedCode.value = nodes.value[0]?.code ?? null
      }
      syncToModel()
    }

    const selected = () =>
      nodes.value.find((n) => n.code === selectedCode.value) || null

    const matchFilter = (name: string) => {
      const q = filter.value.trim().toLowerCase()
      return !q || name.toLowerCase().includes(q)
    }

    onMounted(async () => {
      const initial = parseGraph(props.model.stComputeGraph, props.model.rawScript)
      envText.value = JSON.stringify(initial.env, null, 2)
      nodes.value = initial.nodes
      selectedCode.value = nodes.value[0]?.code ?? null
      props.model.scriptType = 'custom'
      props.model.useCustom = true
      if (!props.model.startupScript) {
        props.model.startupScript = 'seatunnel.sh'
      }
      syncToModel()

      try {
        const res: any = await getSeaTunnelConnectors()
        catalog.sources = res?.sources || catalogFallback.sources
        catalog.sinks = res?.sinks || catalogFallback.sinks
        catalog.transforms = res?.transforms || catalogFallback.transforms
      } catch {
        catalog.sources = catalogFallback.sources
        catalog.sinks = catalogFallback.sinks
        catalog.transforms = catalogFallback.transforms
      }
    })

    watch(
      () => [envText.value, nodes.value.map((n) => n.paramsText + n.name)],
      () => syncToModel(),
      { deep: true }
    )

    return () => {
      const current = selected()
      return (
        <div class={styles.wrap}>
          <div class={styles.palette}>
            <div class={styles.paletteTitle}>
              {t('project.node.st_compute_connectors')}
            </div>
            <NInput
              v-model:value={filter.value}
              size='small'
              clearable
              placeholder={t('project.node.st_compute_search')}
              style={{ marginBottom: '8px' }}
            />
            <NCollapse default-expanded-names={['source']}>
              <NCollapseItem
                title={`Source (${catalog.sources.length})`}
                name='source'
              >
                <div class={styles.pluginList}>
                  {catalog.sources.filter(matchFilter).map((name) => (
                    <div
                      class={styles.pluginItem}
                      onClick={() => addNode('SOURCE', name)}
                    >
                      {name}
                    </div>
                  ))}
                </div>
              </NCollapseItem>
              <NCollapseItem
                title={`Transform (${catalog.transforms.length})`}
                name='transform'
              >
                <div class={styles.pluginList}>
                  {catalog.transforms.filter(matchFilter).map((name) => (
                    <div
                      class={styles.pluginItem}
                      onClick={() => addNode('TRANSFORM', name)}
                    >
                      {name}
                    </div>
                  ))}
                </div>
              </NCollapseItem>
              <NCollapseItem
                title={`Sink (${catalog.sinks.length})`}
                name='sink'
              >
                <div class={styles.pluginList}>
                  {catalog.sinks.filter(matchFilter).map((name) => (
                    <div
                      class={styles.pluginItem}
                      onClick={() => addNode('SINK', name)}
                    >
                      {name}
                    </div>
                  ))}
                </div>
              </NCollapseItem>
            </NCollapse>
          </div>

          <div class={styles.canvas}>
            <div class={styles.sectionTitle}>
              {t('project.node.st_compute_pipeline')}
            </div>
            {nodes.value.length === 0 ? (
              <NEmpty description={t('project.node.st_compute_empty')} />
            ) : (
              <NSpace vertical>
                {nodes.value.map((n, idx) => (
                  <div
                    class={[
                      styles.nodeCard,
                      selectedCode.value === n.code && styles.nodeCardActive
                    ]}
                    onClick={() => {
                      selectedCode.value = n.code
                    }}
                  >
                    <NSpace align='center'>
                      <NTag
                        type={
                          n.type === 'SOURCE'
                            ? 'success'
                            : n.type === 'SINK'
                            ? 'warning'
                            : 'info'
                        }
                        size='small'
                      >
                        {n.type}
                      </NTag>
                      <span class={styles.nodeName}>
                        {idx + 1}. {n.pluginName}
                      </span>
                      <span class={styles.nodeAlias}>{n.name}</span>
                      <NButton
                        size='tiny'
                        quaternary
                        type='error'
                        onClick={(e: Event) => {
                          e.stopPropagation()
                          removeNode(n.code)
                        }}
                      >
                        {t('project.node.st_compute_remove')}
                      </NButton>
                    </NSpace>
                  </div>
                ))}
              </NSpace>
            )}
          </div>

          <div class={styles.config}>
            <div class={styles.sectionTitle}>
              {t('project.node.st_compute_env')}
            </div>
            <NInput
              type='textarea'
              rows={5}
              v-model:value={envText.value}
              onBlur={syncToModel}
            />
            <div class={styles.sectionTitle} style={{ marginTop: '12px' }}>
              {t('project.node.st_compute_node_config')}
            </div>
            {!current ? (
              <NEmpty description={t('project.node.st_compute_select_node')} />
            ) : (
              <NSpace vertical>
                <NInput
                  v-model:value={current.name}
                  placeholder='plugin_output / name'
                  onBlur={syncToModel}
                />
                <NSelect
                  value={current.type}
                  options={[
                    { label: 'SOURCE', value: 'SOURCE' },
                    { label: 'TRANSFORM', value: 'TRANSFORM' },
                    { label: 'SINK', value: 'SINK' }
                  ]}
                  onUpdateValue={(v: ComputeNodeType) => {
                    current.type = v
                    syncToModel()
                  }}
                />
                <StConnectorParamGuide
                  model={current as any}
                  paramsField='paramsText'
                  pluginName={current.pluginName}
                  connectorType={current.type}
                  upstreamNodes={nodes.value.slice(
                    0,
                    Math.max(
                      0,
                      nodes.value.findIndex((n) => n.code === current.code)
                    )
                  )}
                />
                {isUdpPlugin(current.pluginName, current.type) && (
                  <StUdpUdfPanel
                    model={current as any}
                    paramsField='paramsText'
                  />
                )}
                <NInput
                  type='textarea'
                  rows={14}
                  v-model:value={current.paramsText}
                  placeholder='{"plugin_input":"...","option":"..."}'
                  onBlur={syncToModel}
                />
              </NSpace>
            )}
          </div>
        </div>
      )
    }
  }
})
