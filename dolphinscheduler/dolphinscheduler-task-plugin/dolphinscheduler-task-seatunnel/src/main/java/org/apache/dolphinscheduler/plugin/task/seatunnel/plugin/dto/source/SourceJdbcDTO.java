package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSourceDTO;
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
public class SourceJdbcDTO extends BaseSeatunnelSourceDTO {
    private String url;
    private String driver;
    private String user;
    private String password;
    /**
     * 查询语句
     */
    private String query;
    /**
     * 数据库兼容模式，当数据库支持多种兼容模式时需要设置。
     * 例如使用 OceanBase 数据库时，需要设置为 mysql 或 oracle 。
     * 使用 starrocks 数据库时，需要设置为starrocks
     */
    @JsonProperty("compatible_mode")
    private String compatibleMode;
    /**
     * 等待用于验证连接的数据库操作完成的时间（以秒为单位）。
     */
    @JsonProperty("connection_check_timeout_sec")
    private Integer connectionCheckTimeoutSec = 30;
    /**
     * 分割数据的列名。
     */
    @JsonProperty("partition_column")
    private String partitionColumn;
    /**
     * 扫描的partition_column的最大值，如果没有设置SeaTunnel将查询数据库获取最大值。
     */
    @JsonProperty("partition_upper_bound")
    private Long partitionUpperBound;
    /**
     * 扫描的partition_column最小值，如果没有设置SeaTunnel将查询数据库获取最小值。
     */
    @JsonProperty("partition_lower_bound")
    private Long partitionLowerBound;

    /**
     * 不建议使用，正确的做法是控制split通过split.size
     * 个数注意：该参数只有在使用该参数时才生效query，在使用该参数时不生效table_path。
     */
    @JsonProperty("partition_num")
    private Integer partitionNum;

    /**
     * 十进制类型缩小，如果设置为 true，则在不损失精度的情况下，十进制类型将被缩小为 int 或 long 类型。目前仅支持 Oracle。请参考decimal_type_narrowing下文。
     */
    @JsonProperty("decimal_type_narrowing")
    private Boolean decimalTypeNarrowing;

    /**
     * 在动态块拆分阶段，使用 Select count 进行表计数，而不是其他方法。目前此功能仅适用于 JDBC-Oracle。在这种情况下，如果使用 SQL 从分析表更新统计信息的速度更快，则直接使用 Select count。
     */
    @JsonProperty("use_select_count")
    private Boolean useSelectCount;

    /**
     * 在动态块拆分阶段跳过表计数分析。目前此功能仅适用于 JDBC-Oracle。在此场景中，您安排分析表 SQL 定期更新相关表统计信息，或者您的表数据不会频繁更改
     */
    @JsonProperty("skip_analyze")
    private Boolean skipAnalyze;
    /**
     * 对于返回大量对象的查询，您可以配置查询中使用的行提取大小，以通过减少满足选择条件所需的数据库命中次数来提高性能。零表示使用 JDBC 默认值。
     */
    @JsonProperty("fetch_size")
    private String fetchSize;
    /**
     * 附加连接配置参数，当 properties 和 URL 具有相同的参数时，优先级由驱动程序的具体实现决定
     * 。例如，在 MySQL 中，properties 优先于 URL。
     */
    private Map properties;
    /**
     * 表的完整路径，可以使用此配置代替query。
     * 示例：
     * - mysql: "testdb.table1"
     * - oracle: "test_schema.table1"
     * - sqlserver: "testdb.test_schema.table1"
     * - postgresql: "testdb.test_schema.table1"
     * - iris: "test_schema.table1"
     */
    @JsonProperty("table_path")
    private String tablePath;
    /**
     * table_list = [
     * {
     * # e.g. table_path = "testdb.table1"、
     * table_path = "test_schema.table1"、
     * table_path = "testdb.test_schema.table1"
     * table_path = "testdb.table1"
     * },
     * {
     * table_path = "testdb.table2"
     * # Use query filetr rows & columns
     * query = "select id, name from testdb.table2 where id > 100"
     * }
     * ]
     */
    @JsonProperty("table_list")
    private List tableList;
    /**
     * 所有表/查询的通用行过滤条件必须以where. 开头，例如where id > 100
     */
    @JsonProperty("where_condition")
    private String whereCondition;
    private List selectedColumns;

    public void setTablePath(String tablePath) {
        if (this.query == null || this.query.isEmpty()) {
            this.tablePath = tablePath;
        }
    }

    public void setQuery(String query) {
        this.query = query;
        if (this.query != null && !this.query.isEmpty()) {
            this.tablePath = null;
        }
    }
}

