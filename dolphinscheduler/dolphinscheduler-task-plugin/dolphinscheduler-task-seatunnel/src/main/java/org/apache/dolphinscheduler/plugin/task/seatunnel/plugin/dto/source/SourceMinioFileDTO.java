package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSourceDTO;

import java.util.Map;

/**
 * 源传输对象-Minio文件插件
 *
 * @author ZhaoYuan
 * @date 2025/5/11 9:14
 * @since JDK1.8
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@Data
public class SourceMinioFileDTO extends BaseSeatunnelSourceDTO {

    //主机名
    @JsonProperty("fs.s3a.endpoint")
    private String fsS3aEndpoint;
    //认证方式
    @JsonProperty("fs.s3a.aws.credentials.provider")
    private String fsS3aAwsCredentialsProvider = "org.apache.hadoop.fs.s3a.SimpleAWSCredentialsProvider";
    //ak
    @JsonProperty("access_key")
    private String accessKey;
    //sk
    @JsonProperty("secret_key")
    private String secretKey;
    //其他选项
    @JsonProperty("hadoop_s3_properties")
    private Map<String, Object> hadoopS3Properties;
    //
    private String bucket;
    //源文件路径
    private String path;
    //文件格式类型：text、csv、parquet、orc、json、excel、xml、binary
    @JsonProperty("file_format_type")
    private String fileFormatType;
    //数据的模型信息，file_format_type为text/json/excel/xml/csv
    private Map<String, Object> schema;
    //数据的模型信息，前端配置使用
    private String schemaVo;
    //过滤模式，用于过滤文件，该模式遵循标准正则表达式
    @JsonProperty("file_filter_pattern")
    private String fileFilterPattern;
    //过滤文件扩展名，用于过滤具有特定扩展名的文件：csv/txt/json/xml
    @JsonProperty("filename_extension")
    private String filenameExtension;
    //当file_format为text，字段分隔符，告诉连接器如何对字段进行切片和切块，默认\001，与配置单元的默认分隔符相同
    @JsonProperty("field_delimiter")
    private String fieldDelimiter;
    //读取数据源的列列表，用户可以使用它来实现字段投影。
    //@JsonProperty("read_columns")
    ///private List<String> readColumns;
    //控制是否从文件路径解析分区键和值，默认true
    @JsonProperty("parse_partition_from_path")
    private Boolean parsePartitionFromPath;
    //日期类型格式，支持：yyyy-MM-dd、yyyy.MM.dd、yyyy/MM/dd，默认：yyyy-MM-dd
    @JsonProperty("date_format")
    private String dateFormat;
    //Datetime类型格式，支持yyyy-MM-dd HH:mm:ss、yyyy.MM.dd HH:mm:ss、yyyy/MM/dd HH:mm:ss、yyyyMMddHHmmss，默认yyyy-MM-dd HH:mm:ss
    @JsonProperty("datetime_format")
    private String datetimeFormat;
    //时间类型格式，支持HH:mm:ss、HH:mm:ss.SSS，默认：HH:mm:ss
    @JsonProperty("time_format")
    private String timeFormat;
    //跳过前几行，仅限于txt和csv，默认0
    @JsonProperty("skip_header_row_number")
    private Long skipHeaderRowNumber;
    //读取工作簿中的工作表，仅在文件格式类型为excel时使用
    @JsonProperty("sheet_name")
    private String sheetName;
    //指定XML文件中数据行的标签名称，当file_format为xml时才需要配置，指定XML文件中数据行的标记名称
    @JsonProperty("xml_row_tag")
    private String xmlRowTag;
    //指定是否使用标记属性格式处理数据，当file_format为xml时才需要配置
    @JsonProperty("xml_use_attr_format")
    private Boolean xmlUseAttrFormat;
    //是否使用标题行解析文件，仅当file_format为csv且文件包含与RFC 4180匹配的标题行时使用，默认false
    @JsonProperty("csv_use_header_line")
    private Boolean csvUseHeaderLine;
    //文件的压缩编码，支持的压缩编码如下所示：txt:lzo/none、json:lzo/none、csv:lzo/none、orc/parquet:自动识别压缩类型，无需配置
    @JsonProperty("compress_codec")
    private String compressCodec;
    //归档文件的压缩编解码器，ZIP:txt/json/excel/xml、TAR:txt/json/excel/xml、TAR_GZ:txt/json/excel/xml、GZ:txt/json/excel/xml、NONE
    @JsonProperty("archive_compress_codec")
    private String archiveCompressCodec;
    //读取的文件编码，file_format_type为json/text/csv/xml，默认UTF-8
    private String encoding;
    //定义哪些字符串可以表示为null，仅当file_format_type为文本时使用，例如\N
    @JsonProperty("null_format")
    private String nullFormat;
    //用于定义多表任务，当您有多个表要读取时，可以使用此选项定义多个表
    //@JsonProperty("tables_configs")
    //private List<SourceFtpFileDTO> tablesConfigs;
}