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

import { reactive, ref } from 'vue'
import { useI18n } from 'vue-i18n'
import { getKerberosStartupState } from '@/service/modules/data-source'
import type { FormRules } from 'naive-ui'
import type {
  IDataSourceDetail,
  IDataBase,
  IDataBaseOption,
  IDataBaseOptionKeys,
  IDataSource
} from './types'
import utils from '@/utils'

export function useForm(id?: number) {
  const { t } = useI18n()

  const initialValues = {
    type: 'MYSQL',
    label: 'MYSQL',
    name: '',
    note: '',
    host: '',
    port: datasourceType['MYSQL'].defaultPort,
    principal: '',
    javaSecurityKrb5Conf: '',
    loginUserKeytabUsername: '',
    loginUserKeytabPath: '',
    mode: '',
    userName: '',
    password: '',
    database: '',
    connectType: '',
    other: '',
    endpoint: '',
    MSIClientId: '',
    dbUser: '',
    datawarehouse: '',
    // add http server params
    httpEndpoint: '',
    httpMethod: 'GET',
    httpAuthType: 'NONE',
    httpAuthConfig: '',
    connectTimeout: 5000,
    socketTimeout: 5000,
    bufferSize: 4196,
    // add udp multicast params
    multicastAddress: '',
    udpMulticastType: 'SOURCE',
    maxConnections: 100,
    // add hdfs file
    remoteUser: 'default',
    hdfsSitePath: '',
    //add minio
    secure: false
  } as IDataSourceDetail

  const state = reactive({
    detailFormRef: ref(),
    detailForm: { ...initialValues },
    requiredDataBase: true,
    showHost: true,
    showPort: true,
    showAwsRegion: false,
    showRestEndpoint: false,
    showCompatibleMode: false,
    showConnectType: false,
    showPrincipal: false,
    showMode: false,
    showDataBaseName: true,
    showJDBCConnectParameters: true,
    showKAFKAConnectParameters: false,
    showSocketServerParameters: false,
    showHttpServerParameters: false,
    showUdpMulticastParameters: false,
    showHdfsFileParameters: false,
    showMinioParameters: false,
    showFtpParameters: false,
    showSftpParameters: false,
    showPublicKey: false,
    showHttpEndpoint: false,
    showHttpMethod: false,
    showHttpAuthType: false,
    showHttpAuthConfig: false,
    showConnectTimeout: false,
    showSocketTimeout: false,
    showBufferSize: false,
    showMulticastAddress: false,
    showUdpMulticastType: false,
    showMaxConnections: false,
    showRemoteUser: false,
    showHdfsSitePath: false,
    rules: {
      name: {
        trigger: ['input'],
        validator() {
          if (!state.detailForm.name) {
            return new Error(t('datasource.datasource_name_tips'))
          }
        }
      },
      host: {
        trigger: ['input'],
        validator() {
          if (!state.detailForm.host && state.showHost) {
            return new Error(t('datasource.ip_tips'))
          }
        }
      },
      port: {
        trigger: ['input'],
        validator() {
          if (state.showMode && state.detailForm.mode === 'IAM-accessKey') {
            return
          }
          if (!state.detailForm.port && state.showPort) {
            return new Error(t('datasource.port_tips'))
          }
        }
      },
      principal: {
        trigger: ['input'],
        validator() {
          if (!state.detailForm.principal && state.showPrincipal) {
            return new Error(t('datasource.principal_tips'))
          }
        }
      },
      mode: {
        trigger: ['blur'],
        validator() {
          if (!state.detailForm.mode && state.showMode) {
            return new Error(t('datasource.mode_tips'))
          }
        }
      },
      userName: {
        trigger: ['input'],
        validator() {
          if (
            !state.detailForm.userName &&
            state.detailForm.type !== 'AZURESQL' &&
            state.detailForm.type !== 'KAFKA' &&
            state.detailForm.type !== 'SOCKET_SERVER' &&
            state.detailForm.type !== 'HTTP_SERVER' &&
            state.detailForm.type !== 'UDP_MULTICAST' &&
            state.detailForm.type !== 'HDFS_FILE'
          ) {
            return new Error(t('datasource.user_name_tips'))
          }
        }
      },
      awsRegion: {
        trigger: ['input'],
        validator() {
          if (!state.detailForm.awsRegion && state.showAwsRegion) {
            return new Error(t('datasource.aws_region_tips'))
          }
        }
      },
      database: {
        trigger: ['input'],
        validator() {
          if (!state.detailForm.database && state.requiredDataBase) {
            return new Error(t('datasource.database_name_tips'))
          }
        }
      },
      datawarehouse: {
        trigger: ['input'],
        validator() {
          if (!state.detailForm.datawarehouse) {
            return new Error(t('datasource.datawarehouse_tips'))
          }
        }
      },
      connectType: {
        trigger: ['update'],
        validator() {
          if (!state.detailForm.connectType && state.showConnectType) {
            return new Error(t('datasource.oracle_connect_type_tips'))
          }
        }
      },
      other: {
        trigger: ['input', 'blur'],
        validator() {
          if (state.detailForm.other && !utils.isJson(state.detailForm.other)) {
            return new Error(t('datasource.jdbc_format_tips'))
          }
        }
      },
      endpoint: {
        trigger: ['input'],
        validator() {
          if (
            !state.detailForm.endpoint &&
            state.detailForm.type === 'AZURESQL' &&
            state.detailForm.mode === 'accessToken'
          ) {
            return new Error(t('datasource.endpoint_tips'))
          }
        }
      },
      dbUser: {
        trigger: ['input'],
        validator() {
          if (
            !state.detailForm.dbUser &&
            state.showMode &&
            state.detailForm.mode === 'IAM-accessKey' &&
            state.detailForm.type != 'SAGEMAKER'
          ) {
            return new Error(t('datasource.IAM-accessKey'))
          }
        }
      }
      // databaseUserName: {
      //   trigger: ['input'],
      //   validator() {
      //     if (!state.detailForm.userName) {
      //       return new Error(t('datasource.user_name_tips'))
      //     }
      //   }
      // },
    } as FormRules,
    modeOptions: [
      {
        label: 'SqlPassword',
        value: 'SqlPassword'
      },
      {
        label: 'ActiveDirectoryPassword',
        value: 'ActiveDirectoryPassword'
      },
      {
        label: 'ActiveDirectoryMSI',
        value: 'ActiveDirectoryMSI'
      },
      {
        label: 'ActiveDirectoryServicePrincipal',
        value: 'ActiveDirectoryServicePrincipal'
      },
      {
        label: 'accessToken',
        value: 'accessToken'
      }
    ],
    redShiftModeOptions: [
      {
        label: 'password',
        value: 'password'
      },
      {
        label: 'IAM-accessKey',
        value: 'IAM-accessKey'
      }
    ],
    sagemakerModeOption: [
      {
        label: 'IAM-accessKey',
        value: 'IAM-accessKey'
      }
    ]
  })

  const changeType = async (type: IDataBase, options: IDataBaseOption) => {
    state.detailForm.port = options.previousPort || options.defaultPort
    state.detailForm.type = type
    state.showKAFKAConnectParameters = false
    state.showSocketServerParameters = false
    state.showHttpServerParameters = false
    state.showUdpMulticastParameters = false
    state.showHdfsFileParameters = false
    state.showMinioParameters = false
    state.requiredDataBase = type !== 'POSTGRESQL' && type !== 'ATHENA'

    state.showHost = type !== 'ATHENA'
    state.showPort = type !== 'ATHENA'
    state.showAwsRegion = type === 'ATHENA' || type === 'SAGEMAKER'
    state.showMode = ['AZURESQL', 'REDSHIFT', 'SAGEMAKER'].includes(type)

    if (type === 'ORACLE' && !id) {
      state.detailForm.connectType = 'ORACLE_SERVICE_NAME'
    }
    state.showConnectType = type === 'ORACLE'

    state.showCompatibleMode = type == 'OCEANBASE'

    if (type === 'HIVE' || type === 'SPARK') {
      state.showPrincipal = await getKerberosStartupState()
    } else {
      state.showPrincipal = false
    }
    if (type === 'SSH' || type === 'ZEPPELIN' || type === 'SAGEMAKER') {
      state.showDataBaseName = false
      state.requiredDataBase = false
      state.showJDBCConnectParameters = false
      state.showPublicKey = false
      if (type === 'SSH') {
        state.showPublicKey = true
      }
      if (type === 'ZEPPELIN') {
        state.showHost = false
        state.showPort = false
        state.showRestEndpoint = true
      }
      if (type === 'SAGEMAKER') {
        state.showHost = false
        state.showPort = false
      }
    }else if (type == 'KAFKA') {
      state.showKAFKAConnectParameters = true
      state.showDataBaseName = false
      state.requiredDataBase = false
      state.showJDBCConnectParameters = false
      state.showPublicKey = false
      state.showHost = false
      state.showPort = false
    } else if (type == 'SOCKET_SERVER') {
      state.showSocketServerParameters = true
      state.showDataBaseName = false
      state.requiredDataBase = false
      state.showJDBCConnectParameters = false
      state.showPublicKey = false
      state.showHost = true
      state.showPort = true
      state.showConnectTimeout = false
      state.showBufferSize = true
      state.showMaxConnections = true
      state.showSocketTimeout = true
    } else if (type == 'HTTP_SERVER') {
      state.showHttpServerParameters = true
      state.showDataBaseName = false
      state.requiredDataBase = false
      state.showJDBCConnectParameters = false
      state.showPublicKey = false
      state.showHost = true
      state.showPort = true
      state.showHttpEndpoint = true
      state.showHttpMethod = true
      state.showHttpAuthType = true
      state.showHttpAuthConfig = true
      state.showConnectTimeout = true
      state.showSocketTimeout = false
    } else if (type == 'UDP_MULTICAST') {
      state.showUdpMulticastParameters = true
      state.showDataBaseName = false
      state.requiredDataBase = false
      state.showJDBCConnectParameters = false
      state.showPublicKey = false
      state.showHost = false
      state.showUdpMulticastType = true
      state.showMulticastAddress = true
      state.showPort = true
      state.showConnectTimeout = true
      state.showBufferSize = true
    } else if (type == 'HDFS_FILE') {
      state.showHdfsFileParameters = true
      state.showDataBaseName = false
      state.requiredDataBase = false
      state.showJDBCConnectParameters = false
      state.showPublicKey = false
      state.showHost = false
      state.showPort = false
      state.showHdfsSitePath = true
      state.showRemoteUser = true
    } else if (type == 'MINIO') {
      state.showMinioParameters = true
      state.showDataBaseName = false
      state.requiredDataBase = false
      state.showJDBCConnectParameters = false
    } else if (type == 'FTP') {
      state.showFtpParameters = true
      state.showDataBaseName = false
      state.requiredDataBase = false
      state.showJDBCConnectParameters = false
    } else if (type == 'SFTP') {
      state.showSftpParameters = true
      state.showDataBaseName = false
      state.requiredDataBase = false
      state.showJDBCConnectParameters = false
    } else {
      state.showDataBaseName = true
      state.requiredDataBase = true
      state.showJDBCConnectParameters = true
      state.showPublicKey = false
    }
  }

  const changePort = async () => {
    if (!state.detailForm.type) return
    const currentDataBaseOption = datasourceType[state.detailForm.type]
    currentDataBaseOption.previousPort = state.detailForm.port
  }

  const resetFieldsValue = () => {
    state.detailForm = { ...initialValues }
  }

  const setFieldsValue = (values: IDataSource) => {
    state.detailForm = {
      ...state.detailForm,
      ...values,
      other: values.other ? JSON.stringify(values.other) : values.other
    }
  }

  const getFieldsValue = () => state.detailForm

  return {
    state,
    changeType,
    changePort,
    resetFieldsValue,
    setFieldsValue,
    getFieldsValue
  }
}

export const datasourceType: IDataBaseOptionKeys = {
  MYSQL: {
    value: 'MYSQL',
    label: 'MYSQL',
    defaultPort: 3306
  },
  POSTGRESQL: {
    value: 'POSTGRESQL',
    label: 'POSTGRESQL',
    defaultPort: 5432
  },
  HIVE: {
    value: 'HIVE',
    label: 'HIVE/IMPALA',
    defaultPort: 10000
  },
  KYUUBI: {
    value: 'KYUUBI',
    label: 'KYUUBI',
    defaultPort: 10000
  },
  SPARK: {
    value: 'SPARK',
    label: 'SPARK',
    defaultPort: 10015
  },
  CLICKHOUSE: {
    value: 'CLICKHOUSE',
    label: 'CLICKHOUSE',
    defaultPort: 8123
  },
  ORACLE: {
    value: 'ORACLE',
    label: 'ORACLE',
    defaultPort: 1521
  },
  SQLSERVER: {
    value: 'SQLSERVER',
    label: 'SQLSERVER',
    defaultPort: 1433
  },
  DB2: {
    value: 'DB2',
    label: 'DB2',
    defaultPort: 50000
  },
  VERTICA: {
    value: 'VERTICA',
    label: 'VERTICA',
    defaultPort: 5433
  },
  PRESTO: {
    value: 'PRESTO',
    label: 'PRESTO',
    defaultPort: 8080
  },
  REDSHIFT: {
    value: 'REDSHIFT',
    label: 'REDSHIFT',
    defaultPort: 5439
  },
  ATHENA: {
    value: 'ATHENA',
    label: 'ATHENA',
    defaultPort: 0
  },
  TRINO: {
    value: 'TRINO',
    label: 'TRINO',
    defaultPort: 8080
  },
  AZURESQL: {
    value: 'AZURESQL',
    label: 'AZURESQL',
    defaultPort: 1433
  },
  STARROCKS: {
    value: 'STARROCKS',
    label: 'STARROCKS',
    defaultPort: 9030
  },
  DAMENG: {
    value: 'DAMENG',
    label: 'DAMENG',
    defaultPort: 5236
  },
  OCEANBASE: {
    value: 'OCEANBASE',
    label: 'OCEANBASE',
    defaultPort: 2881
  },
  SNOWFLAKE: {
    value: 'SNOWFLAKE',
    label: 'SNOWFLAKE',
    defaultPort: 3306
  },
  SSH: {
    value: 'SSH',
    label: 'SSH',
    defaultPort: 22
  },
  DATABEND: {
    value: 'DATABEND',
    label: 'DATABEND',
    defaultPort: 8000
  },
  HANA: {
    value: 'HANA',
    label: 'HANA',
    defaultPort: 30015
  },
  ZEPPELIN: {
    value: 'ZEPPELIN',
    label: 'ZEPPELIN',
    defaultPort: 8080
  },
  DORIS: {
    value: 'DORIS',
    label: 'DORIS',
    defaultPort: 9030
  },
  SAGEMAKER: {
    value: 'SAGEMAKER',
    label: 'SAGEMAKER',
    defaultPort: 0
  },
  KAFKA: {
    value: 'KAFKA',
    label: 'KAFKA',
    defaultPort: 9092
  },
  SOCKET_SERVER: {
    value: 'SOCKET_SERVER',
    label: 'SOCKET_SERVER',
    defaultPort: 9999
  },
  HTTP_SERVER: {
    value: 'HTTP_SERVER',
    label: 'HTTP_SERVER',
    defaultPort: 8999
  },
  UDP_MULTICAST: {
    value: 'UDP_MULTICAST',
    label: 'UDP_MULTICAST',
    defaultPort: 9990
  },
  HDFS_FILE: {
    value: 'HDFS_FILE',
    label: 'HDFS_FILE',
    defaultPort: 8020
  },
  MINIO: {
    value: 'MINIO',
    label: 'MINIO',
    defaultPort: 9000
  },
  FTP: {
    value: 'FTP',
    label: 'FTP',
    defaultPort: 21
  },
  SFTP: {
    value: 'SFTP',
    label: 'SFTP',
    defaultPort: 22
  },
  OPENGAUSS:  {
    value: 'OPENGAUSS',
    label: 'OPENGAUSS',
    defaultPort: 5432
  }
}

export const datasourceTypeList: IDataBaseOption[] = Object.values(
  datasourceType
).map((item) => {
  item.class = 'options-datasource-type'
  return item
})
