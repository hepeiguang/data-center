package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSourceDTO;
import shade.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
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
public class SourceHttpDTO extends BaseSeatunnelSourceDTO {
    private String url;
    private String method;
    private Map<String,Object> headers;
    private Map<String,Object> params;
    private String body;
    @JsonProperty(value = "poll_interval_millis")
    private Integer pollIntervalMillis;
    @JsonProperty(value = "retry_backoff_multiplier_ms")
    private Integer retryBackoffMultiplierMs;
    @JsonProperty(value = "retry_backoff_max_ms")
    private Integer retryBackoffMaxMs;
    @JsonProperty(value = "enable_multi_lines")
    private Boolean enableMultiLines;
    @JsonProperty(value = "connect_timeout_ms")
    private Integer connectTimeoutMs;
    @JsonProperty(value = "socket_timeout_ms")
    private Integer socketTimeoutMs;
    @JsonProperty(value = "keep_params_as_form")
    private Boolean keepParamsAsForm;
    @JsonProperty(value = "keep_page_param_as_http_param")
    private Boolean keepPageParamAsHttpParam;
    private Integer retry;
    @JsonProperty(value = "content_field")
    private String contentField;
    private String format;
    private Map<String, Object> httpExtraParams;
    /**
     * 前台使用的数据
     */
    private String schemaVo;
    private Map<String, Object> schema;

    /**
     * 分页参数信息
     */
    private String pageingVo;
    private Map<String, Object> pageing;

    /**
     * 迭代参数信息
     */
    private IterativeConfig iterativeConfig;

    /**
     * httpCheckCondition
     */
    private HttpCheckCondition httpCheckCondition = HttpCheckCondition.STATUS_CODE_DEFAULT;

    /**
     * condition
     */
    private String condition;

    /**
     * 迭代参数信息
     */
    @JsonProperty("isIterative")
    private boolean isIterative;

    /**
     * 前台使用的数据
     */
    private String jsonFieldVo;
    @JsonProperty(value = "json_field")
    private Map<String, Object> jsonField;

    public boolean isIterative() {
        return isIterative;
    }

    public void setIterative(boolean isIterative) {
        this.isIterative = isIterative;
    }
}