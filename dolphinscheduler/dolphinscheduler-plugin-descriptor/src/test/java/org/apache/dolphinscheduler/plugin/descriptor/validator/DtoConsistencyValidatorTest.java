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
import org.apache.dolphinscheduler.plugin.descriptor.model.TaskMetadata;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DtoConsistencyValidator.
 */
class DtoConsistencyValidatorTest {

    private DtoConsistencyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new DtoConsistencyValidator();
    }

    @Test
    void testValidateWithMatchingFields() {
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .apiVersion("ds.plugin/v1")
                .task(TaskMetadata.builder().type("TEST").category("universal").name("Test").build())
                .params(ParamsBinding.builder()
                        .dtoClass(TestDto.class.getName())
                        .strict(true)
                        .build())
                .form(Arrays.asList(
                        FormField.builder().field("name").type("input").build(),
                        FormField.builder().field("value").type("input-number").build()
                ))
                .build();

        List<DtoConsistencyValidator.ValidationResult> results =
                validator.validate(descriptor, getClass().getClassLoader());

        // Should have no errors (name and value exist in TestDto)
        long errorCount = results.stream().filter(DtoConsistencyValidator.ValidationResult::isError).count();
        assertEquals(0, errorCount, "Expected no errors but got: " + results);
    }

    @Test
    void testValidateWithMissingDtoProperty() {
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .apiVersion("ds.plugin/v1")
                .task(TaskMetadata.builder().type("TEST").category("universal").name("Test").build())
                .params(ParamsBinding.builder()
                        .dtoClass(TestDto.class.getName())
                        .strict(true)
                        .build())
                .form(Arrays.asList(
                        FormField.builder().field("name").type("input").build(),
                        FormField.builder().field("nonExistentField").type("input").build()
                ))
                .build();

        List<DtoConsistencyValidator.ValidationResult> results =
                validator.validate(descriptor, getClass().getClassLoader());

        // Should have 1 error for nonExistentField
        long errorCount = results.stream().filter(DtoConsistencyValidator.ValidationResult::isError).count();
        assertEquals(1, errorCount, "Expected 1 error but got: " + results);
        assertTrue(results.stream().anyMatch(r ->
                r.isError() && r.getMessage().contains("nonExistentField")));
    }

    @Test
    void testValidateStrictVsWarn() {
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .apiVersion("ds.plugin/v1")
                .task(TaskMetadata.builder().type("TEST").category("universal").name("Test").build())
                .params(ParamsBinding.builder()
                        .dtoClass(TestDto.class.getName())
                        .strict(false) // warn mode
                        .build())
                .form(Collections.singletonList(
                        FormField.builder().field("extraField").type("input").build()
                ))
                .build();

        List<DtoConsistencyValidator.ValidationResult> results =
                validator.validate(descriptor, getClass().getClassLoader());

        // Should have warnings, not errors
        long errorCount = results.stream().filter(DtoConsistencyValidator.ValidationResult::isError).count();
        long warnCount = results.stream()
                .filter(r -> r.getLevel() == DtoConsistencyValidator.ValidationResult.Level.WARN)
                .count();
        assertEquals(0, errorCount, "Expected no errors in warn mode");
        assertTrue(warnCount > 0, "Expected warnings for extraField");
    }

    @Test
    void testValidateClassNotFound() {
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .apiVersion("ds.plugin/v1")
                .task(TaskMetadata.builder().type("TEST").category("universal").name("Test").build())
                .params(ParamsBinding.builder()
                        .dtoClass("com.nonexistent.SomeClass")
                        .strict(true)
                        .build())
                .form(Collections.singletonList(
                        FormField.builder().field("name").type("input").build()
                ))
                .build();

        List<DtoConsistencyValidator.ValidationResult> results =
                validator.validate(descriptor, getClass().getClassLoader());

        assertTrue(results.stream().anyMatch(r ->
                r.isError() && r.getMessage().contains("not found")));
    }

    @Test
    void testValidateNoParamsSection() {
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .apiVersion("ds.plugin/v1")
                .task(TaskMetadata.builder().type("TEST").category("universal").name("Test").build())
                .params(null)
                .form(Collections.singletonList(
                        FormField.builder().field("name").type("input").build()
                ))
                .build();

        List<DtoConsistencyValidator.ValidationResult> results =
                validator.validate(descriptor, getClass().getClassLoader());

        // Should have a warning about missing dtoClass, not an error
        assertTrue(results.stream().anyMatch(r ->
                r.getLevel() == DtoConsistencyValidator.ValidationResult.Level.WARN
                        && r.getMessage().contains("dtoClass")));
    }

    @Test
    void testValidateDotNotationFields() {
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .apiVersion("ds.plugin/v1")
                .task(TaskMetadata.builder().type("TEST").category("universal").name("Test").build())
                .params(ParamsBinding.builder()
                        .dtoClass(TestDto.class.getName())
                        .strict(true)
                        .build())
                .form(Collections.singletonList(
                        FormField.builder().field("taskParams.name").type("input").build()
                ))
                .build();

        List<DtoConsistencyValidator.ValidationResult> results =
                validator.validate(descriptor, getClass().getClassLoader());

        // "taskParams" is the root field, which is NOT in TestDto. Should error.
        long errorCount = results.stream().filter(DtoConsistencyValidator.ValidationResult::isError).count();
        assertEquals(1, errorCount);
    }

    /**
     * Test DTO class for reflection-based validation.
     */
    public static class TestDto {
        private String name;
        private int value;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public int getValue() { return value; }
        public void setValue(int value) { this.value = value; }
    }
}
