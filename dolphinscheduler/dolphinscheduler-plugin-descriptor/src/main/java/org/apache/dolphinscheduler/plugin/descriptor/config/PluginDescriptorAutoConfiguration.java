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

package org.apache.dolphinscheduler.plugin.descriptor.config;

import org.apache.dolphinscheduler.plugin.descriptor.service.PluginDescriptorCache;
import org.apache.dolphinscheduler.plugin.descriptor.service.PluginDescriptorService;
import org.apache.dolphinscheduler.plugin.descriptor.parser.PluginDescriptorParser;
import org.apache.dolphinscheduler.plugin.descriptor.parser.PluginDirectoryScanner;
import org.apache.dolphinscheduler.plugin.descriptor.validator.PluginDescriptorValidator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Auto-configuration for the plugin descriptor module.
 * Loaded via META-INF/spring.factories by api-server.
 */
@Configuration
@ComponentScan(basePackages = "org.apache.dolphinscheduler.plugin.descriptor.controller")
public class PluginDescriptorAutoConfiguration {

    @Value("${dolphinscheduler.plugins.path:plugins}")
    private String pluginsPath;

    @Value("${dolphinscheduler.plugins.task-plugins_dir:task-plugins}")
    private String taskPluginsDir;

    @Value("${dolphinscheduler.plugins.descriptor.scan_interval_ms:30000}")
    private long scanIntervalMs;

    @Bean
    public PluginDescriptorCache pluginDescriptorCache() {
        return new PluginDescriptorCache();
    }

    @Bean
    public PluginDescriptorParser pluginDescriptorParser() {
        return new PluginDescriptorParser();
    }

    @Bean
    public PluginDescriptorValidator pluginDescriptorValidator() {
        return new PluginDescriptorValidator();
    }

    @Bean
    public PluginDirectoryScanner pluginDirectoryScanner(PluginDescriptorParser parser,
                                                         PluginDescriptorCache cache,
                                                         PluginDescriptorValidator validator) {
        return new PluginDirectoryScanner(pluginsPath, taskPluginsDir, scanIntervalMs,
                parser, cache, validator);
    }

    @Bean
    public PluginDescriptorService pluginDescriptorService(PluginDirectoryScanner scanner,
                                                            PluginDescriptorCache cache) {
        return new PluginDescriptorService(scanner, cache);
    }
}
