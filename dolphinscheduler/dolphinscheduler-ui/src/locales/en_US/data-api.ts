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
    tip_title: 'API Studio (magic-api)',
    tip_body:
      'Uses DS login for SSO. Magic keys prefer datasource-center names (e.g. osint_mysql); console labels include type/id/database. Example: return db.osint_mysql.select("select 1"). Gateway prefix: /data-api/gateway.',
    no_session: 'No login session found. Please sign in again.'
  },
  token: {
    tip_title: 'Access Token required for Data API calls',
    tip_body:
      'Call /data-api/gateway/** with Header token (same as Security → Token). The studio may use sessionId; create and protect Access Tokens for production clients.'
  },
  acl: {
    tip_title: 'API ACL (path × user/token)',
    tip_body:
      'Mode: {mode}. optional=if a path has ALLOW rules, only those subjects; else any authenticated. enforce=non-admin needs explicit ALLOW. off=disabled. Paths: exact or /prefix/**. Admins always allowed.',
    create_title: 'Grant access',
    api_path: 'API path',
    subject_type: 'Subject type',
    subject_id: 'Subject',
    subject_id_ph: 'Pick user id or Access Token',
    effect: 'Effect',
    remark: 'Remark',
    create: 'Create Grant',
    create_ok: 'Created',
    delete: 'Delete',
    delete_ok: 'Deleted',
    refresh: 'Refresh',
    actions: 'Actions',
    form_required: 'API path and subject are required',
    search_tips: 'Please enter API path'
  },
  datasources: {
    tip: 'Keys reuse datasource-center names (append _id on conflict). Labels show type/id/database; copy script usage from the table. Ping runs SELECT 1.',
    key: 'Magic Key',
    name: 'Center Name',
    type: 'Type',
    ds_id: 'DS ID',
    database: 'Database',
    script: 'Script usage',
    actions: 'Actions',
    reload: 'Reload from center',
    refresh: 'Refresh',
    reload_ok: 'Reloaded'
  }
}
