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

package org.apache.seatunnel.transform.desensitize;

import cn.datax.common.data.security.DataSecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.catalog.Column;
import org.apache.seatunnel.api.table.catalog.PhysicalColumn;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelRowAccessor;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.transform.common.MultipleFieldOutputTransform;
import org.apache.seatunnel.transform.exception.TransformCommonError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public class DesensitizeTransform extends MultipleFieldOutputTransform {
    private static final Logger log = LoggerFactory.getLogger(DesensitizeTransform.class);
    public static String PLUGIN_NAME = "Desensitize";
    private final DesensitizeTransformConfig desensitizeTransformConfig;
    private final int[] desensitizeFieldIndex;

    public DesensitizeTransform(
            @NonNull DesensitizeTransformConfig desensitizeTransformConfig,
            @NonNull CatalogTable catalogTable, int[] desensitizeFieldIndex) {
        super(catalogTable);
        this.desensitizeTransformConfig = desensitizeTransformConfig;
        this.desensitizeFieldIndex = desensitizeFieldIndex;
        SeaTunnelRowType seaTunnelRowType = catalogTable.getTableSchema().toPhysicalRowDataType();
        try {
            List<DesensitizeMapping> customList = desensitizeTransformConfig.getCustomList();
            for (int i = 0; i < customList.size(); i++) {
                this.desensitizeFieldIndex[i] = seaTunnelRowType.indexOf(customList.get(i).getDesensitizeField());
            }
        } catch (IllegalArgumentException e) {
            throw TransformCommonError.cannotFindInputFieldError(
                    getPluginName(), desensitizeTransformConfig.getCustomList().get(0).getDesensitizeField());
        }
        this.outputCatalogTable = getProducedCatalogTable();
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    protected Object[] getOutputFieldValues(SeaTunnelRowAccessor inputRow) {
        Object[] desensitizedValues = new Object[desensitizeFieldIndex.length];

        for (int i = 0; i < desensitizeFieldIndex.length; i++) {
            Object originalValue = inputRow.getField(desensitizeFieldIndex[i]);
            try {
                if (originalValue == null) {
                    desensitizedValues[i] = null;
                    continue;
                }
                desensitizedValues[i] = DataSecurityUtils.encrypt(desensitizeTransformConfig.getCustomList().get(i).getDesensitizeType(), originalValue.toString());
            } catch (Exception e) {
                log.error("------------------------------Desensitize error: {}", e.getMessage());
            }
        }

        return desensitizedValues;
    }

    @Override
    protected Column[] getOutputColumns() {
        String[] outputFields =
                desensitizeTransformConfig.getCustomList().stream()
                        .map(DesensitizeMapping::getDesensitizeField)
                        .toArray(String[]::new);
        return Arrays.stream(outputFields)
                .map(
                        fieldName ->
                                PhysicalColumn.of(
                                        fieldName, BasicType.STRING_TYPE, 2000, true, "", ""))
                .toArray(Column[]::new);
    }
}
