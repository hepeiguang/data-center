package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;

/**
 * @Author： 王涛
 * @DATE： 2025/5/14 10:22
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TransFormSplitColumnDTO extends BaseSeatunneTransformDTO {

    //分隔符
    @JsonProperty("separator")
    private String separator;

    // 要拆分的字段
    @JsonProperty("splitField")
    private String splitField;

    // 是否使用正则表达式 1 = 使用；2 = 不使用
    @JsonProperty("regex")
    private Integer regex = 2;

    // 新字段名
    @JsonProperty("newField")
    private String newField;


    @JsonProperty("plugin_input")
    private String pluginInput;

    @JsonProperty("plugin_output")
    private String pluginOutput;


}
