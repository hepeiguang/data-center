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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Parses plugin descriptor YAML from JAR files using ZipFile (no class loading).
 *
 * <p>Security measures:
 * <ul>
 *   <li>Entry count limit to prevent zip-bomb attacks</li>
 *   <li>Uncompressed size limit per entry</li>
 *   <li>Path traversal detection (../ in entry names)</li>
 *   <li>YAML size limit to prevent memory exhaustion</li>
 *   <li>No class loading - pure file I/O only</li>
 * </ul>
 */
public class PluginDescriptorParser {

    private static final Logger logger = LoggerFactory.getLogger(PluginDescriptorParser.class);

    private static final String DESCRIPTOR_ENTRY = "META-INF/ds-plugin.yaml";
    private static final String ICON_ENTRY_PREFIX = "META-INF/ds-plugin/";

    private static final int MAX_ENTRIES = 10000;
    private static final int MAX_YAML_SIZE_BYTES = 1024 * 1024; // 1 MB
    private static final int MAX_ENTRY_UNCOMPRESSED_SIZE = 10 * 1024 * 1024; // 10 MB
    private static final int MAX_DECOMPRESSION_RATIO = 100;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse the plugin descriptor from a JAR file.
     *
     * @param jarFile the JAR file to parse
     * @return the parsed descriptor, or null if no descriptor entry found
     * @throws IOException if the JAR cannot be read
     * @throws PluginDescriptorParseException if the descriptor is malformed
     */
    public PluginDescriptor parse(File jarFile) throws IOException, PluginDescriptorParseException {
        if (jarFile == null || !jarFile.exists() || !jarFile.isFile()) {
            throw new IOException("JAR file does not exist or is not a file: " + jarFile);
        }

        try (JarFile jar = new JarFile(jarFile)) {
            validateJarSecurity(jar);

            JarEntry descriptorEntry = jar.getJarEntry(DESCRIPTOR_ENTRY);
            if (descriptorEntry == null) {
                logger.debug("No descriptor entry {} found in {}", DESCRIPTOR_ENTRY, jarFile.getName());
                return null;
            }

            String yamlContent = readEntrySafely(jar, descriptorEntry);
            return parseYaml(yamlContent, jarFile.getName());
        }
    }

    /**
     * Read the icon bytes from a JAR file.
     *
     * @param jarFile the JAR file
     * @param iconPath the icon path within the JAR (e.g., "META-INF/ds-plugin/icon.png")
     * @return the icon bytes, or null if not found
     * @throws IOException if the JAR cannot be read
     */
    public byte[] readIcon(File jarFile, String iconPath) throws IOException {
        if (iconPath == null || !iconPath.startsWith(ICON_ENTRY_PREFIX)) {
            throw new IOException("Invalid icon path: " + iconPath);
        }

        try (JarFile jar = new JarFile(jarFile)) {
            JarEntry iconEntry = jar.getJarEntry(iconPath);
            if (iconEntry == null) {
                return null;
            }

            try {
                return readEntryBytesSafely(jar, iconEntry);
            } catch (PluginDescriptorParseException e) {
                throw new IOException("Security check failed for icon entry: " + iconPath, e);
            }
        }
    }

    /**
     * Check if a JAR file contains a valid TaskChannelFactory SPI entry.
     */
    public boolean hasTaskChannelFactorySpi(File jarFile) throws IOException {
        try (JarFile jar = new JarFile(jarFile)) {
            return jar.getJarEntry("META-INF/services/org.apache.dolphinscheduler.plugin.task.api.TaskChannelFactory") != null;
        }
    }

    private void validateJarSecurity(JarFile jar) throws PluginDescriptorParseException {
        int entryCount = 0;
        Enumeration<JarEntry> entries = jar.entries();

        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            entryCount++;

            if (entryCount > MAX_ENTRIES) {
                throw new PluginDescriptorParseException(
                        "JAR exceeds maximum entry count (" + MAX_ENTRIES + "): possible zip-bomb");
            }

            String name = entry.getName();
            if (name.contains("../") || name.contains("/..") || name.startsWith("..")) {
                throw new PluginDescriptorParseException(
                        "Path traversal detected in entry: " + name);
            }

            if (entry.getSize() > MAX_ENTRY_UNCOMPRESSED_SIZE) {
                throw new PluginDescriptorParseException(
                        "Entry exceeds maximum uncompressed size: " + name);
            }

            if (entry.getSize() > 0 && entry.getCompressedSize() > 0) {
                long ratio = entry.getSize() / entry.getCompressedSize();
                if (ratio > MAX_DECOMPRESSION_RATIO) {
                    throw new PluginDescriptorParseException(
                            "Decompression ratio too high for entry: " + name + " (ratio=" + ratio + ")");
                }
            }
        }
    }

    private String readEntrySafely(JarFile jar, JarEntry entry) throws IOException, PluginDescriptorParseException {
        byte[] bytes = readEntryBytesSafely(jar, entry);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private byte[] readEntryBytesSafely(JarFile jar, JarEntry entry) throws IOException, PluginDescriptorParseException {
        long size = entry.getSize();
        if (size > MAX_YAML_SIZE_BYTES) {
            throw new PluginDescriptorParseException(
                    "Entry " + entry.getName() + " exceeds maximum size (" + MAX_YAML_SIZE_BYTES + " bytes)");
        }

        int readLimit = size > 0 ? (int) size : MAX_YAML_SIZE_BYTES;
        byte[] buffer = new byte[readLimit];
        int totalRead = 0;

        try (InputStream is = jar.getInputStream(entry)) {
            while (totalRead < buffer.length) {
                int bytesRead = is.read(buffer, totalRead, buffer.length - totalRead);
                if (bytesRead == -1) {
                    break;
                }
                totalRead += bytesRead;
            }
        }

        if (totalRead == buffer.length && size <= 0) {
            throw new PluginDescriptorParseException(
                    "Entry " + entry.getName() + " exceeds maximum size limit");
        }

        byte[] result = new byte[totalRead];
        System.arraycopy(buffer, 0, result, 0, totalRead);
        return result;
    }

    @SuppressWarnings("unchecked")
    private PluginDescriptor parseYaml(String yamlContent, String sourceName) throws PluginDescriptorParseException {
        try {
            Yaml yaml = new Yaml(new Constructor(PluginDescriptor.class));
            PluginDescriptor descriptor = yaml.loadAs(yamlContent, PluginDescriptor.class);

            if (descriptor == null) {
                throw new PluginDescriptorParseException("Empty YAML descriptor in " + sourceName);
            }

            validateDescriptor(descriptor, sourceName);
            return descriptor;
        } catch (YAMLException e) {
            throw new PluginDescriptorParseException(
                    "Failed to parse YAML in " + sourceName + ": " + e.getMessage(), e);
        }
    }

    private void validateDescriptor(PluginDescriptor descriptor, String sourceName) throws PluginDescriptorParseException {
        if (descriptor.getApiVersion() == null || descriptor.getApiVersion().isEmpty()) {
            throw new PluginDescriptorParseException("Missing apiVersion in " + sourceName);
        }

        if (!"ds.plugin/v1".equals(descriptor.getApiVersion())) {
            throw new PluginDescriptorParseException(
                    "Unsupported apiVersion '" + descriptor.getApiVersion() + "' in " + sourceName
                            + ". Expected: ds.plugin/v1");
        }

        if (descriptor.getTask() == null) {
            throw new PluginDescriptorParseException("Missing 'task' section in " + sourceName);
        }

        if (descriptor.getTask().getType() == null || descriptor.getTask().getType().isEmpty()) {
            throw new PluginDescriptorParseException("Missing 'task.type' in " + sourceName);
        }

        if (descriptor.getForm() == null || descriptor.getForm().isEmpty()) {
            throw new PluginDescriptorParseException("Missing or empty 'form' section in " + sourceName);
        }
    }

    /**
     * Exception thrown when plugin descriptor parsing or validation fails.
     */
    public static class PluginDescriptorParseException extends Exception {
        public PluginDescriptorParseException(String message) {
            super(message);
        }

        public PluginDescriptorParseException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
