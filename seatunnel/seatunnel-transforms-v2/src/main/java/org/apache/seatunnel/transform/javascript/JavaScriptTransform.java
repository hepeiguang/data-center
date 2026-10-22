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

package org.apache.seatunnel.transform.javascript;

import jdk.nashorn.api.scripting.NashornScriptEngineFactory;
import lombok.NonNull;
import org.apache.seatunnel.api.table.catalog.CatalogTable;
import org.apache.seatunnel.api.table.catalog.Column;
import org.apache.seatunnel.api.table.catalog.PhysicalColumn;
import org.apache.seatunnel.api.table.type.SeaTunnelRowAccessor;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.transform.common.MultipleFieldOutputTransform;
import org.apache.seatunnel.transform.exception.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.seatunnel.transform.dynamiccompile.CompileTransformErrorCode.COMPILE_TRANSFORM_ERROR_CODE;

public class JavaScriptTransform extends MultipleFieldOutputTransform {
    private static final Logger log = LoggerFactory.getLogger(JavaScriptTransform.class);
    public static String PLUGIN_NAME = "JavaScript";
    public static final String getInlineOutputColumns = "getInlineOutputColumns";
    public static final String getInlineOutputFieldValues = "getInlineOutputFieldValues";
    private final JavaScriptTransformConfig javaScriptTransformConfig;
    private final SeaTunnelRowType seaTunnelRowType;

    public JavaScriptTransform(
            @NonNull JavaScriptTransformConfig javaScriptTransformConfig,
            @NonNull CatalogTable catalogTable) {
        super(catalogTable);
        seaTunnelRowType = catalogTable.getTableSchema().toPhysicalRowDataType();
        this.javaScriptTransformConfig = javaScriptTransformConfig;
        this.outputCatalogTable = getProducedCatalogTable();
    }

    @Override
    public String getPluginName() {
        return PLUGIN_NAME;
    }

    @Override
    protected Object[] getOutputFieldValues(SeaTunnelRowAccessor inputRow) {
        Object[] fieldValues = Arrays.stream(javaScriptParse(getInlineOutputColumns))
                .map(fieldName -> inputRow.getField(seaTunnelRowType.indexOf((String) fieldName)))
                .toArray();
        return javaScriptParse(getInlineOutputFieldValues, fieldValues);
    }

    @Override
    protected Column[] getOutputColumns() {
        return Arrays.stream(javaScriptParse(getInlineOutputColumns))
                .map(
                        fieldName -> PhysicalColumn.of(
                                    (String) fieldName, seaTunnelRowType.getFieldType(seaTunnelRowType.indexOf(fieldName.toString())), 200, true, "", "")
                        )
                .toArray(Column[]::new);
    }

    public Object[] javaScriptParse(String name, Object ...args) {
        NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
        ScriptEngine engine = factory.getScriptEngine();
        log.info("-------------------------JavaScriptTransform: {}", engine);
        try {
            // 加载JS脚本
            engine.eval(javaScriptTransformConfig.getSourceCode());
            Object calculator = engine.get("calculator");
            // 调用JS方法
            Invocable inv = (Invocable) engine;
            Object resultObj = inv.invokeMethod(calculator, name, args);
            // 处理不同类型的返回值
            if (resultObj instanceof Map) {
                Map<?, ?> resultMap = (Map<?, ?>) resultObj;
                return resultMap.values().toArray();
            } else if (resultObj instanceof List) {
                return ((List<?>) resultObj).toArray();
            } else if (resultObj.getClass().isArray()) {
                return (Object[]) resultObj;
            }
            throw new TransformException(COMPILE_TRANSFORM_ERROR_CODE,
                    "Unsupported return type: " + resultObj.getClass().getName());
        } catch (Exception e) {
            throw new TransformException(COMPILE_TRANSFORM_ERROR_CODE, e.getMessage());
        }
    }
}
