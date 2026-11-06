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
import org.apache.dolphinscheduler.plugin.descriptor.model.PluginDescriptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates plugin descriptors against the ds-plugin/v1 contract.
 */
public class PluginDescriptorValidator {

    private static final Logger logger = LoggerFactory.getLogger(PluginDescriptorValidator.class);

    private static final Set<String> VALID_CATEGORIES = new HashSet<>(Arrays.asList(
            "universal", "cloud", "logic", "dataIntegration", "dataComputation", "machineLearning", "other"
    ));

    private static final Set<String> VALID_FIELD_TYPES = new HashSet<>(Arrays.asList(
            "input", "select", "editor", "switch", "input-number", "radio", "checkbox",
            "tree-select", "custom", "custom-parameters", "custom-labels", "multi-input",
            "multi-condition", "code-editor", "json-editor"
    ));

    private static final Set<String> VALID_EXECUTE_TYPES = new HashSet<>(Arrays.asList(
            "BATCH", "STREAM"
    ));

    /**
     * Validate a plugin descriptor and return a list of error messages.
     *
     * @param descriptor the descriptor to validate
     * @return empty list if valid, list of error messages otherwise
     */
    public List<String> validate(PluginDescriptor descriptor) {
        List<String> errors = new ArrayList<>();

        if (descriptor == null) {
            errors.add("Descriptor is null");
            return errors;
        }

        validateApiVersion(descriptor, errors);
        validateTask(descriptor, errors);
        validateIcon(descriptor, errors);
        validateForm(descriptor, errors);
        validateParams(descriptor, errors);

        return errors;
    }

    private void validateApiVersion(PluginDescriptor descriptor, List<String> errors) {
        String apiVersion = descriptor.getApiVersion();
        if (apiVersion == null || apiVersion.isEmpty()) {
            errors.add("Missing 'apiVersion' field");
        } else if (!"ds.plugin/v1".equals(apiVersion)) {
            errors.add("Unsupported apiVersion: " + apiVersion + ". Expected: ds.plugin/v1");
        }
    }

    private void validateTask(PluginDescriptor descriptor, List<String> errors) {
        if (descriptor.getTask() == null) {
            errors.add("Missing 'task' section");
            return;
        }

        if (descriptor.getTask().getType() == null || descriptor.getTask().getType().isEmpty()) {
            errors.add("Missing 'task.type' field");
        } else if (!descriptor.getTask().getType().matches("^[A-Z][A-Z0-9_]*$")) {
            errors.add("Invalid 'task.type': must be uppercase with underscores only, got: "
                    + descriptor.getTask().getType());
        }

        if (descriptor.getTask().getCategory() == null || descriptor.getTask().getCategory().isEmpty()) {
            errors.add("Missing 'task.category' field");
        } else if (!VALID_CATEGORIES.contains(descriptor.getTask().getCategory())) {
            errors.add("Invalid 'task.category': " + descriptor.getTask().getCategory()
                    + ". Must be one of: " + VALID_CATEGORIES);
        }

        if (descriptor.getTask().getName() == null || descriptor.getTask().getName().isEmpty()) {
            errors.add("Missing 'task.name' field");
        }

        if (descriptor.getTask().getExecuteType() != null) {
            for (String execType : descriptor.getTask().getExecuteType()) {
                if (!VALID_EXECUTE_TYPES.contains(execType)) {
                    errors.add("Invalid 'task.executeType' value: " + execType
                            + ". Must be one of: " + VALID_EXECUTE_TYPES);
                }
            }
        }
    }

    private void validateIcon(PluginDescriptor descriptor, List<String> errors) {
        if (descriptor.getIcon() == null) {
            return; // Icon is optional
        }

        if (descriptor.getIcon().getRef() != null
                && !descriptor.getIcon().getRef().startsWith("META-INF/ds-plugin/")) {
            errors.add("Invalid 'icon.ref': must start with 'META-INF/ds-plugin/', got: "
                    + descriptor.getIcon().getRef());
        }

        if (descriptor.getIcon().getFallback() != null
                && !"default".equals(descriptor.getIcon().getFallback())) {
            errors.add("Invalid 'icon.fallback': must be 'default', got: "
                    + descriptor.getIcon().getFallback());
        }
    }

    private void validateForm(PluginDescriptor descriptor, List<String> errors) {
        if (descriptor.getForm() == null || descriptor.getForm().isEmpty()) {
            errors.add("Missing or empty 'form' section");
            return;
        }

        Set<String> fieldNames = new HashSet<>();
        for (int i = 0; i < descriptor.getForm().size(); i++) {
            FormField field = descriptor.getForm().get(i);
            String prefix = "form[" + i + "]";

            if (field.getField() == null || field.getField().isEmpty()) {
                errors.add(prefix + ": missing 'field' name");
            } else if (!fieldNames.add(field.getField())) {
                errors.add(prefix + ": duplicate field name '" + field.getField() + "'");
            }

            if (field.getType() == null || field.getType().isEmpty()) {
                errors.add(prefix + ": missing 'type'");
            } else if (!VALID_FIELD_TYPES.contains(field.getType())) {
                errors.add(prefix + ": invalid type '" + field.getType()
                        + "'. Must be one of: " + VALID_FIELD_TYPES);
            }

            // Validate custom widget reference
            if ("custom".equals(field.getType()) && (field.getWidget() == null || field.getWidget().isEmpty())) {
                errors.add(prefix + ": custom type requires 'widget' reference");
            }

            // Validate nested children
            if (field.getChildren() != null) {
                for (int j = 0; j < field.getChildren().size(); j++) {
                    validateChildField(field.getChildren().get(j), prefix + ".children[" + j + "]", errors);
                }
            }
        }
    }

    private void validateChildField(FormField field, String prefix, List<String> errors) {
        if (field.getField() == null || field.getField().isEmpty()) {
            errors.add(prefix + ": missing 'field' name");
        }
        if (field.getType() == null || field.getType().isEmpty()) {
            errors.add(prefix + ": missing 'type'");
        } else if (!VALID_FIELD_TYPES.contains(field.getType())) {
            errors.add(prefix + ": invalid type '" + field.getType() + "'");
        }
    }

    private void validateParams(PluginDescriptor descriptor, List<String> errors) {
        if (descriptor.getParams() == null) {
            return; // Params section is optional
        }

        if (descriptor.getParams().getDtoClass() == null || descriptor.getParams().getDtoClass().isEmpty()) {
            errors.add("Missing 'params.dtoClass' field");
        } else if (!descriptor.getParams().getDtoClass().matches(
                "^[a-z][a-z0-9_]*(\\.[a-z][a-z0-9_]*)*\\.[A-Z][A-Za-z0-9]*$")) {
            errors.add("Invalid 'params.dtoClass': must be a fully qualified class name, got: "
                    + descriptor.getParams().getDtoClass());
        }
    }
}
