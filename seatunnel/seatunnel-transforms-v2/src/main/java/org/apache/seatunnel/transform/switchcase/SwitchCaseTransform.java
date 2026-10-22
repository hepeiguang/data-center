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

package org.apache.seatunnel.transform.switchcase;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.catalog.Column;
import org.apache.seatunnel.api.table.catalog.PhysicalColumn;
import org.apache.seatunnel.api.table.type.*;
import org.apache.seatunnel.transform.common.SingleFieldOutputTransform;
import org.apache.seatunnel.transform.exception.TransformCommonError;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
public class SwitchCaseTransform extends SingleFieldOutputTransform {

    public static String PLUGIN_NAME = "SwitchCase";
    private final SwitchCaseTransformConfig switchCaseTransformConfig;
    private final int switchFieldIndex;

    public SwitchCaseTransform(
            @NonNull SwitchCaseTransformConfig switchCaseTransformConfig,
            @NonNull CatalogTable catalogTable) {
        super(catalogTable);
        this.switchCaseTransformConfig = switchCaseTransformConfig;
        SeaTunnelRowType seaTunnelRowType = catalogTable.getTableSchema().toPhysicalRowDataType();
        try {
            switchFieldIndex = seaTunnelRowType.indexOf(switchCaseTransformConfig.getSwitchField());
        } catch (IllegalArgumentException e) {
            throw TransformCommonError.cannotFindInputFieldError(
                    getPluginName(), switchCaseTransformConfig.getSwitchField());
        }
        this.outputCatalogTable = getProducedCatalogTable();
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    protected Object getOutputFieldValue(SeaTunnelRowAccessor inputRow) {
        // switch_field
        Object value = inputRow.getField(switchFieldIndex);
        //SeaTunnelDataType<?> fieldType = seaTunnelRowType.getFieldType(switchFieldIndex);
        SeaTunnelDataType<?> fieldType = getOutputFieldType(switchCaseTransformConfig.getSwitchTargetFieldType());
        Object defaultValue = switchCaseTransformConfig.getSwitchFieldValueDefault();
        if (value == null) {
            return defaultValue;
        }
        // 根据 fieldType 的具体类型进行强制转换
        log.info("field sql type mapping is {}", fieldType.getSqlType());
        switch (fieldType.getSqlType()) {
            case STRING:
                for (SwitchCaseMapping mapping : switchCaseTransformConfig.getSwitchCaseMappings()) {
                    if (String.valueOf(mapping.getSwitchCaseWhen()).equals(String.valueOf(value))) {
                        return mapping.getSwitchCaseThen().toString();
                    }
                }
                return defaultValue == null ? value.toString() : defaultValue.toString();
            case INT:
                for (SwitchCaseMapping mapping : switchCaseTransformConfig.getSwitchCaseMappings()) {
                    if (String.valueOf(mapping.getSwitchCaseWhen()).equals(String.valueOf(value))) {
                        return Integer.parseInt(mapping.getSwitchCaseThen().toString());
                    }
                }
                return defaultValue == null ? Integer.parseInt(value.toString()) : Integer.parseInt(defaultValue.toString());
            case BIGINT:
                for (SwitchCaseMapping mapping : switchCaseTransformConfig.getSwitchCaseMappings()) {
                    if (String.valueOf(mapping.getSwitchCaseWhen()).equals(String.valueOf(value))) {
                        return Long.parseLong(mapping.getSwitchCaseThen().toString());
                    }
                }
                return defaultValue == null ? Long.parseLong(value.toString()) : Long.parseLong(defaultValue.toString());
            case FLOAT:
                for (SwitchCaseMapping mapping : switchCaseTransformConfig.getSwitchCaseMappings()) {
                    if (String.valueOf(mapping.getSwitchCaseWhen()).equals(String.valueOf(value))) {
                        return Float.parseFloat(mapping.getSwitchCaseThen().toString());
                    }
                }
                return defaultValue == null ? Float.parseFloat(value.toString()) : Float.parseFloat(defaultValue.toString());
            case DOUBLE:
                for (SwitchCaseMapping mapping : switchCaseTransformConfig.getSwitchCaseMappings()) {
                    if (String.valueOf(mapping.getSwitchCaseWhen()).equals(String.valueOf(value))) {
                        return Double.parseDouble(mapping.getSwitchCaseThen().toString());
                    }
                }
                return defaultValue == null ? Double.parseDouble(value.toString()) : Double.parseDouble(defaultValue.toString());

            // 新增类型 时间类型 布尔类型暂时支持（gauss/pg）
            // timestamp类型特殊 seatunnel归于localdate类型下 所以去除
            case DATE:
                try {
                    for (SwitchCaseMapping mapping : switchCaseTransformConfig.getSwitchCaseMappings()) {
                        // 如果switch_case_when 等于 switch_field字段的下标
                        if (String.valueOf(mapping.getSwitchCaseWhen()).equals(String.valueOf(value))) {
                            return LocalDate.parse(mapping.getSwitchCaseThen().toString());
                        }
                        return defaultValue == null ? LocalDate.parse(value.toString()) : LocalDate.parse(defaultValue.toString());
                    }
                } catch (Exception e) {
                    log.error("Invalid date format, error is cause {}", e.getMessage());
                    throw new RuntimeException("Invalid date format");
                }

//            case BOOLEAN:
//                try{
//                    for (SwitchCaseMapping mapping : switchCaseTransformConfig.getSwitchCaseMappings()) {
//                        String temp = mapping.getSwitchCaseThen().toString();
//                        if (temp == null || temp.isEmpty()) break;
//                        temp = temp.trim().toLowerCase();
//                        switch (temp) {
//                            case "true":
//                            case "1":
//                            case "yes":
//                            case "t":
//                                return "true";
//                            case "false":
//                            case "0":
//                            case "no":
//                            case "f":
//                                return "false";
//                            default:
//                                // 只有当temp完全匹配value时才尝试解析
//                                if (temp.equalsIgnoreCase(String.valueOf(value))) {
//                                    return Boolean.toString(Boolean.parseBoolean(temp));
//                                }
//                                continue;  // 继续检查下一个mapping
//                        }
//                        return defaultValue == null ? Boolean.parseBoolean(value.toString()) : Boolean.parseBoolean(defaultValue.toString());
//                    }
//                }catch (Exception e) {
//                    log.error("Invalid boolean format, error is cause {}", e.getMessage());
//                }

            case BOOLEAN:
                try {
                    for (SwitchCaseMapping mapping : switchCaseTransformConfig.getSwitchCaseMappings()) {
                        String temp = mapping.getSwitchCaseThen().toString();
                        if (temp == null || temp.isEmpty()) continue;  // continue跳过而非break
                        temp = temp.trim().toLowerCase();
                        switch (temp) {
                            case "true":
                            case "1":
                            case "yes":
                            case "t":
                                return true;
                            case "false":
                            case "0":
                            case "no":
                            case "f":
                                return false;
                            default:
                                // 只有当temp完全匹配value时才尝试解析
                                if (temp.equalsIgnoreCase(String.valueOf(value))) {
                                    return Boolean.toString(Boolean.parseBoolean(temp));
                                }
                                continue;  // 继续检查下一个mapping
                        }

                    }
                    // 所有mapping检查完后没有匹配，再处理默认值
                    if (defaultValue != null) {
                        if (defaultValue instanceof Boolean) {
                            return defaultValue;
                        }
                        String defaultStr = defaultValue.toString().trim().toLowerCase();
                        if (defaultStr.matches("true|1|yes|t|y")) {
                            return true;
                        }
                        if (defaultStr.matches("false|0|no|f|n")) {
                            return false;
                        }
                        return Boolean.parseBoolean(defaultStr);
                    }
                    return Boolean.parseBoolean(String.valueOf(value));
                } catch (Exception e) {
                    log.error("Invalid boolean format, error is cause {}", e.getMessage());
                    throw new RuntimeException("Unsupported boolean field type");
                }


            default:
                // todo 后续如果需要兼容处理或处理其他 可以使用tochar函数等方式进行修改，
                throw new IllegalArgumentException("Unsupported field type: " + fieldType);
        }
    }

    @Override
    protected Column getOutputColumn() {
        return PhysicalColumn.of(switchCaseTransformConfig.getSwitchTargetField(), getOutputFieldType(switchCaseTransformConfig.getSwitchTargetFieldType()), 200L, true, null, "");
    }

    public SeaTunnelDataType<?> getOutputFieldType(String type) {
        switch (type) {
            case "string":
                return BasicType.STRING_TYPE;
            case "num":
                return BasicType.INT_TYPE;
            case "date":
                return LocalTimeType.LOCAL_DATE_TYPE;
            case "bool":
                return BasicType.BOOLEAN_TYPE;
            default:
                throw new IllegalArgumentException("Case is default, Unsupported field type: " + type);
        }
    }
}
