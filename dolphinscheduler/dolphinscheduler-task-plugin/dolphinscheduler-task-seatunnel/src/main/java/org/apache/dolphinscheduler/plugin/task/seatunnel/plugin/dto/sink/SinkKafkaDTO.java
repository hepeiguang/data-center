package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSinkDTO;
import shade.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
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
public class SinkKafkaDTO extends BaseSeatunnelSinkDTO {
    /**
     * 当表用作接收器时，topic 名称是要写入数据的 topic
     */
    private String topic;
    /**
     * Kafka brokers 使用逗号分隔
     */
    @JsonProperty("bootstrap.servers")
    private String bootstrapServers;

    /**
     * 	除了上述 Kafka Producer 客户端必须指定的参数外，用户还可以为 Producer 客户端指定多个非强制参数，涵盖 Kafka官方文档中指定的所有生产者参数
     */
    @JsonProperty("kafka.config")
    private Map<String, Object> kafkaConfig;

    /**
     * 可以选择的语义是 EXACTLY_ONCE/AT_LEAST_ONCE/NON，默认 NON。
     */
    private String semantics;

    /**
     * 配置字段用作 kafka 消息的key
     */

    @JsonProperty("partition_key_fields")
    private List partitionKeyFields;

    /**
     * 可以指定分区，所有消息都会发送到此分区
     */
    private int partition;
    /**
     * 可以根据消息的内容决定发送哪个分区,该参数的作用是分发信息
     */
    @JsonProperty("assign_partitions")
    private List assignPartitions;
    /**
     * 	如果语义指定为EXACTLY_ONCE，生产者将把所有消息写入一个 Kafka 事务中，kafka 通过不同的 transactionId 来区分不同的事务。该参数是kafka transactionId的前缀，确保不同的作业使用不同的前缀
     */
    @JsonProperty("transaction_prefix")
    private String transactionPrefix;
    /**
     * 数据格式。默认格式是json。可选文本格式，canal-json、debezium-json 、 avro 、 protobuf 和native。如果使用 json 或文本格式。默认字段分隔符是,。如果自定义分隔符，请添加field_delimiter选项。如果使用canal格式，请参考canal-json。如果使用debezium格式，请参阅 debezium-json 了解详细信息
     */

    private String format;
    /**
     * 自定义数据格式的字段分隔符
     */
    @JsonProperty("field_delimiter")
    private String fieldDelimiter;

    /**
     * format配置为protobuf时生效，取Message名称
     */
    @JsonProperty("protobuf_message_name")
    private String protobufMessageName;

    /**
     * format配置为protobuf时生效取Schema名称
     */
    @JsonProperty("protobuf_schema")
    private String protobufSchema;
}