package org.apache.seatunnel.connectors.seatunnel.udp.multicast.sink;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.table.connector.TableSink;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableSinkFactory;
import org.apache.seatunnel.api.table.factory.TableSinkFactoryContext;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.config.UdpMulticastOptions;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.config.UdpMulticastSinkConfig;

@AutoService(Factory.class)
public class UdpMulticastSinkFactory implements TableSinkFactory {

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
                        UdpMulticastOptions.SEND_BUFFER_SIZE,
                        UdpMulticastOptions.MAX_RETRIES,
                        UdpMulticastOptions.RETRY_INTERVAL_MS,
                        UdpMulticastOptions.WAIT_FOR_REPLY,
                        UdpMulticastOptions.REPLY_TIMEOUT_MS,
                        UdpMulticastOptions.BATCH_SIZE,
                        UdpMulticastOptions.BATCH_INTERVAL_MS,
                        UdpMulticastOptions.BATCH_QUEUE_SIZE,
                        UdpMulticastOptions.ASYNC_ENABLED,
                        UdpMulticastOptions.ASYNC_THREADS,
                        UdpMulticastOptions.ASYNC_QUEUE_SIZE,
                        UdpMulticastOptions.ASYNC_WAIT_MS)
                .build();
    }

    @Override
    public TableSink createSink(TableSinkFactoryContext context) {
        return () -> new UdpMulticastSink(new UdpMulticastSinkConfig(context.getOptions()), context.getCatalogTable().getSeaTunnelRowType());
    }
}
