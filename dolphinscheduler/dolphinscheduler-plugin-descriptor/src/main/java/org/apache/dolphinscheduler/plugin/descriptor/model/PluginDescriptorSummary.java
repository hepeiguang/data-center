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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Summary of a plugin descriptor for list API responses.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginDescriptorSummary {

    /**
     * Task type identifier.
     */
    private String type;

    /**
     * Display name.
     */
    private String name;

    /**
     * Task category.
     */
    private String category;

    /**
     * Descriptor status.
     */
    private PluginDescriptor.DescriptorStatus descriptorStatus;

    /**
     * URL to fetch the icon.
     */
    private String iconUrl;

    /**
     * URL to fetch the full descriptor.
     */
    private String descriptorUrl;

    /**
     * Error message if status is INVALID.
     */
    private String errorMessage;
}
