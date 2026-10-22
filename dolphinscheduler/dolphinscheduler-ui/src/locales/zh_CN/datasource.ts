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
  datasource: '数据源',
  create_datasource: '添加', // '创建源',
  choose_datasource_type: '选择源类型',
  select: '更改',
  search_input_tips: '请输入关键字',
  datasource_name: '源名称',
  datasource_name_tips: '请输入数据源名称',
  datawarehouse: '数据仓库',
  datawarehouse_tips: '请输入数据仓库',
  datasource_user_name: '所属用户',
  datasource_type: '源类型',
  datasource_parameter: '参数',
  description: '描述',
  description_tips: '请输入描述',
  create_time: '创建时间',
  update_time: '更新时间',
  operation: '操作',
  click_to_view: '点击查看',
  delete: '删除',
  confirm: '确定',
  delete_confirm: '删除？',
  cancel: '取消',
  create: '创建',
  edit: '编辑',
  success: '成功',
  test_connect: '测试连接',
  ip: 'IP主机名',
  ip_tips: '请输入IP主机名',
  port: '端口',
  port_tips: '请输入端口',
  database_name: '数据库名',
  database_name_tips: '请输入数据库名',
  oracle_connect_type: '服务名或SID',
  oracle_connect_type_tips: '请选择服务名或SID',
  oracle_service_name: '服务名',
  oracle_sid: 'SID',
  compatible_mode: '兼容模式',
  compatible_mode_tips: '请输入兼容模式',
  jdbc_connect_parameters: 'jdbc连接参数',
  kafka_connect_parameters: 'kafka连接参数',
  hdfs_kbs_parameters: 'kerberos认证参数',
  udp_multicast_connect_parameters: 'UDP组播连接参数',
  udp_multicast_connect_source_tip:
    '可选参数: ' +
    'ttl: 组播TTL值：1（本地),32（站点）,255（全球);' +
    'maxPacketSize: 单个UDP数据包的最大字节数;' +
    'reuseAddress: 是否重用套接字地址;' +
    'replyEnabled: 是否回复消息;' +
    'replyMessage: 回复消息内容;' +
    'charsetName: 字符编码',
  udp_multicast_connect_sink_tip:
    '可选参数：' +
    'maxRetries: 最大重试次数; ' +
    'retryIntervalMs: 重试发送间隔（毫秒）;' +
    'waitForReply: 是否等待回复(true/fasle);' +
    'replyTimeoutMs: 等待回复的超时时间（毫秒）;' +
    'batchIntervalMs: 批量发送的最大时间间隔（毫秒）;' +
    'batchQueueSize: 批量队列的最大消息数;' +
    'asyncEnabled: 是否启用异步发送模式;' +
    'asyncThreads: 异步发送线程数;' +
    'asyncQueueSize: 异步发送队列大小;' +
    'maxPacketSize: 单个UDP数据包的最大字节数;' +
    'charsetName: 字符编码',
  socket_server_connect_parameters: 'Socket Server连接参数',
  principal_tips: '请输入Principal',
  krb5_conf_tips: '请输入kerberos认证参数 java.security.krb5.conf',
  keytab_username_tips: '请输入kerberos认证参数 login.user.keytab.username',
  keytab_path_tips: '请输入kerberos认证参数 login.user.keytab.path',
  format_tips: '请输入格式为',
  connection_parameter: '连接参数',
  user_name: '用户名',
  user_name_tips: '请输入用户名',
  user_password: '密码',
  user_password_tips: '请输入密码',
  aws_region: 'AwsRegion',
  aws_region_tips: '请输入AwsRegion',
  aws_access_key: 'AwsAccessKey',
  aws_access_key_tips: '请输入AwsAccessKey',
  aws_secret_access_key: 'AwsSecretAccessKey',
  aws_secret_access_key_tips: '请输入AwsSecretAccessKey',
  validation: '验证',
  mode_tips: '请选择验证模式',
  jdbc_format_tips: 'jdbc连接参数不是一个正确的JSON格式',
  kafka_format_tips: 'kafka连接参数不是一个正确的JSON格式',
  host: '连接地址',
  host_tips: '请输入连接地址',
  database_username: '数据库用户名',
  database_password: '数据库密码',
  Azure_AD_username: 'Azure AD用户名',
  Azure_AD_password: 'Azure AD密码',
  MSIClientId: 'MSI ClientId',
  clientId: 'ClientId',
  clientSecret: 'ClientSecret',
  OAuth_token_endpoint: 'OAuth 2.0 token endpoint',
  endpoint_tips: '请输入OAuth',
  AccessKeyID: 'AccessKeyID',
  AccessKeyID_tips: '请输入AccessKeyID',
  SecretAccessKey: 'SecretAccessKey',
  SecretAccessKey_tips: '请输入SecretAccessKey',
  dbUser: 'DbUser',
  dbUser_tips: '请输入DbUser',
  zeppelin_rest_endpoint: 'zeppelinRestEndpoint',
  zeppelin_rest_endpoint_tips: '请输入zeppelin server的rest endpoint',
  kubeConfig: 'kubeConfig',
  kubeConfig_tips: '请输入KubeConfig',
  namespace: 'namespace',
  namespace_tips: '请输入namespace',
  http_endpoint: '短路径',
  http_endpoint_tips: '请输入请求短路径',
  http_method: '请求方式',
  http_method_tips: '请输入请求方式(支持GET、PODT)',
  http_auth_config: '认证参数',
  http_auth_config_tips: '请输入认证参数',
  http_auth_type: '认证类型',
  http_auth_type_tips: '请选择认证类型',
  basic_tip: 'Basic认证参数',
  bearer_tip: 'Bearer认证参数',
  api_key_tip: 'API_KEY认证参数',
  none_tip: '无需认证配置',
  connect_timeout: '连接超时时间',
  connect_timeout_tips: '请输入连接超时时间',
  socket_timeout: 'Socket超时时间',
  socket_timeout_tips: '请输入Socket超时时间',
  max_connections: '最大连接数',
  max_connections_tips: '请输入最大连接数',
  buffer_size: '缓冲区大小',
  buffer_size_tips: '请输入缓冲区大小',
  multicast_address: '组播地址',
  multicast_address_tips: '请输入组播地址',
  udp_multicast_type: '类型',
  hdfs_remote_user: 'HDFS用户',
  hdfs_remote_user_tips: '请输入HDFS用户',
  hdfs_site_path: 'HDFS-SITE配置路径',
  hdfs_site_path_tips: '请输入HDFS-SITE配置路径',
  minio_datasource_secure: '是否安全连接HTTPS'
}
