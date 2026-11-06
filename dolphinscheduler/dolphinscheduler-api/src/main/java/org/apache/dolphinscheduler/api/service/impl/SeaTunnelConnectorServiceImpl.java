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

package org.apache.dolphinscheduler.api.service.impl;

import org.apache.dolphinscheduler.api.service.SeaTunnelConnectorService;
import org.apache.dolphinscheduler.common.utils.JSONUtils;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class SeaTunnelConnectorServiceImpl implements SeaTunnelConnectorService {

    private static final String CATALOG_RESOURCE = "seatunnel-connectors-catalog.json";

    private final AtomicReference<Map<String, Object>> cache = new AtomicReference<>();

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConnectorCatalog() {
        Map<String, Object> cached = cache.get();
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            cached = cache.get();
            if (cached != null) {
                return cached;
            }
            try (InputStream in = new ClassPathResource(CATALOG_RESOURCE).getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String json = reader.lines().collect(Collectors.joining("\n"));
                Map<String, Object> catalog = JSONUtils.parseObject(json, Map.class);
                if (catalog == null) {
                    catalog = emptyCatalog();
                }
                cache.set(Collections.unmodifiableMap(new LinkedHashMap<>(catalog)));
                return cache.get();
            } catch (Exception e) {
                log.error("Failed to load SeaTunnel connector catalog from {}", CATALOG_RESOURCE, e);
                return emptyCatalog();
            }
        }
    }

    private static Map<String, Object> emptyCatalog() {
        Map<String, Object> empty = new LinkedHashMap<>();
        empty.put("sources", Collections.emptyList());
        empty.put("sinks", Collections.emptyList());
        empty.put("transforms", Collections.emptyList());
        return empty;
    }

    @Override
    public List<Map<String, Object>> listUdpCodecs() {
        Path dir = resolveUdpCodecDir(true);
        if (dir == null || !Files.isDirectory(dir)) {
            return Collections.emptyList();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(dir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> {
                        String n = p.getFileName().toString().toLowerCase(Locale.ROOT);
                        return n.endsWith(".js") || n.endsWith(".json");
                    })
                    .sorted()
                    .forEach(p -> {
                        Map<String, Object> item = new LinkedHashMap<>();
                        String fileName = p.getFileName().toString();
                        item.put("name", stripExt(fileName));
                        item.put("fileName", fileName);
                        item.put("path", p.toAbsolutePath().toString().replace('\\', '/'));
                        try {
                            item.put("size", Files.size(p));
                            item.put("mtime", Files.getLastModifiedTime(p).toMillis());
                        } catch (Exception e) {
                            item.put("size", 0);
                            item.put("mtime", 0);
                        }
                        item.put(
                                "kind",
                                fileName.toLowerCase(Locale.ROOT).endsWith(".js")
                                        ? "js"
                                        : "offset_table");
                        result.add(item);
                    });
        } catch (Exception e) {
            log.warn("List UDP codec dir failed: {}", dir, e);
        }
        return result;
    }

    @Override
    public Map<String, Object> uploadUdpCodec(MultipartFile file, String name) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("upload file is empty");
        }
        String original = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lower = original.toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".js") && !lower.endsWith(".json")) {
            throw new IllegalArgumentException("only .js or .json UDF files are allowed");
        }
        String ext = lower.endsWith(".js") ? ".js" : ".json";
        String base =
                StringUtils.isNotBlank(name)
                        ? sanitizeBaseName(name)
                        : sanitizeBaseName(stripExt(original));
        if (StringUtils.isBlank(base)) {
            throw new IllegalArgumentException("invalid udf name");
        }
        Path dir = resolveUdpCodecDir(true);
        if (dir == null) {
            throw new IllegalStateException(
                    "SEATUNNEL_HOME is not set; cannot save UDF under udf/udp-codec");
        }
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(base + ext).normalize();
            if (!target.startsWith(dir.normalize())) {
                throw new IllegalArgumentException("invalid target path");
            }
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
            Map<String, Object> saved = new LinkedHashMap<>();
            saved.put("name", base);
            saved.put("fileName", target.getFileName().toString());
            saved.put("path", target.toAbsolutePath().toString().replace('\\', '/'));
            saved.put("size", Files.size(target));
            saved.put("kind", ".js".equals(ext) ? "js" : "offset_table");
            log.info("Uploaded UDP UDF codec: {}", target);
            return saved;
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("upload UDP UDF failed: " + e.getMessage(), e);
        }
    }

    private static Path resolveUdpCodecDir(boolean allowMissingHome) {
        String home = System.getenv("SEATUNNEL_HOME");
        if (StringUtils.isBlank(home)) {
            home = System.getProperty("SEATUNNEL_HOME");
        }
        if (StringUtils.isBlank(home)) {
            home = System.getProperty("seatunnel.home");
        }
        if (StringUtils.isBlank(home)) {
            if (allowMissingHome) {
                return null;
            }
            throw new IllegalStateException("SEATUNNEL_HOME is not set");
        }
        // Windows Git-bash style /d/HPG/... → D:/HPG/...
        String normalized = home.trim().replace('\\', '/');
        if (normalized.matches("^/[a-zA-Z]/.*")) {
            normalized = normalized.charAt(1) + ":" + normalized.substring(2);
        }
        return Paths.get(normalized, "udf", "udp-codec");
    }

    private static String stripExt(String fileName) {
        int i = fileName.lastIndexOf('.');
        return i > 0 ? fileName.substring(0, i) : fileName;
    }

    private static String sanitizeBaseName(String name) {
        String n = name == null ? "" : name.trim();
        n = stripExt(n);
        return n.replaceAll("[^A-Za-z0-9_\\-]", "_");
    }
}
