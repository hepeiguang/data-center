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
 * Form field descriptor, mapping to the frontend IJsonItem interface.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FormField {

    /**
     * Model field name (supports dot notation, e.g., "taskParams.rawScript").
     */
    private String field;

    /**
     * Widget type. Must be one of the IType values supported by getElementByJson:
     * input, select, editor, switch, input-number, radio, checkbox, tree-select, custom, etc.
     */
    private String type;

    /**
     * i18n key for the field label (resolved from locales section or src/locales).
     */
    private String labelKey;

    /**
     * Direct label text (used if labelKey is not provided).
     */
    private String label;

    /**
     * Whether the field is required.
     */
    @Builder.Default
    private boolean required = false;

    /**
     * Default value for the field.
     */
    private Object defaultValue;

    /**
     * Input type hint (e.g., "number", "text", "password").
     */
    private String inputType;

    /**
     * Placeholder text or i18n key.
     */
    private String placeholder;

    /**
     * Tooltip text or i18n key.
     */
    private String tipKey;

    /**
     * Static options for select/radio/checkbox fields.
     */
    private List<Object> options;

    /**
     * API reference for dynamic options (e.g., "api:/task/plugin-descriptors/{type}/options/{field}").
     */
    private String optionsFrom;

    /**
     * Validation rules for the field.
     */
    private FieldValidation validate;

    /**
     * Conditional visibility expression.
     * Map of field name to expected value(s). Field is visible only when all conditions match.
     * Example: { "pluginName": "UDP" } or { "parseMode": ["js", "offset_table"] }
     */
    private Map<String, Object> when;

    /**
     * Widget name for custom type fields (references platform component registry).
     */
    private String widget;

    /**
     * Minimum required version of the referenced widget.
     */
    private Integer widgetMinVersion;

    /**
     * Props to pass to the custom widget component.
     */
    private Map<String, Object> props;

    /**
     * For editor type: programming language (e.g., "json", "sql", "python").
     */
    private String language;

    /**
     * Grid column span (1-24).
     */
    private Integer span;

    /**
     * Nested form fields (for grouped/complex layouts).
     */
    private List<FormField> children;
}
