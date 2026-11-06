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

import { defineComponent, onMounted, PropType, ref, watch } from 'vue'
import {
  NButton,
  NSelect,
  NSpace,
  NText,
  NUpload,
  NUploadDragger,
  useMessage
} from 'naive-ui'
import type { UploadFileInfo } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import {
  listUdpCodecs,
  uploadUdpCodec
} from '@/service/modules/seatunnel'
import { upsertJsonParam } from './st-param-help'
import styles from './seatunnel-compute.module.scss'

type CodecItem = {
  name: string
  fileName: string
  path?: string
  kind: string
  size?: number
}

const props = {
  /** JSON text getter/setter target object */
  model: {
    type: Object as PropType<Record<string, any>>,
    required: true
  },
  /** Field on model that holds params JSON string */
  paramsField: {
    type: String,
    default: 'connectorParams'
  },
  readonly: {
    type: Boolean,
    default: false
  }
}

const PARSE_MODE_OPTIONS = [
  { label: 'none（文本/JSON）', value: 'none' },
  { label: 'js（UDF 脚本）', value: 'js' },
  { label: 'offset_table（偏移表）', value: 'offset_table' },
  { label: 'string', value: 'string' },
  { label: 'bytes', value: 'bytes' },
  { label: 'custom（Java SPI）', value: 'custom' }
]

function readParams(model: Record<string, any>, field: string): Record<string, any> {
  try {
    const raw = model[field]
    return raw && String(raw).trim() ? JSON.parse(String(raw)) : {}
  } catch {
    return {}
  }
}

const StUdpUdfPanel = defineComponent({
  name: 'StUdpUdfPanel',
  props,
  setup(props) {
    const { t } = useI18n()
    const message = useMessage()
    const codecs = ref<CodecItem[]>([])
    const loading = ref(false)
    const parseMode = ref('none')
    const udfName = ref<string | null>(null)

    const syncFromModel = () => {
      const p = readParams(props.model, props.paramsField)
      parseMode.value = p.parseMode || 'none'
      udfName.value = p.udfName || null
    }

    const writeParam = (key: string, value: any) => {
      const next = upsertJsonParam(
        String(props.model[props.paramsField] || '{}'),
        key,
        value
      )
      props.model[props.paramsField] = next
    }

    const refreshList = async () => {
      loading.value = true
      try {
        const res: any = await listUdpCodecs()
        codecs.value = Array.isArray(res) ? res : res?.data || []
      } catch (e: any) {
        codecs.value = []
        message.warning(
          e?.message || t('project.node.st_udp_udf_list_fail')
        )
      } finally {
        loading.value = false
      }
    }

    const onParseModeChange = (v: string) => {
      parseMode.value = v
      writeParam('parseMode', v)
      if (v !== 'js' && v !== 'offset_table') {
        udfName.value = null
        writeParam('udfName', undefined)
        // clean path fields left from previous selection
        const cleaned = readParams(props.model, props.paramsField)
        delete cleaned.udfName
        delete cleaned.scriptPath
        delete cleaned.offsetTablePath
        cleaned.parseMode = v
        props.model[props.paramsField] = JSON.stringify(cleaned, null, 2)
      }
    }

    const onUdfSelect = (name: string | null) => {
      udfName.value = name
      if (!name) {
        writeParam('udfName', undefined)
        return
      }
      writeParam('udfName', name)
      const item = codecs.value.find((c) => c.name === name)
      if (item?.kind === 'js' && parseMode.value !== 'js') {
        onParseModeChange('js')
        writeParam('udfName', name)
      } else if (item?.kind === 'offset_table' && parseMode.value !== 'offset_table') {
        onParseModeChange('offset_table')
        writeParam('udfName', name)
      }
    }

    const filteredOptions = () => {
      const mode = parseMode.value
      return codecs.value
        .filter((c) => {
          if (mode === 'js') return c.kind === 'js'
          if (mode === 'offset_table') return c.kind === 'offset_table'
          return true
        })
        .map((c) => ({
          label: `${c.name} (${c.fileName})`,
          value: c.name
        }))
    }

    const customRequest = async ({
      file,
      onFinish,
      onError
    }: {
      file: UploadFileInfo
      onFinish: () => void
      onError: () => void
    }) => {
      const raw = file.file as File | null
      const name = file.name || ''
      if (!raw || !/\.(js|json)$/i.test(name)) {
        message.error(t('project.node.st_udp_udf_type_tip'))
        onError()
        return
      }
      try {
        const base = name.replace(/\.(js|json)$/i, '')
        const saved: any = await uploadUdpCodec(raw, base)
        const data = saved?.data || saved
        message.success(t('project.node.st_udp_udf_upload_ok'))
        await refreshList()
        if (data?.kind === 'js') {
          onParseModeChange('js')
        } else if (data?.kind === 'offset_table') {
          onParseModeChange('offset_table')
        }
        if (data?.name) {
          onUdfSelect(data.name)
        }
        onFinish()
      } catch (e: any) {
        message.error(e?.message || t('project.node.st_udp_udf_upload_fail'))
        onError()
      }
    }

    onMounted(() => {
      syncFromModel()
      refreshList()
    })

    watch(
      () => props.model[props.paramsField],
      () => syncFromModel()
    )

    return () => (
      <div class={styles.udpUdfPanel}>
        <div class={styles.paramSplitTitle}>
          {t('project.node.st_udp_udf_title')}
        </div>
        <NText depth={3} style='font-size: 12px; display:block; margin-bottom: 8px;'>
          {t('project.node.st_udp_udf_hint')}
        </NText>
        <NSpace vertical size={10}>
          <NSelect
            value={parseMode.value}
            options={PARSE_MODE_OPTIONS}
            disabled={props.readonly}
            onUpdateValue={onParseModeChange}
          />
          {(parseMode.value === 'js' || parseMode.value === 'offset_table') && (
            <>
              <NSelect
                value={udfName.value}
                options={filteredOptions()}
                clearable
                filterable
                loading={loading.value}
                disabled={props.readonly}
                placeholder={t('project.node.st_udp_udf_select')}
                onUpdateValue={onUdfSelect}
              />
              {!props.readonly && (
                <NUpload
                  accept='.js,.json'
                  showFileList={false}
                  customRequest={customRequest as any}
                >
                  <NUploadDragger style='padding: 12px;'>
                    <NButton size='small' type='primary'>
                      {t('project.node.st_udp_udf_upload')}
                    </NButton>
                    <div style='margin-top: 6px; font-size: 12px; color: #888;'>
                      {t('project.node.st_udp_udf_type_tip')}
                    </div>
                  </NUploadDragger>
                </NUpload>
              )}
              <NButton size='tiny' quaternary onClick={refreshList}>
                {t('project.node.st_udp_udf_refresh')}
              </NButton>
            </>
          )}
        </NSpace>
      </div>
    )
  }
})

export default StUdpUdfPanel

/** Show UDF panel for UDP Source only (codec is receive/parse). */
export function isUdpPlugin(pluginName?: string, taskTypeOrKind?: string): boolean {
  const plugin = String(pluginName || '').toUpperCase()
  const kind = String(taskTypeOrKind || '').toUpperCase()
  if (kind.includes('SINK')) return false
  if (plugin === 'UDP') return true
  return kind === 'UDP_SOURCE'
}
