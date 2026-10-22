package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSinkDTO;
import shade.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-24 16:57
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SinkUdpDTO extends BaseSeatunnelSinkDTO {
    private Integer port;
    private String host;
    private Integer maxRetries;
    private Integer timeoutMs;
    private Boolean enableMessageReply;
    private Integer retryIntervalMs;
    // 异步配置
    private Boolean asyncEnabled;
    private Integer asyncThreads;
}