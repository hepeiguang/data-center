package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums;

public enum CompileLanguageEnum {

    /**
     * JAVA
     */
    JAVA("JAVA"),
    /**
     * GROOVY
     */
    GROOVY("GROOVY");

    private final String name;

    CompileLanguageEnum(String name) {
        this.name = name;
    }

    // 根据字符串获取枚举值
    public static CompileLanguageEnum fromString(String name) {
        for (CompileLanguageEnum type : CompileLanguageEnum.values()) {
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
