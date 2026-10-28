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

package org.apache.dolphinscheduler.dataapi.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.sql.DataSource;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.ssssssss.magicapi.core.service.MagicResourceService;

/**
 * Cluster helpers for the database-backed magic resource store:
 * one-shot file→DB import and periodic refresh so instances converge without the cluster plugin.
 */
@Slf4j
@Component
public class MagicResourceClusterSupport implements ApplicationRunner {

    private final DataSource metaDataSource;
    private final MagicResourceService magicResourceService;
    private final DataApiProperties properties;
    private final String fileLocation;
    private final String prefix;

    public MagicResourceClusterSupport(DataSource metaDataSource,
                                       MagicResourceService magicResourceService,
                                       DataApiProperties properties,
                                       @Value("${magic-api.resource.location:./data/magic-api}") String fileLocation,
                                       @Value("${magic-api.resource.prefix:magic-api}") String prefix) {
        this.metaDataSource = metaDataSource;
        this.magicResourceService = magicResourceService;
        this.properties = properties;
        this.fileLocation = fileLocation;
        this.prefix = prefix;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.isMagicResourceMigrate()) {
            migrate();
        }
    }

    @Scheduled(initialDelay = 30000L, fixedDelayString = "${data-api.magic-resource-refresh-ms:3600000}")
    public void scheduledRefresh() {
        if (properties.getMagicResourceRefreshMs() > 0) {
            magicResourceService.refresh();
            log.debug("magic resources refreshed from shared store");
        }
    }

    private synchronized void migrate() {
        try {
            Path root = Paths.get(fileLocation).toAbsolutePath().normalize();
            if (!Files.isDirectory(root)) {
                log.warn("magic resource file location not found: {}", root);
                return;
            }
            List<Path> files;
            try (Stream<Path> stream = Files.walk(root)) {
                files = stream.filter(Files::isRegularFile).collect(Collectors.toList());
            }
            // magic-api itself seeds directory markers (magic-api/api/ ...), so merge by path
            // instead of treating a non-empty table as "already migrated".
            Set<String> existing = existingPaths();
            String dbPrefix = prefix.replaceAll("^/+|/+$", "");
            String sql = "INSERT INTO magic_api_file(file_path, file_content) VALUES (?,?)";
            int inserted = 0;
            try (Connection conn = metaDataSource.getConnection();
                    PreparedStatement ps = conn.prepareStatement(sql)) {
                // DatabaseResource only enumerates explicit directory keys ("path/"), so import
                // every ancestor directory as magic-api itself does in mkdir().
                Set<String> directories = new LinkedHashSet<>();
                for (Path file : files) {
                    Path parent = root.relativize(file).getParent();
                    while (parent != null) {
                        directories.add(dbPrefix + "/" + parent.toString().replace('\\', '/') + "/");
                        parent = parent.getParent();
                    }
                }
                for (String directory : directories) {
                    if (existing.contains(directory)) {
                        continue;
                    }
                    ps.setString(1, directory);
                    ps.setString(2, "this is directory");
                    ps.addBatch();
                    inserted++;
                }
                for (Path file : files) {
                    String relative = root.relativize(file).toString().replace('\\', '/');
                    String key = dbPrefix + "/" + relative;
                    if (existing.contains(key)) {
                        continue;
                    }
                    String content = new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
                    // group.json edited on Windows often carries a BOM; the DB resource does not strip it
                    if (content.startsWith("\uFEFF")) {
                        content = content.substring(1);
                    }
                    ps.setString(1, key);
                    ps.setString(2, content);
                    ps.addBatch();
                    inserted++;
                }
                if (inserted > 0) {
                    ps.executeBatch();
                }
            }
            if (inserted > 0) {
                magicResourceService.refresh();
                log.info("migrated {} magic resource files into magic_api_file (prefix={})", inserted, dbPrefix);
            } else {
                log.info("magic_api_file already contains all files under {}", root);
            }
        } catch (IOException | SQLException e) {
            log.error("magic resource migration failed", e);
        }
    }

    private Set<String> existingPaths() throws SQLException {
        Set<String> paths = new HashSet<>();
        try (Connection conn = metaDataSource.getConnection();
                PreparedStatement ps = conn.prepareStatement("SELECT file_path FROM magic_api_file");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                paths.add(rs.getString(1));
            }
        }
        return paths;
    }
}
