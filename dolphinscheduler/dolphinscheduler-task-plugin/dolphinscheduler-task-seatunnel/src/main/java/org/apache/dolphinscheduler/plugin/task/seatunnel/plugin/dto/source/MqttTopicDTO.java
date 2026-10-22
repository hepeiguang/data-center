package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import lombok.Data;

@Data
public class MqttTopicDTO {
    private String topic;
    private String qos;
}
