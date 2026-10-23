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

import { computed, defineComponent, PropType, ref } from 'vue'
import {
  NButton,
  NCollapse,
  NCollapseItem,
  NDataTable,
  NSpace,
  NTag,
  NText
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useTaskNodeStore } from '@/store/project/task-node'
import {
  collectUpstreamAvailableParams,
  getConnectorHelp,
  upsertJsonParam,
  type StParamOption
} from './st-param-help'
import styles from './seatunnel-compute.module.scss'

const props = {
  model: {
    type: Object as PropType<Record<string, any>>,
    required: true
  },
  /** When set, apply clicks write into this field (default connectorParams). */
  paramsField: {
    type: String,
    default: 'connectorParams'
  },
  pluginName: String,
  connectorType: String,
  /** Optional upstream nodes for editor mode (not DAG modal). */
  upstreamNodes: {
    type: Array as PropType<
      Array<{
        name: string
        pluginName?: string
        params?: Record<string, any>
        paramsText?: string
      }>
    >,
    default: undefined
  }
}

const StConnectorParamGuide = defineComponent({
  name: 'StConnectorParamGuide',
  props,
  setup(props) {
    const { t } = useI18n()
    const taskStore = useTaskNodeStore()
    const expanded = ref(['upstream', 'io', 'process'])

    const pluginName = computed(
      () => props.pluginName || props.model.pluginName || ''
    )
    const kind = computed(
      () =>
        props.connectorType ||
        props.model.connectorType ||
        props.model.kind ||
        'SOURCE'
    )

    const help = computed(() =>
      getConnectorHelp(pluginName.value, String(kind.value).toUpperCase())
    )

    const upstream = computed(() => {
      if (props.upstreamNodes?.length) {
        return props.upstreamNodes.map((n, idx) => {
          let params = n.params || {}
          if (!Object.keys(params).length && n.paramsText) {
            try {
              params = JSON.parse(n.paramsText)
            } catch {
              params = {}
            }
          }
          const out =
            params.plugin_output || params.result_table_name || n.name
          const fields: string[] = []
          const sf = params.schema?.fields
          if (sf && typeof sf === 'object') fields.push(...Object.keys(sf))
          if (Array.isArray(params.fields)) {
            params.fields.forEach((f: any) => {
              if (typeof f === 'string') fields.push(f)
            })
          }
          return {
            taskCode: idx,
            taskName: n.name,
            pluginName: n.pluginName,
            pluginOutput: out,
            fields
          }
        })
      }
      const pre =
        (props.model.preTasks as Array<number | string>) ||
        taskStore.getPreTasks ||
        []
      return collectUpstreamAvailableParams(
        taskStore.getTaskDefinitionList,
        pre
      )
    })

    const applyKey = (key: string, value: any) => {
      const field = props.paramsField
      props.model[field] = upsertJsonParam(props.model[field] || '{}', key, value)
    }

    const applyExample = () => {
      if (!help.value.example) return
      props.model[props.paramsField] = JSON.stringify(
        help.value.example,
        null,
        2
      )
    }

    const columns = [
      {
        title: t('project.node.st_param_name'),
        key: 'name',
        width: 160,
        ellipsis: { tooltip: true }
      },
      {
        title: t('project.node.st_param_type'),
        key: 'type',
        width: 90
      },
      {
        title: t('project.node.st_param_required'),
        key: 'required',
        width: 70
      },
      {
        title: t('project.node.st_param_default'),
        key: 'default',
        width: 90,
        ellipsis: { tooltip: true }
      },
      {
        title: t('project.node.st_param_desc'),
        key: 'desc',
        ellipsis: { tooltip: true }
      },
      {
        title: t('project.node.st_param_action'),
        key: 'action',
        width: 88,
        render: (row: StParamOption) => (
          <NButton
            size='tiny'
            quaternary
            onClick={() =>
              applyKey(
                row.name,
                row.default && row.default !== '-' ? row.default : ''
              )
            }
          >
            {t('project.node.st_param_insert')}
          </NButton>
        )
      }
    ]

    const renderTable = (rows: StParamOption[], maxHeight = 220) => {
      if (!rows?.length) {
        return (
          <NText depth={3}>{t('project.node.st_param_empty_section')}</NText>
        )
      }
      return (
        <NDataTable
          size='small'
          bordered
          single-line={false}
          columns={columns as any}
          data={rows.map((r) => ({
            ...r,
            // Normalize display; stream names are always String in SeaTunnel
            type: r.type || 'String'
          }))}
          max-height={maxHeight}
        />
      )
    }

    return () => (
      <div class={styles.paramGuide}>
        <div class={styles.paramGuideHeader}>
          <div class={styles.paramGuideTitle}>
            {pluginName.value} · {String(kind.value).toUpperCase()}
          </div>
          <div class={styles.paramGuideSummary}>{help.value.summary}</div>
          <ul class={styles.paramGuideTips}>
            {(help.value.tips || []).map((tip) => (
              <li>{tip}</li>
            ))}
          </ul>
          {help.value.example ? (
            <NButton size='tiny' secondary onClick={applyExample}>
              {t('project.node.st_param_fill_example')}
            </NButton>
          ) : null}
        </div>

        <NCollapse
          expandedNames={expanded.value}
          onUpdate:expandedNames={(v: string[]) => {
            expanded.value = v
          }}
        >
          <NCollapseItem
            title={t('project.node.st_param_upstream')}
            name='upstream'
          >
            {!upstream.value.length ? (
              <NText depth={3}>
                {t('project.node.st_param_upstream_empty')}
              </NText>
            ) : (
              <NSpace vertical size={8}>
                {upstream.value.map((u) => (
                  <div class={styles.upstreamBlock}>
                    <div class={styles.upstreamHead}>
                      <NText strong>
                        {u.taskName}
                        {u.pluginName ? ` (${u.pluginName})` : ''}
                      </NText>
                      {u.pluginOutput ? (
                        <NTag
                          size='small'
                          round
                          bordered
                          style={{ cursor: 'pointer' }}
                          onClick={() =>
                            applyKey('plugin_input', u.pluginOutput)
                          }
                        >
                          plugin_output: {u.pluginOutput}
                        </NTag>
                      ) : null}
                    </div>
                    <NSpace size={4} style={{ marginTop: '4px' }}>
                      {u.fields.length ? (
                        u.fields.map((f) => (
                          <NTag
                            size='small'
                            round
                            style={{ cursor: 'pointer' }}
                            title={t('project.node.st_param_insert')}
                            onClick={() => {
                              let obj: any = {}
                              try {
                                obj = JSON.parse(
                                  props.model[props.paramsField] || '{}'
                                )
                              } catch {
                                obj = {}
                              }
                              if (u.pluginOutput && !obj.plugin_input) {
                                obj.plugin_input = u.pluginOutput
                              }
                              const arr = Array.isArray(obj.fields)
                                ? [...obj.fields]
                                : []
                              if (!arr.includes(f)) arr.push(f)
                              obj.fields = arr
                              props.model[props.paramsField] = JSON.stringify(
                                obj,
                                null,
                                2
                              )
                            }}
                          >
                            {f}
                          </NTag>
                        ))
                      ) : (
                        <NText depth={3}>
                          {t('project.node.st_param_upstream_no_fields')}
                        </NText>
                      )}
                    </NSpace>
                  </div>
                ))}
              </NSpace>
            )}
          </NCollapseItem>

          <NCollapseItem title={t('project.node.st_param_io')} name='io'>
            <p class={styles.paramIoHint}>{t('project.node.st_param_io_hint')}</p>
            <div class={styles.paramIoGrid}>
              <div class={styles.paramIoCol}>
                <div class={styles.paramGuideSubtitle}>
                  {t('project.node.st_param_input')}
                </div>
                {renderTable(help.value.input, 160)}
              </div>
              <div class={styles.paramIoCol}>
                <div class={styles.paramGuideSubtitle}>
                  {t('project.node.st_param_output')}
                </div>
                {renderTable(help.value.output, 160)}
              </div>
            </div>
          </NCollapseItem>

          <NCollapseItem
            title={t('project.node.st_param_process')}
            name='process'
          >
            {renderTable(help.value.process, 280)}
          </NCollapseItem>
        </NCollapse>
      </div>
    )
  }
})

export default StConnectorParamGuide
