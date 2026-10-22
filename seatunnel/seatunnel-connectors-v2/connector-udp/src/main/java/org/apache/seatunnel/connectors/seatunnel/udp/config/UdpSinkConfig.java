package org.apache.seatunnel.connectors.seatunnel.udp.config;

import lombok.Data;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.shade.org.apache.arrow.util.Preconditions;

import java.io.Serializable;

import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.ASYNC_ENABLED;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.ASYNC_THREADS;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.BATCH_INTERVAL_MS;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.BATCH_SIZE;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.ENABLE_MESSAGE_REPLY;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.HOST;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.MAX_QUEUE_SIZE;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.MAX_RETRIES;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.MAX_WAIT_MS;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.PORT;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.RETRY_INTERVAL_MS;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.TIMEOUT_MS;

@Data
public class UdpSinkConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private String host;
    private int port;
    private int maxRetries;
    private int timeoutMs;
    private boolean enableMessageReply;
    private int retryIntervalMs;

    // 异步配置
    private boolean asyncEnabled;
    private int asyncThreads;
    private int maxQueueSize;
    private long maxWaitMs;



    public UdpSinkConfig(ReadonlyConfig config) {
        this.port = config.get(PORT);
        this.host = config.get(HOST);
        this.maxRetries = config.get(MAX_RETRIES);
        this.timeoutMs = config.get(TIMEOUT_MS);
        this.enableMessageReply = config.get(ENABLE_MESSAGE_REPLY);
        this.retryIntervalMs = config.get(RETRY_INTERVAL_MS);
        this.asyncEnabled = config.get(ASYNC_ENABLED);
        this.asyncThreads = config.get(ASYNC_THREADS);
        this.maxQueueSize = config.get(MAX_QUEUE_SIZE);
        this.maxWaitMs = config.get(MAX_WAIT_MS);

    }


    public void validate() {
        Preconditions.checkArgument(port > 0 && port <= 65535, "Invalid port number");
        Preconditions.checkNotNull(host, "Host cannot be null");
    }
}
