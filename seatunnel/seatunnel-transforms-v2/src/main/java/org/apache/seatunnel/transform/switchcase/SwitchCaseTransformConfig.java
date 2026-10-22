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

import lombok.Getter;
import lombok.Setter;
import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
public class SwitchCaseTransformConfig implements Serializable {
    public static final Option<String> KEY_SWITCH_FIELD =
            Options.key("switch_field")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The field to be replace");

    public static final Option<String> KEY_SWITCH_TARGET_FIELD =
            Options.key("switch_target_field")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The target field name");

    public static final Option<String> KEY_SWITCH_TARGET_FIELD_TYPE =
            Options.key("switch_target_field_type")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The target field type");

    public static final Option<Object> KEY_SWITCH_FIELD_VALUE_DEFAULT =
            Options.key("switch_field_value_default")
                    .objectType(Object.class)
                    .noDefaultValue()
                    .withDescription("The default result after to be replace");

    public static final Option<List<SwitchCaseMapping>> KEY_SWITCH_CASE_MAPPINGS =
            Options.key("switch_case_mappings")
                    .listType(SwitchCaseMapping.class)
                    .noDefaultValue()
                    .withDescription("List of switch case mappings");

    private String switchField;
    private String switchTargetField;
    private String switchTargetFieldType;
    private Object switchFieldValueDefault;
    private List<SwitchCaseMapping> switchCaseMappings;

    public static SwitchCaseTransformConfig of(ReadonlyConfig config) {
        SwitchCaseTransformConfig switchCaseTransformConfig = new SwitchCaseTransformConfig();
        switchCaseTransformConfig.setSwitchField(config.get(KEY_SWITCH_FIELD));
        switchCaseTransformConfig.setSwitchTargetField(config.get(KEY_SWITCH_TARGET_FIELD));
        switchCaseTransformConfig.setSwitchTargetFieldType(config.get(KEY_SWITCH_TARGET_FIELD_TYPE));
        switchCaseTransformConfig.setSwitchFieldValueDefault(config.get(KEY_SWITCH_FIELD_VALUE_DEFAULT));
        switchCaseTransformConfig.setSwitchCaseMappings(config.get(KEY_SWITCH_CASE_MAPPINGS));
        return switchCaseTransformConfig;
    }
}
