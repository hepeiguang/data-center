package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums;

/**
 * @program: 在启动同步任务之前，针对目标侧已有的表结构选择不同的处理方案
 * @author: hj
 * @create: 2025-04-19 10:31
 **/
public enum SchemaSaveModeEnum {
    /**
     * 当表不存在时会创建，当表已存在时会删除并重建
     */
    RECREATE_SCHEMA,
    /**
     * 当表不存在时会创建，当表已存在时则跳过创建
     */
    CREATE_SCHEMA_WHEN_NOT_EXIST,
    /**
     * 当表不存在时将抛出错误
     */
    ERROR_WHEN_SCHEMA_NOT_EXIST,
    /**
     * 忽略对表的处理
     */
    IGNORE;

    SchemaSaveModeEnum() {
    }

    public static SchemaSaveModeEnum fromString(String name) {
        for (SchemaSaveModeEnum type : SchemaSaveModeEnum.values()) {
            if (type.name().equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown SchemaSaveModeEnum name: " + name);
    }
}
