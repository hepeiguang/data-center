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

package org.apache.dolphinscheduler.dataapi.acl;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class DataApiAclEntry {

    private Long id;
    private String apiPath;
    private int subjectType;
    private String subjectId;
    /** 1=ALLOW 0=DENY */
    private int effect = 1;
    private String remark;
    private Integer createBy;
    private Timestamp createTime;
    private Timestamp updateTime;

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("apiPath", apiPath);
        m.put("subjectType", subjectType);
        m.put("subjectTypeName", AclSubjectType.fromCode(subjectType).name());
        m.put("subjectId", subjectId);
        m.put("effect", effect);
        m.put("effectName", effect == 1 ? "ALLOW" : "DENY");
        m.put("remark", remark);
        m.put("createBy", createBy);
        m.put("createTime", createTime == null ? null : createTime.toString());
        m.put("updateTime", updateTime == null ? null : updateTime.toString());
        return m;
    }
}
