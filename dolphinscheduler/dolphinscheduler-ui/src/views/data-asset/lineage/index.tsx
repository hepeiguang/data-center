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
  NInputNumber,
  NRadioGroup,
  NRadioButton,
  NSelect,
  NSwitch,
  NTabPane,
  NTabs,
  NTag
} from 'naive-ui'
import {
  ApartmentOutlined,
  BranchesOutlined,
  DatabaseOutlined,
  DeleteOutlined,
  EditOutlined,
  PartitionOutlined,
  PlusOutlined,
  ReloadOutlined,
  SearchOutlined
} from '@vicons/antd'
import {
  addKeyMapping,
  existsInOpenMetadata,
  getColumns,
  getDatasourceColumns,
  getLineage,
  listDatasourceSchemas,
  listDatasourceTables,
  listDatasources,
  listKeyMappings,
  listRowSwitches,
  removeKeyMapping,
  removeManualLineage,
  resolveRowLineage,
  saveManualLineage,
  searchAssets,
  setRowSwitch
} from '@/service/modules/data-asset/api'
import { useAssetTheme } from '../theme'

const NODE_WIDTH = 208
const NODE_HEIGHT = 62
const X_GAP = 248
const Y_GAP = 88
const GRAPH_HEIGHT = 440

const STATUS: Record<string, { text: string; color: string; bg: string; border: string }> = {
  OK: { text: '可继续上溯', tone: 'success' },
  NO_UPSTREAM: { text: '源层', tone: 'neutral' },
  DEPTH_LIMIT: { text: '已达层数上限', tone: 'neutral' },
  NO_KEY_LINEAGE: { text: '键链路断开', tone: 'warning' },
  ROW_LINEAGE_DISABLED: { text: '未开启行级', tone: 'warning' },
  ROW_NOT_FOUND: { text: '未匹配到行', tone: 'error' },
  QUERY_FAILED: { text: '反查失败', tone: 'error' },
  CYCLE_SKIPPED: { text: '成环跳过', tone: 'neutral' },
  TRUNCATED: { text: '已截断', tone: 'neutral' }
}

const layerOf = (node: any): string => {
  const tag = (node?.tags || []).find((t: string) => t.startsWith('DataLayer.'))
  if (tag) return tag.split('.').pop() as string
  const prefix = String(node?.name || '').split('_')[0].toUpperCase()
  return ['ODS', 'DWD', 'DWS', 'ADS', 'DIM'].includes(prefix) ? prefix : ''
}

const buildLayout = (data: any) => {
  const nodes: any[] = data?.nodes || []
  const edges: any[] = data?.edges || []
  const layer = new Map<string, number>()
  if (data?.root) layer.set(data.root, 0)
  const rounds = Math.max(3, nodes.length * 2)
  for (let i = 0; i < rounds; i++) {
    edges.forEach((edge) => {
      if (layer.has(edge.from) && !layer.has(edge.to)) {
        layer.set(edge.to, (layer.get(edge.from) as number) + 1)
      }
      if (layer.has(edge.to) && !layer.has(edge.from)) {
        layer.set(edge.from, (layer.get(edge.to) as number) - 1)
      }
    })
  }
  nodes.forEach((node) => {
    if (!layer.has(node.fqn)) layer.set(node.fqn, 0)
  })
  const offset = Math.min(0, ...Array.from(layer.values()))
  layer.forEach((value, key) => layer.set(key, value - offset))
  const rootColumn = layer.get(data?.root) || 0
  const byColumn = new Map<number, string[]>()
  nodes.forEach((node) => {
    const index = layer.get(node.fqn) as number
    if (!byColumn.has(index)) byColumn.set(index, [])
    byColumn.get(index)!.push(node.fqn)
  })
  byColumn.forEach((list) =>
    list.sort((a, b) => String(a).localeCompare(String(b)))
  )
  const positions = new Map<string, { x: number; y: number }>()
  byColumn.forEach((list, index) => {
    list.forEach((fqn, row) => {
      positions.set(fqn, { x: 24 + index * X_GAP, y: 56 + row * Y_GAP })
    })
  })
  const columns = Math.max(...Array.from(byColumn.keys()), 0) + 1
  const maxRows = Math.max(...Array.from(byColumn.values()).map((v) => v.length), 1)
  return {
    positions,
    columns,
    rootColumn,
    width: columns * X_GAP + 24,
    height: 56 + maxRows * Y_GAP + 40
  }
}

const edgePath = (from: { x: number; y: number }, to: { x: number; y: number }) => {
  const forward = to.x >= from.x
  const sx = forward ? from.x + NODE_WIDTH : from.x
  const tx = forward ? to.x : to.x + NODE_WIDTH
  const sy = from.y + NODE_HEIGHT / 2
  const ty = to.y + NODE_HEIGHT / 2
  const delta = Math.max(Math.abs(tx - sx) / 2, 46)
  const c1 = forward ? sx + delta : sx - delta
  const c2 = forward ? tx - delta : tx + delta
  return { d: `M ${sx} ${sy} C ${c1} ${sy}, ${c2} ${ty}, ${tx} ${ty}`, mx: (sx + tx) / 2, my: (sy + ty) / 2 }
}

const Lineage = defineComponent({
  name: 'DataAssetLineage',
  setup() {
    const state = reactive({
      q: '',
      assets: [] as any[],
      selectedFqn: '',
      data: null as any,
      loading: false,
      error: '',
      up: 2,
      down: 2,
      showColumns: true,
      fit: false,
      tab: 'graph',
      drawer: false,
      drawerMode: 'create' as 'create' | 'edit',
      saving: false,
      form: {
        fromFqn: '',
        toFqn: '',
        sqlQuery: '',
        description: '',
        columns: [] as any[]
      },
      fromColumns: [] as any[],
      toColumns: [] as any[],
      ds: {
        datasources: [] as any[],
        from: { mode: 'om', datasource: '', schema: '', schemas: [] as string[], options: [] as any[] },
        to: { mode: 'om', datasource: '', schema: '', schemas: [] as string[], options: [] as any[] }
      },
      missing: { from: false, to: false },
      keys: [] as any[],
      switches: [] as any[],
      newKey: { targetFqn: '', keyColumn: '', sourceFqn: '', sourceKeyColumn: '' },
      targetKeyColumns: [] as any[],
      sourceKeyColumns: [] as any[],
      rowForm: { fqn: '', keys: [{ column: '', value: '' }] as any[], depth: 3 },
      rowColumns: [] as any[],
      rowResult: null as any,
      rowLoading: false,
      rowError: '',
      showRows: true,
      rowExpanded: {} as Record<string, boolean>
    })

    const message = (window as any).$message
    const { t } = useAssetTheme()

    const assetOptions = computed(() =>
      state.assets.map((asset: any) => ({
        label: `${asset.displayName || asset.name}　${asset.fqn}`,
        value: asset.fqn
      }))
    )

    const graphOptions = computed(() =>
      (state.data?.nodes || []).map((node: any) => ({
        label: `${node.displayName || node.name}　${node.fqn}`,
        value: node.fqn
      }))
    )

    const stats = computed(() => {
      const edges: any[] = state.data?.edges || []
      const manual = edges.filter((e) => e.source === 'Manual').length
      const columnMappings = edges.reduce(
        (sum: number, e) => sum + (e.columns?.length || 0),
        0
      )
      const mapped = edges.filter((e: any) => (e.columns || []).length).length
      return [
        {
          label: '表',
          value: (state.data?.nodes || []).length,
          sub: `上游 ${state.up} · 下游 ${state.down}`,
          tone: 'info',
          icon: DatabaseOutlined
        },
        {
          label: '血缘边',
          value: edges.length,
          sub: `手动 ${manual} · 解析 ${edges.length - manual}`,
          tone: 'teal',
          icon: ApartmentOutlined
        },
        {
          label: '列级映射',
          value: columnMappings,
          sub: `${mapped}/${edges.length} 条边已配列`,
          tone: 'success',
          icon: PartitionOutlined
        },
        {
          label: '行级可追',
          value: state.switches.filter((r: any) => r.enabled).length,
          sub: `键映射 ${state.keys.length} 条`,
          tone: 'warning',
          icon: BranchesOutlined
        }
      ]
    })

    const loadAssets = async () => {
      try {
        const result = await searchAssets(state.q || '*', 100)
        state.assets = result.items || []
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '资产搜索失败')
      }
    }

    const loadLineage = async (fqn?: string) => {
      const target = fqn || state.selectedFqn
      if (!target) return
      state.selectedFqn = target
      state.loading = true
      state.error = ''
      try {
        state.data = await getLineage(target, state.up, state.down)
      } catch (e: any) {
        state.error = e?.response?.data?.message || '血缘读取失败'
        state.data = null
      } finally {
        state.loading = false
      }
    }

    const layout = computed(() => buildLayout(state.data))

    const edgeRows = computed(() =>
      (state.data?.edges || []).map((edge: any) => ({
        from: edge.from,
        to: edge.to,
        source: edge.source,
        sqlQuery: edge.sqlQuery,
        columns: (edge.columns || []).map((c: any) => `${c.from} → ${c.to}`).join('、')
      }))
    )

    const loadDatasources = async () => {
      try {
        state.ds.datasources = await listDatasources()
        const first = state.ds.datasources.length ? state.ds.datasources[0].name : ''
        state.ds.from.datasource = state.ds.from.datasource || first
        state.ds.to.datasource = state.ds.to.datasource || first
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '数据源清单读取失败')
      }
    }

    const sideFqn = (side: 'from' | 'to') =>
      side === 'from' ? state.form.fromFqn : state.form.toFqn

    const setSideFqn = (side: 'from' | 'to', fqn: string) => {
      if (side === 'from') state.form.fromFqn = fqn
      else state.form.toFqn = fqn
    }

    const searchSideTables = async (side: 'from' | 'to', q: string) => {
      const cfg = state.ds[side]
      if (!cfg.datasource || !cfg.schema) return
      try {
        const rows = await listDatasourceTables(cfg.datasource, cfg.schema, q, 100)
        cfg.options = rows.map((row: any) => ({
          label: `${row.table} · ${row.type === 'table' ? '表' : '视图'}`,
          value: row.fqn
        }))
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '表清单读取失败')
      }
    }

    const refreshSideColumns = async (side: 'from' | 'to') => {
      const cfg = state.ds[side]
      const fqn = sideFqn(side)
      if (!fqn) return
      try {
        const columns =
          cfg.mode === 'ds'
            ? await getDatasourceColumns(
                cfg.datasource,
                cfg.schema,
                fqn.split('.').pop() as string
              )
            : await getColumns(fqn)
        if (side === 'from') state.fromColumns = columns
        else state.toColumns = columns
        const check: any = await existsInOpenMetadata(fqn)
        state.missing[side] = !check.inOpenMetadata
      } catch (e: any) {
        state.missing[side] = false
        message?.error(e?.response?.data?.message || '字段读取失败')
      }
    }

    const pickSide = async (side: 'from' | 'to', mode: string) => {
      const cfg = state.ds[side]
      cfg.mode = mode
      if (mode === 'ds' && !cfg.schemas.length) {
        try {
          cfg.schemas = await listDatasourceSchemas(cfg.datasource)
          cfg.schema = cfg.schemas[0] || ''
          await searchSideTables(side, '')
        } catch (e: any) {
          message?.error(e?.response?.data?.message || '模式清单读取失败')
        }
      }
      await refreshSideColumns(side)
    }

    const openDrawer = async (edge?: any) => {
      state.drawerMode = edge ? 'edit' : 'create'
      state.form = {
        fromFqn: edge?.from || state.selectedFqn || '',
        toFqn: edge?.to || '',
        sqlQuery: edge?.sqlQuery || '',
        description: '',
        columns: (edge?.columns || []).map((c: any) => ({
          from: c.from,
          to: c.to,
          function: c.function || ''
        }))
      }
      state.ds.from.mode = 'om'
      state.ds.to.mode = 'om'
      state.missing = { from: false, to: false }
      state.drawer = true
      await Promise.all([refreshSideColumns('from'), refreshSideColumns('to')])
    }

    const columnOptions = (list: any[]) =>
      list.map((c: any) => ({ label: `${c.name} (${c.dataType})`, value: c.name }))

    const saveManual = async () => {
      if (!state.form.fromFqn || !state.form.toFqn) {
        message?.warning('请选择上游表和下游表')
        return
      }
      state.saving = true
      try {
        const payload = {
          ...state.form,
          columns: state.form.columns.filter((c) => c.from && c.to)
        }
        const result: any = await saveManualLineage(payload)
        message?.success(`已写入 OpenMetadata（${result.columns} 个列映射）`)
        state.drawer = false
        await loadLineage()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '血缘写入失败')
      } finally {
        state.saving = false
      }
    }

    const deleteEdge = async (row: any) => {
      try {
        await removeManualLineage(row.from, row.to)
        message?.success('血缘边已删除')
        await loadLineage()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '血缘删除失败')
      }
    }

    const loadLineageConfig = async () => {
      try {
        const [keys, switches] = await Promise.all([listKeyMappings(), listRowSwitches()])
        state.keys = keys || []
        state.switches = switches || []
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '血缘配置读取失败')
      }
    }

    const loadKeyColumns = async (side: 'target' | 'source') => {
      const fqn = side === 'target' ? state.newKey.targetFqn : state.newKey.sourceFqn
      if (!fqn) return
      try {
        const columns = await getColumns(fqn)
        if (side === 'target') {
          state.targetKeyColumns = columns
          if (!columns.some((c: any) => c.name === state.newKey.keyColumn)) {
            state.newKey.keyColumn = ''
          }
        } else {
          state.sourceKeyColumns = columns
          if (!columns.some((c: any) => c.name === state.newKey.sourceKeyColumn)) {
            state.newKey.sourceKeyColumn = ''
          }
        }
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '字段读取失败')
      }
    }

    const addKey = async () => {
      const { targetFqn, keyColumn, sourceFqn, sourceKeyColumn } = state.newKey
      if (!targetFqn || !keyColumn || !sourceFqn || !sourceKeyColumn) {
        message?.warning('键映射四个值都要填')
        return
      }
      try {
        await addKeyMapping({ targetFqn, keyColumn, sourceFqn, sourceKeyColumn })
        message?.success('键映射已登记')
        state.newKey = { targetFqn: '', keyColumn: '', sourceFqn: '', sourceKeyColumn: '' }
        state.targetKeyColumns = []
        state.sourceKeyColumns = []
        await loadLineageConfig()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '键映射登记失败')
      }
    }

    const dropKey = async (id: number) => {
      try {
        await removeKeyMapping(id)
        await loadLineageConfig()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '键映射删除失败')
      }
    }

    const toggleSwitch = async (tableFqn: string, enabled: boolean) => {
      try {
        await setRowSwitch(tableFqn, enabled)
        await loadLineageConfig()
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '开关设置失败')
      }
    }

    const enableRootRowLineage = async () => {
      await toggleSwitch(state.rowForm.fqn || state.selectedFqn, true)
    }

    const loadRowColumns = async (fqn: string) => {
      state.rowForm.fqn = fqn
      state.rowForm.keys = [{ column: '', value: '' }]
      if (!fqn) return
      try {
        state.rowColumns = await getColumns(fqn)
      } catch (e: any) {
        message?.error(e?.response?.data?.message || '字段读取失败')
      }
    }

    const resolveRow = async () => {
      const keys: Record<string, string> = {}
      state.rowForm.keys.forEach((item: any) => {
        if (item.column && item.value !== '') keys[item.column] = item.value
      })
      if (!state.rowForm.fqn || Object.keys(keys).length === 0) {
        message?.warning('请选择表并填写至少一个键列取值')
        return
      }
      state.rowLoading = true
      state.rowError = ''
      state.rowResult = null
      state.rowExpanded = {}
      try {
        state.rowResult = await resolveRowLineage(state.rowForm.fqn, keys, state.rowForm.depth)
        if (state.rowResult.status !== 'OK') {
          state.rowError = state.rowResult.message || '未能解析'
        }
      } catch (e: any) {
        state.rowError = e?.response?.data?.message || '行级解析失败'
      } finally {
        state.rowLoading = false
      }
    }

    const edgeColumns = [
      { title: '上游表', key: 'from', ellipsis: { tooltip: true }, minWidth: 180 },
      { title: '下游表', key: 'to', ellipsis: { tooltip: true }, minWidth: 180 },
      {
        title: '来源',
        key: 'source',
        width: 96,
        render: (row: any) =>
          h(
            NTag,
            {
              size: 'small',
              type: 'default',
              style:
                row.source === 'Manual'
                  ? t.value.tone('warning')
                  : t.value.tone('info')
            },
            () => (row.source === 'Manual' ? '手动' : row.source || '自动')
          )
      },
      {
        title: '列级映射',
        key: 'columns',
        ellipsis: { tooltip: true },
        minWidth: 200,
        render: (row: any) =>
          h('span', { style: { color: row.columns ? t.value.tone('teal').color : t.value.textMuted } }, row.columns || '未配置')
      },
      { title: '加工 SQL', key: 'sqlQuery', ellipsis: { tooltip: true }, minWidth: 220 },
      {
        title: '操作',
        key: 'actions',
        width: 128,
        render: (row: any) =>
          h('div', { style: { display: 'flex', gap: '6px' } }, [
            h(
              NButton,
              { size: 'tiny', onClick: () => openDrawer(row) },
              () => '编辑'
            ),
            h(
              NButton,
              {
                size: 'tiny',
                type: 'error',
                ghost: true,
                onClick: () => deleteEdge(row)
              },
              () => '删除'
            )
          ])
      }
    ]

    const keyColumns = [
      { title: '目标表', key: 'target_fqn', ellipsis: { tooltip: true }, minWidth: 200 },
      { title: '目标键列', key: 'key_column', width: 130 },
      { title: '上游表', key: 'source_fqn', ellipsis: { tooltip: true }, minWidth: 200 },
      { title: '上游键列', key: 'source_key_column', width: 130 },
      { title: '维护人', key: 'maintained_by', width: 100 },
      {
        title: '操作',
        key: 'ops',
        width: 72,
        render: (row: any) =>
          h(
            NButton,
            { size: 'tiny', type: 'error', ghost: true, onClick: () => dropKey(row.id) },
            () => '删除'
          )
      }
    ]

    const switchColumns = [
      { title: '表', key: 'table_fqn', ellipsis: { tooltip: true }, minWidth: 240 },
      {
        title: '行级追踪',
        key: 'enabled',
        width: 110,
        render: (row: any) =>
          h(NSwitch, {
            size: 'small',
            value: !!row.enabled,
            'onUpdate:value': (value: boolean) => toggleSwitch(row.table_fqn, value)
          })
      },
      { title: '更新人', key: 'updated_by', width: 110 },
      {
        title: '更新时间',
        key: 'updated_at',
        width: 180,
        render: (row: any) => String(row.updated_at || '').replace('T', ' ').slice(0, 19)
      }
    ]

    onMounted(async () => {
      await loadAssets()
      await loadDatasources()
      await loadLineageConfig()
      const first = state.assets.find((a: any) => String(a.fqn).includes('dwd')) || state.assets[0]
      if (first) {
        await loadLineage(first.fqn)
        state.rowForm.fqn = first.fqn
        loadRowColumns(first.fqn)
      }
    })

    return {
      state,
      layout,
      stats,
      edgeRows,
      assetOptions,
      graphOptions,
      edgeColumns,
      keyColumns,
      switchColumns,
      columnOptions,
      loadAssets,
      loadLineage,
      openDrawer,
      loadDatasources,
      pickSide,
      setSideFqn,
      searchSideTables,
      refreshSideColumns,
      loadKeyColumns,
      saveManual,
      deleteEdge,
      addKey,
      dropKey,
      toggleSwitch,
      enableRootRowLineage,
      loadRowColumns,
      resolveRow,
      layerOf,
      t
    }
  },

  render() {
    const { state, layout, stats, edgeRows, columnOptions, t } = this
    const card = t.cardStyle
    const nodeMap = new Map<string, any>()
    ;(state.data?.nodes || []).forEach((node: any) => nodeMap.set(node.fqn, node))
    const edges: any[] = state.data?.edges || []
    const rootLabel = nodeMap.get(state.selectedFqn)?.displayName || state.selectedFqn

    const bands = Array.from({ length: layout.columns }, (_, index) => {
      const offset = index - layout.rootColumn
      return {
        index,
        label:
          offset === 0
            ? '当前表'
            : offset < 0
            ? `上游 ${-offset}`
            : `下游 ${offset}`
      }
    })

    const renderTreeNode = (node: any, level: number): any => {
      const status = STATUS[node.status] || STATUS.OK
      const statusTone = t.tone(status.tone)
      const origin = node.origin || {}
      const keys = Object.entries(node.keys || {})
      const rows: any[] = node.rows || []
      const rowKey = `${node.fqn}|${JSON.stringify(node.keys)}`
      const expanded = state.rowExpanded[rowKey] === undefined
        ? level <= 1 && state.showRows
        : state.rowExpanded[rowKey]
      const rowColumns = (node.rowColumns && node.rowColumns.length
        ? node.rowColumns
        : Object.keys(rows[0] || {})
      ).map((column: string) => ({
        title: column,
        key: column,
        minWidth: 90,
        ellipsis: { tooltip: true },
        render: (row: any) =>
          h(
            'span',
            {
              style: {
                fontSize: '11px',
                color: String(row[column]) === '******' ? t.tone('warning').color : t.text
              }
            },
            row[column] === null || row[column] === undefined ? '-' : String(row[column])
          )
      }))
      return h('div', { key: `${node.fqn}-${level}-${JSON.stringify(node.keys)}` }, [
        h(
          'div',
          {
            style: {
              marginLeft: `${level * 18}px`,
              paddingLeft: '10px',
              borderLeft: level > 0 ? `1px dashed ${t.tintBorder}` : 'none',
              paddingTop: '6px',
              paddingBottom: '6px'
            }
          },
          [
            h('div', { style: { display: 'flex', alignItems: 'center', gap: '8px' } }, [
              h(NIcon, { size: 14, color: t.primary }, () => h(DatabaseOutlined)),
              h('span', { style: { fontWeight: 600 } }, node.table),
              h(
                NTag,
                {
                  size: 'tiny',
                  type: 'default',
                  style: {
                    background: statusTone.background,
                    borderColor: statusTone.borderColor,
                    color: statusTone.color
                  }
                },
                () => status.text
              ),
              origin.matchedRows
                ? h(
                    NTag,
                    {
                      size: 'tiny',
                      type: 'default',
                      style: t.tone('neutral')
                    },
                    () => `命中 ${origin.matchedRows} 行`
                  )
                : null,
              origin.participatingSet
                ? h(
                    NTag,
                    {
                      size: 'tiny',
                      type: 'default',
                      style: t.tone('teal')
                    },
                    () => '聚合参与集合'
                  )
                : null,
              h('span', { style: { flex: 1 } }),
              node.fqn !== state.selectedFqn
                ? h(
                    NButton,
                    {
                      size: 'tiny',
                      onClick: () => {
                        this.loadLineage(node.fqn)
                        state.tab = 'graph'
                      }
                    },
                    () => '看表级'
                  )
                : null
            ]),
            keys.length
              ? h(
                  'div',
                  { style: { display: 'flex', gap: '6px', flexWrap: 'wrap', marginTop: '4px' } },
                  keys.map(([column, value]) =>
                    h(
                      'span',
                      {
                        key: column,
                        style: {
                          fontSize: '11px',
                          color: t.tone('info').color,
                          background: t.tone('info').background,
                          border: `1px solid ${t.tone('info').borderColor}`,
                          borderRadius: '3px',
                          padding: '1px 6px'
                        }
                      },
                      `${column} = ${value}`
                    )
                  )
                )
              : null,
            rows.length
              ? h(
                  'div',
                  { style: { marginTop: '6px' } },
                  [
                    h(
                      NButton,
                      {
                        size: 'tiny',
                        text: true,
                        type: 'primary',
                        onClick: () => {
                          state.rowExpanded[rowKey] = !(
                            state.rowExpanded[rowKey] === undefined
                              ? level <= 1 && state.showRows
                              : state.rowExpanded[rowKey]
                          )
                        }
                      },
                      () => `${expanded ? '收起整行' : '查看整行'}（${rows.length} 行 · ${rowColumns.length} 列）`
                    ),
                    expanded
                      ? h(NDataTable, {
                          size: 'tiny',
                          bordered: true,
                          columns: rowColumns,
                          data: rows,
                          maxHeight: 180,
                          scrollX: Math.max(rowColumns.length * 110, 400),
                          style: { marginTop: '6px' }
                        })
                      : null
                  ]
                )
              : null,
            node.rowNote
              ? h(
                  'div',
                  {
                    style: {
                      fontSize: '11px',
                      color: t.tone('warning').color,
                      marginTop: '4px'
                    }
                  },
                  `整行数据未展示：${node.rowNote}`
                )
              : null,
            origin.viaRegistry || origin.partialKey || origin.functions
              ? h(
                  'div',
                  {
                    style: {
                      fontSize: '11px',
                      color: t.textSecondary,
                      marginTop: '4px',
                      display: 'flex',
                      gap: '10px',
                      flexWrap: 'wrap'
                    }
                  },
                  [
                    origin.viaRegistry
                      ? h('span', { key: 'reg' }, `登记键映射：${origin.viaRegistry.join('、')}`)
                      : null,
                    origin.partialKey
                      ? h(
                          'span',
                          { key: 'partial', style: { color: t.tone('warning').color } },
                          `仅按已映射键反查，未映射：${origin.partialKey.join('、')}`
                        )
                      : null,
                    origin.functions
                      ? h('span', { key: 'fn' }, `计算：${origin.functions.join('、')}`)
                      : null,
                    origin.edgeSource
                      ? h('span', { key: 'src' }, `边来源：${origin.edgeSource}`)
                      : null
                  ]
                )
              : null,
            node.message
              ? h(
                  'div',
                  { style: { fontSize: '11px', color: t.textMuted, marginTop: '2px' } },
                  node.message
                )
              : null
          ]
        ),
        ...(node.children || []).map((child: any) => renderTreeNode(child, level + 1))
      ])
    }

    return (
      <div style={{ height: 'calc(100vh - 130px)', overflow: 'auto' }}>
        <div style={{ ...t.banner, padding: '16px 18px', marginBottom: '12px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'nowrap' }}>
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
                <ApartmentOutlined />
              </NIcon>
            </div>
            <div style={{ minWidth: 0 }}>
              <div style={{ fontSize: '17px', fontWeight: 700 }}>数据血缘</div>
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
                {rootLabel || '未选择资产'}
                <span style={{ marginLeft: '8px', color: t.textMuted }}>{state.selectedFqn}</span>
              </div>
            </div>
            <span style={{ flex: 1 }} />
            <NInput
              v-model:value={state.q}
              placeholder='搜索表 / 字段，回车检索'
              style={{ width: '260px', flexShrink: 0 }}
              onKeydown={(e: KeyboardEvent) => e.key === 'Enter' && this.loadAssets()}
              v-slots={{
                suffix: () => (
                  <NIcon size={16} color={t.primary} onClick={this.loadAssets}>
                    <SearchOutlined />
                  </NIcon>
                )
              }}
            />
            <div style={{ display: 'flex', alignItems: 'center', gap: '4px', flexShrink: 0 }}>
              <span style={{ color: t.textSecondary, fontSize: '12px' }}>上游</span>
              <NInputNumber
                v-model:value={state.up}
                min={1}
                max={5}
                size='small'
                style={{ width: '68px' }}
                onUpdateValue={() => this.loadLineage()}
              />
              <span style={{ color: t.textSecondary, fontSize: '12px' }}>下游</span>
              <NInputNumber
                v-model:value={state.down}
                min={1}
                max={5}
                size='small'
                style={{ width: '68px' }}
                onUpdateValue={() => this.loadLineage()}
              />
            </div>
            <NButton size='small' onClick={() => this.loadLineage()}>
              {{
                icon: () => h(NIcon, { size: 14 }, () => h(ReloadOutlined)),
                default: () => '刷新'
              }}
            </NButton>
            <NButton size='small' type='primary' onClick={() => this.openDrawer()}>
              {{
                icon: () => h(NIcon, { size: 14 }, () => h(EditOutlined)),
                default: () => '手动配置血缘'
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

        <div style={{ ...card, padding: '10px 12px', marginBottom: '10px' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
            <span style={{ fontWeight: 600, fontSize: '12px', color: t.text }}>资产快选</span>
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
              {state.assets.length}
            </span>
            <div
              style={{ display: 'flex', flexWrap: 'wrap', gap: '6px', flex: 1 }}
            >
              {state.assets.map((asset: any) => {
                const active = asset.fqn === state.selectedFqn
                return (
                  <span
                    key={asset.fqn}
                    onClick={() => this.loadLineage(asset.fqn)}
                    style={{
                      cursor: 'pointer',
                      whiteSpace: 'nowrap',
                      fontSize: '12px',
                      padding: '2px 9px',
                      borderRadius: '4px',
                      background: active ? t.primary : t.card,
                      border: `1px solid ${active ? t.primary : t.borderSoft}`,
                      color: active ? t.onPrimary : t.textStrong,
                      fontWeight: active ? 600 : 400
                    }}
                  >
                    {asset.displayName || asset.name}
                  </span>
                )
              })}
            </div>
          </div>
        </div>

        <div style={{ minWidth: 0 }}>
            {state.error && (
              <NAlert type='error' style={{ marginBottom: '10px' }}>
                {state.error}
              </NAlert>
            )}
            <NTabs v-model:value={state.tab} type='line' animated>
              <NTabPane name='graph' tab='血缘图'>
                {!state.data ? (
                  <NEmpty description='选择资产后展示血缘' style={{ marginTop: '60px' }} />
                ) : (
                  <div style={card}>
                    <div
                      style={{
                        display: 'flex',
                        alignItems: 'center',
                        gap: '14px',
                        padding: '8px 12px',
                        borderBottom: `1px solid ${t.divider}`,
                        fontSize: '12px',
                        color: t.textSecondary,
                        whiteSpace: 'nowrap',
                        flexWrap: 'nowrap'
                      }}
                    >
                      <span>
                        <i
                          style={{
                            display: 'inline-block',
                            width: '14px',
                            height: '2px',
                            background: t.primary,
                            verticalAlign: 'middle',
                            marginRight: '4px'
                          }}
                        />
                        手动血缘
                      </span>
                      <span>
                        <i
                          style={{
                            display: 'inline-block',
                            width: '14px',
                            height: '2px',
                            background: t.edgeLine,
                            verticalAlign: 'middle',
                            marginRight: '4px'
                          }}
                        />
                        解析血缘
                      </span>
                      <span>
                        <i
                          style={{
                            display: 'inline-block',
                            width: '14px',
                            height: '2px',
                            background: t.tone('teal').color,
                            verticalAlign: 'middle',
                            marginRight: '4px'
                          }}
                        />
                        列级映射
                      </span>
                      <span style={{ flex: 1 }} />
                      <span>列标注</span>
                      <NSwitch v-model:value={state.showColumns} size='small' />
                      <span>适应宽度</span>
                      <NSwitch v-model:value={state.fit} size='small' />
                    </div>
                    <div style={{ overflow: 'auto', maxHeight: GRAPH_HEIGHT }}>
                    <svg
                      {...(state.fit
                        ? {
                            viewBox: `0 0 ${layout.width} ${layout.height}`,
                            width: '100%',
                            height: GRAPH_HEIGHT,
                            preserveAspectRatio: 'xMidYMid meet'
                          }
                        : {
                            viewBox: `0 0 ${layout.width} ${layout.height}`,
                            width: layout.width,
                            height: layout.height
                          })}
                      style={{ display: 'block', minWidth: state.fit ? undefined : '100%' }}
                    >
                      <defs>
                        <marker
                          id='lineage-arrow'
                          viewBox='0 0 10 10'
                          refX='9'
                          refY='5'
                          markerWidth='7'
                          markerHeight='7'
                          orient='auto-start-reverse'
                        >
                          <path d='M 0 0 L 10 5 L 0 10 z' fill={t.edgeLine} />
                        </marker>
                        <marker
                          id='lineage-arrow-manual'
                          viewBox='0 0 10 10'
                          refX='9'
                          refY='5'
                          markerWidth='7'
                          markerHeight='7'
                          orient='auto-start-reverse'
                        >
                          <path d='M 0 0 L 10 5 L 0 10 z' fill={t.primary} />
                        </marker>
                      </defs>
                      {bands.map((band: any) => (
                        <g key={`band-${band.index}`}>
                          <rect
                            x={band.index * X_GAP}
                            y={0}
                            width={X_GAP}
                            height={layout.height}
                            fill={band.index % 2 === 0 ? t.bandAlt : t.card}
                          />
                          <text x={band.index * X_GAP + 12} y={22} font-size='11' fill={t.textMuted}>
                            {band.label}
                          </text>
                        </g>
                      ))}
                      {edges.map((edge, index) => {
                        const from = layout.positions.get(edge.from)
                        const to = layout.positions.get(edge.to)
                        if (!from || !to) return null
                        const manual = edge.source === 'Manual'
                        const stroke = manual ? t.primary : t.edgeLine
                        const { d, mx, my } = edgePath(from, to)
                        const label =
                          edge.columns && edge.columns.length
                            ? `${edge.columns[0].from} → ${edge.columns[0].to}` +
                              (edge.columns.length > 1
                                ? ` +${edge.columns.length - 1}`
                                : '')
                            : ''
                        return (
                          <g key={`edge-${index}`}>
                            <path
                              d={d}
                              fill='none'
                              stroke={stroke}
                              stroke-width={manual ? 1.8 : 1.4}
                              stroke-dasharray={edge.columns?.length ? undefined : '4 3'}
                              marker-end={
                                manual
                                  ? 'url(#lineage-arrow-manual)'
                                  : 'url(#lineage-arrow)'
                              }
                            />
                            {state.showColumns && label && (
                              <text
                                x={mx}
                                y={my - 4}
                                font-size='10'
                                fill={t.tone('teal').color}
                                text-anchor='middle'
                                stroke={t.card}
                                stroke-width='3'
                                style={{ paintOrder: 'stroke' }}
                              >
                                {label}
                              </text>
                            )}
                          </g>
                        )
                      })}
                      {Array.from(layout.positions.entries()).map(([fqn, position]) => {
                        const node = nodeMap.get(fqn) || { fqn, name: fqn }
                        const layer = this.layerOf(node)
                        const accent = t.hue(layer)
                        const isRoot = fqn === state.selectedFqn
                        return (
                          <g
                            key={fqn}
                            transform={`translate(${position.x},${position.y})`}
                            style={{ cursor: 'pointer' }}
                            onClick={() => this.loadLineage(fqn)}
                          >
                            <rect
                              width={NODE_WIDTH}
                              height={NODE_HEIGHT}
                              rx='4'
                              fill={isRoot ? t.tint : t.card}
                              stroke={isRoot ? t.primary : t.borderNode}
                              stroke-width={isRoot ? 1.6 : 1}
                            />
                            <rect width='3' height={NODE_HEIGHT} rx='1.5' fill={accent} />
                            <text
                              x='12'
                              y='24'
                              font-size='13'
                              fill={t.text}
                              style={{ fontWeight: isRoot ? 700 : 500 }}
                            >
                              {(node.displayName || node.name || '').slice(0, 18)}
                            </text>
                            <text x='12' y='42' font-size='11' fill={t.textSecondary}>
                              {(node.name || '').slice(0, 26)}
                            </text>
                            {layer ? (
                              <text x='12' y='56' font-size='10' fill={accent}>
                                {layer}
                                {node.columnCount ? ` · ${node.columnCount} 列` : ''}
                              </text>
                            ) : (
                              <text x='12' y='56' font-size='10' fill={t.textMuted}>
                                {node.columnCount ? `${node.columnCount} 列` : ''}
                              </text>
                            )}
                          </g>
                        )
                      })}
                    </svg>
                    </div>
                  </div>
                )}
              </NTabPane>

              <NTabPane name='edges' tab='血缘边与列映射'>
                <div style={{ ...card, padding: '10px 12px' }}>
                  <NDataTable
                    size='small'
                    bordered
                    columns={this.edgeColumns}
                    data={edgeRows}
                    max-height={520}
                    scroll-x={1000}
                  />
                </div>
              </NTabPane>

              <NTabPane name='config' tab='键映射与行级开关'>
                <div style={{ ...card, padding: '12px 14px', marginBottom: '12px' }}>
                  <div
                    style={{
                      fontWeight: 600,
                      marginBottom: '10px',
                      borderLeft: `3px solid ${t.primary}`,
                      paddingLeft: '8px'
                    }}
                  >
                    键映射登记
                    <span style={{ color: t.textSecondary, fontWeight: 400, fontSize: '12px', marginLeft: '8px' }}>
                      列级血缘里没有键语义时，由平台补齐“目标键列 ← 上游键列”
                    </span>
                  </div>
                  <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', marginBottom: '10px' }}>
                    <NSelect
                      v-model:value={state.newKey.targetFqn}
                      options={this.graphOptions}
                      filterable
                      placeholder='目标表'
                      style={{ width: '240px' }}
                      onUpdateValue={(v: string) => {
                        state.newKey.keyColumn = ''
                        this.loadKeyColumns('target')
                      }}
                    />
                    <NSelect
                      v-model:value={state.newKey.keyColumn}
                      options={columnOptions(state.targetKeyColumns)}
                      placeholder='目标键列'
                      filterable
                      style={{ width: '160px' }}
                    />
                    <NSelect
                      v-model:value={state.newKey.sourceFqn}
                      options={this.graphOptions}
                      filterable
                      placeholder='上游表'
                      style={{ width: '240px' }}
                      onUpdateValue={(v: string) => {
                        state.newKey.sourceKeyColumn = ''
                        this.loadKeyColumns('source')
                      }}
                    />
                    <NSelect
                      v-model:value={state.newKey.sourceKeyColumn}
                      options={columnOptions(state.sourceKeyColumns)}
                      placeholder='上游键列'
                      filterable
                      style={{ width: '160px' }}
                    />
                    <NButton size='small' type='primary' onClick={this.addKey}>
                      {{
                        icon: () => h(NIcon, { size: 14 }, () => h(PlusOutlined)),
                        default: () => '登记'
                      }}
                    </NButton>
                  </div>
                  <NDataTable
                    size='small'
                    bordered
                    columns={this.keyColumns}
                    data={state.keys}
                    max-height={260}
                    scroll-x={900}
                  />
                </div>
                <div style={{ ...card, padding: '12px 14px' }}>
                  <div
                    style={{
                      fontWeight: 600,
                      marginBottom: '10px',
                      borderLeft: `3px solid ${t.primary}`,
                      paddingLeft: '8px'
                    }}
                  >
                    行级追踪开关
                    <span style={{ color: t.textSecondary, fontWeight: 400, fontSize: '12px', marginLeft: '8px' }}>
                      默认关闭，按表开放；关闭的表在解析时标为“未开启行级”
                    </span>
                  </div>
                  <div style={{ display: 'flex', gap: '8px', marginBottom: '10px' }}>
                    <NSelect
                      v-model:value={state.newKey.targetFqn}
                      options={this.graphOptions}
                      filterable
                      placeholder='选择表以开启'
                      style={{ width: '300px' }}
                    />
                    <NButton size='small' onClick={() => this.toggleSwitch(state.newKey.targetFqn, true)}>
                      开启行级
                    </NButton>
                    <NButton size='small' onClick={() => this.toggleSwitch(state.newKey.targetFqn, false)}>
                      关闭
                    </NButton>
                  </div>
                  <NDataTable
                    size='small'
                    bordered
                    columns={this.switchColumns}
                    data={state.switches}
                    max-height={260}
                  />
                </div>
              </NTabPane>

              <NTabPane name='row' tab='行级血缘'>
                <div style={{ ...card, padding: '12px 14px', marginBottom: '10px' }}>
                  <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap', alignItems: 'center' }}>
                    <NSelect
                      v-model:value={state.rowForm.fqn}
                      options={this.graphOptions.length ? this.graphOptions : this.assetOptions}
                      filterable
                      placeholder='目标表'
                      style={{ width: '280px' }}
                      onUpdateValue={(v: string) => this.loadRowColumns(v)}
                    />
                    {state.rowForm.keys.map((item: any, index: number) => (
                      <div
                        key={`key-${index}`}
                        style={{ display: 'flex', gap: '6px', alignItems: 'center' }}
                      >
                        <NSelect
                          v-model:value={item.column}
                          options={columnOptions(state.rowColumns)}
                          filterable
                          placeholder='键列'
                          style={{ width: '160px' }}
                        />
                        <NInput
                          v-model:value={item.value}
                          placeholder='取值'
                          style={{ width: '160px' }}
                        />
                        {state.rowForm.keys.length > 1 && (
                          <NButton
                            size='tiny'
                            quaternary
                            type='error'
                            onClick={() => state.rowForm.keys.splice(index, 1)}
                          >
                            {{
                              icon: () => h(NIcon, { size: 12 }, () => h(DeleteOutlined))
                            }}
                          </NButton>
                        )}
                      </div>
                    ))}
                    <NButton
                      size='small'
                      onClick={() => state.rowForm.keys.push({ column: '', value: '' })}
                    >
                      加键列
                    </NButton>
                    <span style={{ color: t.textSecondary, fontSize: '12px' }}>展开层数</span>
                    <NInputNumber
                      v-model:value={state.rowForm.depth}
                      min={1}
                      max={5}
                      size='small'
                      style={{ width: '80px' }}
                    />
                    <NButton size='small' type='primary' loading={state.rowLoading} onClick={this.resolveRow}>
                      {{
                        icon: () => h(NIcon, { size: 14 }, () => h(BranchesOutlined)),
                        default: () => '按值反查'
                      }}
                    </NButton>
                    <span style={{ color: t.textSecondary, fontSize: '12px' }}>展开时显示整行</span>
                    <NSwitch v-model:value={state.showRows} size='small' />
                  </div>
                  <div style={{ color: t.textSecondary, fontSize: '12px', marginTop: '8px' }}>
                    沿列级血缘把键值映射成上游查询条件逐层反查；结果基于当前数据快照，历史不保证可复现。
                    整行列与脱敏沿用该表的浏览白名单规则，未授权的表只显示键值。
                  </div>
                </div>

                {state.rowResult && state.rowResult.status === 'ROW_LINEAGE_DISABLED' && (
                  <NAlert type='warning' style={{ marginBottom: '10px' }}>
                    <div>该表未开启行级追踪，无法按值反查。</div>
                    <NButton size='small' type='primary' style={{ marginTop: '6px' }} onClick={this.enableRootRowLineage}>
                      为该表开启
                    </NButton>
                  </NAlert>
                )}
                {state.rowError && state.rowResult?.status !== 'ROW_LINEAGE_DISABLED' && (
                  <NAlert type='error' style={{ marginBottom: '10px' }}>
                    {state.rowError}
                  </NAlert>
                )}

                {state.rowResult?.tree ? (
                  <div style={{ ...card, padding: '10px 14px' }}>
                    <div
                      style={{
                        display: 'flex',
                        gap: '14px',
                        fontSize: '12px',
                        color: t.textSecondary,
                        paddingBottom: '8px',
                        marginBottom: '6px',
                        borderBottom: `1px solid ${t.divider}`
                      }}
                    >
                      <span>
                        节点
                        <b style={{ color: t.primary, marginLeft: '4px' }}>
                          {state.rowResult.nodeCount}
                        </b>
                      </span>
                      <span>
                        耗时
                        <b style={{ color: t.tone('teal').color, marginLeft: '4px' }}>
                          {state.rowResult.durationMs} ms
                        </b>
                      </span>
                      <span>解析时间 {String(state.rowResult.resolvedAt || '').slice(0, 19)}Z</span>
                      {state.rowResult.truncated && (
                        <span style={{ color: t.tone('warning').color }}>已达节点上限，结果被截断</span>
                      )}
                      <span style={{ flex: 1 }} />
                      <NIcon size={14} color={t.primary}>
                        <ApartmentOutlined />
                      </NIcon>
                    </div>
                    {renderTreeNode(state.rowResult.tree, 0)}
                  </div>
                ) : (
                  <NEmpty description='选择表并填写键列取值后解析' style={{ marginTop: '50px' }} />
                )}
              </NTabPane>
            </NTabs>
        </div>

        <NDrawer v-model:show={state.drawer} width={760}>
          <NDrawerContent
            title={state.drawerMode === 'edit' ? '编辑血缘边' : '手动配置血缘'}
            closable
            v-slots={{
              footer: () => (
                <div style={{ display: 'flex', gap: '8px' }}>
                  <NButton type='primary' loading={state.saving} onClick={this.saveManual}>
                    保存到 OpenMetadata
                  </NButton>
                  {state.drawerMode === 'edit' && (
                    <NButton
                      type='error'
                      ghost
                      onClick={() => {
                        this.deleteEdge({ from: state.form.fromFqn, to: state.form.toFqn })
                        state.drawer = false
                      }}
                    >
                      删除该边
                    </NButton>
                  )}
                  <NButton onClick={() => (state.drawer = false)}>取消</NButton>
                </div>
              )
            }}
          >
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                {(['from', 'to'] as const).map((side) => (
                  <div
                    key={side}
                    style={{
                      ...t.cardStyle,
                      padding: '10px 12px',
                      display: 'flex',
                      flexDirection: 'column',
                      gap: '8px'
                    }}
                  >
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                      <span style={{ fontWeight: 600, fontSize: '12px', color: t.text }}>
                        {side === 'from' ? '上游表' : '下游表'}
                      </span>
                      <NRadioGroup
                        size='tiny'
                        value={state.ds[side].mode}
                        onUpdateValue={(v: string) => this.pickSide(side, v)}
                      >
                        <NRadioButton value='om'>OpenMetadata</NRadioButton>
                        <NRadioButton value='ds'>数据源</NRadioButton>
                      </NRadioGroup>
                      <span style={{ flex: 1 }} />
                      {state.missing[side] && (
                        <span style={{ fontSize: '11px', color: t.tone('warning').color }}>
                          尚未被 OM 采集 · 保存时自动登记
                        </span>
                      )}
                    </div>
                    <NSelect
                      value={side === 'from' ? state.form.fromFqn : state.form.toFqn}
                      onUpdateValue={(v: string) => {
                        setSideFqn(side, v)
                        this.refreshSideColumns(side)
                      }}
                      options={this.assetOptions}
                      filterable
                      placeholder='从已采集资产中选择'
                      disabled={state.ds[side].mode === 'ds'}
                      style={{ width: '100%' }}
                    />
                    {state.ds[side].mode === 'ds' && (
                      <div style={{ display: 'flex', gap: '8px' }}>
                        <NSelect
                          v-model:value={state.ds[side].datasource}
                          options={state.ds.datasources.map((d: any) => ({
                            label: `${d.name} · ${d.database}`,
                            value: d.name
                          }))}
                          style={{ width: '32%' }}
                          placeholder='数据源'
                          onUpdateValue={() => {
                            state.ds[side].schemas = []
                            state.ds[side].options = []
                            this.pickSide(side, 'ds')
                          }}
                        />
                        <NSelect
                          v-model:value={state.ds[side].schema}
                          options={state.ds[side].schemas.map((v: string) => ({
                            label: v,
                            value: v
                          }))}
                          style={{ width: '22%' }}
                          placeholder='模式'
                          onUpdateValue={async () => {
                            if (side === 'from') state.form.fromFqn = ''
                            else state.form.toFqn = ''
                            await this.searchSideTables(side, '')
                          }}
                        />
                        <NSelect
                          value={side === 'from' ? state.form.fromFqn : state.form.toFqn}
                          onUpdateValue={(v: string) => {
                            setSideFqn(side, v)
                            this.refreshSideColumns(side)
                          }}
                          options={state.ds[side].options}
                          filterable
                          remote
                          placeholder='搜索表名'
                          style={{ flex: 1 }}
                          onSearch={(v: string) => this.searchSideTables(side, v)}
                        />
                      </div>
                    )}
                  </div>
                ))}
              </div>
              <div>
                <div style={{ fontSize: '12px', color: t.textSecondary, marginBottom: '4px' }}>
                  加工 SQL（写入 OM 的 lineageDetails.sqlQuery）
                </div>
                <NInput
                  v-model:value={state.form.sqlQuery}
                  type='textarea'
                  autosize={{ minRows: 2, maxRows: 6 }}
                  placeholder='insert into dws_xxx select ... from ods_xxx'
                />
              </div>
              <div>
                <div
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    marginBottom: '6px'
                  }}
                >
                  <span style={{ fontSize: '12px', color: t.textSecondary }}>
                    列级映射（上游列 → 下游列，用于行级按值反查）
                  </span>
                  <span style={{ flex: 1 }} />
                  <NButton size='tiny' onClick={() => state.form.columns.push({ from: '', to: '' })}>
                    添加一行
                  </NButton>
                </div>
                {state.form.columns.length === 0 && (
                  <div style={{ fontSize: '12px', color: t.textMuted }}>
                    未配置列级映射时，该边只能展示表级血缘
                  </div>
                )}
                {state.form.columns.map((item: any, index: number) => (
                  <div
                    key={`col-${index}`}
                    style={{ display: 'flex', gap: '8px', marginBottom: '8px', alignItems: 'center' }}
                  >
                    <NSelect
                      v-model:value={item.from}
                      options={columnOptions(state.fromColumns)}
                      filterable
                      placeholder='上游列'
                      style={{ width: '220px' }}
                    />
                    <NIcon size={14} color={t.textMuted}>
                      <BranchesOutlined />
                    </NIcon>
                    <NSelect
                      v-model:value={item.to}
                      options={columnOptions(state.toColumns)}
                      filterable
                      placeholder='下游列'
                      style={{ width: '220px' }}
                    />
                    <NInput
                      v-model:value={item.function}
                      placeholder='计算（如 avg）'
                      size='small'
                      style={{ width: '120px' }}
                    />
                    <NButton
                      size='tiny'
                      quaternary
                      type='error'
                      onClick={() => state.form.columns.splice(index, 1)}
                    >
                      {{
                        icon: () => h(NIcon, { size: 12 }, () => h(DeleteOutlined))
                      }}
                    </NButton>
                  </div>
                ))}
              </div>
              <NInput
                v-model:value={state.form.description}
                placeholder='变更说明（可选）'
              />
              <div style={{ fontSize: '12px', color: t.textSecondary }}>
                保存会覆盖该边上已有的列级映射；血缘事实仍只存 OpenMetadata，本服务不建副本。
              </div>
            </div>
          </NDrawerContent>
        </NDrawer>
      </div>
    )
  }
})

export default Lineage
