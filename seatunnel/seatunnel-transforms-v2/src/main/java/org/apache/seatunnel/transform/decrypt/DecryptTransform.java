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

package org.apache.seatunnel.transform.decrypt;

import cn.datax.common.data.security.DataSecurityUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DecryptTransform extends MultipleFieldOutputTransform {
    private static final Logger log = LoggerFactory.getLogger(DecryptTransform.class);
    public static String PLUGIN_NAME = "Decrypt";
    private final DecryptTransformConfig decryptTransformConfig;
    private final int[] decryptFieldIndex;

    public DecryptTransform(
            @NonNull DecryptTransformConfig decryptTransformConfig,
            @NonNull CatalogTable catalogTable, int[] decryptFieldIndex) {
        super(catalogTable);
        this.decryptTransformConfig = decryptTransformConfig;
        this.decryptFieldIndex = decryptFieldIndex;
        SeaTunnelRowType seaTunnelRowType = catalogTable.getTableSchema().toPhysicalRowDataType();
        try {
            List<DecryptMapping> customList = decryptTransformConfig.getCustomList();
            for (int i = 0; i < customList.size(); i++) {
                this.decryptFieldIndex[i] = seaTunnelRowType.indexOf(customList.get(i).getDecryptField());
            }
        } catch (IllegalArgumentException e) {
            throw TransformCommonError.cannotFindInputFieldError(
                    getPluginName(), decryptTransformConfig.getCustomList().get(0).getDecryptField());
        }
        this.outputCatalogTable = getProducedCatalogTable();
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    protected Object[] getOutputFieldValues(SeaTunnelRowAccessor inputRow) {
        Object[] encryptValues = new Object[decryptFieldIndex.length];
        for (int i = 0; i < decryptFieldIndex.length; i++) {
            Object originalValue = inputRow.getField(decryptFieldIndex[i]);
            try {
                if (originalValue == null) {
                    encryptValues[i] = null;
                    continue;
                }
                encryptValues[i] = DataSecurityUtils.decrypt(
                        decryptTransformConfig.getCustomList().get(i).getDecryptType(),
                        originalValue.toString(),
                        decryptTransformConfig.getCustomList().get(i).getSecretKey()
                );
            } catch (Exception e) {
                log.error("--------------------encrypt error: {}", e.getMessage());
            }
        }

        return encryptValues;
    }

    @Override
    protected Column[] getOutputColumns() {
        String[] outputFields =
                decryptTransformConfig.getCustomList().stream()
                        .map(DecryptMapping::getDecryptField)
                        .toArray(String[]::new);
        return Arrays.stream(outputFields)
                .map(
                        fieldName ->
                                PhysicalColumn.of(
                                        fieldName, BasicType.STRING_TYPE, 2000, true, "", ""))
                .toArray(Column[]::new);
    }
}
