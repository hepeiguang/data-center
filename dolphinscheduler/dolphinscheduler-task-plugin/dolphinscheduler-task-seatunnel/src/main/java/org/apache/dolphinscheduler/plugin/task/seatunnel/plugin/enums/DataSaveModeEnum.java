package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums;

/**
 * @program: 在启动同步任务之前，针对目标侧已存在的数据选择不同的处理方案
 * @author: hj
 * @create: 2025-04-19 10:36
 **/
public enum DataSaveModeEnum {
    /**
     * 保留数据库结构，删除数据
     */
    DROP_DATA,
    /**
     * 保留数据库结构，保留数据
     */
    APPEND_DATA,
    /**
     * 允许用户自定义数据处理方式
     */
    CUSTOM_PROCESSING,
    /**
     *当有数据时抛出错误
     */
    ERROR_WHEN_DATA_EXISTS;
}
