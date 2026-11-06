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

package org.apache.dolphinscheduler.plugin.descriptor.controller;

import org.apache.dolphinscheduler.plugin.descriptor.model.PluginDescriptorDetail;
import org.apache.dolphinscheduler.plugin.descriptor.model.PluginDescriptorSummary;
import org.apache.dolphinscheduler.plugin.descriptor.service.PluginDescriptorService;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST API for plugin descriptors.
 *
 * <p>Endpoints:
 * <ul>
 *   <li>GET /task/plugin-descriptors - list all descriptors</li>
 *   <li>GET /task/plugin-descriptors/{type} - get descriptor detail</li>
 *   <li>GET /task/plugin-descriptors/{type}/icon - get icon bytes</li>
 *   <li>POST /task/plugin-descriptors/rescan - force rescan</li>
 * </ul>
 */
@RestController
@RequestMapping("/task/plugin-descriptors")
public class PluginDescriptorController {

    @Autowired
    private PluginDescriptorService service;

    /**
     * List all registered plugin descriptors.
     */
    @GetMapping
    public ResponseEntity<List<PluginDescriptorSummary>> listDescriptors() {
        List<PluginDescriptorSummary> summaries = service.listDescriptors();
        return ResponseEntity.ok(summaries);
    }

    /**
     * Get detailed descriptor for a specific task type.
     */
    @GetMapping("/{type}")
    public ResponseEntity<PluginDescriptorDetail> getDescriptor(@PathVariable("type") String type) {
        PluginDescriptorDetail detail = service.getDescriptor(type);
        return ResponseEntity.ok(detail);
    }

    /**
     * Get icon bytes for a specific task type.
     */
    @GetMapping(value = "/{type}/icon", produces = {MediaType.IMAGE_PNG_VALUE, MediaType.IMAGE_JPEG_VALUE})
    public ResponseEntity<byte[]> getIcon(@PathVariable("type") String type) {
        byte[] iconBytes = service.getIcon(type);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(detectIconContentType(iconBytes));
        headers.setContentLength(iconBytes.length);
        headers.setCacheControl("public, max-age=3600");

        return new ResponseEntity<>(iconBytes, headers, HttpStatus.OK);
    }

    /**
     * Force a rescan of the plugin directory.
     */
    @PostMapping("/rescan")
    public ResponseEntity<Void> rescan() {
        service.rescan();
        return ResponseEntity.ok().build();
    }

    private MediaType detectIconContentType(byte[] iconBytes) {
        if (iconBytes == null || iconBytes.length < 8) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        // PNG magic bytes: 89 50 4E 47
        if (iconBytes[0] == (byte) 0x89 && iconBytes[1] == (byte) 0x50
                && iconBytes[2] == (byte) 0x4E && iconBytes[3] == (byte) 0x47) {
            return MediaType.IMAGE_PNG;
        }

        // JPEG magic bytes: FF D8 FF
        if (iconBytes[0] == (byte) 0xFF && iconBytes[1] == (byte) 0xD8 && iconBytes[2] == (byte) 0xFF) {
            return MediaType.IMAGE_JPEG;
        }

        // GIF magic bytes: 47 49 46 38
        if (iconBytes[0] == (byte) 0x47 && iconBytes[1] == (byte) 0x49
                && iconBytes[2] == (byte) 0x46 && iconBytes[3] == (byte) 0x38) {
            return MediaType.IMAGE_GIF;
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
