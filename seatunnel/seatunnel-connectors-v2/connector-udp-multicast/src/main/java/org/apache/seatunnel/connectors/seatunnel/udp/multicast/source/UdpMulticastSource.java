package org.apache.seatunnel.connectors.seatunnel.udp.multicast.source;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.common.SeaTunnelAPIErrorCode;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.api.options.SinkConnectorCommonOptions;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.source.*;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.catalog.CatalogTableUtil;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.common.constants.PluginType;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.config.UdpMulticastOptions;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.config.UdpMulticastSourceConfig;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.exception.UdpMulticastConnectorException;
import org.apache.seatunnel.format.json.JsonDeserializationSchema;

@AutoService(SeaTunnelSource.class)
public class UdpMulticastSource implements SeaTunnelSource<SeaTunnelRow, UdpMulticastSourceSplit, UdpMulticastSourceState>, SupportParallelism {

    private final UdpMulticastSourceConfig config;
    private final SeaTunnelRowType seaTunnelRowType;
    private final CatalogTable catalogTable;
    private final DeserializationSchema<SeaTunnelRow> deserializationSchema;

    public UdpMulticastSource(ReadonlyConfig config) {
        this.config = new UdpMulticastSourceConfig(config);
        if (this.config.isJsonFormat()) {
            if (!config.getOptional(SinkConnectorCommonOptions.SCHEMA).isPresent()) {
                throw new UdpMulticastConnectorException(
                        SeaTunnelAPIErrorCode.CONFIG_VALIDATION_FAILED,
                        String.format(
                                "PluginName: %s, PluginType: %s, Message: %s",
                                getPluginName(),
                                PluginType.SOURCE,
                                "Must config schema when format parameter been config"));
            }
            this.catalogTable = CatalogTableUtil.buildWithConfig(config);
            this.seaTunnelRowType = catalogTable.getSeaTunnelRowType();
            this.deserializationSchema = new JsonDeserializationSchema(catalogTable, false, false);
        } else {
            this.catalogTable = CatalogTableUtil.buildSimpleTextTable();
            this.seaTunnelRowType = catalogTable.getSeaTunnelRowType();
            this.deserializationSchema = new JsonDeserializationSchema(catalogTable, false, false);
        }
    }

    @Override
    public Boundedness getBoundedness() {
        return Boundedness.UNBOUNDED;
    }

    @Override
    public SeaTunnelRowType getProducedType() {
        return this.seaTunnelRowType;
    }

    @Override
    public SourceReader<SeaTunnelRow, UdpMulticastSourceSplit> createReader(SourceReader.Context readerContext) throws Exception {
        return new UdpMulticastSourceReader(readerContext, config, deserializationSchema);
    }

    @Override
    public SourceSplitEnumerator<UdpMulticastSourceSplit, UdpMulticastSourceState> createEnumerator(SourceSplitEnumerator.Context<UdpMulticastSourceSplit> enumeratorContext) throws Exception {
        return new UdpMulticastSourceEnumerator(enumeratorContext, config);
    }

    @Override
    public SourceSplitEnumerator<UdpMulticastSourceSplit, UdpMulticastSourceState> restoreEnumerator(SourceSplitEnumerator.Context<UdpMulticastSourceSplit> enumeratorContext, UdpMulticastSourceState checkpointState) throws Exception {
        return new UdpMulticastSourceEnumerator(enumeratorContext, config);
    }

    @Override
    public String getPluginName() {
        return UdpMulticastOptions.IDENTIFIER;
    }
}
