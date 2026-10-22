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

// All TSX files under the views folder automatically generate mapping relationship
const modules = import.meta.glob('/src/views/**/**.tsx')
const components: { [key: string]: Component } = utils.mapping(modules)

export default {
  path: '/foreignProjects',
  name: 'foreignProjects',
  meta: {
    title: '项目管理'
  },
  redirect: { name: 'foreignProjects-list' },
  // component: () => import('@/layouts/content'),
  children: [
    {
      path: '/foreignProjects/:projectCode/workflow/definitions/create',
      name: 'foreign-workflow-definition-create',
      component: components['projects-workflow-definition-create'],
      meta: {
        title: '创建工作流定义',
        activeMenu: 'foreignProjects',
        activeSide: '/foreignProjects/:projectCode/workflow-definition',
        showSide: false,
        auth: []
      }
    },
    {
      path: '/foreignProjects/:projectCode/workflow/definitions/:code',
      name: 'foreign-workflow-definition-detail',
      component: components['projects-workflow-definition-detail'],
      meta: {
        title: '工作流定义详情',
        activeMenu: 'foreignProjects',
        activeSide: '/foreignProjects/:projectCode/workflow-definition',
        showSide: false,
        auth: []
      }
    },
    
    
  ]
}
