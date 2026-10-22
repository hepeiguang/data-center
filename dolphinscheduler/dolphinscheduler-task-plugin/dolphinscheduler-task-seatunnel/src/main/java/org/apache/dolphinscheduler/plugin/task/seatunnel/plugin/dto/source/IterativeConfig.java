package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 迭代查询配置
 */
@Data
public class IterativeConfig {
    private String resultField;          // 从响应结果中提取的字段名
    private String requestParam;         // 作为查询参数的字段名
    private String initialValue;         // 初始值
    private String stopCondition;        // 停止条件（可选）
    private List<IterativeCondition> condition; //  从响应结果中提取的字段名，获取的条件
    private int maxIterations = 1000;    // 最大迭代次数，防止无限循环
}