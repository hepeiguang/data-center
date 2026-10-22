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
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SourceMqttDTO extends BaseSeatunnelSourceDTO {
    /**
     * 订阅主题和主题分级。成对出现,页面传输的参数
     */
    private List<MqttTopicDTO> topicList;
    /**
     * 订阅主题和主题分级。成对出现，传输给seatunnel的参数，由topicList转换的
     */
    @JsonProperty("topics")
    private Map<String, String> topics;
    /**
     * 客户端ID前缀。
     */
    @JsonProperty("client_id_prefix")
    private String clientIdPrefix;
    @JsonProperty("username")
    private String username;
    @JsonProperty("password")
    private String password;
    /**
     * 是否清理会话。
     */
    @JsonProperty("clean_session")
    private boolean cleanSession;
    /**
     * 心跳间隔。
     */
    @JsonProperty("keep_alive_interval")
    private Integer keepAliveInterval;
    /**
     * 连接超时时间。
     */
    @JsonProperty("connection_timeout")
    private Integer connectionTimeout;
    /**
     * 是否自动重连。
     */
    @JsonProperty("automatic_reconnect")
    private boolean automaticReconnect;
    /**
     * 最大重连延迟。
     */
    @JsonProperty("max_reconnect_delay")
    private Integer maxReconnectDelay;
    /**
     * 是否使用 SSL。暂时不实现
     */
    private boolean useSsl;
    private String sslKeyStore;
    private String sslKeyStorePassword;
    private String sslTrustStore;
    private String sslTrustStorePassword;
    /**
     * 主题列表配置，你可以同时配置一个 table_list 和一个 topic。
     */
    @JsonProperty("table_list")
    private Map<String, Object> tableList;

    /**
     * 逗号分隔的 mqtt brokers 列表。
     */
    @JsonProperty("broker_urls")
    private String brokenUrls;

    /**
     * 数据结构，包括字段名称和字段类型。
     */
    private Map<String, Object> schema;

    private String schemaVo;

    /**
     * 数据格式。默认格式为 json。可选格式包括 text,默认字段分隔符为 ", "。如果自定义分隔符，添加 "field_delimiter" 选项。
     */
    @JsonProperty("message_format")
    private String messageFormat = "json";

    /**
     * 自定义数据格式的字段分隔符。
     */
    @JsonProperty("field_delimiter")
    private String fieldDelimiter;
    @JsonProperty("async_processing")
    private boolean asyncProcessing;
    @JsonProperty("processing_threads")
    private Integer processingThreads;
//    @JsonProperty("max_inflight")
//    private Integer maxInflight;
//    @JsonProperty("receive_buffer_size")
//    private Integer receiveBufferSize;
}