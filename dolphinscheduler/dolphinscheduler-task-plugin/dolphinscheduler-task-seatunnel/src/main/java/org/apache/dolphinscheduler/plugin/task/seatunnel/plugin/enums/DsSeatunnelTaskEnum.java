package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums;

import com.aliyuncs.utils.StringUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Getter
@AllArgsConstructor
public enum DsSeatunnelTaskEnum {

    SOURCE_JDBC("SOURCE_JDBC", SeatunnelPlugin.SOURCE_JDBC, true, false),
    SOURCE_KAFKA("SOURCE_KAFKA", SeatunnelPlugin.SOURCE_KAFKA, false, false),
    SOURCE_FAKE("SOURCE_FAKE", SeatunnelPlugin.SOURCE_FAKE, false, false),
    SOURCE_FTPFILE("SOURCE_FTPFILE", SeatunnelPlugin.SOURCE_FTPFILE, false, false),
    SOURCE_HDFS("SOURCE_HDFS", SeatunnelPlugin.SOURCE_HDFS, false, false),
    SOURCE_HTTP("SOURCE_HTTP", SeatunnelPlugin.SOURCE_HTTP, false, false),
    SOURCE_HTTPSERVER("SOURCE_HTTPSERVER", SeatunnelPlugin.SOURCE_HTTPSERVER, false, false),
    SOURCE_LOCALFILE("SOURCE_LOCALFILE", SeatunnelPlugin.SOURCE_LOCALFILE, false, false),
    SOURCE_SFTPFILE("SOURCE_SFTPFILE", SeatunnelPlugin.SOURCE_SFTPFILE, false, false),
    SOURCE_SOCKETSERVER("SOURCE_SOCKETSERVER", SeatunnelPlugin.SOURCE_SOCKETSERVER, false, false),
    SOURCE_UDPMULTICAST("SOURCE_UDPMULTICAST", SeatunnelPlugin.SOURCE_UDPMULTICAST, false, false),
    SOURCE_UDP("SOURCE_UDP", SeatunnelPlugin.SOURCE_UDP, false, false),
    SOURCE_POSTGRESQL("SOURCE_POSTGRESQL", SeatunnelPlugin.SOURCE_POSTGRESQL, true, false),
    SOURCE_MQTT("SOURCE_MQTT", SeatunnelPlugin.SOURCE_MQTT, false, false),

    SINK_JDBC("SINK_JDBC", SeatunnelPlugin.SINK_JDBC, true, false),
    TRANSFORM_CONSTREFERENCE("TRANSFORM_CONSTREFERENCE", SeatunnelPlugin.TRANSFORM_SQL, false, true),
    TRANSFORM_SQLFUNCTION("TRANSFORM_SQLFUNCTION", SeatunnelPlugin.TRANSFORM_SQL, false, true),
    TRANSFORM_FILTER("TRANSFORM_FILTER", SeatunnelPlugin.TRANSFORM_FILTER, false, true),

    TRANSFORM_SORT("TRANSFORM_SORT", SeatunnelPlugin.INVALID, true, true),
    TRANSFORM_JOIN("TRANSFORM_JOIN", SeatunnelPlugin.INVALID, true, true),
    TRANSFORM_DATADISTINCT("TRANSFORM_DATADISTINCT", SeatunnelPlugin.INVALID, true, true),
    TRANSFORM_GROUP("TRANSFORM_GROUP", SeatunnelPlugin.INVALID, true, true),

    TRANSFORM_SQLCASEWHEN("TRANSFORM_SQLCASEWHEN", SeatunnelPlugin.TRANSFORM_SQL, false, false),
    TRANSFORM_HTTP("TRANSFORM_HTTP",SeatunnelPlugin.TRANSFORM_HTTP, false, true),
    TRANSFORM_SQLCOLUMNTOROW("TRANSFORM_SQLCOLUMNTOROW", SeatunnelPlugin.INVALID, true, true),
    TRANSFORM_SQLROWTOCOLUMN("TRANSFORM_SQLROWTOCOLUMN", SeatunnelPlugin.INVALID, true, true),
    TRANSFORM_SQLUNION("TRANSFORM_SQLUNION", SeatunnelPlugin.INVALID, true, true),
    TRANSFORM_SQLUNIONSORT("TRANSFORM_SQLUNIONSORT", SeatunnelPlugin.INVALID, true, true),
    TRANSFORM_DATANORMALIZE("TRANSFORM_DATANORMALIZE", SeatunnelPlugin.TRANSFORM_DICT, true, true),
	TRANSFORM_SQLJOIN("TRANSFORM_SQLJOIN", SeatunnelPlugin.INVALID, true, true),
    TRANSFORM_SPLITCOLUMNS("TRANSFORM_SPLITCOLUMNS", SeatunnelPlugin.INVALID, true, true),

    ;

    private final String taskName;
    private final SeatunnelPlugin plugin;
    private final Boolean supportCustomSql;
    private final Boolean needOutputField;


    private static Map<String, DsSeatunnelTaskEnum> taskEnumMap = new HashMap<>();

    static {
        for (DsSeatunnelTaskEnum taskEnum : DsSeatunnelTaskEnum.values()) {
            taskEnumMap.put(taskEnum.getTaskName().toUpperCase(Locale.ROOT), taskEnum);
        }
    }

    public static boolean needCustomSql(String type, String taskName) {
        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(taskName)) {
            throw new RuntimeException("task type is empty or task name is empty.");
        }
        return needCustomSql((type + "_" + taskName).toUpperCase(Locale.ROOT));
    }

    public static boolean needOutputField(String type, String taskName) {
        if (StringUtils.isEmpty(type) || StringUtils.isEmpty(taskName)) {
            throw new RuntimeException("task type is empty or task name is empty.");
        }
        return needOutputField((type + "_" + taskName).toUpperCase(Locale.ROOT));
    }

    public static boolean needCustomSql(String taskName) {
        DsSeatunnelTaskEnum taskEnum = taskEnumMap.get(taskName.toUpperCase(Locale.ROOT));
        if (taskEnum == null) {
            return false;
        }
        return taskEnum.getPlugin().getTaskTypeEnum() == SeatunnelTaskTypeEnum.INVALID;
    }

    public static boolean needOutputField(String taskName) {
        DsSeatunnelTaskEnum taskEnum = taskEnumMap.get(taskName.toUpperCase(Locale.ROOT));
        if (taskEnum == null) {
            return false;
        }
        return taskEnum.getNeedOutputField();
    }


}
