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
  datasource: 'DataSource',
  create_datasource: 'Create DataSource',
  choose_datasource_type: 'Choose DataSource Type',
  select: 'Select',
  search_input_tips: 'Please input the keywords',
  datasource_name: 'Datasource Name',
  datasource_name_tips: 'Please enter datasource name',
  datawarehouse: 'Datawarehouse',
  datawarehouse_tips: 'Please enter datawarehouse',
  datasource_user_name: 'Owner',
  datasource_type: 'Datasource Type',
  datasource_parameter: 'Datasource Parameter',
  description: 'Description',
  description_tips: 'Please enter description',
  create_time: 'Create Time',
  update_time: 'Update Time',
  operation: 'Operation',
  click_to_view: 'Click to view',
  delete: 'Delete',
  confirm: 'Confirm',
  delete_confirm: 'Delete?',
  cancel: 'Cancel',
  create: 'Create',
  edit: 'Edit',
  success: 'Success',
  test_connect: 'Test Connect',
  ip: 'IP',
  ip_tips: 'Please enter IP',
  port: 'Port',
  port_tips: 'Please enter port',
  database_name: 'Database Name',
  database_name_tips: 'Please enter database name',
  oracle_connect_type: 'ServiceName or SID',
  oracle_connect_type_tips: 'Please select serviceName or SID',
  oracle_service_name: 'ServiceName',
  oracle_sid: 'SID',
  compatible_mode: 'Compatible Mode',
  compatible_mode_tips: 'Please enter compatible mode',
  jdbc_connect_parameters: 'jdbc connect parameters',
  kafka_connect_parameters: 'kafka connect parameters',
  hdfs_kbs_parameters: 'hdfs kerberos parameters',
  udp_multicast_connect_parameters: 'UDP multicast connect parameters',
  udp_multicast_connect_source_tip:
    'Options: ' +
    'ttl: Time-to-live for multicast packets (1=local network, 32=site, 255=unrestricted);' +
    'maxPacketSize: Maximum size of a single UDP packet in bytes;' +
    'reuseAddress: Whether to reuse the socket address;' +
    'replyEnabled: Whether to send reply messages back to sender;' +
    'replyMessage: The reply message to send when replyEnabled is true;' +
    'charsetName: Character encoding for message serialization',
  udp_multicast_connect_sink_tip:
    'Options：' +
    'maxRetries: Maximum number of send attempts for a message; ' +
    'retryIntervalMs: Interval between send retries in milliseconds;' +
    'waitForReply: Whether to wait for a reply after sending;' +
    'replyTimeoutMs: Timeout for waiting reply in milliseconds;' +
    'batchIntervalMs: Maximum time interval between batch sends in milliseconds;' +
    'batchQueueSize: Maximum number of messages in the batch queue;' +
    'asyncEnabled: Whether to use asynchronous sending;' +
    'asyncThreads: Number of threads in async sender thread pool;' +
    'asyncQueueSize: Maximum number of messages in async send queue;' +
    'maxPacketSize: Maximum size of a single UDP packet in bytes;' +
    'charsetName: Character encoding for message serialization',
  socket_server_connect_parameters: 'socket server connect parameters',
  principal_tips: 'Please enter Principal',
  krb5_conf_tips:
    'Please enter the kerberos authentication parameter java.security.krb5.conf',
  keytab_username_tips:
    'Please enter the kerberos authentication parameter login.user.keytab.username',
  keytab_path_tips:
    'Please enter the kerberos authentication parameter login.user.keytab.path',
  format_tips: 'Please enter format',
  connection_parameter: 'connection parameter',
  user_name: 'User Name',
  user_name_tips: 'Please enter your username',
  user_password: 'Password',
  user_password_tips: 'Please enter your password',
  aws_region: 'Aws Region',
  aws_region_tips: 'Please enter AwsRegion',
  aws_access_key: 'AwsAccessKey',
  aws_access_key_tips: 'Please enter AwsAccessKey',
  aws_secret_access_key: 'AwsSecretAccessKey',
  aws_secret_access_key_tips: 'Please enter AwsSecretAccessKey',
  validation: 'Validation',
  mode_tips: 'Please select a mode',
  jdbc_format_tips: 'jdbc connection parameters is not a correct JSON format',
  kafka_format_tips: 'kafka connection parameters is not a correct JSON format',
  host: 'Host',
  host_tips: 'Please enter Host',
  database_username: 'Database Username',
  database_password: 'Database Password',
  Azure_AD_username: 'Azure AD username',
  Azure_AD_password: 'Azure AD password',
  MSIClientId: 'MSI ClientId',
  clientId: 'ClientId',
  clientSecret: 'ClientSecret',
  OAuth_token_endpoint: 'OAuth 2.0 token endpoint',
  endpoint_tips: 'Please enter OAuth Token',
  AccessKeyID: 'AccessKeyID',
  AccessKeyID_tips: 'Please input AccessKeyID',
  SecretAccessKey: 'SecretAccessKey',
  SecretAccessKey_tips: 'Please input SecretAccessKey',
  dbUser: 'DbUser',
  dbUser_tips: 'Please input DbUser',
  zeppelin_rest_endpoint: 'zeppelinRestEndpoint',
  zeppelin_rest_endpoint_tips: 'Please input zeppelin restEndpoint',
  kubeConfig: 'kubeConfig',
  kubeConfig_tips: 'Please input KubeConfig',
  namespace: 'namespace',
  namespace_tips: 'Please input namespace',
  http_endpoint: 'endpoint',
  http_endpoint_tips: 'Please input Endpoint',
  http_method: 'method',
  http_method_tips: 'Please input Http Method(GET、POST)',
  http_auth_config: 'request authentication parameter',
  http_auth_config_tips: 'Please input Http authorization parameter',
  http_auth_type: 'request authentication type',
  http_auth_type_tips: 'Please input authentication type',
  basic_tip: 'basic parameter',
  bearer_tip: 'Bearer parameter',
  api_key_tip: 'API_KEY parameter',
  none_tip: 'none parameter',
  connect_timeout: 'connectTimeout',
  connect_timeout_tips: 'Please input connectTimeout',
  socket_timeout: 'SocketTimeout',
  max_connections: 'MaxConnections',
  socket_timeout_tips: 'Please input socketTimeout',
  max_connections_tips: 'Please input maxConnections',
  buffer_size: 'bufferSize',
  buffer_size_tips: 'Please input bufferSize',
  multicast_address: 'Multicast address',
  multicast_address_tips: 'Please input multicast address tips',
  hdfs_remote_user: 'Hdfs remote user',
  hdfs_remote_user_tips: 'Please input Hdfs remote user tips',
  hdfs_site_path: 'Hdfs site path',
  hdfs_site_path_tips: 'Please input hdfs site path',
  minio_datasource_secure: 'Flag to indicate to use secure (TLS) connection to S3 service or not.'
}
