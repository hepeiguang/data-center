package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSinkDTO;

import java.util.List;

/**
 * Sink对象-本地文件
 *
 * @author ZhaoYuan
 * @date 2025/5/1 11:24
 * @since JDK1.8
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@Data
public class SinkLocalfileDTO extends BaseSeatunnelSinkDTO {

    //文件路径
    private String path;
    //临时文件路径，结果文件将首先写入临时路径，然后使用mv将临时目录提交到目标目录，默认/tmp/seatunnel
    @JsonProperty("tmp_path")
    private String tmpPath;
    //是否需要自定义文件名
    @JsonProperty("custom_filename")
    private Boolean customFilename;
    //自定义文件名格式，仅在custom_filename为true时使用，默认"${transactionId}"
    @JsonProperty("file_name_expression")
    private String fileNameExpression;
    //自定义文件名时间格式，仅在custom_filename为true时使用，默认yyyy.MM.dd
    @JsonProperty("filename_time_format")
    private String filenameTimeFormat;
    //文件格式类型：text、json、excel，默认csv
    @JsonProperty("file_format_type")
    private String fileFormatType;
    //自定义的文件扩展名，覆盖默认的文件扩展名，例如：.json、.customtype
    @JsonProperty("filename_extension")
    private String filenameExtension;
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
    //是否启用事务，默认true，确保数据在写入目标目录时不会丢失或重复；如果is_enable_transaction为 true，我们将自动在文件名前添加：${transactionId}_，仅支持true
    @JsonProperty("is_enable_transaction")
    private Boolean isEnableTransaction;
    //文件中的最大行数，默认1000000
    @JsonProperty("batch_size")
    private Integer batchSize;
    //每个并行度只会输出一个文件，当此参数开启时，batch_size就不会生效，输出的文件名没有文件块后缀，默认false
    @JsonProperty("single_file_mode")
    private Boolean singleFileMode;
    //当上游没有数据同步时，依然生成对应的数据文件，默认false
    @JsonProperty("create_empty_file_when_no_data")
    private Boolean createEmptyFileWhenNoData;
    //文件压缩编码none，默认：txt:lzo/none、json:lzo/none
    @JsonProperty("compress_codec")
    private String compressCodec;
    //内存最大行数，仅在file_format_type为excel时使用
    @JsonProperty("max_rows_in_memory")
    private String maxRowsInMemory;
    //sheet名称，仅在file_format_type为excel时使用，默认Sheet${随机数}
    @JsonProperty("sheet_name")
    private String sheetName;
    //是否写入表头，仅在file_format_type为text/csv时使用，false不写入表头/true写入表头，默认false
    @JsonProperty("enable_header_write")
    private Boolean enableHeaderWrite;
    //仅在file_format_type为json/text/csv/xml时使用，默认UTF-8
    private String encoding;

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
    //支持将时间戳写入Parquet INT96，file_format_type为parquet，默认false
    @JsonProperty("parquet_avro_write_timestamp_as_int96")
    private Boolean parquetAvroWriteTimestampAsInt96;
    //支持将12-byte字段写入Parquet INT96，file_format_type为parquet
    @JsonProperty("parquet_avro_write_fixed_as_int96")
    private List<String> parquetAvroWriteFixedAsInt96;
}