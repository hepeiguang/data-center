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

import type { Component } from 'vue'
import utils from '@/utils'

const modules = import.meta.glob('/src/views/**/**.tsx')
const components: { [key: string]: Component } = utils.mapping(modules)

// UI path prefix is /dataservice to avoid clashing with Vite proxy of /data-api → microservice
export default {
  path: '/dataservice',
  name: 'dataservice',
  meta: { title: '数据服务' },
  redirect: { name: 'data-api-studio' },
  component: () => import('@/layouts/content'),
  children: [
    {
      path: '/dataservice/studio',
      name: 'data-api-studio',
      component: components['data-api-studio'],
      meta: {
        title: 'API 开发',
        activeMenu: 'dataservice',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/dataservice/token',
      name: 'data-api-token',
      component: components['data-api-token'],
      meta: {
        title: 'Token 配置',
        activeMenu: 'dataservice',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/dataservice/datasources',
      name: 'data-api-datasources',
      component: components['data-api-datasources'],
      meta: {
        title: '数据源状态',
        activeMenu: 'dataservice',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/dataservice/acl',
      name: 'data-api-acl',
      component: components['data-api-acl'],
      meta: {
        title: '接口授权',
        activeMenu: 'dataservice',
        showSide: true,
        auth: []
      }
    }
  ]
}
