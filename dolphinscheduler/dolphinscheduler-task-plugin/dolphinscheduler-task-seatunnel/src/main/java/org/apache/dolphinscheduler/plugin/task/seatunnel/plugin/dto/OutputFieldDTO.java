package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.facebook.presto.jdbc.internal.jackson.annotation.JsonSetter;
import com.facebook.presto.jdbc.internal.jackson.annotation.Nulls;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author : wangyongfeng
 * @version V1.0
 * @Project: data-center-platform
 * @Package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto
 * @Description: TODO
 * @date Date : 2025年05月13日 15:22
 */
@Data
@Slf4j
@NoArgsConstructor
@Accessors(chain = true)
public class OutputFieldDTO {
    // 输出字段类型 表或者其他
    private Type type;
    //当前节点名称
    private String taskName;
    //表名称
    private String tableName;
    //数据源id
    private Integer datasourceId;
    /**
     * 新增列
     */
    @JsonSetter(nulls = Nulls.AS_EMPTY)
    private LinkedHashSet<String> fields;

    public OutputFieldDTO(Type type) {
        this.type = type;
    }
//    public String getKey() {
//        return type == Type.TABLE ? tableName : taskName;
//    }

    public OutputFieldDTO addField(String field) {
        if (fields == null) {
            fields = new java.util.LinkedHashSet<>();
        }
        fields.add(field);
        return this;
    }

    public enum Type {
        TABLE,
        OTHER;
    }
}
