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

import { useFlink } from './use-flink'
import { useFlinkStream } from './use-flink-stream'
import { useShell } from './use-shell'
import { useSubProcess } from './use-sub-process'
import { usePigeon } from './use-pigeon'
import { usePython } from './use-python'
import { useSpark } from './use-spark'
import { useMr } from './use-mr'
import { useHttp } from './use-http'
import { useAsyncstate } from './use-asyncstate'
import { useWebhook } from './use-webhook' 
import { useSql } from './use-sql'
import { useProcedure } from './use-procedure'
import { useSqoop } from './use-sqoop'
import { useSeaTunnel } from './use-sea-tunnel'
import { useSwitch } from './use-switch'
import { useConditions } from './use-conditions'
import { useDataX } from './use-datax'
import { useDependent } from './use-dependent'
import { useDataQuality } from './use-data-quality'
import { useEmr } from './use-emr'
import { useZeppelin } from './use-zeppelin'
import { useK8s } from './use-k8s'
import { useJupyter } from './use-jupyter'
import { useMlflow } from './use-mlflow'
import { useOpenmldb } from './use-openmldb'
import { useDvc } from './use-dvc'
import { useJava } from './use-java'
import { useDinky } from './use-dinky'
import { userSagemaker } from './use-sagemaker'
import { useChunjun } from './use-chunjun'
import { usePytorch } from './use-pytorch'
import { useHiveCli } from './use-hive-cli'
import { useDms } from './use-dms'
import { useDatasync } from './use-datasync'
import { useKubeflow } from './use-kubeflow'
import { useLinkis } from './use-linkis'
import { useDataFactory } from './use-data-factory'
import { useRemoteShell } from './use-remote-shell'
import { useDynamic } from './use-dynamic'
import { useJdbcSource } from './seaTunnel/source/use-jdbc-source'
import { useJdbcSink } from './seaTunnel/sink/use-jdbc-sink'
import { useFilterTransform } from './seaTunnel/transform/use-filter-transform'
import { useJsonpathTransform } from './seaTunnel/transform/use-jsonpath-transform'
import { useRangeTransform } from './seaTunnel/transform/use-range-transform'
import { useJoinTransform } from './seaTunnel/transform/use-Join-transform'
import { useKafkaSource } from './seaTunnel/source/use-kafka-source'
import { useKafkaSink } from './seaTunnel/sink/use-kafka-sink'
import { useSqlFunctionTransform } from './seaTunnel/transform/use-sql-function-transform'
import { useSqlfilterTransform } from './seaTunnel/transform/use-sqlfilter-transform'
import { useEncryptTransform } from './seaTunnel/transform/use-encrypt-transform'
import { useDesensitizeTransform } from './seaTunnel/transform/use-desensitize-transform'
import { useDecryptTransform } from './seaTunnel/transform/use-decrypt-transform'
import { useSplitTransform } from './seaTunnel/transform/use-split-transform'
import { useSwitchcaseTransform } from './seaTunnel/transform/use-switchcass-transform'
import { useJavaTransform } from './seaTunnel/transform/use-java-transform'
import { useJavascriptTransform } from './seaTunnel/transform/use-javascript-transform'
import { useRandomnumberTransform } from './seaTunnel/transform/use-randomnumber-transform'
import { useIncreasesequenceTransform } from './seaTunnel/transform/use-increasesequence-transform'
import { useUdpSource } from './seaTunnel/source/use-udp-source'
import { useUdpSink } from './seaTunnel/sink/use-udp-sink'
import { useSqlcasewhenTransform } from './seaTunnel/transform/use-sqlcasewhen-transform'
import { useSplitcolumnsTransform } from './seaTunnel/transform/use-splitcolumns-transform'
import { useSortTransform } from './seaTunnel/transform/use-sort-transform'
import { useDataDistinctTransform } from './seaTunnel/transform/use-data-distinct-transform'
import { useUnarchive } from './use-unarchive'

import { useHttpSource } from './seaTunnel/source/use-http-source'
import { useHttpSink } from './seaTunnel/sink/use-http-sink'
import { useLocalFileSource } from './seaTunnel/source/use-localfile-source'
import { useLocalFileSink } from './seaTunnel/sink/use-localfile-sink'
import { useSocketServerSource } from './seaTunnel/source/use-socket-server-source'
import { useConstReferenceTransform } from './seaTunnel/transform/use-const-reference-transform'
import { useGroupTransform } from './seaTunnel/transform/use-group-transform'
import { useHttpServerSource } from './seaTunnel/source/use-http-server-source'
import { useFakeSourceSource } from './seaTunnel/source/use-fakesource-source'
import { useUdpMulticastSink } from './seaTunnel/sink/use-udp-multicast-sink'
import { useUdpMulticastSource } from './seaTunnel/source/use-udp-multicast-source'
import { useFtpFileSource } from './seaTunnel/source/use-ftpfile-source'
import { useSftpFileSource } from './seaTunnel/source/use-sftpfile-source'
import { useHdfsfileSource } from './seaTunnel/source/use-hdfsfile-source'
import { useHdfsfileSink } from './seaTunnel/sink/use-hdfsfile-sink'
import { useMinioFileSource } from './seaTunnel/source/use-miniofile-source'
import { useMinioFileSink } from './seaTunnel/sink/use-miniofile-sink'
import { useHiveSink } from './seaTunnel/sink/use-hive-sink'
import { useMysqlSink } from './seaTunnel/sink/use-mysql-sink'
import { useMysqlSource } from './seaTunnel/source/use-mysql-source'
import { usePostgreSQLSource } from './seaTunnel/source/use-postgresql-source'
import { usePostgreSQLSink } from './seaTunnel/sink/use-postgresql-sink'
import { useDamengSource } from './seaTunnel/source/use-dameng-source'
import { useDamengSink } from './seaTunnel/sink/use-dameng-sink'
import { useGaussdbSource} from './seaTunnel/source/use-gaussdb-source'
import { useGaussdbSink } from './seaTunnel/sink/use-gaussdb-sink'
import { useSqlTransform } from './seaTunnel/transform/use-sql-transform'
import { useSqljoinTransform } from './seaTunnel/transform/use-sqljoin-transform'


import { useSqlrowtocolumnTransform } from './seaTunnel/transform/use-sqlrowtocolumn-transform'
import { useSqlcolumntorowTransform } from './seaTunnel/transform/use-sqlcolumntorow-transform'


import { useDorisSource } from './seaTunnel/source/use-doris-source'
import { useDorisSink } from './seaTunnel/sink/use-doris-sink'
import { useDataNormalizeTransform } from './seaTunnel/transform/use-data-normalize-transform'
import { useFlexibleHttp } from './use-flexible-http'
import { useFlexibleAPI } from './use-flexible-api'
import { useMinIO} from './use-minio'

import { useSqlunionTransform } from './seaTunnel/transform/use-sqlunion-transform'
import { useSqlunionsortTransform} from './seaTunnel/transform/use-sqlunionsort-transform'

export default {
  SHELL: useShell,
  SUB_PROCESS: useSubProcess,
  DYNAMIC: useDynamic,
  PYTHON: usePython,
  SPARK: useSpark,
  MR: useMr,
  FLINK: useFlink,
  HTTP: useHttp,
  ASYNCSTATE: useAsyncstate,
  WEBHOOK: useWebhook,
  PIGEON: usePigeon,
  SQL: useSql,
  PROCEDURE: useProcedure,
  SQOOP: useSqoop,
  SEATUNNEL: useSeaTunnel,
  SOURCE: useSeaTunnel,
  SWITCH: useSwitch,
  CONDITIONS: useConditions,
  DATAX: useDataX,
  DEPENDENT: useDependent,
  DATA_QUALITY: useDataQuality,
  EMR: useEmr,
  ZEPPELIN: useZeppelin,
  K8S: useK8s,
  JUPYTER: useJupyter,
  MLFLOW: useMlflow,
  OPENMLDB: useOpenmldb,
  DVC: useDvc,
  DINKY: useDinky,
  SAGEMAKER: userSagemaker,
  CHUNJUN: useChunjun,
  FLINK_STREAM: useFlinkStream,
  JAVA: useJava,
  PYTORCH: usePytorch,
  HIVECLI: useHiveCli,
  DMS: useDms,
  DATASYNC: useDatasync,
  KUBEFLOW: useKubeflow,
  LINKIS: useLinkis,
  DATA_FACTORY: useDataFactory,
  REMOTESHELL: useRemoteShell,
  JDBC_SOURCE: useJdbcSource,
  JDBC_SINK: useJdbcSink,
  FILTER_TRANSFORM: useFilterTransform,
  JSONPATH_TRANSFORM: useJsonpathTransform,
  SQL_TRANSFORM: useSqlTransform,
  SQLFILTER_TRANSFORM: useSqlfilterTransform,
  DESENSITIZE_TRANSFORM: useDesensitizeTransform,
  ENCRYPT_TRANSFORM: useEncryptTransform,
  DECRYPT_TRANSFORM: useDecryptTransform,
  SPLIT_TRANSFORM: useSplitTransform,
  RANGE_TRANSFORM: useRangeTransform,
  SWITCHCASE_TRANSFORM: useSwitchcaseTransform,
  JAVA_TRANSFORM: useJavaTransform,
  JAVASCRIPT_TRANSFORM: useJavascriptTransform,
  RANDOMNUMBER_TRANSFORM: useRandomnumberTransform,
  SEQUENCE_TRANSFORM:useIncreasesequenceTransform,
  UDP_SOURCE: useUdpSource,
  UDP_SINK: useUdpSink,
  SQLCASEWHEN_TRANSFORM: useSqlcasewhenTransform,
  SPLITCOLUMNS_TRANSFORM: useSplitcolumnsTransform,
  SQLFUNCTION_TRANSFORM: useSqlFunctionTransform,
  JOIN_TRANSFORM: useJoinTransform,
  SQLJOIN_TRANSFORM: useSqljoinTransform,
  CONSTREFERENCE_TRANSFORM: useConstReferenceTransform,
  SORT_TRANSFORM: useSortTransform,
  DATADISTINCT_TRANSFORM: useDataDistinctTransform,
  GROUP_TRANSFORM: useGroupTransform,
  KAFKA_SINK: useKafkaSink,
  KAFKA_SOURCE: useKafkaSource,
  EXTRACT:useUnarchive,
  MINIO: useMinIO,
  UNARCHIVE:useUnarchive,
  HTTP_SOURCE: useHttpSource,
  HTTP_SINK: useHttpSink,
  LOCALFILE_SOURCE: useLocalFileSource,
  LOCALFILE_SINK: useLocalFileSink,
  SOCKETSERVER_SOURCE: useSocketServerSource,
  FAKESOURCE_SOURCE: useFakeSourceSource,
  HTTPSERVER_SOURCE: useHttpServerSource,
  UDPMULTICAST_SINK: useUdpMulticastSink,
  UDPMULTICAST_SOURCE: useUdpMulticastSource,
  FTPFILE_SOURCE: useFtpFileSource,
  SFTPFILE_SOURCE: useSftpFileSource,
  HDFSFILE_SOURCE: useHdfsfileSource,
  HDFSFILE_SINK: useHdfsfileSink,
  MINIOFILE_SOURCE: useMinioFileSource,
  MINIOFILE_SINK: useMinioFileSink,
  HIVE_SINK: useHiveSink,
  MYSQL_SINK: useMysqlSink,
  MYSQL_SOURCE: useMysqlSource,
  POSTGRESQL_SOURCE: usePostgreSQLSource,
  POSTGRESQL_SINK: usePostgreSQLSink,
  DAMENG_SOURCE: useDamengSource,
  DAMENG_SINK: useDamengSink,
  GAUSSDB_SOURCE: useGaussdbSource,
  GAUSSDB_SINK: useGaussdbSink,
  SQLROWTOCOLUMN_TRANSFORM: useSqlrowtocolumnTransform,
  SQLCOLUMNTOROW_TRANSFORM: useSqlcolumntorowTransform,
  DORIS_SOURCE: useDorisSource,
  DORIS_SINK: useDorisSink,
  SQLUNION_TRANSFORM: useSqlunionTransform,
  SQLUNIONSORT_TRANSFORM: useSqlunionsortTransform,
  DATANORMALIZE_TRANSFORM: useDataNormalizeTransform,
  FLEXIBLE_HTTP: useFlexibleHttp,
  FLEXIBLE_API: useFlexibleAPI
  
}

