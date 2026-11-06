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

package org.apache.dolphinscheduler.plugin.task.openmetadata;

import org.apache.dolphinscheduler.plugin.task.api.parameters.AbstractParameters;

import org.apache.commons.lang3.StringUtils;

import lombok.Data;
import lombok.EqualsAndHashCode;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Parameters for a lineage edge reported from a DolphinScheduler workflow to OpenMetadata.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OpenMetadataTaskParameters extends AbstractParameters {

    @JsonProperty("openMetadataUrl")
    private String openMetadataUrl = "http://127.0.0.1:8585";

    /**
     * Bot/admin JWT. If blank, the task falls back to the OM_TOKEN environment variable.
     */
    private String token;

    private String fromFqn;

    private String toFqn;

    private String fromType = "table";

    private String toType = "table";

    /**
     * Optional SQL placed on the lineage edge. For SeaTunnel this can be the SQL transform query.
     */
    private String sql;

    /**
     * Optional SeaTunnel job config; when sql is blank the first query/sql literal is extracted.
     */
    private String seatunnelConfig;

    /**
     * Explicit column lineage. When blank, it is derived from {@link #sql} when possible.
     */
    private java.util.List<ColumnLineageParam> columnsLineage;

    /**
     * Disable SQL column-lineage parsing when false.
     */
    private boolean parseSql = true;

    private String description;

    private int connectTimeout = 10000;

    private int readTimeout = 30000;

    @Override
    public boolean checkParameters() {
        return StringUtils.isNotBlank(openMetadataUrl)
                && StringUtils.isNotBlank(fromFqn)
                && StringUtils.isNotBlank(toFqn);
    }
}
