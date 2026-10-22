package org.apache.dolphinscheduler.api.dto;

import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Data
public class UdfFuncDto {
    private Integer id;
    private Integer userId;

    private String funcName;
    private String className;

    private Integer type;

    private String argTypes;
    private String database;
    private String description;

    private Integer resourceId;
    private String resourceName;

    private String createTime;
    private String updateTime;

    // 嵌套的 list 字段，对应 ParamItem 对象
    private List<ParamItem> list;

    @Getter
    @Setter
    public static class ParamItem {
        private Integer id;
        private String paramName;
        private String paramType;
        private String paramValue;
        private Boolean isRequired;
        private List<String> option;
    }
}
