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

package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin;

import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.plugin.task.api.TaskException;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.spi.plugin.PrioritySPIFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class SeatunnelPluginManager {

    private static final Map<String, TaskTypeFactory> seatunnelPluginFactoryMap = new HashMap<>();

    private static final AtomicBoolean loadedFlag = new AtomicBoolean(false);

    /**
     * Load task plugins from classpath.
     */
    public static void loadSeatunnelPlugin() {
        if (!loadedFlag.compareAndSet(false, true)) {
            log.warn("The seatunnel plugin has already been loaded");
            return;
        }
        PrioritySPIFactory<TaskTypeFactory> prioritySPIFactory = new PrioritySPIFactory<>(TaskTypeFactory.class);
        for (Map.Entry<String, TaskTypeFactory> entry : prioritySPIFactory.getSPIMap().entrySet()) {
            String factoryName = entry.getKey().toLowerCase();
            TaskTypeFactory factory = entry.getValue();

            log.info("Registering seatunnel plugin: {} - {}", factoryName, factory.getClass().getSimpleName());

            seatunnelPluginFactoryMap.put(factoryName, factory);

            log.info("Registered seatunnel plugin: {} - {}", factoryName, factory.getClass().getSimpleName());
        }

    }


    public static Map<String, TaskTypeFactory> getTaskTypeFactoryMap() {
        return Collections.unmodifiableMap(seatunnelPluginFactoryMap);
    }

    public static Map<String, TaskTypeFactory> getTaskTypeFactoryMaptest() {
        return seatunnelPluginFactoryMap;
    }

    public static TaskTypeFactory getTaskTypeFactory(String type, String pluginName) {
        TaskTypeFactory factory = getTaskTypeFactoryMap().get((type + "_" + pluginName).toLowerCase());
        if (factory == null) {
            log.error("SeaTunnel task type {} {} is not supported", type, pluginName);
            throw new TaskException("SeaTunnel task type " + type + " " + pluginName + " is not supported");
        }
        return factory;
    }
}
