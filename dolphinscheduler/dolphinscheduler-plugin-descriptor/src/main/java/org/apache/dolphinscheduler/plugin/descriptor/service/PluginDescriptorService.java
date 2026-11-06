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

package org.apache.dolphinscheduler.plugin.descriptor.service;

import org.apache.dolphinscheduler.plugin.descriptor.model.PluginDescriptor;
import org.apache.dolphinscheduler.plugin.descriptor.model.PluginDescriptorDetail;
import org.apache.dolphinscheduler.plugin.descriptor.model.PluginDescriptorSummary;
import org.apache.dolphinscheduler.plugin.descriptor.parser.PluginDescriptorParser;
import org.apache.dolphinscheduler.plugin.descriptor.parser.PluginDirectoryScanner;
import org.apache.dolphinscheduler.plugin.descriptor.validator.PluginDescriptorValidator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Service for managing plugin descriptors.
 */
public class PluginDescriptorService {

    private static final Logger logger = LoggerFactory.getLogger(PluginDescriptorService.class);

    private final PluginDirectoryScanner scanner;
    private final PluginDescriptorCache cache;
    private final PluginDescriptorParser parser;
    private final PluginDescriptorValidator validator;

    public PluginDescriptorService(PluginDirectoryScanner scanner, PluginDescriptorCache cache) {
        this.scanner = scanner;
        this.cache = cache;
        this.parser = new PluginDescriptorParser();
        this.validator = new PluginDescriptorValidator();
    }

    /**
     * Get summary list of all registered plugin descriptors.
     */
    public List<PluginDescriptorSummary> listDescriptors() {
        List<PluginDescriptorSummary> summaries = new ArrayList<>();

        for (String taskType : cache.getAllTaskTypes()) {
            PluginDescriptorCache.CacheEntry entry = cache.get(taskType);
            if (entry != null && entry.getSummary() != null) {
                summaries.add(entry.getSummary());
            } else if (entry != null && entry.getDescriptor() != null) {
                summaries.add(buildSummary(taskType, entry));
            }
        }

        return summaries;
    }

    /**
     * Get detailed descriptor for a specific task type.
     */
    public PluginDescriptorDetail getDescriptor(String taskType) {
        PluginDescriptorCache.CacheEntry entry = cache.get(taskType);
        if (entry == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No descriptor found for task type: " + taskType);
        }

        PluginDescriptor descriptor = entry.getDescriptor();
        if (descriptor == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Descriptor for task type '" + taskType + "' is invalid or missing");
        }

        List<String> errors = validator.validate(descriptor);
        return PluginDescriptorDetail.builder()
                .descriptor(descriptor)
                .descriptorStatus(entry.getStatus())
                .jarFileName(entry.getJarFile() != null ? entry.getJarFile().getName() : null)
                .jarSha256(entry.getSha256())
                .validationErrors(errors.stream()
                        .map(e -> PluginDescriptorDetail.ValidationError.builder()
                                .field("descriptor")
                                .message(e)
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    /**
     * Get icon bytes for a specific task type.
     */
    public byte[] getIcon(String taskType) {
        PluginDescriptorCache.CacheEntry entry = cache.get(taskType);
        if (entry == null || entry.getDescriptor() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No icon found for task type: " + taskType);
        }

        PluginDescriptor descriptor = entry.getDescriptor();
        if (descriptor.getIcon() == null || descriptor.getIcon().getRef() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "No icon reference in descriptor for task type: " + taskType);
        }

        File jarFile = entry.getJarFile();
        if (jarFile == null || !jarFile.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "JAR file not found for task type: " + taskType);
        }

        try {
            byte[] iconBytes = parser.readIcon(jarFile, descriptor.getIcon().getRef());
            if (iconBytes == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Icon file not found in JAR for task type: " + taskType);
            }
            return iconBytes;
        } catch (IOException e) {
            logger.error("Failed to read icon from JAR for task type: {}", taskType, e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to read icon: " + e.getMessage());
        }
    }

    /**
     * Force a rescan of the plugin directory.
     */
    public void rescan() {
        scanner.forceRescan();
    }

    /**
     * Get the status of a specific task type.
     */
    public PluginDescriptor.DescriptorStatus getStatus(String taskType) {
        return cache.getStatus(taskType);
    }

    private PluginDescriptorSummary buildSummary(String taskType, PluginDescriptorCache.CacheEntry entry) {
        PluginDescriptor descriptor = entry.getDescriptor();
        PluginDescriptorSummary summary = PluginDescriptorSummary.builder()
                .type(taskType)
                .name(descriptor.getTask() != null ? descriptor.getTask().getName() : taskType)
                .category(descriptor.getTask() != null ? descriptor.getTask().getCategory() : "other")
                .descriptorStatus(entry.getStatus())
                .iconUrl("/task/plugin-descriptors/" + taskType + "/icon")
                .descriptorUrl("/task/plugin-descriptors/" + taskType)
                .build();

        entry.setSummary(summary);
        return summary;
    }
}
