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

export default {
  path: '/asset',
  name: 'data-asset',
  meta: { title: '数据资产' },
  redirect: { name: 'data-asset-catalog' },
  component: () => import('@/layouts/content'),
  children: [
    {
      path: '/asset/catalog',
      name: 'data-asset-catalog',
      component: components['data-asset-catalog'],
      meta: {
        title: '资产目录',
        activeMenu: 'data-asset',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/asset/lineage',
      name: 'data-asset-lineage',
      component: components['data-asset-lineage'],
      meta: {
        title: '数据血缘',
        activeMenu: 'data-asset',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/asset/governance',
      name: 'data-asset-governance',
      component: components['data-asset-governance'],
      meta: {
        title: '治理配置',
        activeMenu: 'data-asset',
        showSide: true,
        auth: []
      }
    }
  ]
}
