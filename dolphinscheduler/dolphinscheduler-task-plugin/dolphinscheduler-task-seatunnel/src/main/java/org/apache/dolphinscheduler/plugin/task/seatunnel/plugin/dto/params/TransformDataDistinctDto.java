package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.params;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.*;

@Data
public class TransformDataDistinctDto {

    @JsonProperty("distinct_fields")
    private LinkedHashSet<String> fieldList;

    // 前端返回的 keywords complateMatch regex NOTNULL

    /**
     * 条件类型
     * <p>
     * keywords 关键字
     * regex 正则
     * NOTNULL 非空
     */
    @JsonProperty("cond_type")
    private String condType;

    /**
     * 条件
     * <p>
     * 示例
     * [{"column": "aa", "value":"tt"}]
     */
    @JsonProperty("conds")
    private List<Cond> conds;


    @JsonProperty("db_type")
    private String dbType;

    @Data
    public static class Cond {
        @JsonProperty("column")
        private String column;
        @JsonProperty("value")
        private String value;
    }
}
