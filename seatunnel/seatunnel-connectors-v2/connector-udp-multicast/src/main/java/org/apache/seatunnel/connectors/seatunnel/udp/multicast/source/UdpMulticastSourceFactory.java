package org.apache.seatunnel.connectors.seatunnel.udp.multicast.source;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.source.SeaTunnelSource;
import org.apache.seatunnel.api.source.SourceSplit;
import org.apache.seatunnel.api.table.connector.TableSource;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableSourceFactory;
import org.apache.seatunnel.api.table.factory.TableSourceFactoryContext;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.config.UdpMulticastOptions;

import java.io.Serializable;

@AutoService(Factory.class)
public class UdpMulticastSourceFactory implements TableSourceFactory {

    @Override
    public String factoryIdentifier() {
        return UdpMulticastOptions.IDENTIFIER;
    }

    @Override
    public OptionRule optionRule() {
        return OptionRule.builder()
                .required(
                        UdpMulticastOptions.GROUP_ADDRESS,
                        UdpMulticastOptions.PORT)
                .optional(
                        UdpMulticastOptions.NETWORK_INTERFACE,
                        UdpMulticastOptions.SO_TIMEOUT,
                        UdpMulticastOptions.TTL,
                        UdpMulticastOptions.CHARSET,
                        UdpMulticastOptions.RECEIVE_BUFFER_SIZE,
                        UdpMulticastOptions.MAX_PACKET_SIZE,
                        UdpMulticastOptions.REUSE_ADDRESS,
                        UdpMulticastOptions.REPLY_ENABLED,
                        UdpMulticastOptions.REPLY_MESSAGE)
                .build();
    }

    @Override
    public Class<? extends SeaTunnelSource> getSourceClass() {
        return UdpMulticastSource.class;
    }

    @Override
    public <T, SplitT extends SourceSplit, StateT extends Serializable>
    TableSource<T, SplitT, StateT> createSource(TableSourceFactoryContext context) {
        return () ->
                (SeaTunnelSource<T, SplitT, StateT>)
                        new UdpMulticastSource(context.getOptions());
    }

}
