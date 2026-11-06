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
import { useI18n } from 'vue-i18n'
import type { IJsonItem } from '../types'

const ENTITY_TYPES = [
  { label: 'table', value: 'table' },
  { label: 'apiEndpoint', value: 'apiEndpoint' },
  { label: 'apiCollection', value: 'apiCollection' },
  { label: 'dashboard', value: 'dashboard' },
  { label: 'pipeline', value: 'pipeline' }
]

export function useOpenMetadata(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()
  return [
    {
      type: 'input',
      field: 'openMetadataUrl',
      name: t('project.node.openmetadata_url'),
      span: 24,
      props: {
        placeholder: 'http://127.0.0.1:8585'
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(_: any, value: string) {
          if (!value) {
            return new Error(t('project.node.openmetadata_url_tips'))
          }
          if (!/^https?:\/\//i.test(value)) {
            return new Error(t('project.node.openmetadata_url_tips'))
          }
        }
      }
    },
    {
      type: 'input',
      field: 'token',
      name: t('project.node.openmetadata_token'),
      span: 24,
      props: {
        type: 'password',
        placeholder: t('project.node.openmetadata_token_tips')
      }
    },
    {
      type: 'select',
      field: 'fromType',
      name: t('project.node.openmetadata_from_type'),
      span: 12,
      options: ENTITY_TYPES
    },
    {
      type: 'select',
      field: 'toType',
      name: t('project.node.openmetadata_to_type'),
      span: 12,
      options: ENTITY_TYPES
    },
    {
      type: 'input',
      field: 'fromFqn',
      name: t('project.node.openmetadata_from_fqn'),
      span: 24,
      props: {
        placeholder: 'service.database.schema.table'
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(_: any, value: string) {
          if (!value) {
            return new Error(t('project.node.openmetadata_from_fqn'))
          }
        }
      }
    },
    {
      type: 'input',
      field: 'toFqn',
      name: t('project.node.openmetadata_to_fqn'),
      span: 24,
      props: {
        placeholder: 'service.database.schema.table'
      },
      validate: {
        trigger: ['input', 'blur'],
        required: true,
        validator(_: any, value: string) {
          if (!value) {
            return new Error(t('project.node.openmetadata_to_fqn'))
          }
        }
      }
    },
    {
      type: 'textarea',
      field: 'sql',
      name: t('project.node.openmetadata_sql'),
      span: 24,
      props: {
        placeholder: t('project.node.openmetadata_sql_tips'),
        autosize: { minRows: 3, maxRows: 8 }
      }
    },
    {
      type: 'textarea',
      field: 'seatunnelConfig',
      name: t('project.node.openmetadata_seatunnel_config'),
      span: 24,
      props: {
        placeholder: t('project.node.openmetadata_seatunnel_config_tips'),
        autosize: { minRows: 3, maxRows: 10 }
      }
    },
    {
      type: 'textarea',
      field: 'columnsLineage',
      name: t('project.node.openmetadata_columns_lineage'),
      span: 24,
      props: {
        placeholder:
          '[{"fromColumns":["svc.db.schema.src.id"],"toColumn":"svc.db.schema.dst.id"}]',
        autosize: { minRows: 3, maxRows: 8 }
      }
    },
    {
      type: 'input',
      field: 'edgeDescription',
      name: t('project.node.openmetadata_edge_description'),
      span: 24,
      props: {
        placeholder: t('project.node.openmetadata_edge_description')
      }
    },
    {
      type: 'switch',
      field: 'parseSql',
      name: t('project.node.openmetadata_parse_sql'),
      span: 24
    }
  ]
}
