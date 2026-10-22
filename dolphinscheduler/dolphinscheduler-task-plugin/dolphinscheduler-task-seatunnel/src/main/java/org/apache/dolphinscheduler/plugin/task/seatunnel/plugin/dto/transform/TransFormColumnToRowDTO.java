package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TransFormColumnToRowDTO extends BaseSeatunneTransformDTO {
    /**
     * 分组后的字段名
     */
    @JsonProperty("groupKey")
    private String groupKey;
    /**
     * 分组后的值
     */
    @JsonProperty("groupValue")
    private String groupValue;

    @JsonProperty("fields")
    private List<FieldMapping> fields; // 多个字段映射

    @Data
    public static class FieldMapping {
        /**
         * 转换后行的字段名
         */
        @JsonProperty("fieldName")
        private String fieldName;
        /**
         * 转换后行的字段值取值的字段名
         */
        @JsonProperty("fieldValue")
        private String fieldValue;


    }
}
