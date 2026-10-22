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

import {
  createRouter,
  createWebHistory,
  NavigationGuardNext,
  RouteLocationNormalized
} from 'vue-router'
import routes from './routes'
// import { useUserStore } from '@/store/user/user'
import type { UserInfoRes } from '@/service/modules/users/types'
import { useUserStore } from '@/store/user/user'

// NProgress
import NProgress from 'nprogress'
import 'nprogress/nprogress.css'
import { getUserInfo } from '@/service/modules/users'
import { basicSso } from '@/service/modules/login'
import cookies from 'js-cookie'


const router = createRouter({
  history: createWebHistory(
    import.meta.env.MODE === 'production' ? '/ds/' : '/'
  ),
  routes
})

interface metaData {
  title?: string
  activeMenu?: string
  showSide?: boolean
  auth?: Array<string>
}

/**
 * Routing to intercept
 */
router.beforeEach(
  async (
    to: RouteLocationNormalized,
    from: RouteLocationNormalized,
    next: NavigationGuardNext
  ) => {
    NProgress.start()
    const userStore = useUserStore()
    const metaData: metaData = to.meta
    const sessionId = userStore.getSessionId
    const ticket = to.query.token
    // 单点登录模式
    if (ticket && (!sessionId || sessionId != ticket)) {
      const res = await basicSso(ticket)
      const { sessionId } = res

      if (sessionId) {
        userStore.setSessionId(sessionId)
        cookies.set('sessionId', sessionId)
        const userInfoRes: UserInfoRes = await getUserInfo()
        if (userInfoRes) {
          await userStore.setUserInfo(userInfoRes)
          next({...to, replace: true})
        } else {
          if (window.APP_CONFIG.singleSignOnValue) {
            ssoRedirect()
          } else {
            next('/login')
          }
        }
      } else {
        if (window.APP_CONFIG.singleSignOnValue) {
            ssoRedirect()
          } else {
            next('/login')
          }
      }
    } else {
      if (
        metaData.auth?.includes('ADMIN_USER') &&
        (userStore.getUserInfo as UserInfoRes).userType !== 'ADMIN_USER' &&
        metaData.activeMenu === 'security'
      ) {
        to.fullPath = '/security/token-manage'
        next({ name: 'token-manage' })
      } else {
        next()
      }
    }

    NProgress.done()
  }
)

const ssoRedirect = () => {
  // const service = window.location.href
  const service = ''
  window.location.href = `${window.APP_CONFIG.ssoConfig.url}?response_type=code&client_id=${window.APP_CONFIG.ssoConfig.clientId}&redirect_uri=${service}`
}

router.afterEach(() => {
  NProgress.done()
})

export default router
