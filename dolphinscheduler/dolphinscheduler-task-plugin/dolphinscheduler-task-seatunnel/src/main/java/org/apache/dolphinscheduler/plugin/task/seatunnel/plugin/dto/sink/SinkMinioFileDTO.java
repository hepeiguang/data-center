package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSinkDTO;

import java.util.List;
import java.util.Map;

/**
 * Sink对象-Minio文件
 *
 * @author ZhaoYuan
 * @date 2025/5/11 17:26
 * @since JDK1.8
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@Data
public class SinkMinioFileDTO extends BaseSeatunnelSinkDTO {

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
    //临时文件路径，结果文件将首先写入临时路径，然后使用mv将临时目录提交到目标目录，默认/tmp/seatunnel
    @JsonProperty("tmp_path")
    private String tmpPath;
    //文件格式类型：text、csv、parquet、orc、json、excel、xml、binary
    @JsonProperty("file_format_type")
    private String fileFormatType;
    //是否需要自定义文件名
    @JsonProperty("custom_filename")
    private Boolean customFilename;
    //自定义文件名格式，仅在custom_filename为true时使用，默认"${transactionId}"
    @JsonProperty("file_name_expression")
    private String fileNameExpression;
    //自定义文件名时间格式，仅在custom_filename为true时使用，默认yyyy.MM.dd
    @JsonProperty("filename_time_format")
    private String filenameTimeFormat;
    //字段分隔符，仅当file_format为text，默认\001
    @JsonProperty("field_delimiter")
    private String fieldDelimiter;
    //行分隔符，仅在file_format_type为text、csv、json时使用，默认\n
    @JsonProperty("row_delimiter")
    private String rowDelimiter;
    //是否需要处理分区，默认false
    @JsonProperty("have_partition")
    private Boolean havePartition;
    //基于选定字段进行数据分区，仅在have_partition为true时使用
    @JsonProperty("partition_by")
    private List<String> partitionBy;
    //基于选定字段进行数据分区，用于前后端交互
    private String partitionByVo;
    //如果指定了partition_by，我们将基于分区信息生成相应的分区目录，最终文件将放置在分区目录中；仅在have_partition为true时使用，默认"${k0}=${v0}/${k1}=${v1}/.../${kn}=${vn}/"
    @JsonProperty("partition_dir_expression")
    private String partitionDirExpression;
    //分区字段及其值是否写入数据文件，仅在have_partition为true时使用，默认false
    @JsonProperty("is_partition_field_write_in_file")
    private Boolean isPartitionFieldWriteInFile;
    //需要写入文件的列，字段的顺序决定了实际写入文件的顺序，当此参数为空时，默认值为从Transform或Source获取的所有列
    @JsonProperty("sink_columns")
    private List<String> sinkColumns;
    //需要写入文件的列，用于前后端交互
    private String sinkColumnsVo;
    //是否启用事务，确保数据在写入目标目录时不会丢失或重复，默认true，将自动在文件名前添加：${transactionId}_
    @JsonProperty("is_enable_transaction")
    private Boolean isEnableTransaction;
    //文件中的最大行数，默认1000000，文件中的行数由batch_size和checkpoint.interval共同决定
    @JsonProperty("batch_size")
    private Integer batchSize;
    //文件的压缩编码，支持的压缩编码如下所示：txt:lzo/none、json:lzo/none、csv:lzo/none、orc/parquet:自动识别压缩类型，无需配置
    @JsonProperty("compress_codec")
    private String compressCodec;
    //内存可以缓存的最大数据行数，仅当file_format_type为excel
    @JsonProperty("max_rows_in_memory")
    private String maxRowsInMemory;
    //sheet名称，仅当file_format_type为excel，默认Sheet${随机数}/Sheet${Random number}
    @JsonProperty("sheet_name")
    private String sheetName;
    //CSV的字符串引用模式，默认MINIMAL，ALL所有字符串字段都会被引用/MINIMAL引用包含特殊字符的字段，如字段分隔符、引用字符或行分隔符字符串中的任何字符/NONE从不引用字段，当数据中出现分隔符时，打印机会在其前面加上转义字符，如果未设置转义字符，格式验证将抛出异常，file_format_type为csv
    @JsonProperty("csv_string_quote_mode")
    private String csvStringQuoteMode;
    //指定XML文件中根元素的标签名称，默认RECORDS，仅当file_format_type为xml
    @JsonProperty("xml_root_tag")
    private String xmlRootTag;
    //指定XML文件中数据行的标签名称，默认RECORD，仅当file_format_type为xml
    @JsonProperty("xml_row_tag")
    private String xmlRowTag;
    //指定是否使用标签属性格式处理数据，默认false，仅当file_format_type为xml
    @JsonProperty("xml_use_attr_format")
    private Boolean xmlUseAttrFormat;
    //每个并行度是否输出一个文件，当此参数开启时，batch_size将不会生效，输出文件名不会有文件块后缀
    @JsonProperty("single_file_mode")
    private Boolean singleFileMode;
    //当上游没有数据同步时，是否生成相应的数据文件，默认false
    @JsonProperty("create_empty_file_when_no_data")
    private Boolean createEmptyFileWhenNoData;
    //支持将时间戳写入Parquet INT96，file_format_type为parquet，默认false
    @JsonProperty("parquet_avro_write_timestamp_as_int96")
    private Boolean parquetAvroWriteTimestampAsInt96;
    //支持将12-byte字段写入Parquet INT96，file_format_type为parquet
    @JsonProperty("parquet_avro_write_fixed_as_int96")
    private List<String> parquetAvroWriteFixedAsInt96;
    //在开启同步任务之前，对目标路径进行不同的处理，默认CREATE_SCHEMA_WHEN_NOT_EXIST，RECREATE_SCHEMA当路径不存在时创建，如果路径已存在则删除路径并重新创建/CREATE_SCHEMA_WHEN_NOT_EXIST当路径不存在时创建，路径存在时使用路径/ERROR_WHEN_SCHEMA_NOT_EXIST当路径不存在时报错/IGNORE忽略表的处理
    @JsonProperty("schema_save_mode")
    private String schemaSaveMode;
    //在开启同步任务之前，对目标路径中的数据文件进行不同的处理，默认APPEND_DATA，DROP_DATA使用路径但删除路径中的数据文件/APPEND_DATA使用路径并在路径中添加新文件以写入数据/ERROR_WHEN_DATA_EXISTS当路径中存在数据文件时，将报错
    @JsonProperty("data_save_mode")
    private String dataSaveMode;
    //是否写入表头，仅在file_format_type为text/csv时使用，false不写入表头/true写入表头，默认false
    @JsonProperty("enable_header_write")
    private Boolean enableHeaderWrite;
    //仅在file_format_type为json/text/csv/xml时使用，默认UTF-8
    private String encoding;
}