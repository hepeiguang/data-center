package org.apache.dolphinscheduler.api.vo;

import lombok.Data;

import java.util.List;

@Data
public class ResourceTreeNodeVO {
    private Long id;
    private String pid;
    private String name;
    private String fullName;
    private String description;
    private List<ResourceTreeNodeVO> children;
    private String type;
    private Object currentDir; // 可以是 String 或其他类型
    private String idValue;
    private Boolean dirctory;
    private String customType;
}