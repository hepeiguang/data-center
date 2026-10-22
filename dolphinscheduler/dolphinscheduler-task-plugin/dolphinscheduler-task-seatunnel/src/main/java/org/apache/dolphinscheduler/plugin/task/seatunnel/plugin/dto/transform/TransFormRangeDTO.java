package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;

import java.util.List;

/**
 * @program: 20250425
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-25
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TransFormRangeDTO extends BaseSeatunneTransformDTO {
    /**
     * 页面传过来的参数
     *
     * 需求分析
     * 根据需求，我们需要实现一个名为“数值范围计算器”的算子，该算子会对表中的某一列（range_field）进行计算转换。具体逻辑如下：
     * 输入参数：
     * range_type：判断是“大于等于”还是“小于等于”。大于等于(greater_than_or_equal) 或 小于等于(less_than_or_equal)
     * range_value_default：默认值，当没有匹配的范围时使用。
     * range_field：需要计算的列名。
     * range_to_field：结果字段。
     * range_to_field_type：类型。
     * range_mappings：范围映射列表，包含起始值、结束值和对应的映射值。
     * plugin_name：插件名称。
     * plugin_input：输入表名。
     * plugin_output：输出表名。
     * 计算逻辑：
     * 遍历表中的每一行数据，提取 range_field 的值。
     * 根据 range_type 判断是“大于等于”还是“小于等于”，并匹配 range_mappings 中的范围。
     * 如果匹配到某个范围，则返回该范围的 range_value；否则返回 range_value_default。
     * 输出：
     * 输出表中新增一列，包含计算结果。
     */

    @JsonProperty("range_type")
    private String rangeType;

    @JsonProperty("range_value_default")
    private String rangeValueDefault;

    @JsonProperty("range_field")
    private String rangeField;

    @JsonProperty("range_to_field")
    private String rangeToField;

    @JsonProperty("range_to_field_type")
    private String rangeToFieldType;

    @JsonProperty("range_mappings")
    private List<TransFormRangeItemDTO> rangeMappings;

    @JsonProperty("plugin_input")
    private String pluginInput;

    @JsonProperty("plugin_output")
    private String pluginOutput;


}

