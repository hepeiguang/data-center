package org.apache.seatunnel.connectors.seatunnel.socket.server.source;

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
import org.apache.seatunnel.connectors.seatunnel.socket.server.config.SocketServerOptions;
import org.apache.seatunnel.connectors.seatunnel.socket.server.config.SocketServerSourceConfig;
import org.apache.seatunnel.connectors.seatunnel.socket.server.exception.SocketServerConnectorException;
import org.apache.seatunnel.format.json.JsonDeserializationSchema;

import java.util.Collections;
import java.util.List;

import static org.apache.seatunnel.connectors.seatunnel.socket.server.config.SocketServerOptions.IDENTIFIER;

@AutoService(SeaTunnelSource.class)
public class SocketServerSource implements SeaTunnelSource<SeaTunnelRow, SocketServerSourceSplit, SocketServerSourceState>, SupportParallelism {

    private final SocketServerSourceConfig config;
    private final SeaTunnelRowType seaTunnelRowType;
    private final CatalogTable catalogTable;
    private final DeserializationSchema<SeaTunnelRow> deserializationSchema;

    public SocketServerSource(ReadonlyConfig config) {
        this.config = new SocketServerSourceConfig(config);
        // config format type json
        if (this.config.isJsonFormat()) {
            if (!config.getOptional(SinkConnectorCommonOptions.SCHEMA).isPresent()) {
                throw new SocketServerConnectorException(
                        SeaTunnelAPIErrorCode.CONFIG_VALIDATION_FAILED,
                        String.format(
                                "PluginName: %s, PluginType: %s, Message: %s",
                                IDENTIFIER,
                                PluginType.SOURCE,
                                "Must config schema when format parameter been config"));
            }
            this.catalogTable = CatalogTableUtil.buildWithConfig(config);
            this.seaTunnelRowType = catalogTable.getSeaTunnelRowType();
            this.deserializationSchema = new JsonDeserializationSchema(catalogTable, false, false);
        } else { // raw text
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
    public SourceReader<SeaTunnelRow, SocketServerSourceSplit> createReader(SourceReader.Context readerContext) throws Exception {
        return new SocketServerSourceReader(readerContext, config, this.deserializationSchema);
    }

    @Override
    public SourceSplitEnumerator<SocketServerSourceSplit, SocketServerSourceState> createEnumerator(SourceSplitEnumerator.Context<SocketServerSourceSplit> enumeratorContext) throws Exception {
        return new SocketServerSourceSplitEnumerator(enumeratorContext, config);
    }

    @Override
    public SourceSplitEnumerator<SocketServerSourceSplit, SocketServerSourceState> restoreEnumerator(SourceSplitEnumerator.Context<SocketServerSourceSplit> enumeratorContext, SocketServerSourceState checkpointState) throws Exception {
        return new SocketServerSourceSplitEnumerator(enumeratorContext, config);
    }

    @Override
    public SeaTunnelRowType getProducedType() {
        return this.seaTunnelRowType;
    }

    @Override
    public List<CatalogTable> getProducedCatalogTables() {
        return Collections.singletonList(this.catalogTable);
    }

    @Override
    public String getPluginName() {
        return SocketServerOptions.IDENTIFIER;
    }
}
