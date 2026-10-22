package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSinkDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DataSaveModeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SchemaSaveModeEnum;
import shade.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * @program: sink 的JDBC示例
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-19 10:16
 * <p>
 * 在 is_exactly_once = "true" 的情况下，使用 XA 事务。这需要数据库支持，有些数据库需要一些设置：
 * 1 postgres 需要设置 max_prepared_transactions > 1 例如 ALTER SYSTEM set max_prepared_transactions to 10
 * 2 mysql 版本需要 >= 8.0.29 并且非 root 用户需要授予 XA_RECOVER_ADMIN 权限。例如:将 test_db.* 上的 XA_RECOVER_ADMIN 授予 'user1'@'%'
 * 3 mysql可以尝试在url中添加 rewriteBatchedStatements=true 参数以获得更好的性能
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SinkJdbcDTO extends BaseSeatunnelSinkDTO {
    private String url;
    private String driver;
    private String user;
    private String password;
    /**
     * 使用 sql 语句将上游输入数据写入到数据库。如 INSERT ...
     */
    private String query;
    /**
     * 数据库的兼容模式，当数据库支持多种兼容模式时需要。
     * <p>
     * 例如，使用 OceanBase 数据库时，需要将其设置为 'mysql' 或 'oracle' 。使用StarRocks时，需要将其设置为starrocks。
     * <p>
     * Postgres 9.5及以下版本，请设置为 postgresLow 来支持 CDC
     */
    @JsonProperty("compatible_mode")
    private String compatibleMode;
    /**
     * 使用此 database 和 table-name 自动生成 SQL，并接收上游输入的数据写入数据库。
     * <p>
     * 此选项与 query 选项是互斥的，此选项具有更高的优先级。
     */
    private String database;
    /**
     * 使用 database 和此 table-name 自动生成 SQL，并接收上游输入的数据写入数据库。
     * <p>
     * 此选项与 query 选项是互斥的，此选项具有更高的优先级。
     * <p>
     * table参数可以填入一个任意的表名，这个名字最终会被用作创建表的表名，并且支持变量（${table_name}，${schema_name}）。 替换规则如下：${schema_name} 将替换传递给目标端的 SCHEMA 名称，${table_name} 将替换传递给目标端的表名。
     * <p>
     * mysql 接收器示例:
     * <p>
     * test${schema_name}${table_name}_test
     * sink_sinktable
     * ss_${table_name}
     * pgsql (Oracle Sqlserver ...) 接收器示例:
     * <p>
     * ${schema_name}.${table_name}_test
     * dbo.tt_${table_name}_sink
     * public.sink_table
     * Tip: 如果目标数据库有 SCHEMA 的概念，则表参数必须写成 xxx.xxx
     */
    private String table;
    /**
     * 该选项用于辅助生成 insert、delete、update 等 sql 语句。设置了该选项，将会根据该选项生成对应的 sql 语句
     */
    @JsonProperty("primary_keys")
    private List primaryKeys;
    /**
     * 根据查询主键是否存在来选择使用 INSERT sql、UPDATE sql 来处理变更事件(INSERT、UPDATE_AFTER)。仅当数据库不支持 upsert 语法时才使用此配置 注意：该方法性能较低
     */
    @JsonProperty("support_upsert_by_query_primary_key_exist")
    private Boolean supportUpsertByQueryPrimaryKeyExist;
    /**
     * 用于验证数据库连接的有效性时等待数据库操作完成所需的时间，单位是秒
     */
    @JsonProperty("connection_check_timeout_sec")
    private Integer connectionCheckTimeoutSec;
    /**
     * 重试提交失败的最大次数（executeBatch）
     */
    @JsonProperty("max_retries")
    private Integer maxRetries = 0;
    /**
     * 对于批量写入，当缓冲的记录数达到 batch_size 数量或者时间达到 checkpoint.interval 时，数据将被刷新到数据库中
     */
    @JsonProperty("batch_size")
    private Integer batchSize = 1000;
    /**
     * 默认启用自动事务提交
     */
    @JsonProperty("auto_commit")
    private Boolean autoCommit = true;

    /**
     * 字段 field_ide 用于在从 source 同步到 sink 时，确定字段是否需要转换为大写或小写。'ORIGINAL' 表示不需要转换，'UPPERCASE' 表示转换为大写，'LOWERCASE' 表示转换为小写
     */
    @JsonProperty("field_ide")
    private String fieldIde;
    /**
     * 附加连接配置参数，当属性和URL具有相同参数时，优先级由驱动程序的具体实现确定。例如，在 MySQL 中，属性配置优先于 URL。
     */
    private Map properties;
    /**
     * 是否启用通过XA事务实现的精确一次语义。开启，你还需要设置 xa_data_source_class_name
     */
    @JsonProperty("is_exactly_once")
    private Boolean isExactlyOnce;
    /**
     * 根据要写入的数据库表结构生成 sql 语句
     */
    @JsonProperty("generate_sink_sql")
    private Boolean generateSinkSql = true;
    /**
     * 指数据库驱动的 XA 数据源的类名。以 MySQL 为例，其类名为 com.mysql.cj.jdbc.MysqlXADataSource。了解其他数据库的数据源类名，可以参考文档的附录部分
     */
    @JsonProperty("xa_data_source_class_name")
    private String xaDataSourceClassName;
    /**
     * 事务提交失败的最大重试次数
     */
    @JsonProperty("max_commit_attempts")
    private Integer maxCommitAttempts;

    /**
     * 在事务开启后的超时时间，默认值为-1（即永不超时）。请注意，设置超时时间可能会影响到精确一次（exactly-once）的语义
     */
    @JsonProperty("transaction_timeout_sec")
    private Integer transactionTimeoutSec = -1;
    /**
     * 在启动同步任务之前，针对目标侧已有的表结构选择不同的处理方案
     * 选项介绍：
     * RECREATE_SCHEMA：当表不存在时会创建，当表已存在时会删除并重建
     * CREATE_SCHEMA_WHEN_NOT_EXIST：当表不存在时会创建，当表已存在时则跳过创建
     * ERROR_WHEN_SCHEMA_NOT_EXIST：当表不存在时将抛出错误
     * IGNORE ：忽略对表的处理
     */
    @JsonProperty("schema_save_mode")
    private SchemaSaveModeEnum schemaSaveMode;

    /**
     * 在启动同步任务之前，针对目标侧已存在的数据选择不同的处理方案
     * 选项介绍：
     * DROP_DATA：保留数据库结构，删除数据
     * APPEND_DATA：保留数据库结构，保留数据
     * CUSTOM_PROCESSING：允许用户自定义数据处理方式
     * ERROR_WHEN_DATA_EXISTS：当有数据时抛出错误
     */
    @JsonProperty("data_save_mode")
    private DataSaveModeEnum dataSaveMode;

    /**
     * 当data_save_mode选择CUSTOM_PROCESSING时，需要填写CUSTOM_SQL参数。该参数通常填写一条可以执行的SQL。SQL将在同步任务之前执行
     */
    @JsonProperty("custom_sql")
    private String customSql;

    /**
     * 启用通过主键更新插入，如果任务没有key重复数据，设置该参数为 false 可以加快数据导入速度
     */
    @JsonProperty("enable_upsert")
    private Boolean enableUpsert = true;

    /**
     * 使用 COPY ${table} FROM STDIN 语句导入数据。仅支持具有 getCopyAPI() 方法连接的驱动程序。例如：Postgresql 驱动程序 org.postgresql.Driver
     * <p>
     * 注意：不支持 MAP、ARRAY、ROW类型
     */
    @JsonProperty("use_copy_statement")
    private Boolean useCopyStatement;

    /**
     * 节点输出字段
     */
    @JsonProperty("outputFields")
    private Set<String> outputFields;
}
