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

import axios, { AxiosRequestConfig, AxiosResponse, AxiosError } from 'axios'
import { useUserStore } from '@/store/user/user'
import { useUISettingStore } from '@/store/ui-setting/ui-setting'
import qs from 'qs'
import _ from 'lodash'
import cookies from 'js-cookie'
import router from '@/router'
import utils from '@/utils'

// const userStore = useUserStore()
// const uiSettingStore = useUISettingStore()

/**
 * @description Log and display errors
 * @param {Error} error Error object
 */
const handleError = (res: AxiosResponse<any, any>) => {
  // Print to console
  if (import.meta.env.MODE === 'development') {
    utils.log.capsule('DolphinScheduler', 'UI')
    utils.log.error(res)
  }
  window.$message.error(res.data.msg)
}
const baseRequestConfig: AxiosRequestConfig = {
  baseURL:
    import.meta.env.MODE === 'development'
      ? '/dolphinscheduler'
      : import.meta.env.VITE_APP_PROD_WEB_URL + '/dolphinscheduler',
  timeout: 50000,
  transformRequest: (params) => {
    if (_.isPlainObject(params)) {
      return qs.stringify(params, { arrayFormat: 'repeat' })
    } else {
      return params
    }
  },
  paramsSerializer: (params) => {
    return qs.stringify(params, { arrayFormat: 'repeat' })
  }
}

const service = axios.create(baseRequestConfig)

const err = (err: AxiosError): Promise<AxiosError> => {
  if (err.response?.status === 401 || err.response?.status === 504) {
    if (window.APP_CONFIG.singleSignOnValue) {
      window.location.href = window.APP_CONFIG.ssoConfig.url
    }else if(!window.APP_CONFIG.singleSignOnValue && window.location.href.indexOf('token=') == -1) {
      const userStore = useUserStore()
      let logoutUrl = userStore.getLogoutUrl
      userStore.clear()
      cookies.remove('sessionId')
      if(logoutUrl) {
        window.location.href = logoutUrl
      } else {
        router.push({ path: '/login' })
      }
    } else {
      router.push({ path: '/login' })
    }
    // userStore.setSessionId('')
    // userStore.setSecurityConfigType('')
    // userStore.setUserInfo({})
    // userStore.setBaseResDir('')
    // userStore.setBaseUdfDir('')
    // router.push({ path: '/login' })
  }

  return Promise.reject(err)
}

service.interceptors.request.use((config: AxiosRequestConfig<any>) => {
  const userStore = useUserStore()
  config.headers = config.headers || {}
  config.headers.sessionId = userStore.getSessionId
  const language = cookies.get('language')
  if (language) config.headers.language = language

  return config
}, err)

// The response to intercept
service.interceptors.response.use((res: AxiosResponse) => {
  // No code will be processed
  if (res.data.code === undefined) {
    return res.data
  }

  switch (res.data.code) {
    case 0:
      return res.data.data
    case 200:
      return res.data
    default:
      handleError(res)
      if (res && res.data && res.data.data && res.data.data.logoutUrl) {
        window.location.href = res.data.data.logoutUrl
      }
      throw new Error()
  }
}, err)

export { service as axios }
