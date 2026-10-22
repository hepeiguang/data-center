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

package org.apache.seatunnel.transform.encrypt;

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

public class EncryptTransform extends MultipleFieldOutputTransform {
    private static final Logger log = LoggerFactory.getLogger(EncryptTransform.class);
    public static String PLUGIN_NAME = "Encrypt";
    private final EncryptTransformConfig encryptTransformConfig;
    private final int[] encryptFieldIndex;

    public EncryptTransform(
            @NonNull EncryptTransformConfig encryptTransformConfig,
            @NonNull CatalogTable catalogTable, int[] switchFieldIndex) {
        super(catalogTable);
        this.encryptTransformConfig = encryptTransformConfig;
        this.encryptFieldIndex = switchFieldIndex;
        SeaTunnelRowType seaTunnelRowType = catalogTable.getTableSchema().toPhysicalRowDataType();
        try {
            List<EncryptMapping> customList = encryptTransformConfig.getCustomList();
            for (int i = 0; i < customList.size(); i++) {
                this.encryptFieldIndex[i] = seaTunnelRowType.indexOf(customList.get(i).getEncryptField());
            }
        } catch (IllegalArgumentException e) {
            throw TransformCommonError.cannotFindInputFieldError(
                    getPluginName(), encryptTransformConfig.getCustomList().get(0).getEncryptField());
        }
        this.outputCatalogTable = getProducedCatalogTable();
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    protected Object[] getOutputFieldValues(SeaTunnelRowAccessor inputRow) {
        Object[] encryptValues = new Object[encryptFieldIndex.length];
        try {
            for (int i = 0; i < encryptFieldIndex.length; i++) {
                Object originalValue = inputRow.getField(encryptFieldIndex[i]);
                String encryptType = encryptTransformConfig.getCustomList().get(i).getEncryptType();
                String secretKey = encryptTransformConfig.getCustomList().get(i).getSecretKey();
                if (originalValue == null) {
                    encryptValues[i] = null;
                    continue;
                }
                encryptValues[i] = DataSecurityUtils.encrypt(encryptType, originalValue.toString(), secretKey);
            }
        } catch (Exception e) {
            log.error("-------------------------------------加密处理失败: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        return encryptValues;
    }

    @Override
    protected Column[] getOutputColumns() {
        String[] outputFields =
                encryptTransformConfig.getCustomList().stream()
                        .map(EncryptMapping::getEncryptField)
                        .toArray(String[]::new);
        return Arrays.stream(outputFields)
                .map(
                        fieldName ->
                                PhysicalColumn.of(
                                        fieldName, BasicType.STRING_TYPE, 2000, true, "", ""))
                .toArray(Column[]::new);
    }
}
