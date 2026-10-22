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
export type TaskType =
  | 'SHELL'
  | 'SUB_PROCESS'
  | 'DYNAMIC'
  | 'PROCEDURE'
  | 'SQL'
  | 'SPARK'
  | 'FLINK'
  | 'MR'
  | 'PYTHON'
  | 'DEPENDENT'
  | 'HTTP'
  | 'ASYNCSTATE'
  | 'WEBHOOK'
  | 'DATAX'
  | 'PIGEON'
  | 'SQOOP'
  | 'CONDITIONS'
  | 'DATA_QUALITY'
  | 'SWITCH'
  | 'SEATUNNEL'
  | 'EMR'
  | 'ZEPPELIN'
  | 'K8S'
  | 'JUPYTER'
  | 'MLFLOW'
  | 'OPENMLDB'
  | 'DVC'
  | 'JAVA'
  | 'DINKY'
  | 'SAGEMAKER'
  | 'CHUNJUN'
  | 'FLINK_STREAM'
  | 'PYTORCH'
  | 'HIVECLI'
  | 'DMS'
  | 'DATASYNC'
  | 'KUBEFLOW'
  | 'LINKIS'
  | 'DATA_FACTORY'
  | 'REMOTESHELL'
  | 'JDBC_SOURCE'
  | 'JDBC_SINK'
  | 'MYSQL_SINK'
  | 'MYSQL_SOURCE'
  | 'POSTGRESQL_SOURCE'
  | 'POSTGRESQL_SINK'
  | 'DAMENG_SOURCE'
  | 'DAMENG_SINK'
  | 'GAUSSDB_SOURCE'
  | 'GAUSSDB_SINK'
  | 'UDP_SINK'
  | 'UDP_SOURCE'
  | 'EXTRACT'
  | 'MINIO'


  | 'FILTER_TRANSFORM'
  | 'JSONPATH_TRANSFORM'
  | 'SQLJOIN_TRANSFORM'
  | 'RANGE_TRANSFORM'
  | 'SQL_TRANSFORM'
  | 'SQLFILTER_TRANSFORM'
  | 'ENCRYPT_TRANSFORM'
  | 'DESENSITIZE_TRANSFORM'
  | 'DECRYPT_TRANSFORM'
  | 'SPLIT_TRANSFORM'
  | 'SWITCHCASE_TRANSFORM'
  | 'JAVA_TRANSFORM'
  | 'SQLCASEWHEN_TRANSFORM'
  | 'JAVASCRIPT_TRANSFORM'
  | 'RANDOMNUMBER_TRANSFORM'
  | 'SEQUENCE_TRANSFORM'
  | 'SPLITCOLUMNS_TRANSFORM'
  | 'SQLFUNCTION_TRANSFORM'
  | 'CONSTREFERENCE_TRANSFORM'
  | 'SORT_TRANSFORM'
  | 'DATADISTINCT_TRANSFORM'
  | 'GROUP_TRANSFORM'
  | 'KAFKA_SOURCE'
  | 'KAFKA_SINK'
  | 'SQLROWTOCOLUMN_TRANSFORM'
  | 'SQLCOLUMNTOROW_TRANSFORM'
  | 'DORIS_SOURCE'
  | 'DORIS_SINK'
  | 'FLEXIBLE_HTTP'
  | 'FLEXIBLE_API'

export type TaskExecuteType = 'STREAM' | 'BATCH'

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
  EXTRACT: {
    alias: 'EXTRACT',
    helperLinkDisable: true
  },
  MINIO: {
    alias: 'MINIO',
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
    alias: 'FILTER_TRANSFORM',
    helperLinkDisable: true
  },
  JSONPATH_TRANSFORM: {
    alias: 'JSONPATH_TRANSFORM',
    helperLinkDisable: true
  },
  SQLJOIN_TRANSFORM: {
    alias: 'SQLJOIN_TRANSFORM',
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
  KAFKA_SOURCE: {
    alias: 'KAFKA_SOURCE',
    helperLinkDisable: true
  },
  KAFKA_SINK: {
    alias: 'KAFKA_SINK',
    helperLinkDisable: true
  },
  UDP_SOURCE: {
    alias: 'UDP_SOURCE',
    helperLinkDisable: true
  },
  UDP_SINK: {
    alias: 'UDP_SINK',
    helperLinkDisable: true
  },
  FLEXIBLE_HTTP: {
    alias: 'FLEXIBLE_HTTP',
    helperLinkDisable: true
  },
  FLEXIBLE_API: {
    alias: 'FLEXIBLE_API',
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
  POSTGRESQL_SOURCE: {
    alias: 'POSTGRESQL_SOURCE',
    helperLinkDisable: true
  },
  POSTGRESQL_SINK: {
    alias: 'POSTGRESQL_SINK',
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

} as {
    [key in TaskType]: {
      alias: string
      helperLinkDisable?: boolean
      taskExecuteType?: TaskExecuteType
    }
  }
