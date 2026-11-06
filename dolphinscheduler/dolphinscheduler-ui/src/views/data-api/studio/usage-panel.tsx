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

import { computed, defineComponent, PropType, reactive, watch } from 'vue'
import {
  NAlert,
  NButton,
  NCode,
  NRadioButton,
  NRadioGroup,
  NSpace,
  NTable,
  NTag,
  useMessage
} from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user/user'
import SectionCard from './section-card'
import {
  apiPathOf,
  gatewayUrlOf,
  type GroupInfo,
  type ResourceDetail
} from '@/service/modules/data-api/magic'
import { listDataApiAcl } from '@/service/modules/data-api'
import {
  ApiOutlined,
  AppstoreOutlined,
  CodeOutlined,
  FileTextOutlined,
  FunctionOutlined,
  InfoCircleOutlined,
  LineChartOutlined,
  LoginOutlined,
  NodeExpandOutlined,
  ProfileOutlined,
  ReadOutlined,
  SafetyCertificateOutlined,
  TeamOutlined,
  WarningOutlined
} from '@vicons/antd'
import styles from './index.module.scss'

type Row = Record<string, any>

const props = {
  detail: {
    type: Object as PropType<ResourceDetail>,
    required: true as const
  },
  groups: { type: Array as PropType<GroupInfo[]>, default: () => [] },
  revision: { type: Number, default: 0 },
  maxRows: { type: Number, default: 0 },
  folder: { type: String as PropType<'api' | 'function'>, default: 'api' },
  lastResponse: { type: String as PropType<string>, default: '' }
}

const sampleFields = (raw: string): Row[] => {
  if (!raw) return []
  let parsed: any
  try {
    parsed = JSON.parse(raw)
  } catch (error) {
    return []
  }
  const source = Array.isArray(parsed)
    ? parsed[0]
    : parsed && typeof parsed === 'object'
    ? parsed
    : null
  if (!source) return []
  return Object.keys(source).map((key) => ({
    name: key,
    dataType: Array.isArray(source[key])
      ? 'Array'
      : source[key] === null
      ? 'null'
      : typeof source[key],
    description: ''
  }))
}

const definitionFields = (definition: any, depth = 0): Row[] => {
  if (!definition || !definition.children) return []
  return definition.children.map((child: any) => ({
    name: `${'　'.repeat(depth)}${child.name || '-'}`,
    dataType: child.dataType || child.type || '',
    required: !!child.required,
    description: child.description || ''
  }))
}

export default defineComponent({
  name: 'UsagePanel',
  props,
  setup(props) {
    const { t } = useI18n()
    const message = useMessage()
    const userStore = useUserStore()
    const state = reactive({
      aclItems: [] as Row[],
      aclMode: '',
      aclLoaded: false,
      language: 'curl'
    })

    watch(
      () => props.revision,
      async () => {
        const path = apiPathOf(props.detail, props.groups)
        state.aclLoaded = false
        try {
          const result = await listDataApiAcl(
            { sessionId: userStore.getSessionId || '' },
            path
          )
          if (result && !result.error && typeof result === 'object') {
            state.aclItems = (result.items || []) as Row[]
            state.aclMode = result.aclMode || ''
          } else {
            state.aclItems = []
            state.aclMode = ''
          }
        } catch (error) {
          state.aclItems = []
          state.aclMode = ''
        }
        state.aclLoaded = true
      },
      { immediate: true }
    )

    const method = computed(() => (props.detail.method || 'GET').toUpperCase())
    const relativePath = computed(() => apiPathOf(props.detail, props.groups))
    const fullUrl = computed(
      () =>
        `${window.location.origin}${gatewayUrlOf(props.detail, props.groups)}`
    )
    const queryUrl = computed(() => {
      const params = (props.detail.parameters || [])
        .filter(
          (param) =>
            param.name &&
            param.defaultValue != null &&
            param.defaultValue !== ''
        )
        .map(
          (param) =>
            `${param.name}=${encodeURIComponent(String(param.defaultValue))}`
        )
      return params.length
        ? `${fullUrl.value}?${params.join('&')}`
        : fullUrl.value
    })
    const hasBody = computed(() =>
      ['POST', 'PUT', 'PATCH'].includes(method.value)
    )
    const bodyText = computed(
      () => props.detail.requestBody || '{\n  "key": "value"\n}'
    )

    const examples = computed<Record<string, string>>(() => {
      const token = '<ACCESS_TOKEN>'
      const curl = [
        `curl -X ${method.value} "${queryUrl.value}" \\`,
        `  -H "token: ${token}"${
          hasBody.value
            ? ` \\
  -H "Content-Type: application/json" \\
  --data-raw '${bodyText.value.replace(/\n/g, '')}'`
            : ''
        }`
      ].join('\n')
      const bodyCompact = bodyText.value
        .replace(/\n/g, ' ')
        .replace(/"/g, '\\"')
      const js = [
        `const response = await fetch(${JSON.stringify(queryUrl.value)}, {`,
        `  method: ${JSON.stringify(method.value)},`,
        '  headers: {',
        `    token: ${JSON.stringify(token)},`,
        hasBody.value ? '    "Content-Type": "application/json"' : '',
        '  },',
        hasBody.value ? '  body: JSON.stringify({ "key": "value" })' : '',
        '}).then((res) => res.json())',
        'console.log(response)'
      ]
        .filter((line) => line !== '')
        .join('\n')
      const python = [
        'import requests',
        '',
        `url = ${JSON.stringify(queryUrl.value)}`,
        `headers = {'token': '${token}'}${
          hasBody.value ? '\nbody = {"key": "value"}' : ''
        }`,
        '',
        `response = requests.request(${JSON.stringify(
          method.value
        )}, url, headers=headers${
          hasBody.value ? ', json=body' : ''
        }, timeout=30)`,
        'print(response.status_code, response.json())'
      ].join('\n')
      const javaBody = hasBody.value
        ? `RequestBody.create("${bodyCompact}", MediaType.parse("application/json"))`
        : 'null'
      const java = [
        'OkHttpClient client = new OkHttpClient();',
        'Request request = new Request.Builder()',
        `    .url(${JSON.stringify(queryUrl.value)})`,
        `    .method(${JSON.stringify(method.value)}, ${javaBody})`,
        `    .addHeader("token", "${token}")`,
        '    .build();',
        'try (Response response = client.newCall(request).execute()) {',
        '    System.out.println(response.code() + " " + response.body().string());',
        '}'
      ].join('\n')
      return { curl, js, python, java }
    })

    const responseSample = computed(() => {
      const raw = props.detail.responseBody
      if (raw && raw.trim()) return { text: raw, source: 'saved' }
      if (props.lastResponse && props.lastResponse.trim()) {
        try {
          return {
            text: JSON.stringify(JSON.parse(props.lastResponse), null, 2),
            source: 'test'
          }
        } catch (error) {
          return { text: props.lastResponse, source: 'test' }
        }
      }
      return { text: '', source: '' }
    })

    const responseFields = computed<Row[]>(() => {
      const fromDefinition = definitionFields(
        props.detail.responseBodyDefinition
      )
      if (fromDefinition.length > 0) return fromDefinition
      return sampleFields(responseSample.value.text)
    })

    const copy = async (text: string) => {
      try {
        await navigator.clipboard.writeText(text)
        message.success(t('data_api.studio.copied'))
      } catch (error) {
        message.error(String(error))
      }
    }

    return {
      t,
      state,
      method,
      relativePath,
      fullUrl,
      hasBody,
      bodyText,
      examples,
      responseSample,
      responseFields,
      copy
    }
  },
  render() {
    const {
      t,
      state,
      method,
      relativePath,
      fullUrl,
      hasBody,
      bodyText,
      examples,
      responseSample,
      responseFields,
      copy,
      detail
    } = this
    const parameterRows = (detail.parameters || []).filter(
      (param) => param.name
    )
    const pathRows = (detail.paths || []).filter((param) => param.name)
    const headerRows = (detail.headers || []).filter((param) => param.name)
    const optionRows = (detail.options || []).filter((param) => param.name)
    const codeRows = [
      { code: '200', desc: t('data_api.studio.usage_code_200') },
      { code: '401', desc: t('data_api.studio.usage_code_401') },
      { code: '403', desc: t('data_api.studio.usage_code_403') },
      { code: '413', desc: t('data_api.studio.usage_code_413') },
      { code: '429', desc: t('data_api.studio.usage_code_429') },
      { code: '500', desc: t('data_api.studio.usage_code_500') }
    ]
    const tableIcon = (title: string) =>
      title === t('data_api.studio.usage_path_table')
        ? NodeExpandOutlined
        : title === t('data_api.studio.usage_header_table')
        ? LoginOutlined
        : ProfileOutlined
    const renderParamTable = (
      title: string,
      rows: any[],
      withValue: boolean
    ) => (
      <SectionCard title={title} icon={tableIcon(title)}>
        {rows.length === 0 ? (
          <span class={styles['section-hint']}>
            {t('data_api.studio.usage_table_empty')}
          </span>
        ) : (
          <NTable size='small' singleLine class={styles['readonly-table']}>
            <thead>
              <tr>
                <th>{t('data_api.studio.col_name')}</th>
                <th>{t('data_api.studio.col_type')}</th>
                <th>{t('data_api.studio.col_required')}</th>
                <th>{t('data_api.studio.col_default')}</th>
                {withValue && <th>{t('data_api.studio.col_value')}</th>}
                <th>{t('data_api.studio.col_desc')}</th>
              </tr>
            </thead>
            <tbody>
              {rows.map((row, index) => (
                <tr key={index}>
                  <td>{row.name}</td>
                  <td>{row.dataType || row.type || '-'}</td>
                  <td>
                    {row.required ? (
                      <NTag size='small' type='error'>
                        {t('data_api.studio.usage_required_yes')}
                      </NTag>
                    ) : (
                      t('data_api.studio.usage_required_no')
                    )}
                  </td>
                  <td>
                    {row.defaultValue == null || row.defaultValue === ''
                      ? '-'
                      : String(row.defaultValue)}
                  </td>
                  {withValue && (
                    <td>
                      {row.value == null || row.value === ''
                        ? '-'
                        : String(row.value)}
                    </td>
                  )}
                  <td>{row.description || '-'}</td>
                </tr>
              ))}
            </tbody>
          </NTable>
        )}
      </SectionCard>
    )
    if (this.folder === 'function') {
      return (
        <NSpace vertical size='large'>
          <SectionCard
            title={t('data_api.studio.usage_function_title')}
            icon={FunctionOutlined}
          >
            <div>{t('data_api.studio.usage_function_body')}</div>
            <div class={styles['path-line']} style={{ marginTop: '8px' }}>
              <span>{relativePath}</span>
              <NTag size='tiny' type='info'>
                {detail.returnType ||
                  t('data_api.studio.usage_function_return')}
              </NTag>
            </div>
          </SectionCard>
          {renderParamTable(
            t('data_api.studio.usage_param_table'),
            (detail.parameters || []) as any[],
            false
          )}
        </NSpace>
      )
    }
    return (
      <NSpace vertical size='large'>
        <SectionCard
          title={t('data_api.studio.usage_endpoint')}
          icon={ApiOutlined}
        >
          <div class={styles['path-line']}>
            <NTag size='small' type='info'>
              {method}
            </NTag>
            <span>{fullUrl}</span>
            <NButton size='tiny' quaternary onClick={() => copy(fullUrl)}>
              {t('data_api.studio.copy')}
            </NButton>
          </div>
          <div class={styles['section-hint']} style={{ marginTop: '8px' }}>
            {t('data_api.studio.usage_gateway_path')}: {relativePath}
            {detail.description ? ` · ${detail.description}` : ''}
          </div>
        </SectionCard>
        <SectionCard
          title={t('data_api.studio.usage_auth')}
          icon={SafetyCertificateOutlined}
        >
          <div>{t('data_api.studio.usage_auth_note')}</div>
          <div class={styles['section-hint']} style={{ marginTop: '6px' }}>
            {t('data_api.studio.usage_app_note')}
          </div>
        </SectionCard>
        {renderParamTable(
          t('data_api.studio.usage_param_table'),
          parameterRows as any[],
          false
        )}
        {pathRows.length > 0 &&
          renderParamTable(
            t('data_api.studio.usage_path_table'),
            pathRows as any[],
            false
          )}
        {headerRows.length > 0 &&
          renderParamTable(
            t('data_api.studio.usage_header_table'),
            headerRows as any[],
            true
          )}
        {optionRows.length > 0 && (
          <SectionCard
            title={t('data_api.studio.usage_option_table')}
            icon={AppstoreOutlined}
          >
            <NTable size='small' singleLine class={styles['readonly-table']}>
              <thead>
                <tr>
                  <th>{t('data_api.studio.col_name')}</th>
                  <th>{t('data_api.studio.col_value')}</th>
                  <th>{t('data_api.studio.col_desc')}</th>
                </tr>
              </thead>
              <tbody>
                {optionRows.map((row, index) => (
                  <tr key={index}>
                    <td>{row.name}</td>
                    <td>{row.value == null ? '-' : String(row.value)}</td>
                    <td>{row.description || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </NTable>
          </SectionCard>
        )}
        {hasBody && (
          <SectionCard
            title={t('data_api.studio.usage_request_body')}
            icon={FileTextOutlined}
          >
            <div class={styles['response-box']}>
              <NCode word-wrap code={bodyText} />
            </div>
          </SectionCard>
        )}
        <SectionCard
          title={t('data_api.studio.usage_examples')}
          icon={CodeOutlined}
        >
          <NSpace vertical size='small'>
            <NRadioGroup size='small' v-model={[state.language, 'value']}>
              <NRadioButton value='curl'>curl</NRadioButton>
              <NRadioButton value='js'>JavaScript</NRadioButton>
              <NRadioButton value='python'>Python</NRadioButton>
              <NRadioButton value='java'>Java (OkHttp)</NRadioButton>
            </NRadioGroup>
            <div class={styles['response-box']}>
              <NCode
                word-wrap
                code={examples[state.language] || examples.curl}
              />
            </div>
            <NButton
              size='tiny'
              onClick={() => copy(examples[state.language] || examples.curl)}
            >
              {t('data_api.studio.copy')}
            </NButton>
            <div class={styles['section-hint']}>
              {t('data_api.studio.usage_example_note')}
            </div>
          </NSpace>
        </SectionCard>
        <SectionCard
          title={t('data_api.studio.usage_response_sample')}
          icon={ReadOutlined}
        >
          {responseSample.text ? (
            <>
              <NTag size='small'>
                {responseSample.source === 'saved'
                  ? t('data_api.studio.usage_response_source_saved')
                  : t('data_api.studio.usage_response_source_test')}
              </NTag>
              <div class={styles['response-box']} style={{ marginTop: '8px' }}>
                <NCode word-wrap code={responseSample.text} />
              </div>
            </>
          ) : (
            <NAlert type='warning'>
              {t('data_api.studio.usage_response_sample_empty')}
            </NAlert>
          )}
        </SectionCard>
        {responseFields.length > 0 && (
          <SectionCard
            title={t('data_api.studio.usage_response_fields')}
            icon={LineChartOutlined}
          >
            <NTable size='small' singleLine class={styles['readonly-table']}>
              <thead>
                <tr>
                  <th>{t('data_api.studio.col_name')}</th>
                  <th>{t('data_api.studio.col_type')}</th>
                  <th>{t('data_api.studio.col_desc')}</th>
                </tr>
              </thead>
              <tbody>
                {responseFields.map((row, index) => (
                  <tr key={index}>
                    <td>{row.name}</td>
                    <td>{row.dataType || '-'}</td>
                    <td>{row.description || '-'}</td>
                  </tr>
                ))}
              </tbody>
            </NTable>
          </SectionCard>
        )}
        <SectionCard
          title={t('data_api.studio.usage_acl_title')}
          icon={TeamOutlined}
        >
          {!state.aclLoaded ? (
            <span class={styles['section-hint']}>
              {t('data_api.studio.loading')}
            </span>
          ) : (
            <>
              <div class={styles['section-hint']}>
                {t('data_api.studio.usage_acl_mode', {
                  mode: state.aclMode || 'optional'
                })}
              </div>
              {state.aclItems.length === 0 ? (
                <div style={{ marginTop: '6px' }}>
                  <span class={styles['section-hint']}>
                    {t('data_api.studio.usage_acl_none')}
                  </span>
                </div>
              ) : (
                <NTable
                  size='small'
                  singleLine
                  class={styles['readonly-table']}
                >
                  <thead>
                    <tr>
                      <th>{t('data_api.studio.usage_acl_col_subject')}</th>
                      <th>{t('data_api.studio.usage_acl_col_effect')}</th>
                      <th>{t('data_api.studio.col_desc')}</th>
                    </tr>
                  </thead>
                  <tbody>
                    {state.aclItems.map((row) => (
                      <tr key={row.id}>
                        <td>
                          {row.subjectTypeName || row.subjectType} /{' '}
                          {row.subjectId}
                        </td>
                        <td>
                          <NTag
                            size='small'
                            type={row.effect === 1 ? 'success' : 'error'}
                          >
                            {row.effectName ||
                              (row.effect === 1
                                ? t('data_api.studio.usage_acl_allow')
                                : t('data_api.studio.usage_acl_deny'))}
                          </NTag>
                        </td>
                        <td>{row.remark || '-'}</td>
                      </tr>
                    ))}
                  </tbody>
                </NTable>
              )}
            </>
          )}
        </SectionCard>
        <SectionCard
          title={t('data_api.studio.usage_codes_title')}
          icon={WarningOutlined}
        >
          <NTable size='small' singleLine class={styles['readonly-table']}>
            <thead>
              <tr>
                <th style={{ width: '80px' }}>HTTP</th>
                <th>{t('data_api.studio.col_desc')}</th>
              </tr>
            </thead>
            <tbody>
              {codeRows.map((row) => (
                <tr key={row.code}>
                  <td>{row.code}</td>
                  <td>{row.desc}</td>
                </tr>
              ))}
            </tbody>
          </NTable>
        </SectionCard>
        <SectionCard
          title={t('data_api.studio.usage_limits_title')}
          icon={InfoCircleOutlined}
        >
          <ul class={styles['section-hint']}>
            <li>
              {t('data_api.studio.usage_limit_rows', {
                rows: this.maxRows || '-'
              })}
            </li>
            <li>{t('data_api.studio.usage_limit_readonly')}</li>
            <li>{t('data_api.studio.usage_limit_rate')}</li>
            <li>{t('data_api.studio.usage_limit_timeout')}</li>
          </ul>
        </SectionCard>
      </NSpace>
    )
  }
})
