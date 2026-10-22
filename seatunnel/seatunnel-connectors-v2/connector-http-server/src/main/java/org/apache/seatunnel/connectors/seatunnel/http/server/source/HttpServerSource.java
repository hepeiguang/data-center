package org.apache.seatunnel.connectors.seatunnel.http.server.source;

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
import org.apache.seatunnel.connectors.seatunnel.http.server.config.HttpServerOptions;
import org.apache.seatunnel.connectors.seatunnel.http.server.config.HttpServerSourceConfig;
import org.apache.seatunnel.connectors.seatunnel.http.server.exception.HttpServerConnectorException;
import org.apache.seatunnel.format.json.JsonDeserializationSchema;

@AutoService(SeaTunnelSource.class)
public class HttpServerSource implements SeaTunnelSource<SeaTunnelRow, HttpServerSourceSplit, HttpServerSourceState>, SupportParallelism {

    private final HttpServerSourceConfig config;
    private final DeserializationSchema<SeaTunnelRow> deserializationSchema;
    private final SeaTunnelRowType seaTunnelRowType;
    private final CatalogTable catalogTable;

    public HttpServerSource(ReadonlyConfig config) {
        this.config = new HttpServerSourceConfig(config);
        this.config.validate();
        // Define the row type based on expected data structure
        if (this.config.isJsonFormat()) {
            if (!config.getOptional(SinkConnectorCommonOptions.SCHEMA).isPresent()) {
                throw new HttpServerConnectorException(
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
    public SourceReader<SeaTunnelRow, HttpServerSourceSplit> createReader(SourceReader.Context readerContext) throws Exception {
        return new HttpServerSourceReader(readerContext, config, deserializationSchema);
    }

    @Override
    public SourceSplitEnumerator<HttpServerSourceSplit, HttpServerSourceState> createEnumerator(SourceSplitEnumerator.Context<HttpServerSourceSplit> enumeratorContext) throws Exception {
        return new HttpServerSourceEnumerator(enumeratorContext);
    }

    @Override
    public SourceSplitEnumerator<HttpServerSourceSplit, HttpServerSourceState> restoreEnumerator(SourceSplitEnumerator.Context<HttpServerSourceSplit> enumeratorContext, HttpServerSourceState checkpointState) throws Exception {
        return new HttpServerSourceEnumerator(enumeratorContext, checkpointState);
    }

    @Override
    public String getPluginName() {
        return HttpServerOptions.IDENTIFIER;
    }
}
