package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class SinkHttpDTO extends BaseSeatunnelSinkDTO {
    private String url;
    private Map<String,Object> headers;
    private Integer retryBackoffMultiplierMs;
    private Integer retryBackoffMaxMs;
    private Integer connectTimeoutMs;
    private Integer socketTimeoutMs;
}