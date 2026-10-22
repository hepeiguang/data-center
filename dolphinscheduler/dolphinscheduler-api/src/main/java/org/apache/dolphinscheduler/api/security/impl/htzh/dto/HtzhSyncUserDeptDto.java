package org.apache.dolphinscheduler.api.security.impl.htzh.dto;

import lombok.Data;

import java.util.List;

@Data
public class HtzhSyncUserDeptDto {
    private List<HtzhUser> userList;

    public boolean isValid() {
        return (userList != null && !userList.isEmpty());
    }
}
