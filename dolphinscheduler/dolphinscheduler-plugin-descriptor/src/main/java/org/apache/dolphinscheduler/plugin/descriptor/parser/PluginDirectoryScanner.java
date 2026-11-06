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

package org.apache.dolphinscheduler.plugin.descriptor.parser;

import org.apache.dolphinscheduler.plugin.descriptor.model.PluginDescriptor;
import org.apache.dolphinscheduler.plugin.descriptor.service.PluginDescriptorCache;
import org.apache.dolphinscheduler.plugin.descriptor.validator.PluginDescriptorValidator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans the task-plugins directory for JAR files and parses their descriptors.
 * Runs periodically to detect new/changed JARs (hot-reload of form metadata).
 */
public class PluginDirectoryScanner {

    private static final Logger logger = LoggerFactory.getLogger(PluginDirectoryScanner.class);

    private final String pluginsPath;
    private final String taskPluginsDir;
    private final long scanIntervalMs;
    private final PluginDescriptorParser parser;
    private final PluginDescriptorCache cache;
    private final PluginDescriptorValidator validator;
    private final ScheduledExecutorService scheduler;

    public PluginDirectoryScanner(String pluginsPath, String taskPluginsDir, long scanIntervalMs,
                                   PluginDescriptorParser parser, PluginDescriptorCache cache,
                                   PluginDescriptorValidator validator) {
        this.pluginsPath = pluginsPath;
        this.taskPluginsDir = taskPluginsDir;
        this.scanIntervalMs = scanIntervalMs;
        this.parser = parser;
        this.cache = cache;
        this.validator = validator;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "plugin-descriptor-scanner");
            t.setDaemon(true);
            return t;
        });
    }

    @PostConstruct
    public void start() {
        logger.info("Starting plugin directory scanner (interval={}ms, path={}/{})",
                scanIntervalMs, pluginsPath, taskPluginsDir);

        // Initial scan
        scan();

        // Schedule periodic scans
        scheduler.scheduleWithFixedDelay(this::scan, scanIntervalMs, scanIntervalMs, TimeUnit.MILLISECONDS);
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Perform a single scan of the task-plugins directory.
     */
    public void scan() {
        Path taskPluginsPath = resolveTaskPluginsPath();
        if (taskPluginsPath == null || !Files.isDirectory(taskPluginsPath)) {
            logger.warn("Task plugins directory does not exist: {}", taskPluginsPath);
            return;
        }

        List<File> jarFiles = findJarFiles(taskPluginsPath);
        logger.debug("Found {} JAR files in {}", jarFiles.size(), taskPluginsPath);

        for (File jarFile : jarFiles) {
            processJarFile(jarFile);
        }
    }

    /**
     * Force a rescan (e.g., after uploading a new JAR).
     */
    public void forceRescan() {
        scan();
    }

    private Path resolveTaskPluginsPath() {
        // Try relative to working directory first
        Path relative = Paths.get(pluginsPath, taskPluginsDir);
        if (Files.isDirectory(relative)) {
            return relative.toAbsolutePath();
        }

        // Try relative to DOLPHINSCHEDULER_HOME
        String dsHome = System.getenv("DOLPHINSCHEDULER_HOME");
        if (dsHome == null) {
            dsHome = System.getProperty("dolphinscheduler.home");
        }
        if (dsHome != null) {
            Path fromHome = Paths.get(dsHome, pluginsPath, taskPluginsDir);
            if (Files.isDirectory(fromHome)) {
                return fromHome;
            }
        }

        return relative.toAbsolutePath();
    }

    private List<File> findJarFiles(Path directory) {
        List<File> jars = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.jar")) {
            for (Path entry : stream) {
                jars.add(entry.toFile());
            }
        } catch (IOException e) {
            logger.error("Failed to list JAR files in {}", directory, e);
        }
        return jars;
    }

    private void processJarFile(File jarFile) {
        String taskType = null;
        try {
            // Check if already cached and not stale
            String sha256 = computeSha256(jarFile);
            PluginDescriptorCache.CacheEntry existing = null;

            // Find existing entry by JAR file
            for (String type : cache.getAllTaskTypes()) {
                PluginDescriptorCache.CacheEntry entry = cache.get(type);
                if (entry != null && jarFile.equals(entry.getJarFile())) {
                    existing = entry;
                    taskType = type;
                    break;
                }
            }

            if (existing != null && sha256.equals(existing.getSha256()) && !existing.isStale(jarFile)) {
                return; // No change
            }

            // Check for SPI
            if (!parser.hasTaskChannelFactorySpi(jarFile)) {
                logger.debug("JAR {} has no TaskChannelFactory SPI, skipping", jarFile.getName());
                return;
            }

            // Parse descriptor
            PluginDescriptor descriptor = parser.parse(jarFile);
            if (descriptor == null) {
                // No descriptor YAML - mark as MISSING
                if (existing != null) {
                    cache.invalidate(taskType);
                }
                return;
            }

            taskType = descriptor.getTask().getType();

            // Validate
            List<String> errors = validator.validate(descriptor);
            if (!errors.isEmpty()) {
                logger.warn("Descriptor validation failed for {} ({}): {}",
                        taskType, jarFile.getName(), errors);
                cache.put(taskType, descriptor, jarFile, sha256,
                        PluginDescriptor.DescriptorStatus.INVALID);
                return;
            }

            // Check SPI name consistency
            // (In a real implementation, we'd load the SPI name without instantiating the class.
            // For now, we trust the descriptor's task.type field.)

            // Cache the valid descriptor
            cache.put(taskType, descriptor, jarFile, sha256,
                    PluginDescriptor.DescriptorStatus.PENDING_ACTIVATION);

            logger.info("Successfully parsed descriptor for task type '{}' from {}",
                    taskType, jarFile.getName());

        } catch (PluginDescriptorParser.PluginDescriptorParseException e) {
            logger.error("Failed to parse descriptor from {}: {}", jarFile.getName(), e.getMessage());
            if (taskType != null) {
                cache.put(taskType, null, jarFile, null, PluginDescriptor.DescriptorStatus.INVALID);
            }
        } catch (IOException e) {
            logger.error("Failed to read JAR file {}: {}", jarFile.getName(), e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            logger.error("SHA-256 algorithm not available", e);
        }
    }

    private String computeSha256(File file) throws IOException, NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }
        byte[] hash = digest.digest();
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
