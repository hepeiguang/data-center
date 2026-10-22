package org.apache.seatunnel.connectors.seatunnel.socket.server.config;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;

public class SocketServerOptions {

    public static final String IDENTIFIER = "SocketServer";

    public static final Option<String> HOST = Options.key("host")
            .stringType()
            .defaultValue("0.0.0.0")
            .withDescription("The host the socket server will bind to");

    public static final Option<Integer> PORT = Options.key("port")
            .intType()
            .defaultValue(9999)
            .withDescription("The port the socket server will listen on");

    public static final Option<String> CHARSET = Options.key("charset")
            .stringType()
            .defaultValue("UTF-8")
            .withDescription("The charset for encoding/decoding messages");

    public static final Option<Integer> BUFFER_SIZE = Options.key("bufferSize")
            .intType()
            .defaultValue(8192)
            .withDescription("The size of the receive buffer in bytes");

    public static final Option<Boolean> REPLY_ENABLED = Options.key("replyEnabled")
            .booleanType()
            .defaultValue(false)
            .withDescription("Whether to reply to the client after receiving a message");

    public static final Option<String> REPLY_MESSAGE = Options.key("replyMessage")
            .stringType()
            .defaultValue("ACK")
            .withDescription("The message to reply to the client if reply is enabled");

    public static final Option<Integer> MAX_CONNECTIONS = Options.key("maxConnections")
            .intType()
            .defaultValue(100)
            .withDescription("Maximum number of concurrent connections");

    public static final Option<Integer> SOCKET_TIMEOUT = Options.key("socketTimeout")
            .intType()
            .defaultValue(30000)
            .withDescription("Socket timeout in milliseconds");

    public static final Option<String> DELIMITER = Options.key("delimiter")
            .stringType()
            .defaultValue("\n")
            .withDescription("Message delimiter for splitting incoming data");

    public static final Option<String> FORMAT = Options.key("format")
            .stringType()
            .defaultValue("text")
            .withDescription("Output format (text or json)");
}
