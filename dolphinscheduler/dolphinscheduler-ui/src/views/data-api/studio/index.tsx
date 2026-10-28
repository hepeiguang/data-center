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

import { defineComponent, onBeforeUnmount, onMounted, ref } from 'vue'
import { NAlert, NSpace } from 'naive-ui'
import { useI18n } from 'vue-i18n'
import { useUserStore } from '@/store/user/user'
import Card from '@/components/card'

declare global {
  interface Window {
    MAGIC_EDITOR_CONFIG?: Record<string, unknown>
  }
}

const DataApiStudio = defineComponent({
  name: 'DataApiStudio',
  setup() {
    const { t } = useI18n()
    const userStore = useUserStore()
    const iframeRef = ref<HTMLIFrameElement | null>(null)
    const ready = ref(false)
    const errorMsg = ref('')

    const magicBase =
      import.meta.env.VITE_APP_DATA_API_MAGIC_URL || '/magic/web'

    onMounted(() => {
      const sessionId = userStore.getSessionId
      if (!sessionId) {
        errorMsg.value = t('data_api.studio.no_session')
        return
      }
      // magic-editor reads parent.MAGIC_EDITOR_CONFIG for SSO auto-login
      window.MAGIC_EDITOR_CONFIG = {
        baseURL: magicBase.endsWith('/') ? magicBase : `${magicBase}/`,
        serverURL: magicBase.endsWith('/') ? magicBase : `${magicBase}/`,
        title: 'Data API',
        getMagicTokenValue: () => sessionId
      }
      ready.value = true
    })

    onBeforeUnmount(() => {
      delete window.MAGIC_EDITOR_CONFIG
    })

    return { t, iframeRef, ready, errorMsg, magicBase }
  },
  render() {
    const { t, ready, errorMsg, magicBase } = this
    return (
      <NSpace vertical style={{ height: 'calc(100vh - 140px)' }}>
        <NAlert type='info' title={t('data_api.studio.tip_title')}>
          {t('data_api.studio.tip_body')}
        </NAlert>
        {errorMsg ? <NAlert type='error'>{errorMsg}</NAlert> : null}
        <Card style={{ flex: 1, minHeight: 0 }}>
          {ready ? (
            <iframe
              ref='iframeRef'
              src={`${magicBase}/index.html`}
              style={{
                width: '100%',
                height: 'calc(100vh - 240px)',
                border: 'none',
                background: '#fff'
              }}
              title='magic-api'
            />
          ) : null}
        </Card>
      </NSpace>
    )
  }
})

export default DataApiStudio
