package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSinkDTO;
import shade.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

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
public class SinkUdpMulticastDTO extends BaseSeatunnelSinkDTO {
    private String groupAddress;
    private Integer port;
    private Integer soTimeout;
    private Integer sendBufferSize;
    private Integer maxRetries;
    private Integer retryIntervalMs;
    private Boolean waitForReply;
    private Integer replyTimeoutMs;
    private Integer batchSize;
    private Integer batchIntervalMs;
    private Integer batchQueueSize;
    private Boolean asyncEnabled;
    private Integer asyncThreads;
    private Integer asyncQueueSize;
    private Long asyncWaitNanos;
    private String charsetName;
    private Integer maxPacketSize;

    private Map<String, Object> otherConfig;

}