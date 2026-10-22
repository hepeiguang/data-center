package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunnelSinkDTO;

import java.util.Map;

/**
 * Sink对象-Hive
 *
 * @author ZhaoYuan
 * @date 2025/5/14 21:47
 * @since JDK1.8
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@NoArgsConstructor
@Data
public class SinkHiveDTO extends BaseSeatunnelSinkDTO {

    //Hive元存储URI
    @JsonProperty("metastore_uri")
    private String metastoreUri;
    //目标Hive表名，例如：db1.table1。如果源是多模式，您可以使用${database_name}.${table_name}来生成表名，它将用源生成的CatalogTable的值替换${database_name}和${table_name}。
    @JsonProperty("table_name")
    private String tableName;
    //文件的压缩编码，支持的压缩编码如下所示：txt:lzo/none、json:lzo/none、csv:lzo/none、orc/parquet:自动识别压缩类型，无需配置
    @JsonProperty("compress_codec")
    private String compressCodec;
    //hdfs-site.xml的路径，用于加载Namenode的高可用配置
    @JsonProperty("hdfs_site_path")
    private String hdfsSitePath;
    //hive-site.xml的路径
    @JsonProperty("hive_site_path")
    private String hiveSitePath;
    //其他选项，Hadoop配置中的属性（core-site.xml、hdfs-site.xml、hive-site.xml）
    @JsonProperty("hive.hadoop.conf")
    private Map<String, Object> hiveHadoopConf;
    //其他选项
    private String hiveHadoopConfVo;
    //指定加载core-site.xml、hdfs-site.xml、hive-site.xml文件的路径
    @JsonProperty("hive.hadoop.conf-path")
    private String hiveHadoopConfPath;
    //krb5.conf的路径，用于Kerberos认证，默认：/etc/krb5.conf
    //hive-site.xml的路径，用于Hive元存储认证
    @JsonProperty("krb5_path")
    private String krb5Path;
    //Kerberos的主体
    @JsonProperty("kerberos_principal")
    private String kerberosPrincipal;
    //Kerberos的keytab文件路径
    @JsonProperty("kerberos_keytab_path")
    private String kerberosKeytabPath;
    //在中止操作期间是否从Hive Metastore中删除分区元数据的标志。注意：这只影响元存储中的元数据，分区中的数据将始终被删除（同步过程中生成的数据）。默认：true
    @JsonProperty("abort_drop_partition_metadata")
    private Boolean abortDropPartitionMetadata;
    //支持从时间戳写入Parquet INT96，仅对parquet文件有效，默认：false
    @JsonProperty("parquet_avro_write_timestamp_as_int96")
    private Boolean parquetAvroWriteTimestampAsInt96;
}