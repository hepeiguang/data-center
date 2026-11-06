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

package org.apache.dolphinscheduler.plugin.descriptor.model;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Detailed descriptor response including validation results.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDescriptorDetail {

    /**
     * The full plugin descriptor.
     */
    private PluginDescriptor descriptor;

    /**
     * Current status.
     */
    private PluginDescriptor.DescriptorStatus descriptorStatus;

    /**
     * Source JAR file name.
     */
    private String jarFileName;

    /**
     * SHA-256 hash of the source JAR.
     */
    private String jarSha256;

    /**
     * Validation errors (empty if status is VALID).
     */
    private List<ValidationError> validationErrors;

    /**
     * A single validation error.
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ValidationError {
        private String field;
        private String message;
        private Integer line;
    }
}
