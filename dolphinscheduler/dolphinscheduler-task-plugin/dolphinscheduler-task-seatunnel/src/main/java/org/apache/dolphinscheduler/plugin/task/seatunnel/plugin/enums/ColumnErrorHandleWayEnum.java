package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums;

public enum ColumnErrorHandleWayEnum {

    /**
     * 数据处理异常时抛出
     */
    FAIL("FAIL"),
    /**
     * 数据处理异常时跳过
     */
    SKIP("SKIP"),
    /**
     * 数据处理异常时跳过当前行
     */
    SKIP_ROW("SKIP_ROW");

    private final String name;

    ColumnErrorHandleWayEnum(String name) {
        this.name = name;
    }

    // 根据字符串获取枚举值
    public static ColumnErrorHandleWayEnum fromString(String name) {
        for (ColumnErrorHandleWayEnum type : ColumnErrorHandleWayEnum.values()) {
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
