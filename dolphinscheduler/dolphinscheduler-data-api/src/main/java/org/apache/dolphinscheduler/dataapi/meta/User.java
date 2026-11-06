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

import lombok.Getter;
import lombok.Setter;

/**
 * Read-only projection of {@code t_ds_user} replacing
 * {@code org.apache.dolphinscheduler.dao.entity.User}. Field/getter names mirror the DS entity
 * so migration touched call sites only through imports.
 */
@Getter
@Setter
public class User {

    private int id;
    private String userName;
    private UserType userType;
    /** 0 disabled / 1 enabled, same encoding as DS. */
    private int state;

    public boolean isAdmin() {
        return userType == UserType.ADMIN_USER;
    }

    public boolean isDisabled() {
        return state == Flag.NO.ordinal();
    }
}
