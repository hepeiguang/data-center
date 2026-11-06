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

package org.apache.dolphinscheduler.dataapi.meta;

import java.util.Date;

import lombok.Getter;
import lombok.Setter;

/**
 * Read-only projection of {@code t_ds_datasource} replacing
 * {@code org.apache.dolphinscheduler.dao.entity.DataSource}. Named {@code DsDataSource} to avoid
 * clashing with {@code javax.sql.DataSource} which several call sites also use.
 */
@Getter
@Setter
public class DsDataSource {

    private Integer id;
    private String name;
    private String note;
    /** null when the stored code has no {@link DbType} match (unsupported connector). */
    private DbType type;
    private Integer userId;
    /** raw {@code connection_params} JSON as written by the DS UI. */
    private String connectionParams;
    private Date createTime;
    private Date updateTime;
}
