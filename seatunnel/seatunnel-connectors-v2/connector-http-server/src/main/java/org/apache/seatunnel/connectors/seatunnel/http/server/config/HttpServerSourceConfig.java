package org.apache.seatunnel.connectors.seatunnel.http.server.config;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;

import java.io.Serializable;
import java.util.Collections;
import java.util.Map;

import static org.apache.seatunnel.connectors.seatunnel.http.server.config.HttpServerOptions.*;

/**
 * Configuration holder and validator for HttpServerSource
 */
@Data
public class HttpServerSourceConfig implements Serializable {

    private static final long serialVersionUID = 1L;

    private final ReadonlyConfig config;
    private final String host;
    private final int port;
    private final String endpoint;
    private final String method;
    private final String authType;
    private final int timeoutMs;
    private final int maxContentLength;
    private final int maxThreads;
    private final int queueCapacity;
    private final Map<String, String> authConfig;
    private final String format;

    public HttpServerSourceConfig(ReadonlyConfig config) {
        this.config = config;
        this.host = config.get(HOST);
        this.port = config.get(PORT);
        this.endpoint = config.get(ENDPOINT);
        this.method = config.get(METHOD);
        this.maxContentLength = config.get(MAX_CONTENT_LENGTH);
        this.format = config.get(FORMAT);
        this.authType = config.get(AUTH_TYPE);
        this.timeoutMs = config.get(TIMEOUT_MS);
        this.maxThreads = config.get(MAX_THREADS);
        this.queueCapacity = config.get(QUEUE_CAPACITY);
        this.authConfig = config.get(HttpServerOptions.AUTH_CONFIG) != null ?
                config.get(HttpServerOptions.AUTH_CONFIG) : Collections.emptyMap();
    }

    /**
     * Validate the configuration
     *
     * @throws IllegalArgumentException if configuration is invalid
     */
    public void validate() {
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 1 and 65535");
        }
        if (timeoutMs <= 0) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        if (maxContentLength <= 0) {
            throw new IllegalArgumentException("Max content length must be positive");
        }
        if (!this.endpoint.startsWith("/")) {
            throw new IllegalArgumentException("Path must start with '/'");
        }
        if (!this.format.equals(HttpServerOptions.FORMAT_TEXT) &&
                !format.equals(HttpServerOptions.FORMAT_JSON)) {
            throw new IllegalArgumentException("Output format must be either 'text' or 'json'");
        }
        if (StringUtils.isBlank(this.method)) {
            throw new IllegalArgumentException("At least one HTTP method must be supported");
        }
        if (maxThreads <= 0) {
            throw new IllegalArgumentException("Max threads must be positive");
        }
        if (queueCapacity <= 0) {
            throw new IllegalArgumentException("Queue capacity must be positive");
        }
    }

    public boolean isJsonFormat() {
        return format.equalsIgnoreCase("json");
    }

}
