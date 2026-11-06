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

import { defineComponent, PropType } from 'vue'
import {
  NForm,
  NFormItem,
  NGrid,
  NGi,
  NInput,
  NSelect,
  NSpace,
  NTag
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import MonacoEditor from '@/components/monaco-editor'
import SectionCard from './section-card'
import {
  AppstoreOutlined,
  FileTextOutlined,
  NodeExpandOutlined,
  ProfileOutlined,
  ReadOutlined,
  SafetyCertificateOutlined
} from '@vicons/antd'
import styles from './index.module.scss'
import type {
  GroupInfo,
  ResourceDetail
} from '@/service/modules/data-api/magic'
import DefinitionTable, { type DefColumn } from './definition-table'

const DATA_TYPES = [
  'String',
  'Integer',
  'Long',
  'Boolean',
  'Number',
  'Date',
  'Object',
  'Array'
]

const props = {
  detail: {
    type: Object as PropType<ResourceDetail>,
    required: true as const
  },
  groups: { type: Array as PropType<GroupInfo[]>, default: () => [] },
  optionMeta: { type: Array as PropType<string[][]>, default: () => [] },
  folder: { type: String as PropType<'api' | 'function'>, default: 'api' },
  methods: {
    type: Array as PropType<{ label: string; value: string }[]>,
    default: () => []
  },
  locked: { type: Boolean, default: false }
}

export default defineComponent({
  name: 'BasicPanel',
  props,
  setup() {
    const { t } = useI18n()
    return { t }
  },
  render() {
    const { detail, groups, optionMeta, folder, methods, locked, t } = this
    const isApi = folder === 'api'
    const disabled = locked
    const dataTypeOptions = DATA_TYPES.map((item) => ({
      label: item,
      value: item
    }))
    const parameterColumns: DefColumn[] = [
      { key: 'name', label: t('data_api.studio.col_name'), width: '22%' },
      {
        key: 'dataType',
        label: t('data_api.studio.col_type'),
        kind: 'select',
        width: '15%',
        options: dataTypeOptions
      },
      {
        key: 'required',
        label: t('data_api.studio.col_required'),
        kind: 'switch',
        width: '9%'
      },
      {
        key: 'defaultValue',
        label: t('data_api.studio.col_default'),
        width: '18%'
      },
      { key: 'description', label: t('data_api.studio.col_desc') }
    ]
    const headerColumns: DefColumn[] = [
      { key: 'name', label: t('data_api.studio.col_name'), width: '28%' },
      { key: 'value', label: t('data_api.studio.col_value'), width: '28%' },
      { key: 'description', label: t('data_api.studio.col_desc') }
    ]
    const optionColumns: DefColumn[] = [
      {
        key: 'name',
        label: t('data_api.studio.col_name'),
        kind: 'select',
        width: '40%',
        options: optionMeta.map((row) => ({ label: row[1], value: row[0] }))
      },
      { key: 'value', label: t('data_api.studio.col_value') },
      { key: 'description', label: t('data_api.studio.col_desc'), width: '30%' }
    ]
    return (
      <NSpace vertical size='large'>
        {locked && (
          <NTag type='warning' size='small'>
            {t('data_api.studio.locked_readonly')}
          </NTag>
        )}
        <SectionCard
          title={t('data_api.studio.section_basic')}
          icon={FileTextOutlined}
        >
          <NForm labelPlacement='top'>
            <NGrid cols={4} xGap={12}>
              <NGi>
                <NFormItem label={t('data_api.studio.field_name')}>
                  <NInput
                    v-model={[detail.name, 'value']}
                    disabled={disabled}
                    placeholder={t('data_api.studio.field_name_ph')}
                  />
                </NFormItem>
              </NGi>
              <NGi>
                <NFormItem label={t('data_api.studio.field_path')}>
                  <NInput
                    v-model={[detail.path, 'value']}
                    disabled={disabled}
                    placeholder='/articles'
                  />
                </NFormItem>
              </NGi>
              {isApi && (
                <NGi>
                  <NFormItem label={t('data_api.studio.field_method')}>
                    <NSelect
                      v-model={[detail.method, 'value']}
                      options={methods}
                      disabled={disabled}
                    />
                  </NFormItem>
                </NGi>
              )}
              <NGi>
                <NFormItem label={t('data_api.studio.field_parent')}>
                  <NSelect
                    v-model={[detail.groupId, 'value']}
                    options={[
                      {
                        label: t('data_api.studio.group_root'),
                        value: '0'
                      },
                      ...groups.map((group) => ({
                        label: group.label,
                        value: group.id
                      }))
                    ]}
                    disabled={disabled}
                    filterable
                  />
                </NFormItem>
              </NGi>
              {!isApi && (
                <NGi>
                  <NFormItem label={t('data_api.studio.col_return_type')}>
                    <NInput
                      v-model={[detail.returnType, 'value']}
                      disabled={disabled}
                      placeholder='Object'
                    />
                  </NFormItem>
                </NGi>
              )}
              <NGi span={isApi ? 4 : 3}>
                <NFormItem label={t('data_api.studio.field_desc')}>
                  <NInput
                    type='textarea'
                    autosize={{ minRows: 2, maxRows: 6 }}
                    v-model={[detail.description, 'value']}
                    disabled={disabled}
                    placeholder={t('data_api.studio.field_desc_ph')}
                  />
                </NFormItem>
              </NGi>
            </NGrid>
          </NForm>
          <div class={styles['meta-list']}>
            {[
              `${t('data_api.studio.meta_id')}: ${detail.id || '-'}`,
              `${t('data_api.studio.meta_create_by')}: ${
                detail.createBy || '-'
              }`,
              `${t('data_api.studio.meta_update_by')}: ${
                detail.updateBy || '-'
              }`,
              `${t('data_api.studio.meta_update_time')}: ${
                detail.updateTime
                  ? new Date(detail.updateTime).toLocaleString()
                  : '-'
              }`
            ].map((item) => (
              <span key={item} class={styles['meta-item']}>
                {item}
              </span>
            ))}
          </div>
        </SectionCard>
        <SectionCard
          title={t('data_api.studio.section_parameters')}
          icon={ProfileOutlined}
        >
          <DefinitionTable
            columns={parameterColumns}
            v-model={[detail.parameters, 'value']}
            disabled={disabled}
            addLabel={t('data_api.studio.add_row')}
            emptyText={t('data_api.studio.no_rows')}
          />
        </SectionCard>
        {isApi && (
          <SectionCard
            title={t('data_api.studio.section_paths')}
            icon={NodeExpandOutlined}
          >
            <DefinitionTable
              columns={parameterColumns}
              v-model={[detail.paths, 'value']}
              disabled={disabled}
              addLabel={t('data_api.studio.add_row')}
              emptyText={t('data_api.studio.no_rows')}
            />
          </SectionCard>
        )}
        {isApi && (
          <SectionCard
            title={t('data_api.studio.section_headers')}
            icon={SafetyCertificateOutlined}
          >
            <DefinitionTable
              columns={headerColumns}
              v-model={[detail.headers, 'value']}
              disabled={disabled}
              addLabel={t('data_api.studio.add_row')}
              emptyText={t('data_api.studio.no_rows')}
            />
          </SectionCard>
        )}
        {isApi && (
          <SectionCard
            title={t('data_api.studio.section_options')}
            icon={AppstoreOutlined}
          >
            <DefinitionTable
              columns={optionColumns}
              v-model={[detail.options, 'value']}
              disabled={disabled}
              addLabel={t('data_api.studio.add_row')}
              emptyText={t('data_api.studio.no_option_rows')}
            />
          </SectionCard>
        )}
        {isApi && (
          <SectionCard
            title={t('data_api.studio.section_response_sample')}
            icon={ReadOutlined}
          >
            <MonacoEditor
              v-model={[detail.responseBody, 'value']}
              height='200px'
              options={{
                readOnly: disabled,
                language: 'json',
                minimap: { enabled: false },
                lineNumbers: 'on',
                scrollBeyondLastLine: false
              }}
            />
            <div class={styles['section-hint']}>
              {t('data_api.studio.section_response_sample_hint')}
            </div>
          </SectionCard>
        )}
      </NSpace>
    )
  }
})
