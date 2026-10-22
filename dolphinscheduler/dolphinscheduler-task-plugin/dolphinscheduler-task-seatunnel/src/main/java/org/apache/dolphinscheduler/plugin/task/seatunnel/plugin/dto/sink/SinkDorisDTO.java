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

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SinkDorisDTO extends BaseSeatunnelSinkDTO {
    private String fenodes;
    private String username;
    private String password;
    @JsonProperty("table_list")
    private List tableList;
    /* Doris 表的名称，2.3.5 版本后将弃用*/
    @JsonProperty("table.identifier")
    private String tableIdentifier;
    /* Doris 表的名称，2.3.5 版本后将弃用*/
    @JsonProperty("sink.label-prefix")
    private String sinkLabelPrefix;
    /* 是否启动删除*/
    @JsonProperty("sink.enable-delete")
    private Boolean sinkEnableDelete;
    /* 是否启用不支持的类型转换，例如 Decimal64 到 Double  boolean 默认false*/
    @JsonProperty("needs_Unsupported_type_casting")
    private Boolean needsUnsupportedTypeCasting;
    private String database;
    private String table;
    @JsonProperty("query-port")
    private Integer queryPort;
    /* 是否保留表名和字段名的原始大小写。当设置为 false 时，表名和字段名将被转换为小写。 boolean 默认true*/
    @JsonProperty("case_sensitive")
    private Boolean caseSensitive;
    /* 	是否启用两阶段提交（2pc），默认为 false*/
    @JsonProperty("sink.enable-2pc")
    private Boolean sinkEnable2pc ;

    /* Doris扫描数据的超时时间，单位秒*/
    @JsonProperty("sink.check-interval")
    private Integer sinkCheckInterval;

    /* 	向数据库写入记录失败时的最大重试次数*/
    @JsonProperty("sink.max-retries")
    private Integer sinkMaxRetries;
    /* 	用于缓存stream load数据的缓冲区大小。*/
    @JsonProperty("sink.buffer-size")
    private Integer sinkBufferSize;
    /* 用于缓存stream load数据的缓冲区计数。*/
    @JsonProperty("sink.buffer-count")
    private Integer sinkBufferCount;
    /* 	每次http请求写入doris的批量大小，当row达到该大小或者执行checkpoint时，缓存的数据就会写入服务器。*/
    @JsonProperty("doris.batch.size")
    private Integer dorisBatchSize;
    /**
     * 当data_save_mode选择CUSTOM_PROCESSING时，需要填写CUSTOM_SQL参数。该参数通常填写一条可以执行的SQL。SQL将在同步任务之前执行
     */
    @JsonProperty("custom_sql")
    private String customSql;
    /**
     * 	该选项用于支持自动生成sql时的insert、delete、update等操作，以及支持的格式。。
     */
    @JsonProperty("doris.config")
    private Map<String, String> dorisConfig;
    /**
     * schema保存模式
     */
    @JsonProperty("schema_save_mode")
    private String schemaSaveMode;
    /**
     * 数据保存模式
     */
    @JsonProperty("data_save_mode")
    private String dataSaveMode;
    /**
     * 数据保存模式
     */
    @JsonProperty("save_mode_create_template")
    private String saveModeCreateTemplate;

    private String format;
    private String readJsonByLine;
    private String columnSeparator;

    // 增加主键设置
    @JsonProperty("primary_keys")
    private List<String> primaryKeys;

    // 增加主键设置
    @JsonProperty("dup_keys")
    private List<String> dupKeys;

}