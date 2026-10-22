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

import type { EditWorkflowDefinition } from '@/views/projects/workflow/components/dag/types'
import type { IOption } from '@/components/form/types'

type TaskExecuteType = 'STREAM' | 'BATCH'

type TaskType =
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
  | 'DATAX'
  | 'SQOOP'
  | 'SOURCE'
  | 'SINK'
  | 'TRANSFORM'
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
  | 'UNARCHIVE'
  | 'EXTRACT'
  | 'MINIO'

  | 'UDP_SINK'
  | 'UDP_SOURCE'
  | 'FILTER_TRANSFORM'
  | 'JSONPATH_TRANSFORM'
  | 'RANGE_TRANSFORM'
  | 'SQL_TRANSFORM'
  | 'SQLFILTER_TRANSFORM'
  | 'ENCRYPT_TRANSFORM'
  | 'DESENSITIZE_TRANSFORM'
  | 'DECRYPT_TRANSFORM'
  | 'SPLIT_TRANSFORM'
  | 'SWITCHCASE_TRANSFORM'
  | 'JAVA_TRANSFORM'
  | 'JAVASCRIPT_TRANSFORM'
  | 'RANDOMNUMBER_TRANSFORM'
  | 'SEQUENCE_TRANSFORM'

  | 'SQLCASEWHEN_TRANSFORM'
  | 'SPLITCOLUMNS_TRANSFORM'
  | 'SQLFUNCTION_TRANSFORM'
  | 'CONSTREFERENCE_TRANSFORM'
  | 'SORT_TRANSFORM'
  | 'DATADISTINCT_TRANSFORM'
  | 'GROUP_TRANSFORM'
  | 'TASK_REMOTE'
  | 'KAFKA_SOURCE'
  | 'KAFKA_SINK'

  | 'HTTP_SOURCE'
  | 'HTTP_SINK'
  | 'LOCALFILE_SOURCE'
  | 'LOCALFILE_SINK'
  | 'SOCKETSERVER_SOURCE'
  | 'HTTPSERVER_SOURCE'
  | 'FAKESOURCE_SOURCE'
  | 'UDPMULTICAST_SINK'
  | 'UDPMULTICAST_SOURCE'
  | 'FTPFILE_SOURCE'
  | 'SFTPFILE_SOURCE'
  | 'HDFSFILE_SOURCE'
  | 'HDFSFILE_SINK'
  | 'MINIOFILE_SOURCE'
  | 'MINIOFILE_SINK'
  | 'HIVE_SINK'
  | 'JOIN_TRANSFORM'
  | 'SQLJOIN_TRANSFORM'

  | 'SQLROWTOCOLUMN_TRANSFORM'
  | 'SQLCOLUMNTOROW_TRANSFORM'

  | 'DORIS_SINK'
  | 'DORIS_SOURCE'
  | 'SQLUNION_TRANSFORM'
  | 'SQLUNIONSORT_TRANSFORM'
  | 'DATANORMALIZE_TRANSFORM'
  | 'FLEXIBLE_HTTP'
  // | 'FLEXIBLE_API'

  | 'SQLUNION_TRANSFORM'
  | 'SQLUNIONSORT_TRANSFORM'

type ProgramType = 'JAVA' | 'SCALA' | 'PYTHON'

type DependentResultType = {
  [key: string]: 'SUCCESS' | 'FAILURE'
}

type BDependentResultType = {
  [key: string]: 'SUCCESS' | 'FAILED'
}

interface IResource {
  id: number
  name: string
  children?: IResource[]
}
interface IMainJar {
  id: number
  fullName: string
  children: IMainJar[]
}
interface TaskNodeState {
  postTaskOptions: IOption[]
  preTaskOptions: IOption[]
  preTasks: number[]
  resources: IResource[]
  mainJars: { [key in ProgramType]?: IMainJar[] }
  name: string
  dependentResult: DependentResultType
}

interface ITaskType {
  alias: string
  helperLinkDisable?: boolean
  taskDefinitionDisable?: boolean
  taskExecuteType?: TaskExecuteType
}
interface ITaskTypeItem extends ITaskType {
  type: TaskType
}
interface TaskTypeState {
  types: ITaskTypeItem[]
}

export {
  TaskNodeState,
  EditWorkflowDefinition,
  IOption,
  IResource,
  ProgramType,
  DependentResultType,
  BDependentResultType,
  IMainJar,
  TaskType,
  ITaskType,
  ITaskTypeItem,
  TaskTypeState,
  TaskExecuteType
}
