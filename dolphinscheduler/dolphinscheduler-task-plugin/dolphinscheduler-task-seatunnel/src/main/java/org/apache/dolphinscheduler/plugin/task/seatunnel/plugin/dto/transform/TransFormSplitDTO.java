package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TransFormSplitDTO extends BaseSeatunneTransformDTO {

    @JsonProperty("separator")
    private String separator;

    @JsonProperty("split_field")
    private String splitField;

    @JsonProperty("output_fields")
    private List<String> outputFields;

    @JsonProperty("plugin_input")
    private String pluginInput;

    @JsonProperty("plugin_output")
    private String pluginOutput;

}
