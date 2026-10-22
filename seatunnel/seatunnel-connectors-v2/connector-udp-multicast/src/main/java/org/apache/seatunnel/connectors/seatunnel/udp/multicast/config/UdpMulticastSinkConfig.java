package org.apache.seatunnel.connectors.seatunnel.udp.multicast.config;

import lombok.Getter;
import lombok.ToString;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.connectors.seatunnel.udp.multicast.utils.NetworkInterfaceUtil;

import java.io.Serializable;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

@ToString
@Getter
public class UdpMulticastSinkConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String groupAddress;
    private final int port;
    private final NetworkInterface networkInterface;
    private final int soTimeout;
    private final int ttl;
    private final int sendBufferSize;
    private final int maxRetries;
    private final int retryIntervalMs;
    private final boolean waitForReply;
    private final int replyTimeoutMs;
    private final int batchSize;
    private final int batchIntervalMs;
    private final int batchQueueSize;
    private final boolean asyncEnabled;
    private final int asyncThreads;
    private final int asyncQueueSize;
    private final long asyncWaitNanos;
    private final String charsetName;
    private final int maxPacketSize;

    public UdpMulticastSinkConfig(ReadonlyConfig config) {
        this.groupAddress = config.get(UdpMulticastOptions.GROUP_ADDRESS);
        this.port = config.get(UdpMulticastOptions.PORT);
        this.soTimeout = config.get(UdpMulticastOptions.SO_TIMEOUT);
        this.ttl = config.get(UdpMulticastOptions.TTL);
        this.sendBufferSize = config.get(UdpMulticastOptions.SEND_BUFFER_SIZE);
        this.maxRetries = config.get(UdpMulticastOptions.MAX_RETRIES);
        this.retryIntervalMs = config.get(UdpMulticastOptions.RETRY_INTERVAL_MS);
        this.waitForReply = config.get(UdpMulticastOptions.WAIT_FOR_REPLY);
        this.replyTimeoutMs = config.get(UdpMulticastOptions.REPLY_TIMEOUT_MS);
        this.batchSize = config.get(UdpMulticastOptions.BATCH_SIZE);
        this.batchIntervalMs = config.get(UdpMulticastOptions.BATCH_INTERVAL_MS);
        this.batchQueueSize = config.get(UdpMulticastOptions.BATCH_QUEUE_SIZE);
        this.asyncEnabled = config.get(UdpMulticastOptions.ASYNC_ENABLED);
        this.asyncThreads = config.get(UdpMulticastOptions.ASYNC_THREADS);
        this.asyncQueueSize = config.get(UdpMulticastOptions.ASYNC_QUEUE_SIZE);
        this.asyncWaitNanos = TimeUnit.MILLISECONDS.toNanos(
                config.get(UdpMulticastOptions.ASYNC_WAIT_MS));
        this.charsetName = config.get(UdpMulticastOptions.CHARSET);
        this.maxPacketSize = config.get(UdpMulticastOptions.MAX_PACKET_SIZE);

        String interfaceName = config.get(UdpMulticastOptions.NETWORK_INTERFACE);
        try {
            this.networkInterface = NetworkInterfaceUtil.getNetworkInterface(interfaceName);
        } catch (SocketException e) {
            throw new RuntimeException(e);
        }
    }

    public Charset getCharset() {
        return Charset.forName(charsetName);
    }

}
