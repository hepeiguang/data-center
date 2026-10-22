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

package org.apache.seatunnel.connectors.seatunnel.udp.sink;

import com.google.auto.service.AutoService;
import org.apache.seatunnel.api.configuration.util.OptionRule;
import org.apache.seatunnel.api.table.connector.TableSink;
import org.apache.seatunnel.api.table.factory.Factory;
import org.apache.seatunnel.api.table.factory.TableSinkFactory;
import org.apache.seatunnel.api.table.factory.TableSinkFactoryContext;
import org.apache.seatunnel.connectors.seatunnel.udp.config.UdpSinkConfig;

import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.ASYNC_ENABLED;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.ASYNC_THREADS;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.ENABLE_MESSAGE_REPLY;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.HOST;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.IDENTIFIER;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.MAX_RETRIES;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.PORT;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.RETRY_INTERVAL_MS;
import static org.apache.seatunnel.connectors.seatunnel.udp.config.UdpOptions.TIMEOUT_MS;

@AutoService(Factory.class)
public class UdpSinkFactory implements TableSinkFactory {
    @Override
    public String factoryIdentifier() {
        return IDENTIFIER;
    }

    @Override
    public OptionRule optionRule() {
        return OptionRule.builder()
                .required(HOST, PORT)
                .optional(
                        MAX_RETRIES,
                        TIMEOUT_MS,
                        RETRY_INTERVAL_MS,
                        ENABLE_MESSAGE_REPLY,
                        ASYNC_ENABLED,
                        ASYNC_THREADS)
                .build();
    }

    @Override
    public TableSink createSink(TableSinkFactoryContext context) {
        return () -> new UdpSink(new UdpSinkConfig(context.getOptions()), context.getCatalogTable().getSeaTunnelRowType());
    }
}
