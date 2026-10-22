package org.apache.dolphinscheduler.api.security.impl.htzh.dto;

import lombok.Data;

@Data
public class HtzhSsoAuthInfo {
//    {"token":"", "username":"超级管理员", "account":"admin", "userId": "1", "expiration": 86400, "loginStatus": true, "userAttrs": {"tenantId": "-1"}}
    private String token;
    private String username;
    private String account;
    private String userId;
    private Integer expiration;
    private Boolean loginStatus;
    private UserAttrs userAttrs;

    @Data
    class UserAttrs {
        private String tenantId;
    }


    public boolean valid() {
        return account != null && userId != null;
    }
}
