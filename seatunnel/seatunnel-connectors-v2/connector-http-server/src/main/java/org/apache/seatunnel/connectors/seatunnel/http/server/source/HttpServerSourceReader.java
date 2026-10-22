package org.apache.seatunnel.connectors.seatunnel.http.server.source;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.seatunnel.api.serialization.DeserializationSchema;
import org.apache.seatunnel.api.source.Collector;
import org.apache.seatunnel.api.source.SourceReader;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.common.utils.JsonUtils;
import org.apache.seatunnel.connectors.seatunnel.http.server.config.HttpServerOptions;
import org.apache.seatunnel.connectors.seatunnel.http.server.config.HttpServerSourceConfig;
import org.apache.seatunnel.shade.com.fasterxml.jackson.databind.node.ObjectNode;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Slf4j
public class HttpServerSourceReader implements SourceReader<SeaTunnelRow, HttpServerSourceSplit> {

    private final SourceReader.Context context;
    private final DeserializationSchema<SeaTunnelRow> deserializationSchema;
    private final HttpServerSourceConfig config;
    private HttpServer server;
    private final BlockingQueue<String> requestQueue;
    private final ExecutorService requestExecutor;

    public HttpServerSourceReader(SourceReader.Context context, HttpServerSourceConfig config, DeserializationSchema<SeaTunnelRow> deserializer) {
        this.context = context;
        this.config = config;
        this.deserializationSchema = deserializer;
        this.requestQueue = new LinkedBlockingQueue<>(config.getQueueCapacity());
        this.requestExecutor = Executors.newFixedThreadPool(config.getMaxThreads());
    }

    @Override
    public void open() throws Exception {
        // Start HTTP server with configurable thread pool
        server = HttpServer.create(new InetSocketAddress(config.getHost(), config.getPort()), 0);
        server.createContext(config.getEndpoint(), new RequestHandler());
        server.setExecutor(requestExecutor);
        server.start();
    }

    @Override
    public void close() throws IOException {
        if (server != null) {
            server.stop(0);
        }
        requestExecutor.shutdownNow();
    }

    @Override
    public void pollNext(Collector<SeaTunnelRow> output) throws Exception {
        String requestData = requestQueue.poll();
        if (requestData != null) {
            if (config.isJsonFormat()) {
                deserializationSchema.deserialize(requestData.getBytes(), output);
            } else {
                ObjectNode node = JsonUtils.createObjectNode();
                node.put("content", requestData);
                deserializationSchema.deserialize(JsonUtils.toJsonString(node).getBytes(), output);
            }
        } else {
            Thread.sleep(100);
        }
    }

    @Override
    public List<HttpServerSourceSplit> snapshotState(long checkpointId) throws Exception {
        return Collections.emptyList();
    }

    @Override
    public void addSplits(List<HttpServerSourceSplit> splits) {

    }

    @Override
    public void handleNoMoreSplits() {

    }

    @Override
    public void notifyCheckpointComplete(long checkpointId) throws Exception {

    }

    /**
     * HTTP request handler
     */
    private class RequestHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Future<?> future = requestExecutor.submit(() -> {
                try {
                    String method = exchange.getRequestMethod();
                    // 判断请求方式是否和定义的请求方式一致
                    if (!method.equalsIgnoreCase(config.getMethod())) {
                        sendErrorResponse(exchange, 405, "Method Not Allowed");
                        return;
                    }
                    // 判断请求短路径是否和定义的一致
                    String requestPath = normalizePath(exchange.getRequestURI().getPath());
                    String configPath = normalizePath(config.getEndpoint());
                    if (!requestPath.equals(configPath)) {
                        sendErrorResponse(exchange, 404, "Not Found");
                        return;
                    }
                    switch (method.toUpperCase()) {
                        case "GET":
                            processGetRequest(exchange);
                            break;
                        case "POST":
                            processPostRequest(exchange);
                            break;
                        default:
                            sendErrorResponse(exchange, 405, "Method Not Allowed");
                    }
                } catch (Exception e) {
                    sendErrorResponse(exchange, 500, "Internal Server Error");
                } finally {
                    exchange.close();
                }
            });

            try {
                future.get(config.getTimeoutMs(), TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                future.cancel(true);
                sendErrorResponse(exchange, 408, "Request Timeout");
            } catch (Exception e) {
                sendErrorResponse(exchange, 500, "Internal Server Error");
            }
        }

        private void processGetRequest(HttpExchange exchange) throws IOException, InterruptedException {
            // 1. Authenticate
            if (!authenticate(exchange)) {
                sendErrorResponse(exchange, 401, "Unauthorized");
                return;
            }

            // 2. Parse query parameters
            String query = exchange.getRequestURI().getQuery();
            Map<String, String> params = parseQueryParams(query);

            // 3. Convert params to JSON string
            String requestData = JsonUtils.toJsonString(params);

            // 4. Add to queue
            if (!requestQueue.offer(requestData, config.getTimeoutMs(), TimeUnit.MILLISECONDS)) {
                sendErrorResponse(exchange, 429, "Too Many Requests");
                return;
            }

            // 5. Send success response
            sendSuccessResponse(exchange);
        }

        private void processPostRequest(HttpExchange exchange) throws IOException, InterruptedException {
            // 1. Authenticate
            if (!authenticate(exchange)) {
                sendErrorResponse(exchange, 401, "Unauthorized");
                return;
            }

            // 2. Check content type
            String contentType = exchange.getRequestHeaders().getFirst("Content-Type");
            if (StringUtils.isBlank(contentType)) {
                sendErrorResponse(exchange, 400, "Content-Type header is required");
                return;
            }

            // 3. Process request based on content type
            String requestData;
            if (contentType.startsWith("application/x-www-form-urlencoded")) {
                String formData = readRequest(exchange);
                requestData = JsonUtils.toJsonString(parseQueryParams(formData));
            } else if (contentType.startsWith("application/json")) {
                requestData = readRequest(exchange);
            } else {
                sendErrorResponse(exchange, 415, "Unsupported Media Type");
                return;
            }

            // 4. Add to queue
            if (!requestQueue.offer(requestData, config.getTimeoutMs(), TimeUnit.MILLISECONDS)) {
                sendErrorResponse(exchange, 429, "Too Many Requests");
                return;
            }

            // 5. Send success response
            sendSuccessResponse(exchange);
        }

        private Map<String, String> parseQueryParams(String query) {
            Map<String, String> params = new HashMap<>();
            if (StringUtils.isBlank(query)) {
                return params;
            }

            try {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 0) {
                        String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8.name());
                        String value = pair.length > 1 ?
                                URLDecoder.decode(pair[1], StandardCharsets.UTF_8.name()) : "";
                        params.put(key, value);
                    }
                }
            } catch (Exception e) {
                log.warn("Failed to parse query parameters", e);
            }
            return params;
        }

        // 短路径判断，移除末尾的/
        private String normalizePath(String path) {
            if (path == null) return "";
            // 移除末尾斜杠，但保留根路径"/"
            return path.endsWith("/") && path.length() > 1
                    ? path.substring(0, path.length() - 1)
                    : path;
        }

        private String readRequest(HttpExchange exchange) throws IOException {
            try (InputStream is = exchange.getRequestBody();
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                int totalBytes = 0;
                long startTime = System.currentTimeMillis();

                while ((bytesRead = is.read(buffer)) != -1) {
                    // Check timeout
                    if (System.currentTimeMillis() - startTime > config.getTimeoutMs()) {
                        sendErrorResponse(exchange, 408, "Request Timeout");
                        return null;
                    }
                    // Check content length
                    totalBytes += bytesRead;
                    if (totalBytes > config.getMaxContentLength()) {
                        sendErrorResponse(exchange, 413, "Payload Too Large");
                        return null;
                    }
                    baos.write(buffer, 0, bytesRead);
                }
                return baos.toString();
            }
        }

        /**
         * auth valid
         *
         * @param exchange exchange
         * @return true/false
         */
        private boolean authenticate(HttpExchange exchange) {
            switch (config.getAuthType()) {
                case HttpServerOptions.AUTH_BASIC:
                    return checkBasicAuth(exchange);
                case HttpServerOptions.AUTH_BEARER:
                    return checkBearerToken(exchange);
                case HttpServerOptions.AUTH_API_KEY:
                    return checkApiKey(exchange);
                case HttpServerOptions.AUTH_NONE:
                default:
                    return true;
            }
        }

        private boolean checkBasicAuth(HttpExchange exchange) {
            String authHeader = exchange.getRequestHeaders().getFirst(HttpServerOptions.HEADER_AUTHORIZATION);
            String expected = "Basic " + config.getAuthConfig().get("credentials");
            return expected.equals(authHeader);
        }

        private boolean checkBearerToken(HttpExchange exchange) {
            String authHeader = exchange.getRequestHeaders().getFirst(HttpServerOptions.HEADER_AUTHORIZATION);
            String expected = config.getAuthConfig().get("token");
            return authHeader != null && authHeader.equals("Bearer " + expected);
        }

        private boolean checkApiKey(HttpExchange exchange) {
            String headerName = config.getAuthConfig().get("header_name");
            String expected = config.getAuthConfig().get("api_key");
            String actual = exchange.getRequestHeaders().getFirst(headerName);
            return expected.equals(actual);
        }

        private void sendSuccessResponse(HttpExchange exchange) throws IOException {
            Map<String, Object> response = new HashMap<>();
            response.put("code", 200);
            response.put("msg", "OK");
            response.put("timestamp", System.currentTimeMillis());
            sendJsonResponse(exchange, 200, response);
        }

        private void sendErrorResponse(HttpExchange exchange, int code, String message) {
            try {
                Map<String, Object> response = new HashMap<>();
                response.put("code", code);
                response.put("msg", message);
                response.put("timestamp", System.currentTimeMillis());
                sendJsonResponse(exchange, code, response);
            } catch (IOException e) {
                // Ignore secondary errors
            }
        }

        private void sendJsonResponse(HttpExchange exchange, int code, Map<String, Object> response) throws IOException {
            String jsonResponse = JsonUtils.toJsonString(response);
            byte[] responseBytes = jsonResponse.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
            exchange.sendResponseHeaders(code, responseBytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(responseBytes);
            }
        }
    }
}
