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

package org.apache.dolphinscheduler.plugin.descriptor.validator;

import org.apache.dolphinscheduler.plugin.descriptor.model.FormField;
import org.apache.dolphinscheduler.plugin.descriptor.model.ParamsBinding;
import org.apache.dolphinscheduler.plugin.descriptor.model.PluginDescriptor;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates consistency between plugin descriptor form fields and the backend Parameters DTO.
 *
 * <p>Uses Java reflection (Introspector) to discover DTO properties and compares them
 * against the form field names declared in the descriptor. This catches field name drift
 * before it causes runtime parameter loss.
 *
 * <p>Two modes:
 * <ul>
 *   <li>strict=true: form fields must be a subset of DTO properties; extra fields are errors</li>
 *   <li>strict=false: extra fields generate warnings but are allowed (transition mode)</li>
 * </ul>
 */
public class DtoConsistencyValidator {

    private static final Logger logger = LoggerFactory.getLogger(DtoConsistencyValidator.class);

    /**
     * Validate form fields against the DTO class properties.
     *
     * @param descriptor the plugin descriptor to validate
     * @param classLoader the classloader to use for loading the DTO class
     * @return list of validation results (errors and warnings)
     */
    public List<ValidationResult> validate(PluginDescriptor descriptor, ClassLoader classLoader) {
        List<ValidationResult> results = new ArrayList<>();

        ParamsBinding params = descriptor.getParams();
        if (params == null || params.getDtoClass() == null || params.getDtoClass().isEmpty()) {
            results.add(ValidationResult.warn("params",
                    "No 'params.dtoClass' specified; skipping DTO consistency check"));
            return results;
        }

        // Load the DTO class
        Class<?> dtoClass;
        try {
            dtoClass = classLoader.loadClass(params.getDtoClass());
        } catch (ClassNotFoundException e) {
            results.add(ValidationResult.error("params.dtoClass",
                    "DTO class not found: " + params.getDtoClass()));
            return results;
        }

        // Discover DTO properties via introspection
        Set<String> dtoProperties = getDtoProperties(dtoClass);
        if (dtoProperties.isEmpty()) {
            results.add(ValidationResult.warn("params.dtoClass",
                    "DTO class has no discoverable properties: " + params.getDtoClass()));
            return results;
        }

        // Collect form field names
        Set<String> formFields = new HashSet<>();
        if (descriptor.getForm() != null) {
            for (FormField field : descriptor.getForm()) {
                String rootField = field.getField().split("\\.")[0];
                formFields.add(rootField);
                collectChildFields(field, formFields);
            }
        }

        // Check: form fields that are NOT in DTO properties
        boolean strict = params.isStrict();
        for (String field : formFields) {
            if (!dtoProperties.contains(field)) {
                String msg = String.format(
                        "Form field '%s' not found in DTO class '%s' properties %s",
                        field, params.getDtoClass(), dtoProperties);
                if (strict) {
                    results.add(ValidationResult.error("form." + field, msg));
                } else {
                    results.add(ValidationResult.warn("form." + field, msg));
                }
            }
        }

        // Check: DTO properties that have NO corresponding form field (informational)
        for (String prop : dtoProperties) {
            if (!formFields.contains(prop)) {
                results.add(ValidationResult.info("params.dtoClass",
                        String.format("DTO property '%s' has no corresponding form field", prop)));
            }
        }

        logger.info("DTO consistency check for {}: {} properties, {} form fields, {} issues",
                params.getDtoClass(), dtoProperties.size(), formFields.size(), results.size());

        return results;
    }

    /**
     * Get all property names from a DTO class using Java Bean introspection.
     */
    private Set<String> getDtoProperties(Class<?> dtoClass) {
        Set<String> properties = new HashSet<>();
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(dtoClass, Object.class);
            for (PropertyDescriptor pd : beanInfo.getPropertyDescriptors()) {
                properties.add(pd.getName());
            }
        } catch (IntrospectionException e) {
            logger.warn("Failed to introspect DTO class: {}", dtoClass.getName(), e);
        }

        // Also check declared fields (for classes not following JavaBean conventions)
        try {
            for (java.lang.reflect.Field field : dtoClass.getDeclaredFields()) {
                if (!java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    properties.add(field.getName());
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to inspect declared fields of: {}", dtoClass.getName(), e);
        }

        // Check superclass fields
        Class<?> superClass = dtoClass.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            properties.addAll(getDtoProperties(superClass));
        }

        return properties;
    }

    /**
     * Recursively collect field names from nested children.
     */
    private void collectChildFields(FormField field, Set<String> fieldNames) {
        if (field.getChildren() != null) {
            for (FormField child : field.getChildren()) {
                String rootField = child.getField().split("\\.")[0];
                fieldNames.add(rootField);
                collectChildFields(child, fieldNames);
            }
        }
    }

    /**
     * A single validation result.
     */
    public static class ValidationResult {
        public enum Level { INFO, WARN, ERROR }

        private final Level level;
        private final String field;
        private final String message;

        private ValidationResult(Level level, String field, String message) {
            this.level = level;
            this.field = field;
            this.message = message;
        }

        public static ValidationResult info(String field, String message) {
            return new ValidationResult(Level.INFO, field, message);
        }

        public static ValidationResult warn(String field, String message) {
            return new ValidationResult(Level.WARN, field, message);
        }

        public static ValidationResult error(String field, String message) {
            return new ValidationResult(Level.ERROR, field, message);
        }

        public Level getLevel() {
            return level;
        }

        public String getField() {
            return field;
        }

        public String getMessage() {
            return message;
        }

        public boolean isError() {
            return level == Level.ERROR;
        }
    }
}
