package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSourceDTO;
import shade.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
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
@Accessors(chain = true)
public class SourceMysqlDTO extends BaseSeatunnelSourceDTO {
    /**
     * JDBC 连接的 URL。参见示例:
     * jdbc:mysql://localhost:3306:3306/test
     */
    @JsonProperty("url")
    private String url;
    /**
     * 用于连接远程数据源的 JDBC 类名，
     * 如果使用 MySQL，值为 com.mysql.cj.jdbc.Driver
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
     * 查询语句
     */
    @JsonProperty("query")
    private String query;
    /**
     * 等待用于验证连接的数据库操作完成的时间（以秒为单位）。
     */
    @JsonProperty("connection_check_timeout_sec")
    private Integer connectionCheckTimeoutSec = 30;
    /**
     * 用于并行度分区的列名，仅支持数字类型，仅支持数字类型的主键，并且只能配置一列
     */
    @JsonProperty("partition_column")
    private String partitionColumn;
    /**
     * 扫描的partition_column最小值，如果没有设置SeaTunnel将查询数据库获取最小值。
     */
    @JsonProperty("partition_lower_bound")
    private BigDecimal partitionLowerBound;
    /**
     * 扫描的partition_column的最大值，如果没有设置SeaTunnel将查询数据库获取最大值。
     */
    @JsonProperty("partition_upper_bound")
    private BigDecimal partitionUpperBound;
    /**
     * 不建议使用，正确的做法是控制split通过split.size
     * 个数注意：该参数只有在使用该参数时才生效query，在使用该参数时不生效table_path。
     */
    @JsonProperty("partition_num")
    private Integer partitionNum;
    /**
     * 对于返回大量对象的查询，可以配置查询的行提取大小，以通过减少满足选择条件所需的数据库访问次数来提高性能。
     * 设置为零表示使用 JDBC 的默认值
     */
    @JsonProperty("fetch_size")
    private Integer fetchSize = 0;

    /**
     * 附加连接配置参数，当 properties 和 URL 具有相同的参数时，优先级由驱动程序的具体实现决定
     * 。例如，在 MySQL 中，properties 优先于 URL。
     */
    private Map<String,Object> properties;
    /**
     * 表的完整路径，可以使用此配置代替query。
     * 示例：
     * - mysql: "testdb.table1"
     * - oracle: "test_schema.table1"
     * - sqlserver: "testdb.test_schema.table1"
     * - postgresql: "testdb.test_schema.table1"
     */
    @JsonProperty("table_path")
    private String tablePath;
    /**
     * 要读取的表的列表，您可以使用此配置代替 table_path
     * [
     *     {
     *     table_path = "testdb.table1
     *     },
     *     {
     *     table_path = "testdb.table2",
     *     query = "select * id, name from testdb.table2"
     *     }
     *  ]
     */
    @JsonProperty("table_list")
    private List tableList;
    /**
     * 所有表/查询的通用行过滤条件必须以where. 开头，例如where id > 100
     */
    @JsonProperty("where_condition")
    private String whereCondition;
    /**
     * 表的分割大小（行数），当读取表时，捕获的表会被分割成多个分片
     */
    @JsonProperty("split.size")
    private Integer splitSize = 8096;
    /**
     * 分片键分布因子的下限。该因子用于判断表数据的分布是否均匀。
     * 如果计算得到的分布因子大于或等于该下限（即，(MAX(id) - MIN(id) + 1) / 行数），
     * 则会对表的分片进行优化，以确保数据的均匀分布。反之，如果分布因子较低，则表数据将被视为分布不均匀。
     * 如果估算的分片数量超过 sample-sharding.threshold 所指定的值，则会采用基于采样的分片策略。
     * 默认值为 0.05
     */
    @JsonProperty("split.even-distribution.factor.lower-bound")
    private Double splitEvenDistributionFactorLowerBound = 0.05;
    /**
     * 分片键分布因子的上限。该因子用于判断表数据的分布是否均匀。
     * 如果计算得到的分布因子小于或等于该上限（即，(MAX(id) - MIN(id) + 1) / 行数），
     * 则会对表的分片进行优化，以确保数据的均匀分布。反之，如果分布因子较大，则表数据将被视为分布不均匀，
     * 并且如果估算的分片数量超过 sample-sharding.threshold 所指定的值，则会采用基于采样的分片策略。
     * 默认值为 100.0
     */
    @JsonProperty("split.even-distribution.factor.upper-bound")
    private Double splitEvenDistributionFactorUpperBound = 100.0;
    /**
     * 此配置指定了触发样本分片策略的估算分片数阈值。
     * 当分布因子超出由 chunk-key.even-distribution.factor.upper-bound 和 chunk-key.even-distribution.factor.lower-bound 指定的范围，
     * 并且估算的分片数量（计算方法为大致行数 / 分片大小）超过此阈值时，将使用样本分片策略。此配置有助于更高效地处理大型数据集。
     * 默认值为 1000 个分片
     */
    @JsonProperty("split.sample-sharding.threshold")
    private Integer splitSampleShardingThreshold = 10000;
    /**
     * 样本分片策略中使用的采样率的倒数。
     * 例如，如果该值设置为 1000，则表示在采样过程中应用 1/1000 的采样率。此选项提供了灵活性，可以控制采样的粒度，从而影响最终的分片数量。
     * 特别适用于处理非常大的数据集，在这种情况下通常会选择较低的采样率。
     * 默认值为 1000。
     */
    @JsonProperty("split.inverse-sampling.rate")
    private Integer splitInverseSamplingRate = 1000;

}
