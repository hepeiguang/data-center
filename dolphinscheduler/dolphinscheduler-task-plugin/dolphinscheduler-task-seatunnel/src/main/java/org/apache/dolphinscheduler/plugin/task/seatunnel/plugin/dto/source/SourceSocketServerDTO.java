package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSourceDTO;
import shade.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-24 16:56
 **/

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceSocketServerDTO extends BaseSeatunnelSourceDTO {
    private String host;
    private Integer port;
    private String charset;
    private Integer bufferSize;
    private Boolean replyEnabled;
    private String replyMessage;
    private Integer maxConnections;
    private Integer socketTimeout;
    private String delimiter;
    private String format;
    /**
     * 前台使用的数据
     */
    private String schemaVo;
    private Map<String, Object> schema;
}