package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSourceDTO;

import java.util.List;
import java.util.Map;


@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@Data
public class SourceHdfsFileDTO extends BaseSeatunnelSourceDTO {

    //文件路径
    private String path;
    //文件格式类型：text、csv、parquet、orc、json、excel、xml、binary
    @JsonProperty("file_format_type")
    private String fileFormatType;
    @JsonProperty("fs.defaultFS")
    private String fsDefaultFS;
    //读取数据源的列列表，用户可以使用它来实现字段投影。
    @JsonProperty("read_columns")
    private List<String> readColumns;
    @JsonProperty("hdfs_site_path")
    private String hdfsSitePath;
    //当file_format为text，字段分隔符，告诉连接器如何对字段进行切片和切块，默认\001，与配置单元的默认分隔符相同
    @JsonProperty("field_delimiter")
    private String fieldDelimiter;
    //控制是否从文件路径解析分区键和值
    @JsonProperty("parse_partition_from_path")
    private Boolean parsePartitionFromPath;
    //日期类型格式，支持：yyyy-MM-dd、yyyy.MM.dd、yyyy/MM/dd，默认：yyyy-MM-dd
    @JsonProperty("date_format")
    private String dateFormat;
    @JsonProperty("datetime_format")
    private String datetimeFormat;
    //时间类型格式，支持HH:mm:ss、HH:mm:ss.SSS，默认：HH:mm:ss
    @JsonProperty("time_format")
    private String timeFormat;
    @JsonProperty("remote_user")
    private String remoteUser;
    @JsonProperty("krb5_path")
    private String krb5Path;
    @JsonProperty("kerberos_principal")
    private String kerberosPrincipal;
    @JsonProperty("kerberos_keytab_path")
    private String kerberosKeytabPath;
    //跳过前几行，仅限于txt和csv
    @JsonProperty("skip_header_row_number")
    private Long skipHeaderRowNumber;
    @JsonProperty("file_filter_pattern")
    private String fileFilter;
    @JsonProperty("filename_extension")
    private String fileExtension;
    //定义哪些字符串可以表示为text，仅当file_format_type为文本时使用，例如\N
    @JsonProperty("null_format")
    private String nullFormat;
    //数据的模型信息，file_format_type为text/json/excel/xml/csv
    private Map<String, Object> schema;
    //数据的模型信息，前端配置使用
    private String schemaVo;
    //sheet名称，file_format为excel时才需要配置
    @JsonProperty("sheet_name")
    private String sheetName;
    @JsonProperty("compress_codec")
    private String compressCodec;
    private String encoding;

    //用于定义多表任务，当您有多个表要读取时，可以使用此选项定义多个表
    @JsonProperty("tables_configs")
    private List<SourceHdfsFileDTO> tablesConfigs;
}