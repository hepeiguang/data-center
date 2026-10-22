package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import lombok.Data;

/**
 * 迭代查询条件
 */
@Data
public class IterativeCondition {
    /**
     * 迭代条件的从结果中获取条件的key
     */
    private String prop;
    /**
     * 迭代条件的值
     */
    private String value;
}
