package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-19 11:29
 **/
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@Data
public class SeatunnelEnvDTO {
    @JsonProperty("job.name")
    private String jobName;
    @JsonProperty("jars")
    private String jars;

    @JsonProperty("job.mode")
    private String jobMode;
    @JsonProperty("checkpoint.interval")
    private Integer checkpointInterval;
    @JsonProperty("checkpoint.timeout")
    private String checkpointTimeout;
    @JsonProperty("parallelism")
    private Integer parallelism;
    @JsonProperty("shade.identifier")
    private String shadeIdentifier;
}