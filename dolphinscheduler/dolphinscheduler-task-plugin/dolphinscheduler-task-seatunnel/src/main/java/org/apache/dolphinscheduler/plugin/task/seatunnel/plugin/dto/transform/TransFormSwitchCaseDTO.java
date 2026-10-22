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
public class TransFormSwitchCaseDTO extends BaseSeatunneTransformDTO {

    @JsonProperty("switch_field")
    private String switchField;

    @JsonProperty("switch_target_field")
    private String switchTargetField;

    @JsonProperty("switch_target_field_type")
    private String switchTargetFieldType;

    @JsonProperty("switch_field_value_default")
    private Object switchFieldValueDefault;

    @JsonProperty("switch_case_mappings")
    private List<TransFormSwitchCaseItemDTO> switchCaseMappings;

    @JsonProperty("plugin_input")
    private String pluginInput;

    @JsonProperty("plugin_output")
    private String pluginOutput;

}
