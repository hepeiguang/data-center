package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UdpMulticastParamDTO {

    private String multicastAddress;
    private Integer port;
    private Integer connectTimeout;
    private Integer bufferSize;
    private UdpMulticastParamDTOBuilder other;

    @Data
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UdpMulticastParamDTOBuilder {
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
        private Integer ttl;
        private Boolean reuseAddress;
        private Boolean replyEnabled;
        private String replyMessage;
    }
}
