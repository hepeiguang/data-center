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

package org.apache.dolphinscheduler.spi.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.google.common.base.Functions;

import java.util.Arrays;
import java.util.Map;
import java.util.NoSuchElementException;

import static java.util.stream.Collectors.toMap;

public enum DbType {

    /**
     * 关系型数据库
     */
    MYSQL(0, "mysql", "mysql", "database"),
    POSTGRESQL(1, "postgresql", "postgresql", "database"),
    ORACLE(5, "oracle", "oracle", "database"),
    SQLSERVER(6, "sqlserver", "sqlserver", "database"),
    DB2(7, "db2", "db2", "database"),
    H2(9, "h2", "h2", "database"),
    DAMENG(15, "dameng", "dameng", "database"),
    OCEANBASE(16, "oceanbase", "oceanbase", "database"),
    OPENGAUSS(34, "opengauss", "opengauss", "database"),


    /**
     * 数据仓库 (Data Warehouse)
     */
    REDSHIFT(10, "redshift", "redshift", "warehouse"),
    SNOWFLAKE(20, "snowflake", "snowflake", "warehouse"),
    VERTICA(21, "vertica", "vertica", "warehouse"),
    STARROCKS(13, "starrocks", "starrocks", "warehouse"),
    CLICKHOUSE(4, "clickhouse", "clickhouse", "warehouse"),

    DORIS(23, "doris", "doris", "warehouse"),

    /**
     * 大数据分析引擎
     */
    DATABEND(19, "databend", "databend", "dataAnalysisEngine"),

    HIVE(2, "hive", "hive", "dataAnalysisEngine"),
    PRESTO(8, "presto", "presto", "dataAnalysisEngine"),
    TRINO(12, "trino", "trino", "dataAnalysisEngine"),

    SPARK(3, "spark", "spark", "dataAnalysisEngine"),
    ATHENA(11, "athena", "athena", "dataAnalysisEngine"),
    KYUUBI(18, "kyuubi", "kyuubi", "dataAnalysisEngine"),

    // 文件/对象存储系统
    HDFS_FILE(30, "hdfs_file", "hdfs_file", "fileStorageSystem"),
    MINIO(31, "minio", "minio", "fileStorageSystem"),
    YUNHEZI(36, "yunhezi", "yunhezi", "fileStorageSystem"),

    //消息与流处理系统
    KAFKA(26, "kafka", "kafka", "messageSystem"),
    MQTT(37, "mqtt", "mqtt", "messageSystem"),


    // 服务器/协议连接
    SSH(17, "ssh", "ssh", "server"),
    FTP(32, "ftp", "ftp", "server"),
    SFTP(33, "sftp", "sftp", "server"),
    SOCKET_SERVER(27, "socket_server", "socket_server", "server"),
    HTTP(28, "http", "http", "server"),
    UDP_MULTICAST(29, "udp_multicast", "udp_multicast", "server"),

    RABBITMQ(35, "rabbitmq", "rabbitmq", "server"),


    // 其他工具与服务
    AZURESQL(14, "azuresql", "azuresql", "other"),
    HANA(22, "hana", "hana", ""),
    ZEPPELIN(24, "zeppelin", "zeppelin", "other"),
    SAGEMAKER(25, "sagemaker", "sagemaker", "other"),
    ;
    private static final Map<Integer, DbType> DB_TYPE_MAP =
            Arrays.stream(DbType.values()).collect(toMap(DbType::getCode, Functions.identity()));
    @EnumValue
    private final int code;
    private final String name;
    private final String descp;

    private final String serverType;

    DbType(int code, String name, String descp, String serverType) {
        this.code = code;
        this.name = name;
        this.descp = descp;
        this.serverType = serverType;
    }

    public static DbType of(int type) {
        if (DB_TYPE_MAP.containsKey(type)) {
            return DB_TYPE_MAP.get(type);
        }
        return null;
    }

    public static DbType ofName(String name) {
        return Arrays.stream(DbType.values()).filter(e -> e.name().equals(name)).findFirst()
                .orElseThrow(() -> new NoSuchElementException("no such db type"));
    }

    public int getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getDescp() {
        return descp;
    }

    public String getServerType() {
        return serverType;
    }

    public boolean isHive() {
        return this == DbType.HIVE;
    }

    /**
     * support execute multiple segmented statements at a time
     *
     * @return
     */
    public boolean isSupportMultipleStatement() {
        return isHive() || this == DbType.SPARK;
    }
}
