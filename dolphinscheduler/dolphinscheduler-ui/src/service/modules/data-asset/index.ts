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

import axios from 'axios'
import { useUserStore } from '@/store/user/user'

const baseURL =
  import.meta.env.MODE === 'development'
    ? '/data-asset'
    : (import.meta.env.VITE_APP_DATA_ASSET_URL || '') + '/data-asset'

const service = axios.create({ baseURL, timeout: 60000 })

service.interceptors.request.use((config) => {
  const store = useUserStore()
  const sessionId = store.getSessionId
  if (sessionId) {
    ;(config.headers as any).sessionId = sessionId
  }
  const user = store.getUserInfo as any
  if (user?.userName) {
    ;(config.headers as any)['X-Debug-User'] = user.userName
  }
  return config
})

export default service
