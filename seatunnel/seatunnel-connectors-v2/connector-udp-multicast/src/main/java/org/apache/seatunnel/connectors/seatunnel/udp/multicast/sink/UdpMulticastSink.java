package org.apache.seatunnel.connectors.seatunnel.udp.multicast.sink;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.sink.SeaTunnelSink;
import org.apache.seatunnel.api.sink.SinkWriter;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.connectors.seatunnel.common.sink.AbstractSimpleSink;
import org.apache.seatunnel.connectors.seatunnel.common.sink.AbstractSinkWriter;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.config.UdpMulticastOptions;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.config.UdpMulticastSinkConfig;

import java.io.IOException;
import java.util.Optional;

@AutoService(SeaTunnelSink.class)
public class UdpMulticastSink extends AbstractSimpleSink<SeaTunnelRow, Void> {

    private final UdpMulticastSinkConfig config;
    private SeaTunnelRowType seaTunnelRowType;

    public UdpMulticastSink(UdpMulticastSinkConfig config, SeaTunnelRowType seaTunnelRowType) {
        this.config = config;
        this.seaTunnelRowType = seaTunnelRowType;
    }

    @Override
    public AbstractSinkWriter<SeaTunnelRow, Void> createWriter(SinkWriter.Context context) throws IOException {
        return new UdpMulticastSinkWriter(config, seaTunnelRowType);
    }

    @Override
    public String getPluginName() {
        return UdpMulticastOptions.IDENTIFIER;
    }

    @Override
    public void setTypeInfo(SeaTunnelRowType seaTunnelRowType) {
        this.seaTunnelRowType = seaTunnelRowType;
    }

    @Override
    public Optional<CatalogTable> getWriteCatalogTable() {
        return super.getWriteCatalogTable();
    }

}
