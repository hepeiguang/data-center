package org.apache.dolphinscheduler.plugin.datasource.kafka;/*
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

import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.kafka.param.KafkaDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.kafka.param.KafkaDataSourceProcessor;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class KafkaDataSourceProcessorTest {

    private KafkaDataSourceProcessor kafkaDataSourceProcessor;

    private String connectJson =
            "{\"bootstrapServers\":\"192.168.0.203:9092\"}";

    @BeforeEach
    public void init() {
        kafkaDataSourceProcessor = new KafkaDataSourceProcessor();
    }

    @Test
    void testCheckDatasourceParam() {
        BaseDataSourceParamDTO baseDataSourceParamDTO = new KafkaDataSourceParamDTO();
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> kafkaDataSourceProcessor.checkDatasourceParam(baseDataSourceParamDTO));
        baseDataSourceParamDTO.setHost("localhost:9092");
        Assertions.assertThrows(IllegalArgumentException.class,
                () -> kafkaDataSourceProcessor.checkDatasourceParam(baseDataSourceParamDTO));
        baseDataSourceParamDTO.setUserName("root");
        Assertions.assertDoesNotThrow(() -> kafkaDataSourceProcessor.checkDatasourceParam(baseDataSourceParamDTO));

    }

    @Test
    void testTestConnection() {
        KafkaDataSourceParamDTO sshDataSourceParamDTO =
                (KafkaDataSourceParamDTO) kafkaDataSourceProcessor.createDatasourceParamDTO(connectJson);
        ConnectionParam connectionParam = kafkaDataSourceProcessor.createConnectionParams(sshDataSourceParamDTO);

        Assertions.assertTrue(kafkaDataSourceProcessor.checkDataSourceConnectivity(connectionParam));
    }

}
