package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSinkDTO;

import java.util.List;


@EqualsAndHashCode(callSuper = true)
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@Data
public class SinkHdfsFileDTO extends BaseSeatunnelSinkDTO {

    //以 hdfs:// 开头的 Hadoop 集群地址，例如：hdfs://hadoopcluster
    @JsonProperty("fs.defaultFS")
    private String fsDefaultFS;
    //目标目录路径是必需的
    private String path;
    //临时文件路径，结果文件将首先写入临时路径，然后使用mv将临时目录提交到目标目录，默认/tmp/seatunnel
    @JsonProperty("tmp_path")
    private String tmpPath;
    @JsonProperty("hdfs_site_path")
    private String hdfsSitePath;
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
    @JsonProperty("krb5_path")
    private String krb5Path;
    @JsonProperty("kerberos_principal")
    private String kerberosPrincipal;
    @JsonProperty("kerberos_keytab_path")
    private String kerberosKeytabPath;
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
    @JsonProperty("remote_user")
    private String remoteUser;
}