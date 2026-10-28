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

export default {
  studio: {
    tip_title: 'API 开发（magic-api）',
    tip_body:
      '使用 DS 登录态自动进入控制台。数据源 key 优先复用源中心「名称」（如 osint_mysql），控制台显示名带类型/ID/库名便于对照。脚本示例：return db.osint_mysql.select("select 1")。发布路径前缀 /data-api/gateway。',
    no_session: '未检测到登录会话，请重新登录后再打开本页。'
  },
  token: {
    tip_title: '调用数据服务需要 Access Token',
    tip_body:
      '对外调用 /data-api/gateway/** 时请在 Header 携带 token（与安全中心令牌相同）。控制台页面可使用登录 sessionId；生产调用请创建并妥善保管下方 Token。'
  },
  acl: {
    tip_title: '接口授权（API × 人/Token）',
    tip_body:
      '当前模式：{mode}。optional=某路径有 ALLOW 规则时仅授权主体可调，无规则则任意已登录可调；enforce=非管理员必须显式 ALLOW；off=关闭路径 ACL。路径支持精确匹配或 /prefix/**。管理员始终放行。',
    create_title: '新增授权',
    api_path: 'API 路径',
    subject_type: '主体类型',
    subject_id: '主体',
    subject_id_ph: '选择用户 ID 或 Access Token',
    effect: '效果',
    remark: '备注',
    create: '创建授权',
    create_ok: '已添加',
    delete: '删除',
    delete_ok: '已删除',
    refresh: '刷新',
    actions: '操作',
    form_required: '请填写 API 路径与主体',
    search_tips: '请输入 API 路径'
  },
  datasources: {
    tip: 'key 与源中心名称对应（冲突或非法名时追加 _id）。显示名含类型/ID/库名；脚本列可直接复制。Ping 执行 SELECT 1。',
    key: 'Magic Key',
    name: '源中心名称',
    type: '类型',
    ds_id: '源 ID',
    database: '库名',
    script: '脚本用法',
    actions: '操作',
    reload: '从源中心重新加载',
    refresh: '刷新列表',
    reload_ok: '已重新加载'
  }
}
