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

import { defineComponent } from 'vue'
import { NAlert, NCode, NSpace } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import TokenManage from '@/views/security/token-manage'

const DataApiToken = defineComponent({
  name: 'DataApiToken',
  setup() {
    const { t } = useI18n()
    return { t }
  },
  render() {
    const { t } = this
    const sample = `curl -H "token: <ACCESS_TOKEN>" \\
  "http://host:12346/data-api/gateway/smoke/select1"`
    return (
      <NSpace vertical>
        <NAlert type='warning' title={t('data_api.token.tip_title')}>
          <div>{t('data_api.token.tip_body')}</div>
          <div style={{ marginTop: '8px' }}>
            <NCode code={sample} language='bash' word-wrap />
          </div>
        </NAlert>
        <TokenManage />
      </NSpace>
    )
  }
})

export default DataApiToken
