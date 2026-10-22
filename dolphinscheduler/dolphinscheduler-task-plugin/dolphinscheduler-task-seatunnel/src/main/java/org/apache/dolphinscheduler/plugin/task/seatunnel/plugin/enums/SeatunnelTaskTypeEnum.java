package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 18:25
 **/
public enum SeatunnelTaskTypeEnum {
    SOURCE("source"),
    TRANSFORM("transform"),
    SINK("sink"),
    INVALID("invalid")
    ;

    private final String typeName;

    SeatunnelTaskTypeEnum(String typeName) {
        this.typeName = typeName;
    }

    // 根据字符串获取枚举值
    public static SeatunnelTaskTypeEnum fromString(String typeName) {
        for (SeatunnelTaskTypeEnum type : SeatunnelTaskTypeEnum.values()) {
            if (type.typeName.equalsIgnoreCase(typeName)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown task type: " + typeName);
    }

    public String getTypeName() {
        return typeName;
    }
}
