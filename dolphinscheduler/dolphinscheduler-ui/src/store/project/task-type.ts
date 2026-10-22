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
import { defineStore } from 'pinia'
import type {
  TaskTypeState,
  TaskType,
  ITaskTypeItem,
  TaskExecuteType
} from './types'
import { helper } from 'echarts'

export const TASK_TYPES_MAP = {
  JAVA: {
    alias: 'JAVA'
  },
  SHELL: {
    alias: 'SHELL'
  },
  SUB_PROCESS: {
    alias: 'SUB_PROCESS'
  },
  DYNAMIC: {
    alias: 'DYNAMIC'
  },
  PROCEDURE: {
    alias: 'PROCEDURE'
  },
  SQL: {
    alias: 'SQL'
  },
  SPARK: {
    alias: 'SPARK'
  },
  FLINK: {
    alias: 'FLINK'
  },
  MR: {
    alias: 'MapReduce',
    helperLinkDisable: true
  },
  PYTHON: {
    alias: 'PYTHON'
  },
  DEPENDENT: {
    alias: 'DEPENDENT'
  },
  HTTP: {
    alias: 'HTTP'
  },
  ASYNCSTATE: {
    alias: 'ASYNCSTATE'
  },
  WEBHOOK: {
    alias: 'WEBHOOK'
  },
  DATAX: {
    alias: 'DataX'
  },
  PIGEON: {
    alias: 'PIGEON'
  },
  SQOOP: {
    alias: 'SQOOP',
    helperLinkDisable: true
  },
  CONDITIONS: {
    alias: 'CONDITIONS'
  },
  DATA_QUALITY: {
    alias: 'DATA_QUALITY',
    helperLinkDisable: true
  },
  SWITCH: {
    alias: 'SWITCH'
  },
  SEATUNNEL: {
    alias: 'SeaTunnel',
    helperLinkDisable: true
  },
  SOURCE: {
    alias: 'Source',
    helperLinkDisable: true
  },
  EMR: {
    alias: 'AmazonEMR',
    helperLinkDisable: true
  },
  ZEPPELIN: {
    alias: 'ZEPPELIN',
    helperLinkDisable: true
  },
  JUPYTER: {
    alias: 'JUPYTER',
    helperLinkDisable: true
  },
  K8S: {
    alias: 'K8S',
    helperLinkDisable: true
  },
  MLFLOW: {
    alias: 'MLFLOW',
    helperLinkDisable: true
  },
  OPENMLDB: {
    alias: 'OPENMLDB',
    helperLinkDisable: true
  },
  DVC: {
    alias: 'DVC',
    helperLinkDisable: true
  },
  DINKY: {
    alias: 'DINKY',
    helperLinkDisable: true
  },
  SAGEMAKER: {
    alias: 'SageMaker',
    helperLinkDisable: true
  },
  EXTRACT: {
    alias: 'EXTRACT',
    helperLinkDisable: true
  },
  MINIO: {
    alias: 'MINIO',
    helperLinkDisable: true
  },
  CHUNJUN: {
    alias: 'CHUNJUN',
    helperLinkDisable: true
  },
  FLINK_STREAM: {
    alias: 'FLINK_STREAM',
    helperLinkDisable: true,
    taskExecuteType: 'STREAM'
  },
  PYTORCH: {
    alias: 'Pytorch',
    helperLinkDisable: true
  },
  HIVECLI: {
    alias: 'HIVECLI',
    helperLinkDisable: true
  },
  DMS: {
    alias: 'DMS',
    helperLinkDisable: true
  },
  DATASYNC: {
    alias: 'DATASYNC',
    helperLinkDisable: true
  },
  KUBEFLOW: {
    alias: 'KUBEFLOW',
    helperLinkDisable: true
  },
  LINKIS: {
    alias: 'LINKIS',
    helperLinkDisable: true
  },
  DATA_FACTORY: {
    alias: 'DATA_FACTORY',
    helperLinkDisable: true
  },
  REMOTESHELL: {
    alias: 'REMOTESHELL',
    helperLinkDisable: true
  },
  JDBC_SOURCE: {
    alias: 'JDBC_SOURCE',
    helperLinkDisable: true
  },
  JDBC_SINK: {
    alias: 'JDBC_SINK',
    helperLinkDisable: true
  },
  FILTER_TRANSFORM: {
    alias: 'FILTER_TRANSFORM'
  },
  SQLFUNCTION_TRANSFORM: {
    alias: 'SQLFUNCTION_TRANSFORM'
  },
  CONSTREFERENCE_TRANSFORM: {
    alias: 'CONSTREFERENCE_TRANSFORM'
  },
  SORT_TRANSFORM: {
    alias: 'SORT_TRANSFORM'
  },
  DATADISTINCT_TRANSFORM: {
    alias: 'DATADISTINCT_TRANSFORM'
  },
  GROUP_TRANSFORM: {
    alias: 'GROUP_TRANSFORM'
  },
  TASK_REMOTE: {
    alias: 'TASK_REMOTE'
  },
  KAFKA_SOURCE: {
    alias: 'KAFKA_SOURCE',
    helperLinkDisable: true
  },
  KAFKA_SINK: {
    alias: 'KAFKA_SINK',
    helperLinkDisable: true
  },
  UDP_SINK: {
    alias: 'UDP_SINK',
    helperLinkDisable: true
  },
    UDP_SOURCE: {
    alias: 'UDP_SOURCE',
    helperLinkDisable: true
  },
  HTTP_SOURCE: {
    alias: 'HTTP_SOURCE',
    helperLinkDisable: true
  },
  HTTP_SINK: {
    alias: 'HTTP_SINK',
    helperLinkDisable: true
  },
  LOCALFILE_SOURCE: {
    alias: 'LOCALFILE_SOURCE',
    helperLinkDisable: true
  },
  LOCALFILE_SINK: {
    alias: 'LOCALFILE_SINK',
    helperLinkDisable: true
  },
  SOCKETSERVER_SOURCE: {
    alias: 'SOCKETSERVER_SOURCE',
    helperLinkDisable: true
  },
  HTTPSERVER_SOURCE: {
    alias: 'HTTPSERVER_SOURCE',
    helperLinkDisable: true
  },
  FAKESOURCE_SOURCE: {
    alias: 'FAKESOURCE_SOURCE',
    helperLinkDisable: true
  },
  UDPMULTICAST_SINK: {
    alias: 'UDPMULTICAST_SINK',
    helperLinkDisable: true
  },
  UDPMULTICAST_SOURCE: {
    alias: 'UDPMULTICAST_SOURCE',
    helperLinkDisable: true
  },
  FTPFILE_SOURCE: {
    alias: 'FTPFILE_SOURCE',
    helperLinkDisable: true
  },
  SFTPFILE_SOURCE: {
    alias: 'SFTPFILE_SOURCE',
    helperLinkDisable: true
  },
  HDFSFILE_SOURCE: {
    alias: 'HDFSFILE_SOURCE',
    helperLinkDisable: true
  },
  HDFSFILE_SINK: {
    alias: 'HDFSFILE_SINK',
    helperLinkDisable: true
  },
  MINIOFILE_SOURCE: {
    alias: 'MINIOFILE_SOURCE',
    helperLinkDisable: true
  },
  MINIOFILE_SINK: {
    alias: 'MINIOFILE_SINK',
    helperLinkDisable: true
  },
  MYSQL_SINK: {
    alias: 'MYSQL_SINK',
    helperLinkDisable: true
  },
  MYSQL_SOURCE: {
    alias: 'MYSQL_SOURCE',
    helperLinkDisable: true
  },
  POSTGRESQL_SINK: {
    alias: 'POSTGRESQL_SINK',
    helperLinkDisable: true
  },
  POSTGRESQL_SOURCE: {
    alias: 'POSTGRESQL_SOURCE',
    helperLinkDisable: true
  },
  DAMENG_SOURCE: {
    alias: 'DAMENG_SOURCE',
    helperLinkDisable: true
  },
  DAMENG_SINK: {
    alias: 'DAMENG_SINK',
    helperLinkDisable: true
  },
  GAUSSDB_SOURCE: {
    alias: 'GAUSSDB_SOURCE',
    helperLinkDisable: true
  },
  GAUSSDB_SINK: {
    alias: 'GAUSSDB_SINK',
    helperLinkDisable: true
  },
  HIVE_SINK: {
    alias: 'HIVE_SINK',
    helperLinkDisable: true
  },

  JOIN_TRANSFORM: {
    alias: 'JOIN_TRANSFORM',
    helperLinkDisable: true
  },
  SQLJOIN_TRANSFORM: {
    alias: 'SQLJOIN_TRANSFORM',
    helperLinkDisable: true
  },
  JSONPATH_TRANSFORM: {
    alias: 'JSONPATH_TRANSFORM',
    helperLinkDisable: true
  },
  RANGE_TRANSFORM: {
    alias: 'RANGE_TRANSFORM',
    helperLinkDisable: true
  },
  SQL_TRANSFORM: {
    alias: 'SQL_TRANSFORM',
    helperLinkDisable: true
  },
  // SeaTunnel
  SQLFILTER_TRANSFORM: {
    alias: 'SQLFILTER_TRANSFORM',
    helperLinkDisable: true
  },
  ENCRYPT_TRANSFORM: {
    alias: 'ENCRYPT_TRANSFORM',
    helperLinkDisable: true
  },
  DESENSITIZE_TRANSFORM: {
    alias: 'DESENSITIZE_TRANSFORM',
    helperLinkDisable: true
  },
  DECRYPT_TRANSFORM: {
    alias: 'DECRYPT_TRANSFORM',
    helperLinkDisable: true
  },
  SPLIT_TRANSFORM: {
    alias: 'SPLIT_TRANSFORM',
    helperLinkDisable: true
  },
  SWITCHCASE_TRANSFORM: {
    alias: 'SWITCHCASE_TRANSFORM',
    helperLinkDisable: true
  },
  JAVA_TRANSFORM: {
    alias: 'JAVA_TRANSFORM',
    helperLinkDisable: true
  },

  SQLCASEWHEN_TRANSFORM: {
    alias: 'SQLCASEWHEN_TRANSFORM',
    helperLinkDisable: true
  },

  JAVASCRIPT_TRANSFORM: {
    alias: 'JAVASCRIPT_TRANSFORM',
    helperLinkDisable: true
  },

  RANDOMNUMBER_TRANSFORM: {
    alias: 'RANDOMNUMBER_TRANSFORM',
    helperLinkDisable: true
  },
  SEQUENCE_TRANSFORM: {
    alias: 'SEQUENCE_TRANSFORM',
    helperLinkDisable: true
  },

  SPLITCOLUMNS_TRANSFORM: {
    alias: 'SPLITCOLUMNS_TRANSFORM',
    helperLinkDisable: true
  },

  SQLROWTOCOLUMN_TRANSFORM: {
    alias: 'SQLROWTOCOLUMN_TRANSFORM',
    helperLinkDisable: true
  },
  SQLCOLUMNTOROW_TRANSFORM: {
    alias: 'SQLCOLUMNTOROW_TRANSFORM',
    helperLinkDisable: true
  },

  DORIS_SOURCE: {
    alias: 'DORIS_SOURCE',
    helperLinkDisable: true
  },
  DORIS_SINK: {
    alias: 'DORIS_SINK',
    helperLinkDisable: true
  },
  SQLUNION_TRANSFORM: {
    alias: 'SQLUNION_TRANSFORM',
    helperLinkDisable: true
  },
  SQLUNIONSORT_TRANSFORM: {
    alias: 'SQLUNIONSORT_TRANSFORM',
    helperLinkDisable: true
  },
  DATANORMALIZE_TRANSFORM: {
    alias: 'DATANORMALIZE_TRANSFORM',
    helperLinkDisable: true
  },
  FLEXIBLE_HTTP: {
    alias: 'FLEXIBLE_HTTP',
    helperLinkDisable: true
  },
  FLEXIBLE_API: {
    alias: 'FLEXIBLE_API',
    helperLinkDisable: true
  }


} as {
    [key in TaskType]: {
      alias: string
      helperLinkDisable?: boolean
      taskExecuteType?: TaskExecuteType
    }
  }

export const useTaskTypeStore = defineStore({
  id: 'project-task-type',
  state: (): TaskTypeState => ({
    types: []
  }),
  persist: true,
  getters: {
    getTaskType(): ITaskTypeItem[] {
      return this.types
    }
  },
  actions: {
    setTaskTypes(types: TaskType[]): void {
      try {
        this.types = types
          .filter((type) => !!TASK_TYPES_MAP[type])
          .map((type) => ({ ...TASK_TYPES_MAP[type], type }))
      } catch (err) {
        this.types = []
      }
    }
  }
})
