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

package org.apache.seatunnel.transform.dynamicdict;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.seatunnel.api.table.catalog.*;
import org.apache.seatunnel.api.table.type.*;
import org.apache.seatunnel.common.utils.DateTimeUtils;
import org.apache.seatunnel.transform.common.AbstractCatalogSupportFlatMapTransform;
import org.apache.seatunnel.transform.common.MultipleFieldOutputTransform;
import org.apache.seatunnel.transform.exception.TransformCommonError;

import static org.apache.seatunnel.transform.dynamicdict.DictType.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class DictTransform extends MultipleFieldOutputTransform {

    public static String PLUGIN_NAME = "Dict";
    private final DictTransformConfig dictTransformConfig;
    private int[] dictFieldIndex;
    List<DictFieldMapping> dictFieldMappings;
    SeaTunnelRowType seaTunnelRowType;
    private static final HttpClient HTTP_CLIENT = HttpClientBuilder.create().build();
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    List<DictInfoCfg> dictInfoCfgList = new ArrayList<>();

    public DictTransform(
            @NonNull DictTransformConfig dictTransformConfig,
            @NonNull CatalogTable catalogTable, int[] dictFieldIndex) {
        super(catalogTable);
        this.dictTransformConfig = dictTransformConfig;
        seaTunnelRowType = catalogTable.getTableSchema().toPhysicalRowDataType();
        this.dictFieldIndex = dictFieldIndex;
        try {
            dictFieldMappings = dictTransformConfig.getSourceFields();
            for (int i = 0; i < dictFieldMappings.size(); i++) {
                this.dictFieldIndex[i] = seaTunnelRowType.indexOf(dictFieldMappings.get(i).getSource_field());
            }
        } catch (IllegalArgumentException e) {
            throw TransformCommonError.cannotFindInputFieldError(
                    getPluginName(), dictTransformConfig.getSourceFields().get(0).getSource_field());
        }
        this.outputCatalogTable = getProducedCatalogTable();
        // 获取字典集合
        HttpGet get = new HttpGet(dictTransformConfig.getApiUrl());

        try {
            ObjectNode rootNode = null;
            HttpResponse result = HTTP_CLIENT.execute(get);
            String resultValue = EntityUtils.toString(result.getEntity(), "UTF-8");
//        String resultValue = "{\n" +
//                "    \"success\": true,\n" +
//                "    \"code\": 200,\n" +
//                "    \"msg\": \"操作成功\",\n" +
//                "    \"data\": {\n" +
//                "        \"records\": [\n" +
//                "            {\n" +
//                "                \"id\": \"1931974060425326594\",\n" +
//                "                \"createTime\": \"2025-06-09 15:18:05\",\n" +
//                "                \"createBy\": \"1214835832967581698\",\n" +
//                "                \"updateTime\": \"2025-06-09 15:18:05\",\n" +
//                "                \"updateBy\": \"1214835832967581698\",\n" +
//                "                \"status\": \"1\",\n" +
//                "                \"remark\": null,\n" +
//                "                \"itemType\": \"0\",\n" +
//                "                \"itemLevel\": \"3\",\n" +
//                "                \"itemCode\": \"1\",\n" +
//                "                \"itemCnName\": \"1號用戶\",\n" +
//                "                \"itemEnName\": \"userType\",\n" +
//                "                \"refTopicId\": \"1931973065272176641\",\n" +
//                "                \"relativeStandard\": null,\n" +
//                "                \"businessMeaning\": \"userType\",\n" +
//                "                \"createDept\": \"1197789917762031617\",\n" +
//                "                \"delFlag\": \"0\",\n" +
//                "                \"topicName\": \"至於標準\",\n" +
//                "                \"active\": true\n" +
//                "            }\n" +
//                "        ],\n" +
//                "        \"total\": \"1\",\n" +
//                "        \"size\": \"20\",\n" +
//                "        \"current\": \"1\",\n" +
//                "        \"orders\": [\n" +
//                "            {\n" +
//                "                \"column\": \"id\",\n" +
//                "                \"asc\": false\n" +
//                "            }\n" +
//                "        ],\n" +
//                "        \"optimizeCountSql\": true,\n" +
//                "        \"searchCount\": true,\n" +
//                "        \"optimizeJoinOfCountSql\": true,\n" +
//                "        \"countId\": null,\n" +
//                "        \"maxLimit\": null,\n" +
//                "        \"pages\": \"1\"\n" +
//                "    },\n" +
//                "    \"timestamp\": \"1749453531054\"\n" +
//                "}";

            try {
                rootNode = (ObjectNode) OBJECT_MAPPER.readTree(resultValue);
            } catch (Exception e) {
                log.error(" get dict data error", e);
            }
            if (rootNode != null && rootNode.get("data") != null) {
                ArrayNode recordNode = (ArrayNode) rootNode.get("data").get("records");
                if (recordNode != null) {
                    recordNode.forEach(node -> {
                        DictInfoCfg item = new DictInfoCfg();
                        item.setId(node.get("id").asText());
                        item.setItemCnName(node.get("itemCnName").asText());
                        item.setItemEnName(node.get("itemEnName").asText());
                        item.setTopicName(node.get("topicName").asText());
                        item.setRefTopicId(node.get("refTopicId").asText());
                        dictInfoCfgList.add(item);


                    });
                }

            }
        } catch (IOException e) {
            log.error("获取字典失败");
            throw new RuntimeException(e);
        }

    }

    @Override
    public String getPluginName() {
        return "Dict";
    }

    @Override
    protected Object[] getOutputFieldValues(SeaTunnelRowAccessor inputRow) {
        log.info("========================= dict SeaTunnelRowAccessor inputRow=======================:{}", inputRow);

        Object[] dictFieldValues = new Object[dictFieldIndex.length];

        dictFieldMappings = dictTransformConfig.getSourceFields();
        for (int i = 0; i < dictFieldMappings.size(); i++) {
            try {
                dictFieldIndex[i] = seaTunnelRowType.indexOf(dictFieldMappings.get(i).getSource_field());
                Object dictFieldValue = inputRow.getField(dictFieldIndex[i]);
                if (dictFieldValue == null) continue;
                switch (DictType.fromName(dictFieldMappings.get(i).getDict_type())) {
                    case NAMESPACE:

                        dictFieldValues[i] = StringUtils.isNoneBlank(getDictionaryValue(dictFieldMappings.get(i).getDict_code(), dictFieldValue.toString()))
                                ? getDictionaryValue(dictFieldMappings.get(i).getDict_code(), dictFieldValue.toString()) : dictFieldValue.toString();
                        ;
                        break;
                    case RANGE:
                        dictFieldValues[i] = StringUtils.isNoneBlank(dictFieldValue.toString()) ?
                                clamp(Double.parseDouble(dictFieldValue.toString())
                                        , Double.parseDouble(dictFieldMappings.get(i).getMin_num())
                                        , Double.parseDouble(dictFieldMappings.get(i).getMax_num())) : dictFieldValue.toString();
                        break;
                    case TIMESTAMP:
                        log.info("============================TIMESTAMP dictFieldValue old=======:{}", dictFieldValue);

                        try {
                            LocalDateTime dateTime = LocalDateTime.parse(dictFieldValue.toString());
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(dictFieldMappings.get(i).getTime_type());
                            dictFieldValues[i] = dateTime.format(dateTimeFormatter);
                        } catch (DateTimeParseException e) {
                            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                            LocalDateTime dateTime = LocalDateTime.parse(dictFieldValue.toString(), dateTimeFormatter);
                            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(dictFieldMappings.get(i).getTime_type());
                            dictFieldValues[i] = dateTime.format(formatter);
                        }
                        break;
                    case NUMACCURACY:

                        dictFieldValues[i] = StringUtils.isNoneBlank(dictFieldValue.toString())
                                ? new BigDecimal(dictFieldValue.toString()).setScale(dictFieldMappings.get(i).getNum_accuracy()
                                , RoundingMode.HALF_UP).toString() : dictFieldValue.toString();


                    default:
                        break;

                }
            } catch (Exception e) {
                log.error("dict convert error", e);
            }
        }
        return dictFieldValues;
    }


    @Override
    protected Column[] getOutputColumns() {

        String[] outputFields =
                dictTransformConfig.getSourceFields().stream()
                        .map(DictFieldMapping::getSource_field)
                        .toArray(String[]::new);
        return Arrays.stream(outputFields)
                .map(
                        fieldName ->
                                PhysicalColumn.of(
                                        fieldName, BasicType.STRING_TYPE, 200, true, "", ""))
                .toArray(Column[]::new);

    }


//    @Override
//    protected List<SeaTunnelRow> transformRow(SeaTunnelRow inputRow) {
//        log.info("========================= dict SeaTunnelRowAccessor inputRow=======================:{}", inputRow);
//        List<SeaTunnelRow> result = new ArrayList<>();
//        Object[] dictFieldValues = new Object[dictFieldIndex.length];
//        dictFieldMappings = dictTransformConfig.getSourceFields();
//        for (int i = 0; i < dictFieldMappings.size(); i++) {
//            try {
//                dictFieldIndex[i] = seaTunnelRowType.indexOf(dictFieldMappings.get(i).getSource_field());
//                Object dictFieldValue = inputRow.getField(dictFieldIndex[i]);
//                if (dictFieldValue == null) continue;
//                switch (DictType.fromName(dictFieldMappings.get(i).getDict_type())) {
//                    case NAMESPACE:
//
//                        dictFieldValues[i] = StringUtils.isNoneBlank(getDictionaryValue(dictFieldMappings.get(i).getDict_code(), dictFieldValue.toString()))
//                                ? getDictionaryValue(dictFieldMappings.get(i).getDict_code(), dictFieldValue.toString()) : dictFieldValue.toString();
//                        ;
//                        break;
//                    case RANGE:
//                        dictFieldValues[i] = StringUtils.isNoneBlank(dictFieldValue.toString()) ?
//                                clamp(Double.parseDouble(dictFieldValue.toString())
//                                        , Double.parseDouble(dictFieldMappings.get(i).getMin_num())
//                                        , Double.parseDouble(dictFieldMappings.get(i).getMax_num())) : dictFieldValue.toString();
//                        break;
//                    case TIMESTAMP:
//                        log.info("============================TIMESTAMP dictFieldValue old=======:{}", dictFieldValue.toString());
//
////                        Locale locale = Locale.getDefault();
////                        // 利用 SimpleDateFormat 进行时间转换
////                        SimpleDateFormat sdf = new SimpleDateFormat(dictFieldMappings.get(i).getTime_type(), locale);
////                        Date parse = sdf.parse(dictFieldValue.toString());
////                        dictFieldValues[i] = sdf.format(sdf.parse(dictFieldValue.toString()));
////                        log.info("===========================TIMESTAMP dictFieldValue new=========:{}"
////                                , DateTimeUtils.Formatter.parse(sdf.format(sdf.parse(dictFieldValue.toString()))));
//                        try{   LocalDateTime dateTime = LocalDateTime.parse(dictFieldValue.toString());
//                            DateTimeFormatter dateTimeFormatter =DateTimeFormatter.ofPattern(dictFieldMappings.get(i).getTime_type());
//                            dictFieldValues[i] =dateTime.format(dateTimeFormatter);
//                        }catch (DateTimeParseException e){
//                            DateTimeFormatter dateTimeFormatter =DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//                            LocalDateTime dateTime= LocalDateTime.parse(dictFieldValue.toString(), dateTimeFormatter);
//                            DateTimeFormatter formatter =DateTimeFormatter.ofPattern(dictFieldMappings.get(i).getTime_type());
//                            dictFieldValues[i] =dateTime.format(formatter);
//                        }
//
//                        break;
//                    case NUMACCURACY:
//
//                        dictFieldValues[i] = StringUtils.isNoneBlank(dictFieldValue.toString())
//                                ? new BigDecimal(dictFieldValue.toString()).setScale(dictFieldMappings.get(i).getNum_accuracy()
//                                , RoundingMode.HALF_UP).toString() : dictFieldValue.toString();
//
//
//                    default:
//                        break;
//
//                }
//            } catch (Exception e) {
//                log.error("dict convert error", e);
//            }
//        }
//        result.add(new SeaTunnelRow(dictFieldValues));
//        return result;
//    }
//
//    @Override
//    protected TableSchema transformTableSchema() {
//        String[] outputFields =
//                dictTransformConfig.getSourceFields().stream()
//                        .map(DictFieldMapping::getSource_field)
//                        .toArray(String[]::new);
//        SeaTunnelDataType<?>[] fieldTypes = seaTunnelRowType.getFieldTypes();
//        for (int i = 0; i < fieldTypes.length; i++) {
//            if (SqlType.TIMESTAMP.equals(fieldTypes[i].getSqlType())) {
//                fieldTypes[i] = BasicType.STRING_TYPE;
//            }
//        }
//        String[] fileNames=seaTunnelRowType.getFieldNames();
//        List<String> outputColumns = Arrays.asList(fileNames);
//        List<String> outputFieldColumns = Arrays.asList(outputFields);
//
//        TableSchema.Builder builder = TableSchema.builder();
//        if (inputCatalogTable.getTableSchema().getPrimaryKey() != null
//                && outputColumns.containsAll(
//                inputCatalogTable.getTableSchema().getPrimaryKey().getColumnNames())) {
//            builder.primaryKey(inputCatalogTable.getTableSchema().getPrimaryKey().copy());
//        }
//
//        List<ConstraintKey> outputConstraintKeys =
//                inputCatalogTable.getTableSchema().getConstraintKeys().stream()
//                        .filter(
//                                key -> {
//                                    List<String> constraintColumnNames =
//                                            key.getColumnNames().stream()
//                                                    .map(
//                                                            ConstraintKey.ConstraintKeyColumn
//                                                                    ::getColumnName)
//                                                    .collect(Collectors.toList());
//                                    return outputColumns.containsAll(constraintColumnNames);
//                                })
//                        .map(ConstraintKey::copy)
//                        .collect(Collectors.toList());
//
//        builder.constraintKey(outputConstraintKeys);
//
//        List<Column> columns = new ArrayList<>(fileNames.length);
//        for (int i = 0; i < fileNames.length; i++) {
//            Column simpleColumn = null;
//            String inputColumnName =fileNames[i];
//            if (inputColumnName != null&& !outputFieldColumns.contains(inputColumnName)) {
//                for (Column inputColumn : inputCatalogTable.getTableSchema().getColumns()) {
//                    if (inputColumnName.equals(inputColumn.getName())) {
//                        simpleColumn = inputColumn;
//                        break;
//                    }
//                }
//            }
//            Column column;
//            if (simpleColumn != null) {
//                column =
//                        new PhysicalColumn(
//                                fileNames[i],
//                                fieldTypes[i],
//                                simpleColumn.getColumnLength(),
//                                simpleColumn.getScale(),
//                                simpleColumn.isNullable(),
//                                simpleColumn.getDefaultValue(),
//                                simpleColumn.getComment(),
//                                simpleColumn.getSourceType(),
//                                simpleColumn.getOptions());
//            } else {
//                column = PhysicalColumn.of(fileNames[i], BasicType.STRING_TYPE, 200, true, null, null);
//            }
//            columns.add(column);
//        }
//        return builder.columns(columns).build();
//    }
//
//    @Override
//    protected TableIdentifier transformTableIdentifier() {
//        return inputCatalogTable.getTableId().copy();
//    }


    private String getDictionaryValue(String id, String value) {
        String mappingValue = "";
        for (DictInfoCfg dictInfoCfg : dictInfoCfgList) {
            if (dictInfoCfg.getRefTopicId().equals(id) && dictInfoCfg.getItemEnName().equals(value)) {
                return dictInfoCfg.getItemCnName();
            }
        }
        return mappingValue;
    }

    private String clamp(double value, double min, double max) {
        if (value < min) {
            return String.valueOf(min);
        } else if (value > max) {
            return String.valueOf(max);
        }
        return String.valueOf(value);
    }
}
