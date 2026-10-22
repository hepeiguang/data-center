package org.apache.seatunnel.connectors.seatunnel.socket.server.config;

import lombok.Getter;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;

import java.io.Serializable;

import static org.apache.seatunnel.connectors.seatunnel.socket.server.config.SocketServerOptions.*;

@Getter
public class SocketServerSourceConfig implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ReadonlyConfig config;
    private final String host;
    private final int port;
    private final int bufferSize;
    private final String charset;
    private final int maxConnections;
    private final String format;
    private final boolean replyEnabled;
    private final String replyMessage;
    private final int socketTimeout;
    private final String delimiter;

    public SocketServerSourceConfig(ReadonlyConfig config) {
        this.config = config;
        this.host = config.get(HOST);
        this.port = config.get(PORT);
        this.bufferSize = config.get(BUFFER_SIZE);
        this.maxConnections = config.get(MAX_CONNECTIONS);
        this.charset = config.get(CHARSET);
        this.format = config.get(FORMAT);
        this.replyEnabled = config.get(REPLY_ENABLED);
        this.replyMessage = config.get(REPLY_MESSAGE);
        this.socketTimeout = config.get(SOCKET_TIMEOUT);
        this.delimiter = config.get(DELIMITER);
    }

    public boolean isJsonFormat() {
        return format.equalsIgnoreCase("json");
    }

}
