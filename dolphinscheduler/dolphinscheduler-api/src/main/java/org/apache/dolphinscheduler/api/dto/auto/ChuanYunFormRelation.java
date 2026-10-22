package org.apache.dolphinscheduler.api.dto.auto;

import lombok.Data;

import java.util.Date;

// 新增实体类：表单-工作流关联表
@Data
public class ChuanYunFormRelation {
    private String id;                 // 表单在氚云的ObjectId
    private String formCode;         // 表单编码
    private String formName;         // 表单名称
    private String appCode;          // 应用编码
    private String appName;          // 应用名称
    private String tableName;        // 生成的表名
    private Long workflowCode;       // 工作流编码
    private String workflowName;     // 工作流名称
    private Long projectCode;        // 项目编码
    private String projectName;        // 项目编码
    private Date CreateTime;
}