package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TransFormSequenceDTO extends BaseSeatunneTransformDTO {

    @JsonProperty("field_name")
    private String fieldName;
    //start_value：起始值，默认为1。
    //add_value：增长根据，每次累加值。
    //max_value：最大值，如果未传不限制。
    @JsonProperty("start_value")
    private String startValue;

    @JsonProperty("add_value")
    private String addValue;

    @JsonProperty("max_value")
    private String maxValue;

    @JsonProperty("plugin_input")
    private String pluginInput;

    @JsonProperty("plugin_output")
    private String pluginOutput;

    private String query;
}
