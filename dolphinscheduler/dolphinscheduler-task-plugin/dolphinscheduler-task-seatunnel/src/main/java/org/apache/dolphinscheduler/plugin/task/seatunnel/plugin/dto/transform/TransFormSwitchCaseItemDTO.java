package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @program: 20250425
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-25
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TransFormSwitchCaseItemDTO {

    @JsonProperty("switch_case_when")
    private Object switchCaseWhen;

    @JsonProperty("switch_case_then")
    private Object switchCaseThen;

}
