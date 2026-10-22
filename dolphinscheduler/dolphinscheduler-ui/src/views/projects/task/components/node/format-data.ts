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

import { omit } from 'lodash'
import type {
  INodeData,
  ITaskData,
  ITaskParams,
  ISqoopTargetParams,
  ISqoopSourceParams,
  ILocalParam,
  IDependentParameters
} from './types'
import { ref } from 'vue'

export function formatParams(data: INodeData): {
  processDefinitionCode: string
  upstreamCodes: string
  taskDefinitionJsonObj: object
} {
  const rdbmsSourceTypes = ref(['MYSQL', 'ORACLE', 'SQLSERVER', 'HANA'])
  const taskParams: ITaskParams = {}
  if (data && data.taskType && (data.taskType.indexOf('_SOURCE') > 0 || data.taskType.indexOf('_SINK') > 0 || data.taskType.indexOf('_TRANSFORM') > 0)) {
    taskParams.outputFields = data.outputFields
  }
  if (data.taskType === 'SUB_PROCESS' || data.taskType === 'DYNAMIC') {
    taskParams.processDefinitionCode = data.processDefinitionCode
  }

  if (data.taskType === 'JAVA') {
    taskParams.runType = data.runType
    taskParams.mainArgs = data.mainArgs
    taskParams.jvmArgs = data.jvmArgs
    taskParams.isModulePath = data.isModulePath
    if (data.runType === 'JAR' && data.mainJar) {
      taskParams.mainJar = { resourceName: data.mainJar }
    }
  }

  if (
    data.taskType &&
    ['SPARK', 'MR', 'FLINK', 'FLINK_STREAM'].includes(data.taskType)
  ) {
    taskParams.programType = data.programType
    taskParams.mainClass = data.mainClass
    if (data.mainJar) {
      taskParams.mainJar = { resourceName: data.mainJar }
    }
    taskParams.deployMode = data.deployMode
    taskParams.appName = data.appName
    taskParams.mainArgs = data.mainArgs
    taskParams.others = data.others
    if (data.namespace) {
      taskParams.namespace = data.namespace
    }
    taskParams.yarnQueue = data.yarnQueue
  }

  if (data.taskType === 'SPARK') {
    taskParams.master = data.master
    taskParams.driverCores = data.driverCores
    taskParams.driverMemory = data.driverMemory
    taskParams.numExecutors = data.numExecutors
    taskParams.executorMemory = data.executorMemory
    taskParams.executorCores = data.executorCores
    taskParams.sqlExecutionType = data.sqlExecutionType
  }

  if (data.taskType === 'FLINK' || data.taskType === 'FLINK_STREAM') {
    taskParams.flinkVersion = data.flinkVersion
    taskParams.jobManagerMemory = data.jobManagerMemory
    taskParams.taskManagerMemory = data.taskManagerMemory
    taskParams.slot = data.slot
    taskParams.taskManager = data.taskManager
    taskParams.parallelism = data.parallelism
  }
  if (data.taskType === 'HTTP') {
    taskParams.httpMethod = data.httpMethod
    taskParams.httpBody = data.httpBody
    taskParams.httpCheckCondition = data.httpCheckCondition
    taskParams.httpParams = data.httpParams
    taskParams.url = data.url
    taskParams.condition = data.condition
    taskParams.connectTimeout = data.connectTimeout
    taskParams.socketTimeout = data.socketTimeout
  }
  if (data.taskType === 'ASYNCSTATE') {
    taskParams.asyncStateMethod = data.asyncStateMethod
    taskParams.httpBody = data.httpBody
    taskParams.asyncStateCheckCondition = data.asyncStateCheckCondition
    taskParams.httpParams = data.httpParams
    taskParams.url = data.url
    taskParams.condition = data.condition
    taskParams.connectTimeout = data.connectTimeout
    taskParams.socketTimeout = data.socketTimeout
  }
  if (data.taskType === 'WEBHOOK') {
    taskParams.webHookMethod = data.webHookMethod
    taskParams.url = data.url
    taskParams.response = data.response
  }

  if (data.taskType === 'SQOOP') {
    taskParams.jobType = data.isCustomTask ? 'CUSTOM' : 'TEMPLATE'
    taskParams.localParams = data.localParams
    if (data.isCustomTask) {
      taskParams.customShell = data.customShell
    } else {
      taskParams.jobName = data.jobName
      taskParams.hadoopCustomParams = data.hadoopCustomParams
      taskParams.sqoopAdvancedParams = data.sqoopAdvancedParams
      taskParams.concurrency = data.concurrency
      taskParams.splitBy = data.splitBy
      taskParams.modelType = data.modelType
      taskParams.sourceType = data.sourceType
      taskParams.targetType = data.targetType
      let targetParams: ISqoopTargetParams = {}
      let sourceParams: ISqoopSourceParams = {}
      if (data.targetType === 'HIVE') {
        targetParams = {
          hiveDatabase: data.targetHiveDatabase,
          hiveTable: data.targetHiveTable,
          createHiveTable: data.targetHiveCreateTable,
          dropDelimiter: data.targetHiveDropDelimiter,
          hiveOverWrite: data.targetHiveOverWrite,
          hiveTargetDir: data.targetHiveTargetDir,
          replaceDelimiter: data.targetHiveReplaceDelimiter,
          hivePartitionKey: data.targetHivePartitionKey,
          hivePartitionValue: data.targetHivePartitionValue
        }
      } else if (data.targetType === 'HDFS') {
        targetParams = {
          targetPath: data.targetHdfsTargetPath,
          deleteTargetDir: data.targetHdfsDeleteTargetDir,
          compressionCodec: data.targetHdfsCompressionCodec,
          fileType: data.targetHdfsFileType,
          fieldsTerminated: data.targetHdfsFieldsTerminated,
          linesTerminated: data.targetHdfsLinesTerminated
        }
      } else if (
        rdbmsSourceTypes.value.some((target) => target === data.targetType)
      ) {
        targetParams = {
          targetType: data.targetMysqlType,
          targetDatasource: data.targetMysqlDatasource,
          targetTable: data.targetMysqlTable,
          targetColumns: data.targetMysqlColumns,
          fieldsTerminated: data.targetMysqlFieldsTerminated,
          linesTerminated: data.targetMysqlLinesTerminated,
          isUpdate: data.targetMysqlIsUpdate,
          targetUpdateKey: data.targetMysqlTargetUpdateKey,
          targetUpdateMode: data.targetMysqlUpdateMode
        }
      }
      if (rdbmsSourceTypes.value.some((target) => target === data.sourceType)) {
        sourceParams = {
          srcTable: data.srcQueryType === '1' ? '' : data.srcTable,
          srcColumnType: data.srcQueryType === '1' ? '0' : data.srcColumnType,
          srcColumns:
            data.srcQueryType === '1' || data.srcColumnType === '0'
              ? ''
              : data.srcColumns,
          srcQuerySql:
            data.srcQueryType === '0' ? '' : data.sourceMysqlSrcQuerySql,
          srcQueryType: data.srcQueryType,
          srcType: data.sourceMysqlType,
          srcDatasource: data.sourceMysqlDatasource,
          mapColumnHive: data.mapColumnHive,
          mapColumnJava: data.mapColumnJava
        }
      } else if (data.sourceType === 'HDFS') {
        sourceParams = {
          exportDir: data.sourceHdfsExportDir
        }
      } else if (data.sourceType === 'HIVE') {
        sourceParams = {
          hiveDatabase: data.sourceHiveDatabase,
          hiveTable: data.sourceHiveTable,
          hivePartitionKey: data.sourceHivePartitionKey,
          hivePartitionValue: data.sourceHivePartitionValue
        }
      }
      taskParams.targetParams = JSON.stringify(targetParams)
      taskParams.sourceParams = JSON.stringify(sourceParams)
    }
  }

  if (data.taskType === 'SQL') {
    taskParams.type = data.type
    taskParams.datasource = data.datasource
    taskParams.sql = data.sql
    taskParams.sqlType = data.sqlType
    taskParams.preStatements = data.preStatements
    taskParams.postStatements = data.postStatements
    taskParams.sendEmail = data.sendEmail
    taskParams.displayRows = data.displayRows
    if (data.sqlType === '0' && data.sendEmail) {
      taskParams.title = data.title
      taskParams.groupId = data.groupId
    }
    if (data.type === 'HIVE') {
      if (data.udfs) taskParams.udfs = data.udfs.join(',')
      taskParams.connParams = data.connParams
    }

    if (data.type === 'KYUUBI') {
      if (data.udfs) taskParams.udfs = data.udfs.join(',')
    }
  }

  if (data.taskType === 'PROCEDURE') {
    taskParams.type = data.type
    taskParams.datasource = data.datasource
    taskParams.method = data.method
  }

  if (data.taskType === 'SEATUNNEL') {
    taskParams.startupScript = data.startupScript
    taskParams.useCustom = data.useCustom
    taskParams.scriptType = data.scriptType
    taskParams.rawScript = data.rawScript
    if (data.startupScript?.includes('flink')) {
      taskParams.runMode = data.runMode
      taskParams.others = data.others
    }
    if (data.startupScript?.includes('spark')) {
      taskParams.deployMode = data.deployMode
      taskParams.master = data.master
      taskParams.masterUrl = data.masterUrl
    }
    if (data.startupScript === 'seatunnel.sh') {
      taskParams.deployMode = data.deployMode
      taskParams.others = data.others
    }
  }

  if (data.taskType === 'SWITCH') {
    taskParams.switchResult = {}
    taskParams.switchResult.dependTaskList = data.dependTaskList
    taskParams.switchResult.nextNode = data.nextNode
  }

  if (data.taskType === 'CONDITIONS') {
    taskParams.dependence = {
      relation: data.relation,
      dependTaskList: data.dependTaskList
    }
    taskParams.conditionResult = {}
    if (data.successBranch) {
      taskParams.conditionResult.successNode = [data.successBranch]
    }
    if (data.failedBranch) {
      taskParams.conditionResult.failedNode = [data.failedBranch]
    }
  }

  if (data.taskType === 'DATAX') {
    taskParams.customConfig = data.customConfig ? 1 : 0
    if (taskParams.customConfig === 0) {
      taskParams.dsType = data.dsType
      taskParams.dataSource = data.dataSource
      taskParams.dtType = data.dtType
      taskParams.dataTarget = data.dataTarget
      taskParams.sql = data.sql
      taskParams.targetTable = data.targetTable
      taskParams.jobSpeedByte = data.jobSpeedByte
      taskParams.jobSpeedRecord = data.jobSpeedRecord
      taskParams.preStatements = data.preStatements
      taskParams.postStatements = data.postStatements
    } else {
      taskParams.json = data.json
      data?.localParams?.map((param: ILocalParam) => {
        param.direct = 'IN'
        param.type = 'VARCHAR'
      })
    }
    taskParams.xms = data.xms
    taskParams.xmx = data.xmx
  }
  if (data.taskType === 'DEPENDENT') {
    taskParams.dependence = {
      checkInterval: data.checkInterval,
      failurePolicy: data.failurePolicy,
      failureWaitingTime: data.failureWaitingTime,
      relation: data.relation,
      dependTaskList: data.dependTaskList
    }
  }
  if (data.taskType === 'DATA_QUALITY') {
    taskParams.ruleId = data.ruleId
    taskParams.ruleInputParameter = {
      check_type: data.check_type,
      comparison_execute_sql: data.comparison_execute_sql,
      comparison_type: data.comparison_type,
      comparison_name: data.comparison_name,
      failure_strategy: data.failure_strategy,
      operator: data.operator,
      src_connector_type: data.src_connector_type,
      src_datasource_id: data.src_datasource_id,
      src_database: data.src_database,
      field_length: data.field_length,
      begin_time: data.begin_time,
      deadline: data.deadline,
      datetime_format: data.datetime_format,
      enum_list: data.enum_list,
      regexp_pattern: data.regexp_pattern,
      target_filter: data.target_filter,
      src_filter: data.src_filter,
      src_field: data.src_field,
      src_table: data.src_table,
      statistics_execute_sql: data.statistics_execute_sql,
      statistics_name: data.statistics_name,
      target_connector_type: data.target_connector_type,
      target_datasource_id: data.target_datasource_id,
      target_database: data.target_database,
      target_table: data.target_table,
      threshold: data.threshold,
      mapping_columns: JSON.stringify(data.mapping_columns)
    }
    taskParams.sparkParameters = {
      deployMode: data.deployMode,
      driverCores: data.driverCores,
      driverMemory: data.driverMemory,
      executorCores: data.executorCores,
      executorMemory: data.executorMemory,
      numExecutors: data.numExecutors,
      others: data.others,
      yarnQueue: data.yarnQueue,
      sqlExecutionType: data.sqlExecutionType
    }
  }

  if (data.taskType === 'EMR') {
    taskParams.type = data.type
    taskParams.programType = data.programType
    taskParams.jobFlowDefineJson = data.jobFlowDefineJson
    taskParams.stepsDefineJson = data.stepsDefineJson
  }

  if (data.taskType === 'ZEPPELIN') {
    taskParams.noteId = data.noteId
    taskParams.paragraphId = data.paragraphId
    taskParams.restEndpoint = data.restEndpoint
    taskParams.username = data.username
    taskParams.password = data.password
    taskParams.productionNoteDirectory = data.productionNoteDirectory
    taskParams.parameters = data.parameters
    taskParams.datasource = data.datasource
    taskParams.type = data.type
  }

  if (data.taskType === 'K8S') {
    taskParams.namespace = data.namespace
    taskParams.minCpuCores = data.minCpuCores
    taskParams.minMemorySpace = data.minMemorySpace
    taskParams.image = data.image
    taskParams.imagePullPolicy = data.imagePullPolicy
    taskParams.command = data.command
    taskParams.args = data.args
    taskParams.customizedLabels = data.customizedLabels
    taskParams.nodeSelectors = data.nodeSelectors
    taskParams.pullSecret = data.pullSecret
  }

  if (data.taskType === 'JUPYTER') {
    taskParams.condaEnvName = data.condaEnvName
    taskParams.inputNotePath = data.inputNotePath
    taskParams.outputNotePath = data.outputNotePath
    taskParams.parameters = data.parameters
    taskParams.kernel = data.kernel
    taskParams.engine = data.engine
    taskParams.executionTimeout = data.executionTimeout
    taskParams.startTimeout = data.startTimeout
    taskParams.others = data.others
  }

  if (data.taskType === 'MLFLOW') {
    taskParams.algorithm = data.algorithm
    taskParams.params = data.params
    taskParams.searchParams = data.searchParams
    taskParams.dataPath = data.dataPath
    taskParams.experimentName = data.experimentName
    taskParams.modelName = data.modelName
    taskParams.mlflowTrackingUri = data.mlflowTrackingUri
    taskParams.mlflowJobType = data.mlflowJobType
    taskParams.automlTool = data.automlTool
    taskParams.registerModel = data.registerModel
    taskParams.mlflowTaskType = data.mlflowTaskType
    taskParams.deployType = data.deployType
    taskParams.deployPort = data.deployPort
    taskParams.deployModelKey = data.deployModelKey
    taskParams.mlflowProjectRepository = data.mlflowProjectRepository
    taskParams.mlflowProjectVersion = data.mlflowProjectVersion
  }

  if (data.taskType === 'DVC') {
    taskParams.dvcTaskType = data.dvcTaskType
    taskParams.dvcRepository = data.dvcRepository
    taskParams.dvcVersion = data.dvcVersion
    taskParams.dvcDataLocation = data.dvcDataLocation
    taskParams.dvcMessage = data.dvcMessage
    taskParams.dvcLoadSaveDataPath = data.dvcLoadSaveDataPath
    taskParams.dvcStoreUrl = data.dvcStoreUrl
  }

  if (data.taskType === 'SAGEMAKER') {
    taskParams.sagemakerRequestJson = data.sagemakerRequestJson
    taskParams.username = data.username
    taskParams.password = data.password
    taskParams.datasource = data.datasource
    taskParams.type = data.type
    taskParams.awsRegion = data.awsRegion
  }
  if (data.taskType === 'PYTORCH') {
    taskParams.script = data.script
    taskParams.scriptParams = data.scriptParams
    taskParams.pythonPath = data.pythonPath
    taskParams.isCreateEnvironment = data.isCreateEnvironment
    taskParams.pythonCommand = data.pythonCommand
    taskParams.pythonEnvTool = data.pythonEnvTool
    taskParams.requirements = data.requirements
    taskParams.condaPythonVersion = data.condaPythonVersion
  }

  if (data.taskType === 'DINKY') {
    taskParams.address = data.address
    taskParams.taskId = data.taskId
    taskParams.online = data.online
  }

  if (data.taskType === 'OPENMLDB') {
    taskParams.zk = data.zk
    taskParams.zkPath = data.zkPath
    taskParams.executeMode = data.executeMode
    taskParams.sql = data.sql
  }

  if (data.taskType === 'CHUNJUN') {
    taskParams.customConfig = data.customConfig ? 1 : 0
    taskParams.json = data.json
    taskParams.deployMode = data.deployMode
    taskParams.others = data.others
  }

  if (data.taskType === 'PIGEON') {
    taskParams.targetJobName = data.targetJobName
  }

  if (data.taskType === 'HIVECLI') {
    taskParams.hiveCliTaskExecutionType = data.hiveCliTaskExecutionType
    taskParams.hiveSqlScript = data.hiveSqlScript
    taskParams.hiveCliOptions = data.hiveCliOptions
  }
  if (data.taskType === 'DMS') {
    taskParams.isRestartTask = data.isRestartTask
    taskParams.isJsonFormat = data.isJsonFormat
    taskParams.jsonData = data.jsonData
    taskParams.migrationType = data.migrationType
    taskParams.replicationTaskIdentifier = data.replicationTaskIdentifier
    taskParams.sourceEndpointArn = data.sourceEndpointArn
    taskParams.targetEndpointArn = data.targetEndpointArn
    taskParams.replicationInstanceArn = data.replicationInstanceArn
    taskParams.tableMappings = data.tableMappings
    taskParams.replicationTaskArn = data.replicationTaskArn
  }

  if (data.taskType === 'DATASYNC') {
    taskParams.jsonFormat = data.jsonFormat
    taskParams.json = data.json
    taskParams.destinationLocationArn = data.destinationLocationArn
    taskParams.sourceLocationArn = data.sourceLocationArn
    taskParams.name = data.name
    taskParams.cloudWatchLogGroupArn = data.cloudWatchLogGroupArn
  }

  if (data.taskType === 'KUBEFLOW') {
    taskParams.yamlContent = data.yamlContent
    taskParams.namespace = data.namespace
  }

  if (data.taskType === 'LINKIS') {
    taskParams.useCustom = data.useCustom
    taskParams.paramScript = data.paramScript
    taskParams.rawScript = data.rawScript
  }

  if (data.taskType === 'DATA_FACTORY') {
    taskParams.factoryName = data.factoryName
    taskParams.resourceGroupName = data.resourceGroupName
    taskParams.pipelineName = data.pipelineName
  }

  if (data.taskType === 'REMOTESHELL') {
    taskParams.type = data.type
    taskParams.datasource = data.datasource
  }

  if (data.taskType === 'DYNAMIC') {
    taskParams.processDefinitionCode = data.processDefinitionCode
    taskParams.maxNumOfSubWorkflowInstances = data.maxNumOfSubWorkflowInstances
    taskParams.degreeOfParallelism = data.degreeOfParallelism
    taskParams.filterCondition = data.filterCondition
    taskParams.listParameters = data.listParameters
  }
  if (data.taskType == 'EXTRACT') {
    taskParams.sourceId = data.sourceId
    taskParams.targetDir = data.targetDir
    taskParams.sourceFile = data.sourceFile
    // taskParams.unarchiveType = data.unarchiveType
  }

  if (data.taskType == 'MINIO') {
    taskParams.sourceId = data.sourceId
  }

  // llxl 待进行数据处理
  if (data.taskType == 'JDBC_SOURCE') {
    taskParams.datasourceId = data.datasourceId
    taskParams.query = data.query
    taskParams.fetch_size = data.fetch_size
    taskParams.table_path = data.table_path
    taskParams.where_condition = data.where_condition
    taskParams.plugin_output = data.plugin_output
    taskParams.result_table_name = data.result_table_name
    taskParams.outputFields = data.outputFields
    taskParams.selectedColumns = data.selectedColumns
    taskParams.is_custom_sql = data.is_custom_sql
    taskParams.parallelism = data.parallelism

  }
   if (data.taskType == 'FILTER_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.plugin_input = data.plugin_input
    taskParams.plugin_output = data.plugin_output
    taskParams.exclude_fields = data.exclude_fields
    taskParams.include_fields = data.include_fields
  }
  if (data.taskType == 'JSONPATH_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.row_error_handle_way= data.row_error_handle_way
    taskParams.columns = data.columns
    taskParams.datasource = data.datasource
  }
  if (data.taskType == 'SQL_TRANSFORM') {
    taskParams.query = data.query
    taskParams.type = data.type
    taskParams.localParams = data.localParams
    taskParams.flag = data.flag
    taskParams.sql = data.sql
    taskParams.sqlType = data.sqlType
  }

  if (data.taskType == 'SQLFILTER_TRANSFORM') {

    const sqlfiltermapstr = data.sqlfiltermap?data.sqlfiltermap.map(item =>  {
      let val:any = item.type == 'string' ?`'${item.value}'`  : item.value
      val = item.status == 'is null' ||item.status == 'is not null'? '' : val
      return `${item.com.toUpperCase()} ${item.prop} ${item.status} ${val}`
     } ).join(' '):''

     let val = data.type_init == 'string'? `'${data.value_init}'`:data.value_init
    taskParams.query = `${data.prop_init} ${data.status_init} ${val} ${sqlfiltermapstr}`

    taskParams.sqlfiltermap = data.sqlfiltermap
    taskParams.prop_init = data.prop_init
    taskParams.status_init = data.status_init
    taskParams.value_init = data.value_init
    taskParams.type_init = data.type_init
    taskParams.datasource = data.datasource

  }


  if (data.taskType == 'SQLCASEWHEN_TRANSFORM') {
    let columnArray = []
    data.columns.forEach(element => {
      let obj = {};
      obj = { ...element, columnName: data.columnName, fieldType: data.fieldType }
      columnArray.push(obj)
    });
    console.log('SQLCASEWHEN_TRANSFORM>>>>', columnArray)
    taskParams.datasource = data.datasource
    taskParams.columnName = data.columnName
    taskParams.fieldType = data.fieldType
    taskParams.columns = columnArray
    taskParams.defaultValue = data.defaultValue
    taskParams.targetField = data.targetField
    taskParams.sql = data.sql
    taskParams.sqlType = data.sqlType
    taskParams.preStatements = data.preStatements
    taskParams.postStatements = data.postStatements
    taskParams.sendEmail = data.sendEmail
    taskParams.displayRows = data.displayRows
  }

  if (data.taskType == 'SPLITCOLUMNS_TRANSFORM') {
    console.log('SPLITCOLUMNS_TRANSFORM>>>>', data)
    taskParams.datasource = data.datasource
    taskParams.splitField = data.splitField
    taskParams.separator = data.separator
    taskParams.regex = data.regex ? '1' : '2'
    taskParams.newField = data.newField
    taskParams.sql = data.sql
    taskParams.sqlType = data.sqlType
    taskParams.preStatements = data.preStatements
    taskParams.postStatements = data.postStatements
    taskParams.sendEmail = data.sendEmail
    taskParams.displayRows = data.displayRows
  }

  if (data.taskType == 'SQLROWTOCOLUMN_TRANSFORM') {
    console.log('SQLROWTOCOLUMN_TRANSFORM>>>>', data)
    taskParams.datasource = data.datasource
    taskParams.groupField = data.groupField
    taskParams.targetFields = data.targetFields
    taskParams.sql = data.sql
    taskParams.sqlType = data.sqlType
    taskParams.preStatements = data.preStatements
    taskParams.postStatements = data.postStatements
    taskParams.sendEmail = data.sendEmail
    taskParams.displayRows = data.displayRows
  }

  if (data.taskType == 'SQLCOLUMNTOROW_TRANSFORM') {
    console.log('SQLCOLUMNTOROW_TRANSFORM>>>>', data)
    taskParams.datasource = data.datasource
    taskParams.columns = data.columns
    taskParams.sql = data.sql
    taskParams.sqlType = data.sqlType
    taskParams.preStatements = data.preStatements
    taskParams.postStatements = data.postStatements
    taskParams.sendEmail = data.sendEmail
    taskParams.displayRows = data.displayRows
  }

  if (data.taskType == 'SQLUNION_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.fields = data.fields
    taskParams.tableName = data.tableName
    data.columns = {
      tableName: data.tableName,
      fields: data.fields
    }
    console.log('SQLUNION_TRANSFORM>>>>', data.columns)
    taskParams.columns = data.columns
  }
  if (data.taskType == 'SQLUNIONSORT_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.fields = data.fields
    taskParams.tableName = data.tableName
    taskParams.sortFields = data.sortFields
    data.columns = {
      tableName: data.tableName,
      fields: data.fields,
      sortFields: data.sortFields
    }
    taskParams.columns = data.columns
  }

  if (data.taskType == 'RANGE_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.range_mappings = data.range_mappings
    taskParams.range_field = data.range_field
    taskParams.range_value_default = data.range_value_default
    taskParams.range_type= data.range_type
    taskParams.range_to_field_type = data.range_to_field_type
    taskParams.range_to_field= data.range_to_field
  }
  if (data.taskType == 'ENCRYPT_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.custom_list = data.custom_list
    taskParams.preStatements = data.preStatements
    taskParams.postStatements = data.postStatements
  }
  if (data.taskType == 'DESENSITIZE_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.custom_list = data.custom_list
    taskParams.preStatements = data.preStatements
    taskParams.postStatements = data.postStatements
  }
  if (data.taskType == 'DECRYPT_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.custom_list = data.custom_list
    taskParams.preStatements = data.preStatements
    taskParams.postStatements = data.postStatements
  }
  if (data.taskType == 'SPLIT_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.split_field = data.split_field
    taskParams.separator = data.separator
    taskParams.output_fields = data.output_field?.map(item => item.out_field)
    taskParams.output_field =data.output_field
    taskParams.preStatements = data.preStatements
    taskParams.postStatements = data.postStatements


  }
  if (data.taskType == 'SWITCHCASE_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.switch_field = data.switch_field
    taskParams.switch_field_value_default = data.switch_field_value_default
    taskParams.switch_case_mappings = data.switch_case_mappings
    taskParams.switch_target_field = data.switch_target_field
    taskParams.switch_target_field_type = data.switch_target_field_type
    taskParams.preStatements = data.preStatements
    taskParams.postStatements = data.postStatements
  }
  if (data.taskType == 'JAVA_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.source_code = data.source_code
    taskParams.compile_language = data.compile_language
    taskParams.compile_pattern = data.compile_pattern
    taskParams.absolute_path = data.absolute_path
    taskParams.preStatements = data.preStatements
    taskParams.postStatements = data.postStatements

  }
  if (data.taskType == 'JAVASCRIPT_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.source_code = data.source_code
    // taskParams.compile_language = data.compile_language
    // taskParams.compile_pattern = data.compile_pattern
    // taskParams.absolute_path = data.absolute_path
    taskParams.preStatements = data.preStatements
    taskParams.postStatements = data.postStatements
  }
  if (data.taskType == 'RANDOMNUMBER_TRANSFORM') {
    taskParams.field_name = data.field_name
    taskParams.datasource = data.datasource
    taskParams.min_number= data.min_number
    taskParams.max_number = data.max_number

  }
  if (data.taskType == 'SEQUENCE_TRANSFORM') {
    taskParams.datasource = data.datasource
    taskParams.field_name = data.field_name
    taskParams.start_value = data.start_value
    taskParams.add_value = data.add_value
    taskParams.max_value = data.max_value
  }

  if (data.taskType == 'JDBC_SINK') {
    taskParams.datasourceId = data.datasourceId
    taskParams.query = data.query

    taskParams.database = data.database
    taskParams.table = data.table
    taskParams.primary_keys = data.primary_keys
    taskParams.batch_size = data.batch_size
    taskParams.is_exactly_once = data.is_exactly_once
    taskParams.transaction_timeout_sec = data.transaction_timeout_sec
    taskParams.field_ide = data.field_ide
    taskParams.plugin_input = data.plugin_input
    taskParams.custom_sql = data.custom_sql
    taskParams.enable_upsert = data.enable_upsert
    taskParams.use_copy_statement = data.use_copy_statement
    taskParams.schema_save_mode = data.schema_save_mode
    taskParams.data_save_mode = data.data_save_mode
    taskParams.outputFields = data.outputFields
  }

  if (data.taskType == 'DORIS_SOURCE') {
    console.log('DORIS_SOURCE>>>>', data)
    taskParams.datasourceId = data.datasourceId
    taskParams.table = data.table
    taskParams.dorisReadField = data.dorisReadField
    taskParams.dorisFilterQuery = data.dorisFilterQuery
    taskParams.dorisBatchSize = data.dorisBatchSize
    taskParams.dorisExecMemLimit = data.dorisExecMemLimit
    taskParams.dorisRequestRetries = data.dorisRequestRetries
    taskParams.dorisRequestReadTimeoutMs = data.dorisRequestReadTimeoutMs
    taskParams.dorisRequestConnectTimeoutMs = data.dorisRequestConnectTimeoutMs
    taskParams.dorisRequestQueryTimeouts = data.dorisRequestQueryTimeouts
    taskParams.fetch_size = data.fetch_size
    taskParams.table_path = data.table_path
    taskParams.where_condition = data.where_condition
    taskParams.plugin_output = data.plugin_output
    taskParams.result_table_name = data.result_table_name
    taskParams.outputFields = data.outputFields
    taskParams.selectedColumns = data.selectedColumns
    taskParams.is_custom_sql = data.is_custom_sql
    taskParams.feHttpPort = data.feHttpPort
    taskParams.parallelism = data.parallelism
  }

  if (data.taskType == 'DORIS_SINK') {
    console.log('DORIS_SINK>>>>', data)
    taskParams.datasourceId = data.datasourceId

    taskParams.table = data.table
    taskParams.schemaSaveMode = data.schemaSaveMode
    taskParams.dataSaveMode = data.dataSaveMode
    taskParams.format = data.format
    taskParams.readJsonByLine = data.readJsonByLine
    taskParams.columnSeparator = data.columnSeparator
    taskParams.sinkEnableDelete = data.sinkEnableDelete
    taskParams.needsUnsupportedTypeCasting = data.needsUnsupportedTypeCasting
    taskParams.caseSensitive = data.caseSensitive
    taskParams.sink = data.sink
    taskParams.sinkEnable2pc = data.sinkEnable2pc
    taskParams.customSql = data.customSql
    taskParams.sinkCheckInterval = data.sinkCheckInterval
    taskParams.sinkMaxRetries = data.sinkMaxRetries
    taskParams.sinkBufferSize = data.sinkBufferSize
    taskParams.sinkBufferCount = data.sinkBufferCount
    taskParams.dorisBatchSize = data.dorisBatchSize

    taskParams.primary_keys = data.primary_keys
    taskParams.batch_size = data.batch_size
    taskParams.is_exactly_once = data.is_exactly_once
    taskParams.transaction_timeout_sec = data.transaction_timeout_sec
    taskParams.field_ide = data.field_ide
    taskParams.plugin_input = data.plugin_input
    taskParams.custom_sql = data.custom_sql
    taskParams.enable_upsert = data.enable_upsert
    taskParams.use_copy_statement = data.use_copy_statement
    taskParams.schema_save_mode = data.schema_save_mode
    taskParams.data_save_mode = data.data_save_mode
    taskParams.feHttpPort = data.feHttpPort
    taskParams.outputFields = data.outputFields
  }

  if (data.taskType == 'FILTER_TRANSFORM') {
    taskParams.include_fields = data.include_fields
  }
  if (data.taskType == 'JOIN_TRANSFORM') {
    taskParams.join_type = data.join_type
    taskParams.right_table = data.right_table
    taskParams.left_table = data.left_table
    taskParams.relation = data.relation
    taskParams.select_fields = data.select_fields
    taskParams.datasourceId = data.datasourceId
  }
  if (data.taskType == 'SQLJOIN_TRANSFORM') {
    taskParams.join_type = data.join_type
    taskParams.right_sql = data.right_sql
    taskParams.left_sql = data.left_sql
    taskParams.relation = data.relation
    taskParams.select_fields_left = data.select_fields_left
    taskParams.select_fields_right = data.select_fields_right
    taskParams.datasourceId = data.datasourceId
  }

  if (data.taskType == 'SQLFUNCTION_TRANSFORM') {
    taskParams.columns = data.columns
  }

  if (data.taskType == 'CONSTREFERENCE_TRANSFORM') {
    taskParams.field_name = data.field_name
    taskParams.variable = JSON.parse(data.variable)
    taskParams.table_name = data.table_name
  }

  if (data.taskType == 'SORT_TRANSFORM') {
    taskParams.sort_fields = data.sort_fields
    taskParams.table_name = data.table_name
  }

  if (data.taskType == 'DATADISTINCT_TRANSFORM') {
    taskParams.distinct_fields = data.distinct_fields
    taskParams.table_name = data.table_name
    taskParams.conds = data.conds
    taskParams.cond_type = data.cond_type
    taskParams.datasourceId = data.datasourceId
    taskParams.db_type = data.db_type
  }

  if (data.taskType == 'GROUP_TRANSFORM') {
    taskParams.group_fields = data.group_fields
    taskParams.aggregation_fields = data.aggregation_fields
    taskParams.table_name = data.table_name
  }

  if (data.taskType == 'KAFKA_SINK') {
    taskParams.datasourceId = data.datasourceId
    taskParams.topic = data.topic
    taskParams.semantics = data.semantics
    taskParams.is_partition = data.is_partition
    taskParams.partition = data.partition
    taskParams.partition_key_fields = data.partition_key_fields
    taskParams.format = data.format
    taskParams.field_delimiter = data.field_delimiter
    taskParams.parallelism = data.parallelism
    taskParams['kafka.config'] = data['kafka.config']
  }

  if (data.taskType == 'KAFKA_SOURCE') {
    taskParams.datasourceId = data.datasourceId
    taskParams.pattern = data.pattern
    taskParams.topic = data.topic
    taskParams['consumer.group'] = data['consumer.group']
    taskParams.start_mode = data.start_mode
    taskParams.format = data.format
    taskParams.debezium_record_table_filter = data.debezium_record_table_filter
    taskParams.schemaVo = data.schemaVo
    taskParams.format_error_handle_way = data.format_error_handle_way
    taskParams.field_delimiter = data.field_delimiter
    taskParams.parallelism = data.parallelism
    taskParams['kafka.config'] = data['kafka.config']
  }

  if (data.taskType == 'UDP_SOURCE') {
    taskParams.host = data.host
    taskParams.port = data.port
    taskParams.format = data.format
    taskParams.schemaVo = data.schemaVo
    taskParams.bufferSize = data.bufferSize
    taskParams.charset = data.charset
    taskParams.replyEnabled = data.replyEnabled
    taskParams.replyMessage = data.replyMessage
  }

  if (data.taskType == 'HTTP_SOURCE') {
    taskParams.url = data.url
    taskParams.method = data.method
    taskParams.httpParams = data.httpParams
    taskParams.httpHeaders = data.httpHeaders
    taskParams.body = data.body
    taskParams.format = data.format
    taskParams.schemaVo = data.schemaVo
    taskParams.jsonField = data.jsonField
    taskParams.pageingVo = data.pageingVo
    taskParams.httpExtra = data.httpExtra
    taskParams.jsonFieldVo = data.jsonFieldVo
  }

  if (data.taskType == 'HTTP_SINK') {
    taskParams.url = data.url
    taskParams.httpHeaders = data.httpHeaders
    taskParams.connectTimeoutMs = data.connectTimeoutMs
    taskParams.socketTimeoutMs = data.socketTimeoutMs
    taskParams.retry = data.retry
    taskParams.retryBackoffMultiplierMs = data.retryBackoffMultiplierMs
    taskParams.retryBackoffMaxMs = data.retryBackoffMaxMs
  }

  if (data.taskType == 'SOCKETSERVER_SOURCE') {
    taskParams.datasourceId = data.datasourceId
    taskParams.charset = data.charset
    taskParams.replyEnabled = data.replyEnabled
    taskParams.format = data.format
    taskParams.schemaVo = data.schemaVo
    taskParams.replyMessage = data.replyMessage
    taskParams.delimiter = data.delimiter
  }

  if (data.taskType == 'HTTPSERVER_SOURCE') {
    taskParams.datasourceId = data.datasourceId
    taskParams.format = data.format
    taskParams.schemaVo = data.schemaVo
  }

  if (data.taskType == 'UDP_SINK') {
    taskParams.host = data.host
    taskParams.port = data.port
    taskParams.maxRetries = data.maxRetries
    taskParams.timeoutMs = data.timeoutMs
    taskParams.retryIntervalMs = data.retryIntervalMs
    taskParams.enableMessageReply = data.enableMessageReply
    taskParams.asyncEnabled = data.asyncEnabled
    taskParams.asyncThreads = data.asyncThreads
  }

  if (data.taskType == 'UDPMULTICAST_SOURCE') {
    taskParams.datasourceId = data.datasourceId
    taskParams.charsetName = data.charsetName
    taskParams.replyEnabled = data.replyEnabled
    taskParams.replyMessage = data.replyMessage
    taskParams.format = data.format
    taskParams.schemaVo = data.schemaVo
    taskParams.otherConfig = data.otherConfig
  }

  if (data.taskType == 'UDPMULTICAST_SINK') {
    taskParams.groupAddress = data.groupAddress
    taskParams.datasourceId = data.datasourceId
    taskParams.port = data.port
    taskParams.maxRetries = data.maxRetries
    taskParams.soTimeout = data.soTimeout
    taskParams.retryIntervalMs = data.retryIntervalMs
    taskParams.asyncEnabled = data.asyncEnabled
    taskParams.asyncThreads = data.asyncThreads
    taskParams.otherConfig = data.otherConfig
  }

  //todo
  if (data.taskType == 'LOCALFILE_SOURCE') {
    taskParams.path = data.path
    taskParams.file_format_type = data.file_format_type
    taskParams.schemaVo = data.schemaVo
    taskParams.field_delimiter = data.field_delimiter
    taskParams.parse_partition_from_path = data.parse_partition_from_path
    taskParams.skip_header_row_number = data.skip_header_row_number
    taskParams.sheet_name = data.sheet_name
    taskParams.excel_engine = data.excel_engine
    taskParams.advanced = data.advanced
    taskParams.file_filter_pattern = data.file_filter_pattern
    taskParams.filename_extension = data.filename_extension
    taskParams.date_format = data.date_format
    taskParams.time_format = data.time_format
    taskParams.datetime_format = data.datetime_format
    taskParams.compress_codec = data.compress_codec
    taskParams.archive_compress_codec = data.archive_compress_codec
    taskParams.encoding = data.encoding
    taskParams.null_format = data.null_format
    taskParams.xml_row_tag = data.xml_row_tag
    taskParams.xml_use_attr_format = data.xml_use_attr_format
    taskParams.csv_use_header_line = data.csv_use_header_line
  }
  if (data.taskType == 'LOCALFILE_SINK') {
    taskParams.path = data.path
    taskParams.file_format_type = data.file_format_type
    taskParams.batch_size = data.batch_size
    taskParams.create_empty_file_when_no_data = data.create_empty_file_when_no_data
    taskParams.row_delimiter = data.row_delimiter
    taskParams.enable_header_write = data.enable_header_write
    taskParams.sheet_name = data.sheet_name
    taskParams.max_rows_in_memory = data.max_rows_in_memory
    taskParams.advanced = data.advanced
    taskParams.tmp_path = data.tmp_path
    taskParams.custom_filename = data.custom_filename
    taskParams.file_name_expression = data.file_name_expression
    taskParams.filename_time_format = data.filename_time_format
    taskParams.have_partition = data.have_partition
    taskParams.partition_dir_expression = data.partition_dir_expression
    taskParams.is_partition_field_write_in_file = data.is_partition_field_write_in_file
    taskParams.sinkColumnsVo = data.sinkColumnsVo
    taskParams.compress_codec = data.compress_codec
    taskParams.encoding = data.encoding
  }

  if (data.taskType == 'HDFSFILE_SOURCE') {
    taskParams.datasourceId = data.datasourceId
    taskParams.path = data.path
    taskParams.file_format_type = data.file_format_type
    taskParams.schemaVo = data.schemaVo
    taskParams.field_delimiter = data.field_delimiter
    taskParams.skip_header_row_number = data.skip_header_row_number
    taskParams.sheet_name = data.sheet_name
    taskParams.advanced = data.advanced
    taskParams.file_filter_pattern = data.file_filter_pattern
    taskParams.filename_extension = data.filename_extension
    taskParams.date_format = data.date_format
    taskParams.time_format = data.time_format
    taskParams.datetime_format = data.datetime_format
    taskParams.compress_codec = data.compress_codec
    taskParams.archive_compress_codec = data.archive_compress_codec
    taskParams.encoding = data.encoding
    taskParams.null_format = data.null_format
  }

  if (data.taskType == 'HDFSFILE_SINK') {
    taskParams.datasourceId = data.datasourceId
    taskParams.path = data.path
    taskParams.file_format_type = data.file_format_type
    taskParams.batch_size = data.batch_size
    taskParams.field_delimiter = data.field_delimiter
    taskParams.create_empty_file_when_no_data = data.create_empty_file_when_no_data
    if (data.row_delimiter == '\n') {
      taskParams.row_delimiter = ''
    } else {
      taskParams.row_delimiter = data.row_delimiter
    }

    taskParams.enable_header_write = data.enable_header_write
    taskParams.sheet_name = data.sheet_name
    taskParams.max_rows_in_memory = data.max_rows_in_memory
    taskParams.advanced = data.advanced
    taskParams.tmp_path = data.tmp_path
    taskParams.custom_filename = data.custom_filename
    taskParams.file_name_expression = data.file_name_expression
    taskParams.filename_time_format = data.filename_time_format
    taskParams.have_partition = data.have_partition
    taskParams.partition_dir_expression = data.partition_dir_expression
    taskParams.is_partition_field_write_in_file = data.is_partition_field_write_in_file
    taskParams.sinkColumnsVo = data.sinkColumnsVo
    taskParams.compress_codec = data.compress_codec
    taskParams.encoding = data.encoding
  }

  if (data.taskType == 'FAKESOURCE_SOURCE') {
    taskParams['row.num'] = data['row.num']
    taskParams['schemaVo'] = data['schemaVo']
    taskParams['split.num'] = data['split.num']
    taskParams['split.read-interval'] = data['split.read-interval']
    taskParams['advanced'] = data['advanced']
    taskParams['map.size'] = data['map.size']
    taskParams['array.size'] = data['array.size']
    taskParams['bytes.length'] = data['bytes.length']
    taskParams['string.length'] = data['string.length']
    taskParams['string.fake.mode'] = data['string.fake.mode']
    taskParams['stringTemplateVo'] = data['stringTemplateVo']
    taskParams['tinyint.fake.mode'] = data['tinyint.fake.mode']
    taskParams['tinyint.min'] = data['tinyint.min']
    taskParams['tinyint.max'] = data['tinyint.max']
    taskParams['tinyintTemplateVo'] = data['tinyintTemplateVo']
    taskParams['smallint.fake.mode'] = data['smallint.fake.mode']
    taskParams['smallint.min'] = data['smallint.min']
    taskParams['smallint.max'] = data['smallint.max']
    taskParams['smallintTemplateVo'] = data['smallintTemplateVo']
    taskParams['int.fake.mode'] = data['int.fake.mode']
    taskParams['int.min'] = data['int.min']
    taskParams['int.max'] = data['int.max']
    taskParams['intTemplateVo'] = data['intTemplateVo']
    taskParams['bigint.fake.mode'] = data['bigint.fake.mode']
    taskParams['bigint.min'] = data['bigint.min']
    taskParams['bigint.max'] = data['bigint.max']
    taskParams['bigintTemplateVo'] = data['bigintTemplateVo']
    taskParams['float.fake.mode'] = data['float.fake.mode']
    taskParams['float.min'] = data['float.min']
    taskParams['float.max'] = data['float.max']
    taskParams['floatTemplateVo'] = data['floatTemplateVo']
    taskParams['double.fake.mode'] = data['double.fake.mode']
    taskParams['double.min'] = data['double.min']
    taskParams['double.max'] = data['double.max']
    taskParams['doubleTemplateVo'] = data['doubleTemplateVo']
    taskParams['vector.dimension'] = data['vector.dimension']
    taskParams['binary.vector.dimension'] = data['binary.vector.dimension']
    taskParams['vector.float.min'] = data['vector.float.min']
    taskParams['vector.float.max'] = data['vector.float.max']
  }

  if (data.taskType == 'FTPFILE_SOURCE') {
    taskParams.datasourceId = data.datasourceId
    taskParams.path = data.path
    taskParams.file_format_type = data.file_format_type
    taskParams.schemaVo = data.schemaVo
    taskParams.connection_mode = data.connection_mode
    taskParams.parse_partition_from_path = data.parse_partition_from_path
    taskParams.advanced = data.advanced
    taskParams.file_filter_pattern = data.file_filter_pattern
    taskParams.date_format = data.date_format
    taskParams.time_format = data.time_format
    taskParams.datetime_format = data.datetime_format
    taskParams.field_delimiter = data.field_delimiter
    taskParams.skip_header_row_number = data.skip_header_row_number
    taskParams.sheet_name = data.sheet_name
    taskParams.xml_row_tag = data.xml_row_tag
    taskParams.xml_use_attr_format = data.xml_use_attr_format
    taskParams.csv_use_header_line = data.csv_use_header_line
    taskParams.compress_codec = data.compress_codec
    taskParams.archive_compress_codec = data.archive_compress_codec
    taskParams.encoding = data.encoding
    taskParams.null_format = data.null_format
  }

  if (data.taskType == 'SFTPFILE_SOURCE') {
    taskParams.datasourceId = data.datasourceId
    taskParams.path = data.path
    taskParams.file_format_type = data.file_format_type
    taskParams.schemaVo = data.schemaVo
    taskParams.parse_partition_from_path = data.parse_partition_from_path
    taskParams.advanced = data.advanced
    taskParams.file_filter_pattern = data.file_filter_pattern
    taskParams.filename_extension = data.filename_extension
    taskParams.date_format = data.date_format
    taskParams.time_format = data.time_format
    taskParams.datetime_format = data.datetime_format
    taskParams.field_delimiter = data.field_delimiter
    taskParams.skip_header_row_number = data.skip_header_row_number
    taskParams.sheet_name = data.sheet_name
    taskParams.xml_row_tag = data.xml_row_tag
    taskParams.xml_use_attr_format = data.xml_use_attr_format
    taskParams.csv_use_header_line = data.csv_use_header_line
    taskParams.compress_codec = data.compress_codec
    taskParams.archive_compress_codec = data.archive_compress_codec
    taskParams.encoding = data.encoding
    taskParams.null_format = data.null_format
  }
  if (data.taskType == 'MINIOFILE_SOURCE') {
    taskParams.datasourceId = data.datasourceId
    taskParams.bucket = data.bucket
    taskParams.path = data.path
    taskParams.file_format_type = data.file_format_type
    taskParams.schemaVo = data.schemaVo
    taskParams.parse_partition_from_path = data.parse_partition_from_path
    taskParams.advanced = data.advanced
    taskParams.file_filter_pattern = data.file_filter_pattern
    taskParams.filename_extension = data.filename_extension
    taskParams.date_format = data.date_format
    taskParams.time_format = data.time_format
    taskParams.datetime_format = data.datetime_format
    taskParams.field_delimiter = data.field_delimiter
    taskParams.skip_header_row_number = data.skip_header_row_number
    taskParams.sheet_name = data.sheet_name
    taskParams.xml_row_tag = data.xml_row_tag
    taskParams.xml_use_attr_format = data.xml_use_attr_format
    taskParams.csv_use_header_line = data.csv_use_header_line
    taskParams.compress_codec = data.compress_codec
    taskParams.archive_compress_codec = data.archive_compress_codec
    taskParams.encoding = data.encoding
    taskParams.null_format = data.null_format
  }
  if (data.taskType == 'MINIOFILE_SINK') {
    taskParams.datasourceId = data.datasourceId
    taskParams.bucket = data.bucket
    taskParams.path = data.path
    taskParams.file_format_type = data.file_format_type
    taskParams.have_partition = data.have_partition
    taskParams.partitionByVo = data.partitionByVo
    taskParams.is_partition_field_write_in_file = data.is_partition_field_write_in_file
    taskParams.single_file_mode = data.single_file_mode
    taskParams.batch_size = data.batch_size
    taskParams.advanced = data.advanced
    taskParams.custom_filename = data.custom_filename
    taskParams.file_name_expression = data.file_name_expression
    taskParams.filename_time_format = data.filename_time_format
    taskParams.sinkColumnsVo = data.sinkColumnsVo
    taskParams.tmp_path = data.tmp_path
    taskParams.field_delimiter = data.field_delimiter
    taskParams.row_delimiter = data.row_delimiter
    taskParams.enable_header_write = data.enable_header_write
    taskParams.sheet_name = data.sheet_name
    taskParams.max_rows_in_memory = data.max_rows_in_memory
    taskParams.csv_string_quote_mode = data.csv_string_quote_mode
    taskParams.xml_root_tag = data.xml_root_tag
    taskParams.xml_row_tag = data.xml_row_tag
    taskParams.xml_use_attr_format = data.xml_use_attr_format
    taskParams.encoding = data.encoding
    taskParams.create_empty_file_when_no_data = data.create_empty_file_when_no_data
    taskParams.schema_save_mode = data.schema_save_mode
    taskParams.data_save_mode = data.data_save_mode
    taskParams.compress_codec = data.compress_codec
  }
  if (data.taskType == 'MYSQL_SINK') {
    taskParams.outputFields = data.outputFields
    taskParams.datasourceId = data.datasourceId
    taskParams.is_custom_sql = data.is_custom_sql
    taskParams.is_partition = data.is_partition
    taskParams.query = data.query
    taskParams.is_exactly_once = data.is_exactly_once
    taskParams.database = data.database
    taskParams.table = data.table
    taskParams.field_ide= data.field_ide
    taskParams.primary_keys = data.primary_keys
    taskParams.support_upsert_by_query_primary_key_exist = data.support_upsert_by_query_primary_key_exist
    taskParams.connection_check_timeout_sec = data.connection_check_timeout_sec
    taskParams.batch_size = data.batch_size
    taskParams.generate_sink_sql = data.generate_sink_sql
    taskParams.xa_data_source_class_name = data.xa_data_source_class_name
    taskParams.max_commit_attempts = data.max_commit_attempts
    taskParams.transaction_timeout_sec = data.transaction_timeout_sec
    taskParams.auto_commit = data.auto_commit
    taskParams.partition_column  = data.partition_column
    taskParams.field_name = data.field_name
    taskParams.properties = data.properties
    taskParams.max_retries = data.max_retries
    taskParams.schema_save_mode = data.schema_save_mode
    taskParams.data_save_mode = data.data_save_mode
    taskParams.custom_sql = data.custom_sql
    taskParams.enable_upsert = data.enable_upsert
    if(data.is_custom_sql){
      taskParams.table= ''
    }else{taskParams.query = ''}
    if(!data.is_partition){
      taskParams.primary_keys = []
      taskParams.support_upsert_by_query_primary_key_exist = ''
      taskParams.is_exactly_once= false
      taskParams.generate_sink_sql= false
      taskParams.auto_commit= true
      taskParams.batch_size= ''
      taskParams.max_retries= ''
      taskParams.properties= {}
    }
    if(taskParams.is_exactly_once){
      taskParams.xa_data_source_class_name= ''
      taskParams.max_commit_attempts= ''
      taskParams.transaction_timeout_sec= ''
    }
  }
  if (data.taskType == 'DAMENG_SINK') {
    taskParams.outputFields = data.outputFields
    taskParams.datasourceId = data.datasourceId
    taskParams.is_custom_sql = data.is_custom_sql
    taskParams.is_partition = data.is_partition
    taskParams.query = data.query
    taskParams.is_exactly_once = data.is_exactly_once
    taskParams.database = data.database
    taskParams.table = data.table
    taskParams.field_ide= data.field_ide
    taskParams.primary_keys = data.primary_keys
    taskParams.support_upsert_by_query_primary_key_exist = data.support_upsert_by_query_primary_key_exist
    taskParams.connection_check_timeout_sec = data.connection_check_timeout_sec
    taskParams.batch_size = data.batch_size
    taskParams.generate_sink_sql = data.generate_sink_sql
    taskParams.xa_data_source_class_name = data.xa_data_source_class_name
    taskParams.max_commit_attempts = data.max_commit_attempts
    taskParams.transaction_timeout_sec = data.transaction_timeout_sec
    taskParams.auto_commit = data.auto_commit
    taskParams.partition_column  = data.partition_column
    taskParams.field_name = data.field_name
    taskParams.properties = data.properties
    taskParams.max_retries = data.max_retries
    taskParams.schema_save_mode = data.schema_save_mode
    taskParams.data_save_mode = data.data_save_mode
    taskParams.custom_sql = data.custom_sql
    taskParams.enable_upsert = data.enable_upsert
    if(data.is_custom_sql){
      taskParams.table= ''
    }else{taskParams.query = ''}
    if(!data.is_partition){
      taskParams.primary_keys = []
      taskParams.support_upsert_by_query_primary_key_exist = ''
      taskParams.is_exactly_once= false
      taskParams.generate_sink_sql= false
      taskParams.auto_commit= true
      taskParams.batch_size= ''
      taskParams.max_retries= ''
      taskParams.properties= {}
    }
    if(taskParams.is_exactly_once){
      taskParams.xa_data_source_class_name= ''
      taskParams.max_commit_attempts= ''
      taskParams.transaction_timeout_sec= ''
    }
  }
   if (data.taskType == 'GAUSSDB_SINK') {
    taskParams.outputFields = data.outputFields
    taskParams.datasourceId = data.datasourceId
    taskParams.is_custom_sql = data.is_custom_sql
    taskParams.is_partition = data.is_partition
    taskParams.query = data.query
    taskParams.is_exactly_once = data.is_exactly_once
    taskParams.database = data.database
    taskParams.table = data.table
    taskParams.field_ide= data.field_ide
    taskParams.primary_keys = data.primary_keys
    taskParams.support_upsert_by_query_primary_key_exist = data.support_upsert_by_query_primary_key_exist
    taskParams.connection_check_timeout_sec = data.connection_check_timeout_sec
    taskParams.batch_size = data.batch_size
    taskParams.generate_sink_sql = data.generate_sink_sql
    taskParams.xa_data_source_class_name = data.xa_data_source_class_name
    taskParams.max_commit_attempts = data.max_commit_attempts
    taskParams.transaction_timeout_sec = data.transaction_timeout_sec
    taskParams.auto_commit = data.auto_commit
    taskParams.partition_column  = data.partition_column
    taskParams.field_name = data.field_name
    taskParams.properties = data.properties
    taskParams.max_retries = data.max_retries
    taskParams.schema_save_mode = data.schema_save_mode
    taskParams.data_save_mode = data.data_save_mode
    taskParams.custom_sql = data.custom_sql
    taskParams.enable_upsert = data.enable_upsert
    if(data.is_custom_sql){
      taskParams.table= ''
    }else{taskParams.query = ''}
    if(!data.is_partition){
      taskParams.primary_keys = []
      taskParams.support_upsert_by_query_primary_key_exist = ''
      taskParams.is_exactly_once= false
      taskParams.generate_sink_sql= false
      taskParams.auto_commit= true
      taskParams.batch_size= ''
      taskParams.max_retries= ''
      taskParams.properties= {}
    }
    if(taskParams.is_exactly_once){
      taskParams.xa_data_source_class_name= ''
      taskParams.max_commit_attempts= ''
      taskParams.transaction_timeout_sec= ''
    }
  }
  if (data.taskType == 'POSTGRESQL_SINK') {
    taskParams.outputFields = data.outputFields
    taskParams.datasourceId = data.datasourceId
    taskParams.is_custom_sql = data.is_custom_sql
    taskParams.is_partition = data.is_partition
    taskParams.query = data.query
    taskParams.is_exactly_once = data.is_exactly_once
    taskParams.database = data.database
    taskParams.table = data.table
    taskParams.field_ide= data.field_ide
    taskParams.primary_keys = data.primary_keys
    taskParams.support_upsert_by_query_primary_key_exist = data.support_upsert_by_query_primary_key_exist
    taskParams.connection_check_timeout_sec = data.connection_check_timeout_sec
    taskParams.batch_size = data.batch_size
    taskParams.generate_sink_sql = data.generate_sink_sql
    taskParams.xa_data_source_class_name = data.xa_data_source_class_name
    taskParams.max_commit_attempts = data.max_commit_attempts
    taskParams.transaction_timeout_sec = data.transaction_timeout_sec
    taskParams.auto_commit = data.auto_commit
    taskParams.partition_column  = data.partition_column
    taskParams.field_name = data.field_name
    taskParams.properties = data.properties
    taskParams.max_retries = data.max_retries
    taskParams.schema_save_mode = data.schema_save_mode
    taskParams.data_save_mode = data.data_save_mode
    taskParams.custom_sql = data.custom_sql
    taskParams.enable_upsert = data.enable_upsert
    if(data.is_custom_sql){
      taskParams.table= ''
    }else{taskParams.query = ''}
    if(!data.is_partition){
      taskParams.primary_keys = []
      taskParams.support_upsert_by_query_primary_key_exist = ''
      taskParams.is_exactly_once= false
      taskParams.generate_sink_sql= false
      taskParams.auto_commit= true
      taskParams.batch_size= ''
      taskParams.max_retries= ''
      taskParams.properties= {}
    }
    if(taskParams.is_exactly_once){
      taskParams.xa_data_source_class_name= ''
      taskParams.max_commit_attempts= ''
      taskParams.transaction_timeout_sec= ''
    }
  }
  if (data.taskType == 'MYSQL_SOURCE') {
    taskParams.outputFields = data.outputFields
    taskParams.datasourceId = data.datasourceId
    taskParams.query= data.query
    taskParams.partition_column = data.partition_column
    taskParams.partition_lower_bound = data.partition_lower_bound
    taskParams.partition_upper_bound = data.partition_upper_bound
    taskParams.fetch_size = data.fetch_size
    taskParams.table_path = data.table_path
    taskParams['split.size'] = data['split.size']
    taskParams.where_condition = {mysql:data.whereCondition}
    taskParams.whereCondition = data.whereCondition
    taskParams.connection_check_timeout_sec = data.connection_check_timeout_sec
    taskParams.partition_num = data.partition_num
    taskParams.properties = data.properties
    taskParams.is_partition = data.is_partition
    taskParams.is_custom_sql = data.is_custom_sql
    taskParams.selectedColumns = data.selectedColumns
    if(data.is_custom_sql){
      taskParams.table_path = ''
      taskParams.where_condition = ''
      taskParams.whereCondition = ''
    }else{taskParams.query = ''}
    if(!data.is_partition){
      taskParams.partition_column = ''
      taskParams.partition_lower_bound = ''
      taskParams.partition_upper_bound = ''
      taskParams['split.size'] = ''
      taskParams.partition_num = ''
      taskParams.fetch_size = ''
    }

  }
  if (data.taskType == 'POSTGRESQL_SOURCE') {
    taskParams.datasourceId = data.datasourceId
    taskParams.query= data.query
    taskParams.partition_column = data.partition_column
    taskParams.partition_lower_bound = data.partition_lower_bound
    taskParams.partition_upper_bound = data.partition_upper_bound
    taskParams.fetch_size = data.fetch_size
    taskParams.table_path = data.table_path
    taskParams['split.size'] = data['split.size']
    taskParams.where_condition = {mysql:data.where_condition}
    taskParams.whereCondition = data.whereCondition
    taskParams.connection_check_timeout_sec = data.connection_check_timeout_sec
    taskParams.partition_num = data.partition_num
    taskParams.properties = data.properties
    taskParams['split.even-distribution.factor.lower-bound'] = data['split.even-distribution.factor.lower-bound']
    taskParams['split.even-distribution.factor.upper-bound'] = data['split.even-distribution.factor.upper-bound']
    taskParams['split.sample-sharding.threshold'] = data['split.sample-sharding.threshold']
    taskParams['split.inverse-sampling.rate'] = data['split.inverse-sampling.rate']
    taskParams.is_partition = data.is_partition
    taskParams.is_custom_sql = data.is_custom_sql
    taskParams.selectedColumns = data.selectedColumns
    if(data.is_custom_sql){
      taskParams.table_path = ''
      taskParams.where_condition = ''
      taskParams.whereCondition = ''
    }else{taskParams.query = ''}
    if(!data.is_partition){
      taskParams.partition_column = ''
      taskParams.partition_lower_bound = ''
      taskParams.partition_upper_bound = ''
      taskParams['split.size'] = ''
      taskParams.partition_num = ''
      taskParams.fetch_size = ''
      taskParams['split.even-distribution.factor.lower-bound'] = ''
      taskParams['split.even-distribution.factor.upper-bound']= ''
      taskParams['split.sample-sharding.threshold']  = ''
      taskParams['split.inverse-sampling.rate']  = ''
    }
  }
   if (data.taskType == 'DAMENG_SOURCE') {
    taskParams.outputFields = data.outputFields
    taskParams.datasourceId = data.datasourceId
    taskParams.query= data.query
    taskParams.partition_column = data.partition_column
    taskParams.partition_lower_bound = data.partition_lower_bound
    taskParams.partition_upper_bound = data.partition_upper_bound
    taskParams.fetch_size = data.fetch_size
    taskParams.table_path = data.table_path
    taskParams['split.size'] = data['split.size']
    taskParams.where_condition = {mysql:data.where_condition}
    taskParams.whereCondition = data.whereCondition
    taskParams.connection_check_timeout_sec = data.connection_check_timeout_sec
    taskParams.partition_num = data.partition_num
    taskParams.properties = data.properties
    taskParams['split.even-distribution.factor.lower-bound'] = data['split.even-distribution.factor.lower-bound']
    taskParams['split.even-distribution.factor.upper-bound'] = data['split.even-distribution.factor.upper-bound']
    taskParams['split.sample-sharding.threshold'] = data['split.sample-sharding.threshold']
    taskParams['split.inverse-sampling.rate'] = data['split.inverse-sampling.rate']
    taskParams.is_partition = data.is_partition
    taskParams.is_custom_sql = data.is_custom_sql
    taskParams.selectedColumns = data.selectedColumns
    if(data.is_custom_sql){
      taskParams.table_path = ''
      taskParams.where_condition = ''
      taskParams.whereCondition = ''
    }else{taskParams.query = ''}
    if(!data.is_partition){
      taskParams.partition_column = ''
      taskParams.partition_lower_bound = ''
      taskParams.partition_upper_bound = ''
      taskParams['split.size'] = ''
      taskParams.partition_num = ''
      taskParams.fetch_size = ''
      taskParams['split.even-distribution.factor.lower-bound'] = ''
      taskParams['split.even-distribution.factor.upper-bound']= ''
      taskParams['split.sample-sharding.threshold']  = ''
      taskParams['split.inverse-sampling.rate']  = ''
    }
  }
  if (data.taskType == 'GAUSSDB_SOURCE') {
    taskParams.datasourceId = data.datasourceId
    taskParams.query= data.query
    taskParams.partition_column = data.partition_column
    taskParams.partition_lower_bound = data.partition_lower_bound
    taskParams.partition_upper_bound = data.partition_upper_bound
    taskParams.fetch_size = data.fetch_size
    taskParams.table_path = data.table_path
    taskParams['split.size'] = data['split.size']
    taskParams.where_condition = {mysql:data.where_condition}
    taskParams.whereCondition = data.whereCondition
    taskParams.connection_check_timeout_sec = data.connection_check_timeout_sec
    taskParams.partition_num = data.partition_num
    taskParams.properties = data.properties
    taskParams['split.even-distribution.factor.lower-bound'] = data['split.even-distribution.factor.lower-bound']
    taskParams['split.even-distribution.factor.upper-bound'] = data['split.even-distribution.factor.upper-bound']
    taskParams['split.sample-sharding.threshold'] = data['split.sample-sharding.threshold']
    taskParams['split.inverse-sampling.rate'] = data['split.inverse-sampling.rate']
    taskParams.is_partition = data.is_partition
    taskParams.is_custom_sql = data.is_custom_sql
    taskParams.selectedColumns = data.selectedColumns
    if(data.is_custom_sql){
      taskParams.table_path = ''
      taskParams.where_condition = ''
      taskParams.whereCondition = ''
    }else{taskParams.query = ''}
    if(!data.is_partition){
      taskParams.partition_column = ''
      taskParams.partition_lower_bound = ''
      taskParams.partition_upper_bound = ''
      taskParams['split.size'] = ''
      taskParams.partition_num = ''
      taskParams.fetch_size = ''
      taskParams['split.even-distribution.factor.lower-bound'] = ''
      taskParams['split.even-distribution.factor.upper-bound']= ''
      taskParams['split.sample-sharding.threshold']  = ''
      taskParams['split.inverse-sampling.rate']  = ''
    }
  }
  if (data.taskType == 'HIVE_SINK') {
    taskParams.metastore_uri = data.metastore_uri
    taskParams.table_name = data.table_name
    taskParams.hdfs_site_path = data.hdfs_site_path
    taskParams.hive_site_path = data.hive_site_path
    taskParams.advanced = data.advanced
    taskParams.hiveHadoopConfVo = data.hiveHadoopConfVo
    taskParams['hive.hadoop.conf-path'] = data['hive.hadoop.conf-path']
    taskParams.krb5_path = data.krb5_path
    taskParams.kerberos_principal = data.kerberos_principal
    taskParams.kerberos_keytab_path = data.kerberos_keytab_path
    taskParams.abort_drop_partition_metadata = data.abort_drop_partition_metadata
    taskParams.compress_codec = data.compress_codec
  }
  if (data.taskType === 'FLEXIBLE_HTTP') {
    taskParams.httpMethod = data.httpMethod
    //TODO 将别名信息放到自定义参数结构中，减少后台改动
    taskParams.alias = data.alias
    taskParams.httpBody = data.httpBody
    taskParams.httpCheckCondition = data.httpCheckCondition
    taskParams.httpParams = data.httpParams
    taskParams.url = data.url
    taskParams.condition = data.condition
    taskParams.connectTimeout = data.connectTimeout
    taskParams.socketTimeout = data.socketTimeout
    taskParams.flexibleHttpTaskType = data.flexibleHttpTaskType
    taskParams.loopConditionField = data.loopConditionField
    taskParams.loopConditionValue = data.loopConditionValue
    taskParams.loopInterval = data.loopInterval
    taskParams.maxLoopNum = data.maxLoopNum
    taskParams.outputParameters = data.outputParameters
    taskParams.iterateParam = data.iterateParam
    taskParams.iterateParamPath = data.iterateParamPath
    taskParams.otherParams = data.otherParams
    taskParams.templateId = data.templateId
    taskParams.trustAllCerts = data.trustAllCerts
    taskParams.sslProtocol = data.sslProtocol
    taskParams.cipherSuites = data.cipherSuites
    taskParams.templateId = data.templateId
    taskParams.expressionConfig = data.expressionConfig
    taskParams.bodyExpression = data.expressionConfig.bodyExpression
    taskParams.headersExpression = data.expressionConfig.headersExpression
    taskParams.paramsExpression = data.expressionConfig.paramsExpression
    taskParams.pathExpression = data.expressionConfig.pathExpression
    taskParams.isOpen = false
    taskParams.isLog = data.isLog
    taskParams.logUrl = data.logUrl
  }
  if (data.taskType === 'FLEXIBLE_API') {
    taskParams.httpMethod = data.httpMethod
    //TODO 将别名信息放到自定义参数结构中，减少后台改动
    taskParams.alias = data.alias
     taskParams.url = data.url
    taskParams.httpBody = data.httpBody
    taskParams.httpCheckCondition = data.httpCheckCondition
    taskParams.httpParams = data.httpParams
    taskParams.condition = data.condition
    taskParams.flexibleHttpTaskType = data.flexibleHttpTaskType
    taskParams.otherParams = data.otherParams
  }
  let timeoutNotifyStrategy = ''
  if (data.timeoutNotifyStrategy) {
    if (data.timeoutNotifyStrategy.length === 1) {
      timeoutNotifyStrategy = data.timeoutNotifyStrategy[0]
    }
    if (data.timeoutNotifyStrategy.length === 2) {
      timeoutNotifyStrategy = 'WARNFAILED'
    }
  }
  const params = {
    processDefinitionCode: data.processName ? String(data.processName) : '',
    upstreamCodes: data?.preTasks?.join(','),
    taskDefinitionJsonObj: {
      code: data.code,
      delayTime: data.delayTime ? String(data.delayTime) : '0',
      description: data.description,
      environmentCode: data.environmentCode || -1,
      failRetryInterval: data.failRetryInterval
        ? String(data.failRetryInterval)
        : '0',
      failRetryTimes: data.failRetryTimes ? String(data.failRetryTimes) : '0',
      flag: data.flag,
      isCache: data.isCache ? 'YES' : 'NO',
      name: data.name,
      taskGroupId: data.taskGroupId,
      taskGroupPriority: data.taskGroupPriority,
      taskParams: {
        localParams: data.localParams?.map((item: any) => {
          item.value = item.value || ''
          return item
        }),
        initScript: data.initScript,
        rawScript: data.rawScript,
        resourceList: data.resourceList?.length
          ? data.resourceList.map((fullName: string) => ({
            resourceName: `${fullName}`
          }))
          : [],
        ...taskParams
      },
      taskPriority: data.taskPriority,
      taskType: data.taskType,
      timeout: data.timeoutFlag ? data.timeout : 0,
      timeoutFlag: data.timeoutFlag ? 'OPEN' : 'CLOSE',
      timeoutNotifyStrategy: data.timeoutFlag ? timeoutNotifyStrategy : '',
      workerGroup: data.workerGroup,
      cpuQuota: data.cpuQuota || -1,
      memoryMax: data.memoryMax || -1,
      taskExecuteType: data.taskExecuteType
    }
  } as {
    processDefinitionCode: string
    upstreamCodes: string
    taskDefinitionJsonObj: { timeout: number; timeoutNotifyStrategy: string }
  }
  if (!data.timeoutFlag) {
    params.taskDefinitionJsonObj.timeout = 0
    params.taskDefinitionJsonObj.timeoutNotifyStrategy = ''
  }
  return params
}

export function formatModel(data: ITaskData) {
  const params = {
    ...omit(data, [
      'environmentCode',
      'timeoutFlag',
      'timeoutNotifyStrategy',
      'taskParams'
    ]),
    ...omit(data.taskParams, ['resourceList', 'mainJar', 'localParams']),
    environmentCode: data.environmentCode === -1 ? null : data.environmentCode,
    timeoutFlag: data.timeoutFlag === 'OPEN',
    isCache: data.isCache === 'YES',
    timeoutNotifyStrategy: data.timeoutNotifyStrategy
      ? [data.timeoutNotifyStrategy]
      : [],
    localParams: data.taskParams?.localParams || []
  } as INodeData

  if (params.localParams?.length == 0 && data.taskType == "SEATUNNEL" && !data.taskParams) {
    params.localParams = [
      { prop: 'parallelism', value: '2', direct: "IN", type: "VARCHAR" },
      { prop: 'job.mode', value: 'BATCH', direct: "IN", type: "VARCHAR" },
      { prop: 'checkpoint.interval', value: '10000', direct: "IN", type: "VARCHAR" }
    ]
  }

  if (data.timeoutNotifyStrategy === 'WARNFAILED') {
    params.timeoutNotifyStrategy = ['WARN', 'FAILED']
  }
  if (data.taskParams?.resourceList) {
    params.resourceList = data.taskParams.resourceList.map(
      (item: { resourceName: string }) => `${item.resourceName}`
    )
  }
  if (data.taskParams?.mainJar) {
    params.mainJar = data.taskParams?.mainJar.resourceName
  }

  if (data.taskParams?.method) {
    params.method = data.taskParams?.method
  }

  if (data.taskParams?.targetParams) {
    const targetParams: ISqoopTargetParams = JSON.parse(
      data.taskParams.targetParams
    )
    params.targetType = data.taskParams.targetType
    params.targetHiveDatabase = targetParams.hiveDatabase
    params.targetHiveTable = targetParams.hiveTable
    params.targetHiveCreateTable = targetParams.createHiveTable
    params.targetHiveDropDelimiter = targetParams.dropDelimiter
    params.targetHiveOverWrite =
      targetParams.hiveOverWrite === void 0 ? true : targetParams.hiveOverWrite
    params.targetHiveTargetDir = targetParams.hiveTargetDir
    params.targetHiveReplaceDelimiter = targetParams.replaceDelimiter
    params.targetHivePartitionKey = targetParams.hivePartitionKey
    params.targetHivePartitionValue = targetParams.hivePartitionValue
    params.targetHdfsTargetPath = targetParams.targetPath
    params.targetHdfsDeleteTargetDir =
      targetParams.deleteTargetDir === void 0
        ? true
        : targetParams.deleteTargetDir
    params.targetHdfsCompressionCodec =
      targetParams.compressionCodec === void 0
        ? 'snappy'
        : targetParams.compressionCodec
    params.targetHdfsFileType =
      targetParams.fileType === void 0
        ? '--as-avrodatafile'
        : targetParams.fileType
    params.targetHdfsFieldsTerminated = targetParams.fieldsTerminated
    params.targetHdfsLinesTerminated = targetParams.linesTerminated
    params.targetMysqlType = targetParams.targetType
    params.targetMysqlDatasource = targetParams.targetDatasource
    params.targetMysqlTable = targetParams.targetTable
    params.targetMysqlColumns = targetParams.targetColumns
    params.targetMysqlFieldsTerminated = targetParams.fieldsTerminated
    params.targetMysqlLinesTerminated = targetParams.linesTerminated
    params.targetMysqlIsUpdate = targetParams.isUpdate
    params.targetMysqlTargetUpdateKey = targetParams.targetUpdateKey
    params.targetMysqlUpdateMode =
      targetParams.targetUpdateMode === void 0
        ? 'allowinsert'
        : targetParams.targetUpdateMode
  }
  if (data.taskParams?.sourceParams) {
    const sourceParams: ISqoopSourceParams = JSON.parse(
      data.taskParams.sourceParams
    )
    params.srcTable = sourceParams.srcTable
    params.srcColumnType = sourceParams.srcColumnType
    params.srcColumns = sourceParams.srcColumns
    params.sourceMysqlSrcQuerySql = sourceParams.srcQuerySql
    params.srcQueryType = sourceParams.srcQueryType
    params.sourceMysqlType = sourceParams.srcType
    params.sourceMysqlDatasource = sourceParams.srcDatasource
    params.mapColumnHive = sourceParams.mapColumnHive || []
    params.mapColumnJava = sourceParams.mapColumnJava || []
    params.sourceHdfsExportDir = sourceParams.exportDir
    params.sourceHiveDatabase = sourceParams.hiveDatabase
    params.sourceHiveTable = sourceParams.hiveTable
    params.sourceHivePartitionKey = sourceParams.hivePartitionKey
    params.sourceHivePartitionValue = sourceParams.hivePartitionValue
  }

  if (data.taskParams?.rawScript) {
    params.rawScript = data.taskParams?.rawScript
  }

  if (data.taskParams?.initScript) {
    params.initScript = data.taskParams?.initScript
  }

  if (data.taskParams?.switchResult) {
    params.switchResult = data.taskParams.switchResult
    params.dependTaskList = data.taskParams.switchResult?.dependTaskList
      ? data.taskParams.switchResult?.dependTaskList
      : []
    params.nextNode = data.taskParams.switchResult?.nextNode
  }

  if (data.taskParams?.dependence) {
    const dependence: IDependentParameters = JSON.parse(
      JSON.stringify(data.taskParams.dependence)
    )
    params.checkInterval = dependence.checkInterval
    params.failurePolicy = dependence.failurePolicy
    params.failureWaitingTime = dependence.failureWaitingTime
    params.dependTaskList = dependence.dependTaskList || []
    params.relation = dependence.relation
  }

  if (data.taskParams?.ruleInputParameter) {
    params.check_type = data.taskParams.ruleInputParameter.check_type
    params.comparison_execute_sql =
      data.taskParams.ruleInputParameter.comparison_execute_sql
    params.comparison_type = data.taskParams.ruleInputParameter.comparison_type
    params.comparison_name = data.taskParams.ruleInputParameter.comparison_name
    params.failure_strategy =
      data.taskParams.ruleInputParameter.failure_strategy
    params.operator = data.taskParams.ruleInputParameter.operator
    params.src_connector_type =
      data.taskParams.ruleInputParameter.src_connector_type
    params.src_datasource_id =
      data.taskParams.ruleInputParameter.src_datasource_id
    params.src_database = data.taskParams.ruleInputParameter.src_database
    params.src_table = data.taskParams.ruleInputParameter.src_table
    params.field_length = data.taskParams.ruleInputParameter.field_length
    params.begin_time = data.taskParams.ruleInputParameter.begin_time
    params.deadline = data.taskParams.ruleInputParameter.deadline
    params.datetime_format = data.taskParams.ruleInputParameter.datetime_format
    params.target_filter = data.taskParams.ruleInputParameter.target_filter
    params.regexp_pattern = data.taskParams.ruleInputParameter.regexp_pattern
    params.enum_list = data.taskParams.ruleInputParameter.enum_list
    params.src_filter = data.taskParams.ruleInputParameter.src_filter
    params.src_field = data.taskParams.ruleInputParameter.src_field
    params.statistics_execute_sql =
      data.taskParams.ruleInputParameter.statistics_execute_sql
    params.statistics_name = data.taskParams.ruleInputParameter.statistics_name
    params.target_connector_type =
      data.taskParams.ruleInputParameter.target_connector_type
    params.target_datasource_id =
      data.taskParams.ruleInputParameter.target_datasource_id
    params.target_database = data.taskParams.ruleInputParameter.target_database
    params.target_table = data.taskParams.ruleInputParameter.target_table
    params.threshold = data.taskParams.ruleInputParameter.threshold
    if (data.taskParams.ruleInputParameter.mapping_columns)
      params.mapping_columns = JSON.parse(
        data.taskParams.ruleInputParameter.mapping_columns
      )
  }
  if (data.taskParams?.sparkParameters) {
    params.deployMode = data.taskParams.sparkParameters.deployMode
    params.driverCores = data.taskParams.sparkParameters.driverCores
    params.driverMemory = data.taskParams.sparkParameters.driverMemory
    params.executorCores = data.taskParams.sparkParameters.executorCores
    params.executorMemory = data.taskParams.sparkParameters.executorMemory
    params.numExecutors = data.taskParams.sparkParameters.numExecutors
    params.others = data.taskParams.sparkParameters.others
    params.sqlExecutionType = data.taskParams.sparkParameters.sqlExecutionType
  }

  if (data.taskParams?.conditionResult?.successNode?.length) {
    params.successBranch = data.taskParams.conditionResult.successNode[0]
  }
  if (data.taskParams?.conditionResult?.failedNode?.length) {
    params.failedBranch = data.taskParams.conditionResult.failedNode[0]
  }
  if (data.taskParams?.udfs) {
    params.udfs = data.taskParams.udfs?.split(',')
  }
  if (data.taskParams?.customConfig !== void 0) {
    params.customConfig = data.taskParams.customConfig === 1 ? true : false
  }
  if (data.taskParams?.jobType) {
    params.isCustomTask = data.taskParams.jobType === 'CUSTOM'
  }

  return params
}
