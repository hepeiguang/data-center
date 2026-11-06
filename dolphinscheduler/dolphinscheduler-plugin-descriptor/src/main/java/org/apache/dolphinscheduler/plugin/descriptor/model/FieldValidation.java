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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Validation rules for a form field.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldValidation {

    /**
     * Whether the field is required.
     */
    @Builder.Default
    private boolean required = false;

    /**
     * Validation trigger: "blur", "change", or "both".
     */
    private String trigger;

    /**
     * Validation type: "non-empty", "email", "url", "regex", "min", "max", etc.
     */
    private String type;

    /**
     * Error message or i18n key shown on validation failure.
     */
    private String message;

    /**
     * Minimum value (for number types) or minimum length (for strings).
     */
    private Number min;

    /**
     * Maximum value (for number types) or maximum length (for strings).
     */
    private Number max;

    /**
     * Regular expression pattern (when type is "regex").
     */
    private String pattern;
}
