package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSourceDTO;
import shade.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonIgnoreProperties(ignoreUnknown = true)
public class SourceUdpDTO extends BaseSeatunnelSourceDTO {
    private String host;
    private Integer port;
    private Integer bufferSize;
    private String charset;
    private Boolean replyEnabled;
    private String replyMessage;
    private String format;
    /** none | js | offset_table */
    private String parseMode;
    /** skip | fail — 单包解析失败时跳过或作业失败 */
    private String parseErrorHandleWay;
    private String scriptPath;
    private String scriptContent;
    private String offsetTable;
    private String offsetTablePath;
    /**
     * UI/UDF 引用名：从 taskParams 反序列化，解析路径后清空，不写入 SeaTunnel conf。
     * 对应 {@code $SEATUNNEL_HOME/udf/udp-codec/} 下文件。
     */
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String udfName;
    /**
     * 前台使用的数据
     */
    private String schemaVo;
    private Map<String, Object> schema;

    public boolean isTextFormat() {
        if (parseMode != null
                && !parseMode.isEmpty()
                && !"none".equalsIgnoreCase(parseMode)) {
            return false;
        }
        return "text".equalsIgnoreCase(format);
    }
}
