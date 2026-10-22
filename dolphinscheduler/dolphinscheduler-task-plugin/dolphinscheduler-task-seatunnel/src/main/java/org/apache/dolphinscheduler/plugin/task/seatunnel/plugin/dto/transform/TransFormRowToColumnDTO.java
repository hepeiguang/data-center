package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.RowErrorHandleWayEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TransFormRowToColumnDTO extends BaseSeatunneTransformDTO {

    //数据字段
    @JsonProperty("dataField")
    private String dataField;

    //数据字段值
    @JsonProperty("srcValue")
    private String srcValue;

    //case when 表达式
    @JsonProperty("expression")
    private String expression;

    //目标字段
    @JsonProperty("targetField")
    private String targetField;

    //目标字段值
    @JsonProperty("targetFieldValue")
    private String targetFieldValue;


    //聚合表达式 如  MAX MIN SUM AVG 等
    @JsonProperty("aggregateFunctions")
    private String aggregateFunctions;

}
