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
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Root descriptor for a task plugin, parsed from META-INF/ds-plugin.yaml.
 * Contract version: ds.plugin/v1
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDescriptor {

    /**
     * Contract version, must be "ds.plugin/v1".
     */
    private String apiVersion;

    /**
     * Task metadata section.
     */
    private TaskMetadata task;

    /**
     * Icon reference within the JAR.
     */
    private IconRef icon;

    /**
     * Internationalization strings.
     */
    private Map<String, Map<String, String>> locales;

    /**
     * Parameter DTO binding and validation mode.
     */
    private ParamsBinding params;

    /**
     * Form field descriptors.
     */
    private List<FormField> form;

    /**
     * Descriptor status as seen by the api-server.
     */
    public enum DescriptorStatus {
        /** Descriptor loaded and validated successfully. */
        VALID,
        /** Descriptor YAML is missing from the JAR. */
        MISSING,
        /** Descriptor YAML failed schema or consistency validation. */
        INVALID,
        /** JAR uploaded but api-server has not rescanned yet. */
        PENDING_ACTIVATION,
        /** Descriptor active and executable (after restart). */
        ACTIVE
    }
}
