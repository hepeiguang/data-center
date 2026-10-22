package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.ColumnErrorHandleWayEnum;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Accessors(chain = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TransFormJsonPathColumnDTO extends BaseSeatunneTransformDTO {

    @JsonProperty("src_field")
    private String srcField;

    @JsonProperty("path")
    private String path;

    @JsonProperty("dest_field")
    private String destField;

    @JsonProperty("dest_type")
    private String destType;

    @JsonProperty("column_error_handle_way")
    private ColumnErrorHandleWayEnum columnErrorHandleWay = ColumnErrorHandleWayEnum.FAIL;

}
