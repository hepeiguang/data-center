package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
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
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SourceKafkaDTO extends BaseSeatunnelSourceDTO {
    /**
     * 使用表作为数据源时要读取数据的主题名称。它也支持通过逗号分隔的多个主题列表，例如 'topic-1,topic-2'。
     */
    private String topic;
    /**
     * 主题列表配置，你可以同时配置一个 table_list 和一个 topic。
     */
    @JsonProperty("table_list")
    private Map<String, Object> tableList;

    /**
     * 逗号分隔的 Kafka brokers 列表。
     */
    @JsonProperty("bootstrap.servers")
    private String bootstrapServers;

    /**
     * 如果 pattern 设置为 true，则会使用指定的正则表达式匹配并订阅主题。
     */
    private Boolean pattern = false;

    /**
     * Kafka 消费者组 ID，用于区分不同的消费者组
     */

    @JsonProperty("consumer.group")
    private String consumerGroup = "SeaTunnel-Consumer-Group";

    /**
     * 如果为 true，消费者的偏移量将会定期在后台提交。
     */

    @JsonProperty("commit_on_checkpoint")
    private Boolean commitOnCheckpoint = true;

    /**
     * kafka主动拉取时间间隔(毫秒)。
     */
    @JsonProperty("poll.timeout")
    private Long pollTimeout = 10000L;

    /**
     * 除了上述必要参数外，用户还可以指定多个非强制的消费者客户端参数，覆盖 Kafka 官方文档 中指定的所有消费者参数。
     */
    @JsonProperty("kafka.config")
    private Map<String, Object> kafkaConfig;

    /**
     * 数据结构，包括字段名称和字段类型。
     */
    private Map<String, Object> schema;

    private String schemaVo;

    /**
     * 数据格式。默认格式为 json。可选格式包括 text, canal_json, debezium_json, ogg_json, maxwell_json, avro , protobuf和native。默认字段分隔符为 ", "。如果自定义分隔符，添加 "field_delimiter" 选项。如果使用 canal 格式，请参考 canal-json 了解详细信息。如果使用 debezium 格式，请参考 debezium-json。一些Format的详细信息请参考 formats
     */

    private String format = "json";

    /**
     * 数据格式错误的处理方式。默认值为 fail，可选值为 fail 和 skip。当选择 fail 时，数据格式错误将阻塞并抛出异常。当选择 skip 时，数据格式错误将跳过此行数据。
     */
    @JsonProperty("format_error_handle_way")
    private String formatErrorHandleWay = "fail";

    /**
     * 用于过滤 debezium 格式的数据，仅当格式设置为 debezium_json 时使用。请参阅下面的 debezium_record_table_filter
     */
    @JsonProperty("debezium_record_table_filter")
    private Map<String, Object> debeziumRecordTableFilter;

    /**
     * 自定义数据格式的字段分隔符。
     */
    @JsonProperty("field_delimiter")
    private String fieldDelimiter;

    /**
     * 消费者的初始消费模式。 StartMode[earliest],[group_offsets]
     */
    @JsonProperty("start_mode")
    private String startMode = "group_offsets";

    /**
     * 用于 specific_offsets 消费模式的偏移量
     */
    @JsonProperty("start_mode.offsets")
    private Map<String, Object> startModeOffsets;

    /**
     * start_mode.timestamp
     */
    @JsonProperty("start_mode.timestamp")
    private Long startModeTimestamp;

    /**
     * 动态发现主题和分区的间隔时间。
     */
    @JsonProperty("partition-discovery.interval-millis")
    private Long partitionDiscoveryIntervalMillis = -1L;

    /**
     * 当格式设置为 protobuf 时有效，指定消息名称。
     */
    @JsonProperty("protobuf_message_name")
    private String protobufMessageName;

    /**
     * 当格式设置为 protobuf 时有效，指定 Schema 定义。
     */
    @JsonProperty("protobufSchema")
    private String protobuf_schema;

    /**
     * 支持保留record的源信息。
     */
    @JsonProperty("is_native")
    private Boolean isNative = false;
    /**
     * 当format 设置为debezium_json时，json中的主句
     * 在此配置中，您必须指定 schema 和 debezium_record_include_schema 选项：
     *
     * schema 应与您的表格式相同
     * 如果您的 json 数据包含 schema 字段，debezium_record_include_schema 应为 true，如果您的 json 数据不包含 schema 字段，debezium_record_include_schema 应为 false
     * {"schema" : {}, "payload": { "before" : {}, "after": {} ... } } --> true
     * {"before" : {}, "after": {} ... } --> false"
     */
    @JsonProperty("debezium_record_include_schema")
    private Boolean debeziumRecordIncludeSchema ;

}