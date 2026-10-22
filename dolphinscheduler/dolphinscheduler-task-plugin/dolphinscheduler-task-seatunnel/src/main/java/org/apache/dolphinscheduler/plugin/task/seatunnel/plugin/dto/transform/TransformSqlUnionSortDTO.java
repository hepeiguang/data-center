package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TransformSqlUnionSortDTO extends BaseSeatunneTransformDTO {
    private String tableName;
    private List<String> selectedColumns ;
    //key是表名和别名aliasName，value是字段
    private List<Map<String, String>> fields; // 多个字段映射

    /**  排序字段  */
    private List<FieldSortDTO> sortFields;
    @Data
    public static class FieldSortDTO {
        // 表字段
        private String fieldName;
        //排序方式 asc desc
        private String sortType;
    }
}
