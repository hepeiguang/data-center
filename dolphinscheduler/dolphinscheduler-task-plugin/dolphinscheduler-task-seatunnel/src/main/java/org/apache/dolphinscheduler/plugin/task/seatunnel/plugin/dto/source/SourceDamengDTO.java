package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
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
public class SourceDamengDTO extends BaseSeatunnelSourceDTO {
    private String url;
    private String driver;
    private String user;
    private String password;
    /**
     * 查询语句
     */
    private String query;

    /**
     *	等待用于验证连接的数据库操作完成的时间（以秒为单位）。
     */
    @JsonProperty("connection_check_timeout_sec")
    private Integer connectionCheckTimeoutSec = 30;
    /**
     *  用于并行化的分区列名，仅支持数字类型，仅支持数字类型主键，并且只能配置一列。
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
     * 作业并行性	分区数量，仅支持正整数。默认值为作业并行性
     */
    @JsonProperty("partition_num")
    private Integer partitionNum;

    /**
     * 对于返回大量对象的查询，您可以配置查询中使用的行提取大小，以通过减少满足选择条件所需的数据库命中次数来提高性能。零表示使用 JDBC 默认值。
     */
    @JsonProperty("fetch_size")
    private Integer fetchSize = 0;
    /**
     * 其他连接配置参数，当属性和 URL 具有相同参数时，
     * 优先级由驱动程序的具体实现决定。在 MySQL 中，属性优先于 URL。
     */
    private Map<String,Object> properties;

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
     *         {
     *           # e.g. table_path = "testdb.table1"、
     *           table_path = "test_schema.table1"、
     *           table_path = "testdb.test_schema.table1"
     *           table_path = "testdb.table1"
     *         },
     *         {
     *           table_path = "testdb.table2"
     *           # Use query filetr rows & columns
     *           query = "select id, name from testdb.table2 where id > 100"
     *         }
     *     ]
     */
    @JsonProperty("table_list")
    private List tableList;

    /**
     * 所有表/查询的通用行过滤条件必须以where. 开头，例如where id > 100
     */
    @JsonProperty("where_condition")
    private String whereCondition;

    /**
     * 块大小，默认为 8096。表的拆分大小（行数），被捕获的表在读取时被拆分为多个拆分。
     */
    @JsonProperty("split.size")
    //@JsonProperty("splitSize")
    private Integer splitSize = 8096;

    /**
     * 块键分布因子的下限。此因子用于确定表数据是否均匀分布。
     * 如果计算出的分布因子大于或等于此下限（即 (MAX(id) - MIN(id) + 1) / 行数），
     * 则表块将优化为均匀分布。否则，如果分布因子较小，则将视为不均匀分布，
     * 当估计的分片数超过 sample-sharding.threshold 指定的值时，
     * 将使用基于采样的分片策略。默认值为 0.05。
     */
    @JsonProperty("split.even-distribution.factor.lower-bound")
    //@JsonProperty("splitEvenDistributionFactorLowerBound")
    private Double splitEvenDistributionFactorLowerBound = 0.05;

    /**
     * 块键分布因子的上限。此因子用于确定表数据是否均匀分布。
     * 如果计算出的分布因子小于或等于此上限（即 (MAX(id) - MIN(id) + 1) / 行数），则表块将优化为均匀分布。否则，如果分布因子较大，则将视为不均匀分布，当估计的分片数超过 sample-sharding.threshold 指定的值时，将使用基于采样的分片策略。默认值为 100.0。
     */
    @JsonProperty("split.even-distribution.factor.upper-bound")
    //@JsonProperty("splitEvenDistributionFactorUpperBound")
    private Double splitEvenDistributionFactorUpperBound = 100.0;

    /**
     * 此配置指定触发样本分片策略的估计分片数阈值。
     * 当分布因子超出 chunk-key.even-distribution.factor.upper-bound 和 chunk-key.even-distribution.factor.lower-bound 指定的范围时，且估计的分片数（计算为近似行数 / 块大小）超过此阈值，将使用样本分片策略。这可以帮助更高效地处理大数据集。默认值为 1000 个分片。     */
    @JsonProperty("split.sample-sharding.threshold")
    //@JsonProperty("splitSampleShardingThreshold")
    private Integer splitSampleShardingThreshold = 10000;

    /**
     * 在样本分片策略中使用的采样率的逆数。例如，如果此值设置为 1000，表示在采样过程中应用 1/1000 的采样率。此选项提供了控制采样粒度的灵活性，从而影响最终的分片数量。在处理非常大的数据集时，较低的采样率尤其有用。默认值为 1000。
     */
    @JsonProperty("split.inverse-sampling.rate")
    //@JsonProperty("splitInverseSamplingRate")
    private Integer splitInverseSamplingRate = 1000;

}

