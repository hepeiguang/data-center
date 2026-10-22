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

package org.apache.seatunnel.e2e.connector.udp;

import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;
import org.apache.seatunnel.api.table.catalog.Catalog;
import org.apache.seatunnel.e2e.common.TestResource;
import org.apache.seatunnel.e2e.common.TestSuiteBase;
import org.apache.seatunnel.shade.com.google.common.collect.Lists;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerLoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

@Slf4j
public class UdpIT extends TestSuiteBase implements TestResource {

    private static final String TYPESENSE_DOCKER_IMAGE = "typesense/typesense:26.0";

    private static final String HOST = "e2e_typesense";

    private static final int PORT = 8108;

    private GenericContainer<?> typesenseServer;

    private static final String sinkCollection = "typesense_test_collection";

    private static final String sourceCollection = "typesense_test_collection_for_source";

    private Catalog catalog;

    @BeforeEach
    @Override
    public void startUp() throws Exception {
        typesenseServer =
                new GenericContainer<>(TYPESENSE_DOCKER_IMAGE)
                        .withNetwork(NETWORK)
                        .withNetworkAliases(HOST)
                        .withPrivilegedMode(true)
                        .withStartupAttempts(5)
                        .withCommand("--data-dir=/", "--api-key=xyz")
                        .withStartupTimeout(Duration.ofMinutes(5))
                        .withLogConsumer(
                                new Slf4jLogConsumer(
                                        DockerLoggerFactory.getLogger(TYPESENSE_DOCKER_IMAGE)));
        typesenseServer.setPortBindings(Lists.newArrayList(String.format("%s:%s", PORT, PORT)));
        Startables.deepStart(Stream.of(typesenseServer)).join();
        log.info("Typesense container started");
        Awaitility.given()
                .ignoreExceptions()
                .atLeast(1L, TimeUnit.SECONDS)
                .pollInterval(1L, TimeUnit.SECONDS)
                .atMost(120L, TimeUnit.SECONDS)
                .untilAsserted(this::initConnection);
    }

    private void initConnection() {
        String host = typesenseServer.getContainerIpAddress();
        Map<String, Object> config = new HashMap<>();
        ReadonlyConfig readonlyConfig = ReadonlyConfig.fromMap(config);

        catalog.open();
    }
    @AfterEach
    @Override
    public void tearDown() {
        typesenseServer.close();
        if (catalog != null) {
            catalog.close();
        }
    }
}
