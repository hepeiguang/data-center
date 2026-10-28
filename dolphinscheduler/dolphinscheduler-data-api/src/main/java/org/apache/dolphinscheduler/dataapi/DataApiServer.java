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

package org.apache.dolphinscheduler.dataapi;

import org.apache.dolphinscheduler.common.CommonConfiguration;
import org.apache.dolphinscheduler.common.lifecycle.ServerLifeCycleManager;
import org.apache.dolphinscheduler.common.thread.DefaultUncaughtExceptionHandler;
import org.apache.dolphinscheduler.dao.DaoConfiguration;
import org.apache.dolphinscheduler.plugin.datasource.api.plugin.DataSourcePluginManager;

import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * DolphinScheduler data-api microservice entry.
 * Shares metadata DB / token / datasource center with api-server; publishes SQL as HTTP via magic-api.
 */
@Slf4j
@EnableScheduling
@Import({DaoConfiguration.class, CommonConfiguration.class})
@SpringBootApplication
public class DataApiServer {

    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler(DefaultUncaughtExceptionHandler.getInstance());
        SpringApplication.run(DataApiServer.class, args);
    }

    @EventListener
    public void onReady(ApplicationReadyEvent event) {
        ServerLifeCycleManager.toRunning();
        DataSourcePluginManager.loadDataSourcePlugin();
        log.info("DataApiServer is ready (magic-api + shared DS metadata)");
    }
}
