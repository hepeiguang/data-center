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
import org.apache.dolphinscheduler.plugin.descriptor.model.IconRef;
import org.apache.dolphinscheduler.plugin.descriptor.model.ParamsBinding;
import org.apache.dolphinscheduler.plugin.descriptor.model.PluginDescriptor;
import org.apache.dolphinscheduler.plugin.descriptor.model.TaskMetadata;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for PluginDescriptorValidator.
 */
class PluginDescriptorValidatorTest {

    private PluginDescriptorValidator validator;

    @BeforeEach
    void setUp() {
        validator = new PluginDescriptorValidator();
    }

    @Test
    void testValidDescriptor() {
        PluginDescriptor descriptor = buildValidDescriptor();
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.isEmpty(), "Expected no errors but got: " + errors);
    }

    @Test
    void testNullDescriptor() {
        List<String> errors = validator.validate(null);
        assertFalse(errors.isEmpty());
        assertTrue(errors.stream().anyMatch(e -> e.contains("null")));
    }

    @Test
    void testMissingApiVersion() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.setApiVersion(null);
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("apiVersion")));
    }

    @Test
    void testWrongApiVersion() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.setApiVersion("ds.plugin/v2");
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("Unsupported apiVersion")));
    }

    @Test
    void testMissingTask() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.setTask(null);
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("task")));
    }

    @Test
    void testMissingTaskType() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.getTask().setType(null);
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("task.type")));
    }

    @Test
    void testInvalidTaskTypeFormat() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.getTask().setType("lowercase");
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("uppercase")));
    }

    @Test
    void testInvalidCategory() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.getTask().setCategory("invalid_category");
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("category")));
    }

    @Test
    void testMissingForm() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.setForm(null);
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("form")));
    }

    @Test
    void testEmptyForm() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.setForm(Collections.emptyList());
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("form")));
    }

    @Test
    void testDuplicateFieldNames() {
        PluginDescriptor descriptor = buildValidDescriptor();
        FormField dup = FormField.builder().field("name").type("input").build();
        descriptor.getForm().add(dup);
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("duplicate")));
    }

    @Test
    void testInvalidFieldType() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.getForm().get(0).setType("nonexistent_type");
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("invalid type")));
    }

    @Test
    void testCustomTypeWithoutWidget() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.getForm().get(0).setType("custom");
        descriptor.getForm().get(0).setWidget(null);
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("widget")));
    }

    @Test
    void testInvalidIconRef() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.setIcon(IconRef.builder().ref("wrong/path/icon.png").build());
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("icon.ref")));
    }

    @Test
    void testInvalidDtoClass() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.setParams(ParamsBinding.builder().dtoClass("not-a-class-name").build());
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("dtoClass")));
    }

    @Test
    void testInvalidExecuteType() {
        PluginDescriptor descriptor = buildValidDescriptor();
        descriptor.getTask().setExecuteType(Arrays.asList("BATCH", "INVALID"));
        List<String> errors = validator.validate(descriptor);
        assertTrue(errors.stream().anyMatch(e -> e.contains("executeType")));
    }

    // --- Helper ---

    private PluginDescriptor buildValidDescriptor() {
        return PluginDescriptor.builder()
                .apiVersion("ds.plugin/v1")
                .task(TaskMetadata.builder()
                        .type("TEST_TASK")
                        .category("universal")
                        .name("Test Task")
                        .order(1)
                        .executeType(Collections.singletonList("BATCH"))
                        .build())
                .icon(IconRef.builder()
                        .ref("META-INF/ds-plugin/icon.png")
                        .fallback("default")
                        .build())
                .params(ParamsBinding.builder()
                        .dtoClass("org.apache.dolphinscheduler.plugin.task.test.TestParameters")
                        .strict(true)
                        .build())
                .form(new ArrayList<>(Arrays.asList(
                        FormField.builder().field("name").type("input").required(true).build(),
                        FormField.builder().field("value").type("input-number").build()
                )))
                .build();
    }
}
