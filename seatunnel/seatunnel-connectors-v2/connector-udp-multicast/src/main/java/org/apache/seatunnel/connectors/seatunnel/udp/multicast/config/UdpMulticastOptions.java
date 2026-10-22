package org.apache.seatunnel.connectors.seatunnel.udp.multicast.config;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;

public class UdpMulticastOptions {

    public static final String IDENTIFIER = "UdpMulticast";

    // Common options for both source and sink
    public static final Option<String> GROUP_ADDRESS =
            Options.key("groupAddress")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The multicast group address to join (e.g., 239.255.255.250)");

    public static final Option<Integer> PORT =
            Options.key("port")
                    .intType()
                    .noDefaultValue()
                    .withDescription("The port number to listen/send on");

    public static final Option<String> NETWORK_INTERFACE =
            Options.key("networkInterface")
                    .stringType()
                    .defaultValue("")
                    .withDescription("The network interface to use for multicast (empty for default)");

    public static final Option<Integer> SO_TIMEOUT =
            Options.key("soTimeout")
                    .intType()
                    .defaultValue(5000)
                    .withDescription("Socket timeout in milliseconds");

    public static final Option<Integer> TTL =
            Options.key("ttl")
                    .intType()
                    .defaultValue(1)
                    .withDescription("Time-to-live for multicast packets (1=local network, 32=site, 255=unrestricted)");

    public static final Option<String> CHARSET =
            Options.key("charset")
                    .stringType()
                    .defaultValue("UTF-8")
                    .withDescription("Character encoding for message serialization");

    // Source specific options
    public static final Option<Integer> RECEIVE_BUFFER_SIZE =
            Options.key("receiveBufferSize")
                    .intType()
                    .defaultValue(65536)
                    .withDescription("Size of the socket receive buffer in bytes");

    public static final Option<Integer> MAX_PACKET_SIZE =
            Options.key("maxPacketSize")
                    .intType()
                    .defaultValue(8192)
                    .withDescription("Maximum size of a single UDP packet in bytes");

    public static final Option<Boolean> REUSE_ADDRESS =
            Options.key("reuseAddress")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Whether to reuse the socket address");

    public static final Option<Boolean> REPLY_ENABLED =
            Options.key("replyEnabled")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to send reply messages back to sender");

    public static final Option<String> REPLY_MESSAGE =
            Options.key("replyMessage")
                    .stringType()
                    .defaultValue("ACK")
                    .withDescription("The reply message to send when replyEnabled is true");

    public static final Option<String> FORMAT = Options.key("format")
            .stringType()
            .defaultValue("text")
            .withDescription("Data format, can be 'text' or 'json'");

    // Sink specific options
    public static final Option<Integer> SEND_BUFFER_SIZE =
            Options.key("sendBufferSize")
                    .intType()
                    .defaultValue(65536)
                    .withDescription("Size of the socket send buffer in bytes");

    public static final Option<Integer> MAX_RETRIES =
            Options.key("maxRetries")
                    .intType()
                    .defaultValue(3)
                    .withDescription("Maximum number of send attempts for a message");

    public static final Option<Integer> RETRY_INTERVAL_MS =
            Options.key("retryIntervalMs")
                    .intType()
                    .defaultValue(1000)
                    .withDescription("Interval between send retries in milliseconds");

    public static final Option<Boolean> WAIT_FOR_REPLY =
            Options.key("waitForReply")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to wait for a reply after sending");

    public static final Option<Integer> REPLY_TIMEOUT_MS =
            Options.key("replyTimeoutMs")
                    .intType()
                    .defaultValue(3000)
                    .withDescription("Timeout for waiting reply in milliseconds");

    public static final Option<Integer> BATCH_SIZE =
            Options.key("batchSize")
                    .intType()
                    .defaultValue(100)
                    .withDescription("Number of messages to batch before sending");

    public static final Option<Integer> BATCH_INTERVAL_MS =
            Options.key("batchIntervalMs")
                    .intType()
                    .defaultValue(100)
                    .withDescription("Maximum time interval between batch sends in milliseconds");

    public static final Option<Integer> BATCH_QUEUE_SIZE =
            Options.key("batchQueueSize")
                    .intType()
                    .defaultValue(1000)
                    .withDescription("Maximum number of messages in the batch queue");

    public static final Option<Boolean> ASYNC_ENABLED =
            Options.key("asyncEnabled")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether to use asynchronous sending");

    public static final Option<Integer> ASYNC_THREADS =
            Options.key("asyncThreads")
                    .intType()
                    .defaultValue(4)
                    .withDescription("Number of threads in async sender thread pool");

    public static final Option<Integer> ASYNC_QUEUE_SIZE =
            Options.key("asyncQueueSize")
                    .intType()
                    .defaultValue(10000)
                    .withDescription("Maximum number of messages in async send queue");

    public static final Option<Integer> ASYNC_WAIT_MS =
            Options.key("asyncWaitMs")
                    .intType()
                    .defaultValue(5000)
                    .withDescription("Maximum wait time for async queue in milliseconds");
}
