package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSourceDTO;

import java.util.List;
import java.util.Map;

/**
 * 源传输对象-虚拟数据源
 *
 * @author ZhaoYuan
 * @date 2025/5/5 17:27
 * @since JDK1.8
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@Data
public class SourceFakeSourceDTO extends BaseSeatunnelSourceDTO {

    //文件路径
    @JsonProperty("tables_configs")
    private List<SourceFakeSourceDTO> tablesConfigs;
    //schema
    private Map<String, Object> schema;
    //前端配置使用
    private String schemaVo;
    //每个并行度输出的伪数据行列表
    //private String rows;
    //每个并行度生成的数据总行数，默认5
    @JsonProperty("row.num")
    private Integer rowNum;
    //枚举器为每个并行度生成的分片数量，默认1
    @JsonProperty("split.num")
    private Integer splitNum;
    //读取器在两个分片读取之间的间隔时间（毫秒），默认1
    @JsonProperty("split.read-interval")
    private Long splitReadInterval;
    //连接器生成的map类型的大小，默认5
    @JsonProperty("map.size")
    private Integer mapSize;
    //连接器生成的array类型的大小，默认5
    @JsonProperty("array.size")
    private Integer arraySize;
    //连接器生成的bytes类型的长度，默认5
    @JsonProperty("bytes.length")
    private Integer bytesLength;
    //连接器生成的string类型的长度，默认5
    @JsonProperty("string.length")
    private Integer stringLength;
    //生成字符串数据的伪数据模式，支持range和template，默认range，如果配置为template，需配置string.template
    @JsonProperty("string.fake.mode")
    private String stringFakeMode;
    //连接器生成的字符串类型的模板列表，如果用户配置了此选项，连接器将从模板列表中随机选择一个项
    @JsonProperty("string.template")
    private List<String> stringTemplate;
    //前端使用
    private String stringTemplateVo;
    //生成tinyint数据的伪数据模式，支持range和template，默认为range，如果配置为template，用户还需配置tinyint.template选项
    @JsonProperty("tinyint.fake.mode")
    private String tinyintFakeMode;
    //连接器生成的tinyint数据的最小值，默认0
    @JsonProperty("tinyint.min")
    private Integer tinyintMin;
    //连接器生成的tinyint数据的最大值，默认127
    @JsonProperty("tinyint.max")
    private Integer tinyintMax;
    //连接器生成的tinyint类型的模板列表，如果用户配置了此选项，连接器将从模板列表中随机选择一个项
    @JsonProperty("tinyint.template")
    private List<Integer> tinyintTemplate;
    //前端使用
    private String tinyintTemplateVo;
    //生成smallint数据的伪数据模式，支持range和template，默认为range，如果配置为template，用户还需配置smallint.template选项
    @JsonProperty("smallint.fake.mode")
    private String smallintFakeMode;
    //连接器生成的smallint数据的最小值，默认0
    @JsonProperty("smallint.min")
    private Integer smallintMin;
    //连接器生成的smallint数据的最大值，默认32767
    @JsonProperty("smallint.max")
    private Integer smallintMax;
    //连接器生成的smallint类型的模板列表，如果用户配置了此选项，连接器将从模板列表中随机选择一个项
    @JsonProperty("smallint.template")
    private List<Integer> smallintTemplate;
    //前端使用
    private String smallintTemplateVo;
    //生成int数据的伪数据模式，支持range和template，默认为range，如果配置为template，用户还需配置int.template选项
    @JsonProperty("int.fake.mode")
    private String intFakeMode;
    //连接器生成的int数据的最小值，默认0
    @JsonProperty("int.min")
    private Integer intMin;
    //连接器生成的int数据的最大值，默认0x7fffffff
    @JsonProperty("int.max")
    private Integer intMax;
    //连接器生成的int类型的模板列表，如果用户配置了此选项，连接器将从模板列表中随机选择一个项
    @JsonProperty("int.template")
    private List<Integer> intTemplate;
    //前端使用
    private String intTemplateVo;
    //生成bigint数据的伪数据模式，支持range和template，默认为range，如果配置为template，用户还需配置bigint.template选项
    @JsonProperty("bigint.fake.mode")
    private String bigintFakeMode;
    //连接器生成的bigint数据的最小值，默认0
    @JsonProperty("bigint.min")
    private Long bigintMin;
    //连接器生成的bigint数据的最大值，默认0x7fffffffffffffff
    @JsonProperty("bigint.max")
    private Long bigintMax;
    //连接器生成的bigint类型的模板列表，如果用户配置了此选项，连接器将从模板列表中随机选择一个项
    @JsonProperty("bigint.template")
    private List<Long> bigintTemplate;
    //前端使用
    private String bigintTemplateVo;
    //生成float数据的伪数据模式，支持range和template，默认为range，如果配置为template，用户还需配置float.template选项
    @JsonProperty("float.fake.mode")
    private String floatFakeMode;
    //连接器生成的float数据的最小值，默认0
    @JsonProperty("float.min")
    private Float floatMin;
    //连接器生成的float数据的最大值，默认0x1.fffffeP+127
    @JsonProperty("float.max")
    private Float floatMax;
    //连接器生成的float类型的模板列表，如果用户配置了此选项，连接器将从模板列表中随机选择一个项
    @JsonProperty("float.template")
    private List<Float> floatTemplate;
    //前端使用
    private String floatTemplateVo;
    //生成double数据的伪数据模式，支持range和template，默认为range，如果配置为template，用户还需配置double.template选项
    @JsonProperty("double.fake.mode")
    private String doubleFakeMode;
    //连接器生成的double数据的最小值，默认0
    @JsonProperty("double.min")
    private Double doubleMin;
    //连接器生成的double数据的最大值，默认0x1.fffffffffffffP+1023
    @JsonProperty("double.max")
    private Double doubleMax;
    //连接器生成的double类型的模板列表，如果用户配置了此选项，连接器将从模板列表中随机选择一个项
    @JsonProperty("double.template")
    private List<Double> doubleTemplate;
    //前端使用
    private String doubleTemplateVo;
    //生成的向量的维度，不包括二进制向量，默认4
    @JsonProperty("vector.dimension")
    private Integer vectorDimension;
    //生成的二进制向量的维度，默认8
    @JsonProperty("binary.vector.dimension")
    private Integer binaryVectorDimension;
    //连接器生成的向量中float数据的最小值，默认0
    @JsonProperty("vector.float.min")
    private Float vectorFloatMin;
    //连接器生成的向量中float数据的最大值，默认0x1.fffffeP+127
    @JsonProperty("vector.float.max")
    private Float vectorFloatMax;
}