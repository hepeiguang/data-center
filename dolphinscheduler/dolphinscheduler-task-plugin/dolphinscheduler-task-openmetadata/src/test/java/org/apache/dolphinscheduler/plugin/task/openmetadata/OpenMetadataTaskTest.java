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

package org.apache.dolphinscheduler.plugin.task.openmetadata;

import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskConstants;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.TaskPluginManager;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class OpenMetadataTaskTest {

    @Test
    void registersTaskChannel() {
        OpenMetadataTaskChannelFactory factory = new OpenMetadataTaskChannelFactory();
        Assertions.assertEquals("OPENMETADATA", factory.getName());
        Assertions.assertNotNull(factory.create());
        // AutoService should have generated META-INF/services for the DS SPI.
        Assertions.assertNotNull(TaskPluginManager.getTaskChannel("OPENMETADATA"));
    }

    @Test
    void resolvesFqnsAndPushesLineage() throws Exception {
        AtomicReference<String> capturedBody = new AtomicReference<>();
        AtomicReference<String> capturedAuth = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/tables/name/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            String id = path.endsWith("from_table") ? "from-id" : "to-id";
            respond(exchange, 200, "{\"id\":\"" + id + "\",\"name\":\"x\"}");
        });
        server.createContext("/api/v1/lineage", exchange -> {
            capturedBody.set(read(exchange.getRequestBody()));
            capturedAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            respond(exchange, 200, "{}");
        });
        server.start();
        try {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("openMetadataUrl", base);
            params.put("token", "test-token");
            params.put("fromFqn", "svc.db.public.from_table");
            params.put("toFqn", "svc.db.public.to_table");
            params.put("sql", "insert into to_table (id, name) select from_id, from_name from from_table");

            TaskExecutionContext context = new TaskExecutionContext();
            context.setTaskParams(JSONUtils.toJsonString(params));
            OpenMetadataTask task = new OpenMetadataTask(context);
            task.init();
            task.handle(null);

            Assertions.assertEquals(TaskConstants.EXIT_CODE_SUCCESS, task.getExitStatusCode());
            Assertions.assertEquals("Bearer test-token", capturedAuth.get());
            Assertions.assertNotNull(capturedBody.get());
            Assertions.assertTrue(capturedBody.get().contains("\"from-id\""));
            Assertions.assertTrue(capturedBody.get().contains("\"to-id\""));
            Assertions.assertTrue(capturedBody.get().contains("from_table"));
            Assertions.assertTrue(capturedBody.get().contains("insert into to_table"));
            Assertions.assertTrue(capturedBody.get().contains("columnsLineage"));
            Assertions.assertTrue(capturedBody.get().contains("from_table.from_id"));
            Assertions.assertTrue(capturedBody.get().contains("to_table.id"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void failsWhenFqnCannotBeResolved() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/tables/name/", exchange -> respond(exchange, 404, "{\"message\":\"not found\"}"));
        server.start();
        try {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("openMetadataUrl", "http://127.0.0.1:" + server.getAddress().getPort());
            params.put("token", "test-token");
            params.put("fromFqn", "missing.from");
            params.put("toFqn", "missing.to");
            TaskExecutionContext context = new TaskExecutionContext();
            context.setTaskParams(JSONUtils.toJsonString(params));
            OpenMetadataTask task = new OpenMetadataTask(context);
            task.init();
            Assertions.assertThrows(RuntimeException.class, () -> task.handle(null));
            Assertions.assertEquals(TaskConstants.EXIT_CODE_FAILURE, task.getExitStatusCode());
        } finally {
            server.stop(0);
        }
    }

    private static void respond(HttpExchange exchange, int code, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(code, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static String read(InputStream input) throws IOException {
        byte[] buffer = new byte[4096];
        StringBuilder value = new StringBuilder();
        int length;
        while ((length = input.read(buffer)) >= 0) {
            value.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
        }
        return value.toString();
    }
}
