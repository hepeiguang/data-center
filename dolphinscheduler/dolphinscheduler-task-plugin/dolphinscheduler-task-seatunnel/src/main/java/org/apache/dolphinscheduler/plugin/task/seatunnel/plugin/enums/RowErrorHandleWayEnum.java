package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums;

public enum RowErrorHandleWayEnum {

    /**
     * 数据处理异常时抛出
     */
    FAIL("FAIL"),
    /**
     * 数据处理异常时跳过
     */
    SKIP("SKIP");

    private final String name;

    RowErrorHandleWayEnum(String name) {
        this.name = name;
    }

    // 根据字符串获取枚举值
    public static RowErrorHandleWayEnum fromString(String name) {
        for (RowErrorHandleWayEnum type : RowErrorHandleWayEnum.values()) {
            if (type.name.equalsIgnoreCase(name)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown: " + name);
    }

    public String getName() {
        return name;
    }
}
