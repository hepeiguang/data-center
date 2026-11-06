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
import org.apache.dolphinscheduler.plugin.descriptor.model.PluginDescriptorSummary;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * In-memory cache for parsed plugin descriptors.
 * Thread-safe, keyed by task type.
 */
public class PluginDescriptorCache {

    private static final Logger logger = LoggerFactory.getLogger(PluginDescriptorCache.class);

    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public void put(String taskType, PluginDescriptor descriptor, File jarFile,
                    String sha256, PluginDescriptor.DescriptorStatus status) {
        cache.put(taskType, new CacheEntry(descriptor, jarFile, sha256, status, System.currentTimeMillis()));
        logger.info("Cached descriptor for task type: {} (status={})", taskType, status);
    }

    public void putSummary(String taskType, PluginDescriptorSummary summary) {
        CacheEntry existing = cache.get(taskType);
        if (existing != null) {
            existing.setSummary(summary);
        } else {
            CacheEntry entry = new CacheEntry(null, null, null,
                    summary.getDescriptorStatus(), System.currentTimeMillis());
            entry.setSummary(summary);
            cache.put(taskType, entry);
        }
    }

    public CacheEntry get(String taskType) {
        return cache.get(taskType);
    }

    public PluginDescriptor getDescriptor(String taskType) {
        CacheEntry entry = cache.get(taskType);
        return entry != null ? entry.getDescriptor() : null;
    }

    public PluginDescriptor.DescriptorStatus getStatus(String taskType) {
        CacheEntry entry = cache.get(taskType);
        return entry != null ? entry.getStatus() : PluginDescriptor.DescriptorStatus.MISSING;
    }

    public Collection<String> getAllTaskTypes() {
        return Collections.unmodifiableSet(cache.keySet());
    }

    public Collection<CacheEntry> getAllEntries() {
        return Collections.unmodifiableCollection(cache.values());
    }

    public void invalidate(String taskType) {
        cache.remove(taskType);
        logger.info("Invalidated cache for task type: {}", taskType);
    }

    public void clear() {
        cache.clear();
        logger.info("Plugin descriptor cache cleared");
    }

    public int size() {
        return cache.size();
    }

    /**
     * Cache entry holding descriptor, source JAR info, and status.
     */
    public static class CacheEntry {
        private final PluginDescriptor descriptor;
        private final File jarFile;
        private final String sha256;
        private volatile PluginDescriptor.DescriptorStatus status;
        private volatile PluginDescriptorSummary summary;
        private final long cachedAt;
        private volatile long lastModified;

        public CacheEntry(PluginDescriptor descriptor, File jarFile, String sha256,
                          PluginDescriptor.DescriptorStatus status, long cachedAt) {
            this.descriptor = descriptor;
            this.jarFile = jarFile;
            this.sha256 = sha256;
            this.status = status;
            this.cachedAt = cachedAt;
            this.lastModified = jarFile != null ? jarFile.lastModified() : 0;
        }

        public PluginDescriptor getDescriptor() {
            return descriptor;
        }

        public File getJarFile() {
            return jarFile;
        }

        public String getSha256() {
            return sha256;
        }

        public PluginDescriptor.DescriptorStatus getStatus() {
            return status;
        }

        public void setStatus(PluginDescriptor.DescriptorStatus status) {
            this.status = status;
        }

        public PluginDescriptorSummary getSummary() {
            return summary;
        }

        public void setSummary(PluginDescriptorSummary summary) {
            this.summary = summary;
        }

        public long getCachedAt() {
            return cachedAt;
        }

        public long getLastModified() {
            return lastModified;
        }

        public boolean isStale(File currentFile) {
            return currentFile != null && currentFile.lastModified() != this.lastModified;
        }
    }
}
