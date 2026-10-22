package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSourceDTO;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SourceDorisDTO extends BaseSeatunnelSourceDTO {
    private String fenodes;
    private String username;
    private String password;
    @JsonProperty("table_list")
    private List tableList;
    /* 请求Doris FE的重试次数*/
    @JsonProperty("doris.request.retries")
    private Integer dorisRequestRetries;
    /* 请求Doris 读取超时时间*/
    @JsonProperty("doris.request.read.timeout.ms")
    private Integer dorisRequestReadTimeoutMs;
    /* 请求Doris 连接超时时间*/
    @JsonProperty("doris.request.connect.timeout.ms")
    private Integer dorisRequestConnectTimeoutMs;
    /* Doris查询端口*/
    @JsonProperty("query-port")
    private Integer queryPort;
    /* Doris扫描数据的超时时间，单位秒*/
    @JsonProperty("doris.request.query.timeout.s")
    private Integer dorisRequestQueryTimeouts;
    private String database;
    @JsonProperty("table_path")
    private String tablePath;

    private String table;
    /* 选择要读取的Doris表字段*/
    @JsonProperty("doris.read.field")
    private String dorisReadField;
    /* 数据过滤. 格式："字段 = 值", 例如：doris.filter.query = "F_ID > 2"*/
    @JsonProperty("doris.filter.query")
    private String dorisFilterQuery;

    /* Doris扫描数据的超时时间，单位秒*/
    @JsonProperty("doris.batch.size")
    private Integer dorisBatchSize;

    /* Doris扫描数据的超时时间，单位秒*/
    @JsonProperty("doris.exec.mem.limit")
    private Long dorisExecMemLimit;

}