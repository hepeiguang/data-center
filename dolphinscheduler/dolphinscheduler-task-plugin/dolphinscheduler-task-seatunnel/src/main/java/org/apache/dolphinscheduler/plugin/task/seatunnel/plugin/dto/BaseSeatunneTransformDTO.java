package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-19 11:31
 **/
@Data
@Slf4j
public abstract class BaseSeatunneTransformDTO {
    @JsonProperty("plugin_name")
    private String pluginName;
}
