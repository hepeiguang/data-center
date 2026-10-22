package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SeatunnelPlugin {
    SOURCE_JDBC("JDBC", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_KAFKA("KAFKA", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_FAKE("FAKE", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_FTPFILE("FTPFILE", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_HDFS("HDFS", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_HTTP("HTTP", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_HTTPSERVER("HTTPSERVER", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_LOCALFILE("LOCALFILE", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_SFTPFILE("SFTPFILE", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_SOCKETSERVER("SOCKETSERVER", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_UDPMULTICAST("UDPMULTICAST", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_UDP("UDP", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_POSTGRESQL("POSTGRESQL", SeatunnelTaskTypeEnum.SOURCE),
    SOURCE_MQTT("MQTT", SeatunnelTaskTypeEnum.SOURCE),

    SINK_JDBC("JDBC", SeatunnelTaskTypeEnum.SINK),
    SINK_KAFKA("KAFKA", SeatunnelTaskTypeEnum.SINK),
    SINK_FTPFILE("FTPFILE", SeatunnelTaskTypeEnum.SINK),
    SINK_HDFS("HDFS", SeatunnelTaskTypeEnum.SINK),
    SINK_HTTP("HTTP", SeatunnelTaskTypeEnum.SINK),
    SINK_LOCALFILE("LOCALFILE", SeatunnelTaskTypeEnum.SINK),
    SINK_SFTPFILE("SFTPFILE", SeatunnelTaskTypeEnum.SINK),
    SINK_UDP("UDP", SeatunnelTaskTypeEnum.SINK),


    TRANSFORM_SQL("SQL", SeatunnelTaskTypeEnum.TRANSFORM),
    TRANSFORM_FILTER("FILTER", SeatunnelTaskTypeEnum.TRANSFORM),
    TRANSFORM_DICT("Dict", SeatunnelTaskTypeEnum.TRANSFORM),
    TRANSFORM_HTTP("HTTP", SeatunnelTaskTypeEnum.TRANSFORM),

    INVALID("", SeatunnelTaskTypeEnum.INVALID),
    ;

    private final String pluginName;
    private final SeatunnelTaskTypeEnum taskTypeEnum;
}
