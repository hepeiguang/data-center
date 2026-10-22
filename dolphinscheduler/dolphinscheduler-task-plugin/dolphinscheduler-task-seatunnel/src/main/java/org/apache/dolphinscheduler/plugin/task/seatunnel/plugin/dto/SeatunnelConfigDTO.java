package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.util.List;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-19 11:28
 **/
@Data
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SeatunnelConfigDTO {
    private SeatunnelEnvDTO env;
    private List<BaseSeatunnelSourceDTO> source;
    private List<BaseSeatunnelSinkDTO> sink;
    private List<BaseSeatunneTransformDTO> transform;
}
