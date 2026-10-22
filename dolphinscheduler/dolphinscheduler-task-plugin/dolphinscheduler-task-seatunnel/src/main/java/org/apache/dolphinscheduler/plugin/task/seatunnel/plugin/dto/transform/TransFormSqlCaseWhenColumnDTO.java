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
public class TransFormSqlCaseWhenColumnDTO extends BaseSeatunneTransformDTO {

    //字段名
    @JsonProperty("columnName")
    private String columnName;

    // 字段类型
    @JsonProperty("fieldType")
    private String fieldType;

    //元数据值
    @JsonProperty("srcValue")
    private String srcValue;

    //表达式
    @JsonProperty("expression")
    private String expression;

    //目标值
    @JsonProperty("desValue")
    private String desValue;

    // 默认值 elses
    @JsonProperty("defaultValue")
    private String defaultValue;

    @JsonProperty("row_error_handle_way")
    private RowErrorHandleWayEnum rowErrorHandleWay = RowErrorHandleWayEnum.FAIL;

}
