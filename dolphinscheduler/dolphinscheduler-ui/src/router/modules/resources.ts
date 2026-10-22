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
  path: '/resource',
  name: 'resource',
  redirect: { name: 'file-manage' },
  meta: { title: '资源中心' },
  component: () => import('@/layouts/content'),
  children: [
    {
      path: '/resource/file-manage',
      name: 'file-manage',
      component: () => import('@/views/resource/file'),
      meta: {
        title: '文件管理',
        activeMenu: 'resource',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/resource/file/create',
      name: 'resource-file-create',
      component: () => import('@/views/resource/file/create'),
      meta: {
        title: '文件创建',
        activeMenu: 'resource',
        activeSide: '/resource/file-manage',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/resource/file/edit',
      name: 'resource-file-edit',
      component: () => import('@/views/resource/file/edit'),
      meta: {
        title: '文件编辑',
        activeMenu: 'resource',
        activeSide: '/resource/file-manage',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/resource/file/subdirectory',
      name: 'resource-file-subdirectory',
      component: () => import('@/views/resource/file'),
      meta: {
        title: '文件管理',
        activeMenu: 'resource',
        activeSide: '/resource/file-manage',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/resource/file/list',
      name: 'resource-file-list',
      component: () => import('@/views/resource/file/edit'),
      meta: {
        title: '文件详情',
        activeMenu: 'resource',
        activeSide: '/resource/file-manage',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/resource/file/create',
      name: 'resource-subfile-create',
      component: () => import('@/views/resource/file/create'),
      meta: {
        title: '文件创建',
        activeMenu: 'resource',
        activeSide: '/resource/file-manage',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/resource/resource-manage',
      name: 'resource-manage',
      component: () => import('@/views/resource/udf/resource'),
      meta: {
        title: '资源管理',
        activeMenu: 'resource',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/resource/resource-manage',
      name: 'resource-sub-manage',
      component: () => import('@/views/resource/udf/resource'),
      meta: {
        title: '资源管理',
        activeMenu: 'resource',
        activeSide: '/resource/resource-manage',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/resource/function-manage',
      name: 'function-manage',
      component: () => import('@/views/resource/udf/function'),
      meta: {
        title: '函数管理',
        activeMenu: 'resource',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/resource/task-group-option',
      name: 'task-group-option',
      component: () => import('@/views/resource/task-group/option'),
      meta: {
        title: '任务组配置',
        activeMenu: 'resource',
        showSide: true,
        auth: []
      }
    },
    {
      path: '/resource/task-group-queue',
      name: 'task-group-queue',
      component: () => import('@/views/resource/task-group/queue'),
      meta: {
        title: '任务组队列',
        activeMenu: 'resource',
        showSide: true,
        auth: []
      }
    }
  ]
}
