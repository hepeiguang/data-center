/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.plugin.task.webhook;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.AbstractTask;
import org.apache.dolphinscheduler.plugin.task.api.TaskCallBack;
import org.apache.dolphinscheduler.plugin.task.api.TaskException;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.parameters.AbstractParameters;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class WebHookTask extends AbstractTask {

    // 端口到服务器实例的映射
    private static final ConcurrentHashMap<Integer, HttpServer> servers = new ConcurrentHashMap<>();

    /**
     * webhook parameters
     */
    private WebHookParameters webHookParameters;

    /**
     * taskExecutionContext
     */
    private TaskExecutionContext taskExecutionContext;

    private boolean serverRunning = false;

    /**
     * constructor
     *
     * @param taskExecutionContext taskExecutionContext
     */
    public WebHookTask(TaskExecutionContext taskExecutionContext) {
        super(taskExecutionContext);
        this.taskExecutionContext = taskExecutionContext;
    }

    @Override
    public void init() {
        log.info("webhook task params {}", taskExecutionContext.getTaskParams());
        this.webHookParameters = JSONUtils.parseObject(taskExecutionContext.getTaskParams(), WebHookParameters.class);

        if (webHookParameters == null || !webHookParameters.checkParameters()) {
            throw new TaskException("webhook task params is not valid");
        }
    }

    @Override
    public void handle(TaskCallBack taskCallBack) throws TaskException {
        exitStatusCode = 0;
        String url = webHookParameters.getUrl();
        //http://43.136.32.15:8024/dolphinscheduler/ui/projects/142834882786624/workflow/definitions/create
        try {
            // 解析url，获取host和port和path
            URI uri = new URI(url);
            // 获取协议（小写）
            String protocol = uri.getScheme();

            // 获取主机（域名或IP）
            String host = uri.getHost();
            if (host == null) {
                log.error("Invalid URL, host is null: {}", url);
                throw new TaskException("Invalid URL, host is null: " + url);
            }

            // 获取端口（如果没有显式指定则返回默认值）
            int port = uri.getPort();
            if (port == -1) {
                // 根据协议确定默认端口
                if ("http".equalsIgnoreCase(protocol)) {
                    port = 80;
                } else if ("https".equalsIgnoreCase(protocol)) {
                    port = 443;
                }
            }

            // 获取路径
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/"; // 路径不存在时设置为根路径
            }
            log.info("host: {}, port: {}, path: {}", host, port, path);

            startServer("0.0.0.0", port, path);

            while (serverRunning) {
                log.info("server is running, Waiting for request...");
                // 当前线程挂起，等待请求处理完成
                Thread.sleep(5000);
            }
        } catch (Exception e) {
            log.error("start server error", e);
            exitStatusCode = -1;
            throw new RuntimeException(e);
        }
    }

    public void startServer(String host, int port, String path) throws IOException {
        // 创建 HTTP 服务器
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        // 使用原子变量确保只停止一次
        AtomicBoolean requestHandled = new AtomicBoolean(false);

        // 设置根路径处理程序
        server.createContext(path, exchange -> {
            log.info("this server received a request, protocol: {}, method: {}, remoteAddress: {}, requestURI: {}",
                    exchange.getProtocol(),
                    exchange.getRequestMethod(),
                    exchange.getRemoteAddress(),
                    exchange.getRequestURI()
            );

            if (!requestHandled.compareAndSet(false, true)) {
                // 重复请求 - 服务已关闭
                sendResponse(exchange, 503, "Service shutting down");
                return;
            }

            try {
                String method = exchange.getRequestMethod();

                // 构建响应
                String response = method.equalsIgnoreCase(webHookParameters.getWebHookMethod().name())
                        ? webHookParameters.getResponse()
                        : "Unsupported Method";

                // 发送响应
                sendResponse(exchange, 200, response);

                // 延迟停止服务器（允许响应完成）
                new Thread(() -> {
                    try {
                        // 等待1秒确保响应完全发送
                        Thread.sleep(1000);
                        stopServer();
                    } catch (Exception e) {
                        log.error("Server stopped error..., port: {}", port);
                        e.printStackTrace();
                    }
                }).start();

            } catch (Exception e) {
                e.printStackTrace();
                sendResponse(exchange, 500, "Internal server error");
            }
        });

        server.start();
        servers.put(port, server);
        serverRunning = true;
        log.info("Server started at {}:{}:{}", host, port, path);
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String content) throws IOException {
        // 输出json
        exchange.getResponseHeaders().add("Content-Type", "application/json; charset=utf-8");
        byte[] responseBytes = content.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, responseBytes.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(responseBytes);
        }
        log.info("Response sent ({}): {}", statusCode, content);
    }

    private void stopServer() {
        log.info("Stopping server...");
        servers.forEach((port, server) -> {
            server.stop(0);
            servers.remove(port);
        });
        serverRunning = false;
        log.info("server stopped successful");
    }

    @Override
    public void cancel() throws TaskException {
        // 停止服务
        stopServer();
    }


    @Override
    public AbstractParameters getParameters() {
        return webHookParameters;
    }

}
