package org.apache.seatunnel.connectors.seatunnel.http.server.config;

import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;

import java.util.Map;

/**
 * Configuration options for HttpServerSource
 */
public class HttpServerOptions {

    public static final String IDENTIFIER = "HttpServer";

    // Network configuration options
    public static final Option<String> HOST = Options.key("host")
            .stringType()
            .defaultValue("0.0.0.0")
            .withDescription("The host address the HTTP server will bind to");

    public static final Option<Integer> PORT = Options.key("port")
            .intType()
            .defaultValue(8080)
            .withDescription("The port the HTTP server will listen on");

    public static final Option<String> ENDPOINT = Options.key("endpoint")
            .stringType()
            .defaultValue("/api")
            .withDescription("The URL path the HTTP server will handle");

    // Request handling options
    public static final Option<String> METHOD = Options.key("method")
            .stringType()
            .defaultValue("POST")
            .withDescription("Supported HTTP method (GET, POST, PUT, DELETE)");

    public static final Option<Integer> TIMEOUT_MS = Options.key("timeoutMs")
            .intType()
            .defaultValue(5000)
            .withDescription("Request timeout in milliseconds");

    public static final Option<Integer> MAX_CONTENT_LENGTH = Options.key("maxContentLength")
            .intType()
            .defaultValue(1048576) // 1MB
            .withDescription("Maximum allowed content length in bytes");

    public static final Option<String> FORMAT = Options.key("format")
            .stringType()
            .defaultValue("text")
            .withDescription("Output format (text or json)");

    // Thread pool configuration for high concurrency
    public static final Option<Integer> MAX_THREADS = Options.key("maxThreads")
            .intType()
            .defaultValue(200)
            .withDescription("Maximum number of worker threads");

    public static final Option<Integer> QUEUE_CAPACITY = Options.key("queueCapacity")
            .intType()
            .defaultValue(1000)
            .withDescription("Maximum queue capacity for pending requests");

    // Authentication options
    public static final Option<String> AUTH_TYPE = Options.key("authType")
            .stringType()
            .defaultValue("none")
            .withDescription("Authentication type (none, basic, bearer, api_key)");

    public static final Option<Map<String, String>> AUTH_CONFIG = Options.key("authConfig")
            .mapType()
            .noDefaultValue()
            .withDescription("Authentication configuration parameters");

    // Supported values
    public static final String FORMAT_TEXT = "text";
    public static final String FORMAT_JSON = "json";

    public static final String AUTH_NONE = "none";
    public static final String AUTH_BASIC = "basic";
    public static final String AUTH_BEARER = "bearer";
    public static final String AUTH_API_KEY = "api_key";
    public static final String HEADER_AUTHORIZATION = "Authorization";
}
