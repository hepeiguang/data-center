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

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-19 10:01
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SinkOpenGaussDTO extends BaseSeatunnelSinkDTO {
    /**
     * JDBC 连接的 URL。参见示例:
     * jdbc:postgres://localhost:3306:5432/test
     */
    @JsonProperty("url")
    private String url;
    /**
     * 用于连接远程数据源的 JDBC 类名，
     * 如果使用 postgres，值为 org.postgresql.Driver
     */
    @JsonProperty("driver")
    private String driver;
    /**
     * 连接实例用户名
     */
    @JsonProperty("user")
    private String user;
    /**
     * 连接实例密码
     */
    @JsonProperty("password")
    private String password;
    /**
     * 使用此sql将上游输入数据写入数据库。例如： INSERT ...,query 具有更高的优先级
     */
    @JsonProperty("query")
    private String query;
    /**
     * 使用此 database 和 table-name 自动生成sql并接收上游输入数据写入数据库。
     * 此选项与query 互斥，具有更高的优先级
     */
    @JsonProperty("database")
    private String database;
    /**
     * 使用数据库和此表名自动生成sql并接收上游输入数据写入数据库。
     * 此选项与query 互斥，具有更高的优先级
     */
    @JsonProperty("table")
    private String table;
    /**
     * 此选项用于支持以下操作，例如 insert, delete, 和 update 当自动生成sql
     */
    @JsonProperty("primary_keys")
    private List primaryKeys;
    /**
     * 选择使用INSERT sql、UPDATE sql根据查询主键是否存在来处理更新事件（INSERT、UPDATE_AFTER）。此配置仅在数据库不支持升级语法时使用注：此方法性能低
     */
    @JsonProperty("support_upsert_by_query_primary_key_exist")
    private Boolean supportUpsertByQueryPrimaryKeyExist = false;
    /**
     * 等待用于验证连接的数据库操作完成的时间（以秒为单位）。
     */
    @JsonProperty("connection_check_timeout_sec")
    private Integer connectionCheckTimeoutSec = 30;
    /**
     * 提交失败的重试次数（executeBatch）
     */
    @JsonProperty("max_retries")
    private Integer maxRetries = 0;
    /**
     * 对于批量写入，当缓冲记录的数量达到“batch_size”的数量或时间达到“checkpoint.interval”
     * 时，数据将被刷新到数据库中
     */
    @JsonProperty("batch_size")
    private Integer batchSize = 1000;
    /**
     * 是否启用精确一次语义，这将使用Xa事务。如果启用，则需要
     * 设置xa_data_source_class_name。
     */
    @JsonProperty("is_exactly_once")
    private Boolean isExactlyOnce = false;
    /**
     * 根据要写入的数据库表生成sql语句
     */
    @JsonProperty("generate_sink_sql")
    private Boolean generateSinkSql = false;

    /**
     * 数据库Driver的xa数据源类名，例如mysql是`com.mysql.cj.jdbc。MysqlXADataSource，和
     * 请参阅附录了解其他数据源
     */
    @JsonProperty("xa_data_source_class_name")
    private String xaDataSourceClassName;
    /**
     * 事务提交失败的重试次数
     */
    @JsonProperty("max_commit_attempts")
    private Integer maxCommitAttempts;
    /**
     * 事务打开后的超时，默认值为-1（永不超时）。请注意，设置超时可能会影响＜br/＞精确一次语义
     */
    @JsonProperty("transaction_timeout_sec")
    private Integer transactionTimeoutSec;
    /**
     * 默认情况下启用自动事务提交
     */
    @JsonProperty("auto_commit")
    private Boolean autoCommit = true;
    /**
     * 确定从源同步到汇时是否需要转换字段ORIGINAL表示不需要转换大写表示转换为大写LOWERCASE表示转换为小写
     */
    @JsonProperty("field_ide")
    private String fieldIde;
    /**
     * 其他连接配置参数，当属性和URL具有相同的参数时，优先级由驱动程序的特定实现决定。例如，在MySQL中，属性优先于URL
     */
    private Map<String,Object> properties;

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
     * 通过primary_keys存在启用upstart，如果任务只有“插入”，将此参数设置为“false”可以加快数据导入
     */
    @JsonProperty("enable_upsert")
    private Boolean enableUpsert = true;

}
