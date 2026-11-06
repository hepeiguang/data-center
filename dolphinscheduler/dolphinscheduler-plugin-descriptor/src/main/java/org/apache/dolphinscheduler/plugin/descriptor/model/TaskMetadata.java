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
 * Task metadata section of the plugin descriptor.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskMetadata {

    /**
     * Task type identifier. Must match TaskChannelFactory.getName().
     */
    private String type;

    /**
     * Task category for sidebar grouping.
     * Valid values: universal, cloud, logic, dataIntegration, dataComputation, machineLearning, other
     */
    private String category;

    /**
     * Display name for the task type.
     */
    private String name;

    /**
     * Sort order within the category.
     */
    private Integer order;

    /**
     * Supported execution types.
     */
    private List<String> executeType;

    /**
     * Help documentation link.
     */
    private String helpLink;

    /**
     * Descriptor schema version for this task type.
     */
    private Integer descriptorVersion;
}
