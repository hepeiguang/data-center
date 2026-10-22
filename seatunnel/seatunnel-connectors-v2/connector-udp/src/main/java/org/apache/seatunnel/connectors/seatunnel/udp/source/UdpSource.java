package org.apache.seatunnel.connectors.seatunnel.udp.source;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.common.SeaTunnelAPIErrorCode;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.api.options.SinkConnectorCommonOptions;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.source.Boundedness;
import org.apache.seatunnel.api.source.SeaTunnelSource;
import org.apache.seatunnel.api.source.SourceReader;
import org.apache.seatunnel.api.source.SourceSplitEnumerator;
import org.apache.seatunnel.api.source.SupportParallelism;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.catalog.CatalogTableUtil;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.common.constants.PluginType;
import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpSourceConfig;
import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions;
import org.apache.seatunnel.connectors.seatunnel.udp.exception.UdpConnectorException;
import org.apache.seatunnel.format.json.JsonDeserializationSchema;

import java.util.Collections;
import java.util.List;

import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.IDENTIFIER;

/**
 * 定义数据源的全局行为（如并行度、分片策略、检查点机制）
 *
 * <p>协作关系图 sequenceDiagram participant Engine participant SeaTunnelSource participant
 * SplitEnumerator participant SourceReader
 *
 * <p>Engine->>SeaTunnelSource: getPluginName() 验证插件 Engine->>SeaTunnelSource: getProducedType()
 * 获取输出类型 Engine->>SeaTunnelSource: getBoundedness() 确定模式 alt 初始启动 Engine->>SeaTunnelSource:
 * createEnumerator() else 故障恢复 Engine->>SeaTunnelSource: restoreEnumerator(state) end
 * Engine->>SeaTunnelSource: createReader() *并行调用 SplitEnumerator->>SourceReader: 分配Split
 * SourceReader->>Engine: 发送数据
 */

@AutoService(SeaTunnelSource.class)
public class UdpSource implements SeaTunnelSource<SeaTunnelRow, UdpSourceSplit, UdpSourceState>, SupportParallelism {
    private static final long serialVersionUID = 1L;  // 显式声明固定值
    private UdpSourceConfig config;
    private SeaTunnelRowType seaTunnelRowType;

    private CatalogTable catalogTable;
    private DeserializationSchema<SeaTunnelRow> deserializationSchema;

    public UdpSource(ReadonlyConfig readonlyConfig) {
        this.config = new UdpSourceConfig(readonlyConfig);
        boolean codecMode =
                this.config.getParseMode() != null
                        && this.config.getParseMode() != UdpOptions.ParseMode.none;
        if (codecMode || UdpOptions.Format.JSON.equals(this.config.getFormat())) {
            if (!readonlyConfig.getOptional(SinkConnectorCommonOptions.SCHEMA).isPresent()) {
                if (!codecMode) {
                    throw new UdpConnectorException(
                            SeaTunnelAPIErrorCode.CONFIG_VALIDATION_FAILED,
                            String.format(
                                    "PluginName: %s, PluginType: %s, Message: %s",
                                    getPluginName(),
                                    PluginType.SOURCE,
                                    "Must config schema when format parameter been config"));
                }
                // codec without schema: map entire JSON into simple text content field
                this.catalogTable = CatalogTableUtil.buildSimpleTextTable();
            } else {
                this.catalogTable = CatalogTableUtil.buildWithConfig(readonlyConfig);
            }
            this.seaTunnelRowType = catalogTable.getSeaTunnelRowType();
            this.deserializationSchema =
                    new JsonDeserializationSchema(catalogTable, false, false);
        } else {
            this.catalogTable = CatalogTableUtil.buildSimpleTextTable();
            this.seaTunnelRowType = catalogTable.getSeaTunnelRowType();
            this.deserializationSchema = new JsonDeserializationSchema(catalogTable, false, false);
        }
    }


    /**
     * 插件标识匹配
     *
     * <p>返回该数据源插件的唯一标识名称，用于在 SeaTunnel 配置文件中匹配对应的插件（如 plugin_name: "UDP"） 注意： 必须与配置文件中的 plugin_name
     * 严格一致，否则引擎无法识别插件。
     *
     * @return
     */
    @Override
    public String getPluginName() {
        return IDENTIFIER;
    }

    /**
     * 定义输出数据结构
     *
     * <p>定义数据源输出的数据结构类型（字段名、字段类型），用于下游 SQL 引擎或转换算子（Transform）的类型校验。 注意：
     * 若实际数据与声明的类型不匹配，会导致序列化错误或数据丢失。
     *
     * @return
     */
    @Override
    public SeaTunnelDataType<SeaTunnelRow> getProducedType() {
        return this.seaTunnelRowType;
    }

    @Override
    public List<CatalogTable> getProducedCatalogTables() {
        return Collections.singletonList(catalogTable);
    }

    /**
     * 生成初始数据分片
     *
     * <p>初始化分片枚举器，在任务启动时生成初始数据分片（Splits），用于批处理或流处理的初始状态。 典型逻辑：
     *
     * <p>批处理：扫描表分区/文件列表，生成固定分片（如按主键范围分片）。 流处理：初始化订阅位置（如 Kafka 的初始 Offset）。
     *
     * @param enumeratorContext enumerator context.
     * @return
     */
    @Override
    public SourceSplitEnumerator<UdpSourceSplit, UdpSourceState> createEnumerator(SourceSplitEnumerator.Context<UdpSourceSplit> enumeratorContext) {
        return new UdpSourceSplitEnumerator(enumeratorContext, config);
    }

    /**
     * 从检查点恢复分片状态
     *
     * <p>从检查点（Checkpoint）恢复分片枚举器状态，用于故障恢复或作业重启时保证数据一致性。
     *
     * @param enumeratorContext enumerator context.
     * @param checkpointState   是之前保存的状态（如未分配的分片列表、消费进度）。
     * @return
     */
    @Override
    public SourceSplitEnumerator<UdpSourceSplit, UdpSourceState> restoreEnumerator(SourceSplitEnumerator.Context<UdpSourceSplit> enumeratorContext, UdpSourceState checkpointState) {
        return new UdpSourceSplitEnumerator(enumeratorContext, config);
    }

    /**
     * 决定批处理/流处理
     *
     * <p>声明数据源的边界模式，决定任务是批处理（Bounded）还是流处理（Unbounded）。 可选值：
     *
     * <p>Boundedness.BOUNDED：有限数据（如 MySQL 表、HDFS 文件）。 Boundedness.UNBOUNDED：无限数据（如 Kafka、RabbitMQ
     * 实时流）。
     *
     * @return
     */
    @Override
    public Boundedness getBoundedness() {
        return Boundedness.UNBOUNDED;
    }

    /**
     * 实际拉取数据并输出
     *
     * <p>创建数据读取器实例，负责实际从分片（Split）中拉取数据并发送给下游。
     *
     * @param readerContext 提供发送数据的方法（如 Collector.output()）和任务上下文信息。
     * @return
     */
    @Override
    public SourceReader<SeaTunnelRow, UdpSourceSplit> createReader(SourceReader.Context readerContext) {
        return new UdpSourceReader(readerContext, config, seaTunnelRowType, deserializationSchema);
    }
}
