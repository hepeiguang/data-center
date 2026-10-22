package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums;

public enum CompilePatternEnum {

    /**
     * 源码
     */
    SOURCE_CODE("SOURCE_CODE"),
    /**
     * 绝对路径
     */
    ABSOLUTE_PATH("ABSOLUTE_PATH");

    private final String name;

    CompilePatternEnum(String name) {
        this.name = name;
    }

    // 根据字符串获取枚举值
    public static CompilePatternEnum fromString(String name) {
        for (CompilePatternEnum type : CompilePatternEnum.values()) {
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
