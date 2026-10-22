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
import lombok.Getter;
import lombok.Setter;
import org.apache.seatunnel.api.configuration.Option;
import org.apache.seatunnel.api.configuration.Options;
import org.apache.seatunnel.api.configuration.ReadonlyConfig;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class DictTransformConfig implements Serializable {
    public static final Option<String> KEY_APIURL =
            Options.key("api_Url")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The apiUrl to get the dict");
    public static final Option<String> KEY_APIKEY =
            Options.key("api_Key")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The apikey to get the dict");
    public static final Option<List<DictFieldMapping>> KEY_SOURCE_FIELD =
            Options.key("source_fields")
                    .listType(DictFieldMapping.class)
                    .noDefaultValue()
                    .withDescription("The source field to be dict");
//
//    public static final Option<List<String>> KEY_OUTPUT_FIELDS =
//            Options.key("output_fields")
//                    .listType()
//                    .noDefaultValue()
//                    .withDescription("The result fields after replace");
    private String apiUrl;
    private String apiKey;
    private List<DictFieldMapping> sourceFields;

    public static DictTransformConfig of(ReadonlyConfig config) {
        DictTransformConfig dictTransformConfig = new DictTransformConfig();
        dictTransformConfig.setApiUrl(config.get(KEY_APIURL));
        dictTransformConfig.setApiKey(config.get(KEY_APIKEY));
        dictTransformConfig.setSourceFields(config.get(KEY_SOURCE_FIELD));
//        dictTransformConfig.setOutputFields(config.get(KEY_OUTPUT_FIELDS).toArray(new String[0]));
        return dictTransformConfig;
    }
}
