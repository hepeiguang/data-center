package org.apache.dolphinscheduler.api.security.impl.htzh.dto;

import lombok.Data;
import org.apache.dolphinscheduler.common.enums.UserType;
import org.apache.dolphinscheduler.common.utils.DateUtils;
import org.apache.dolphinscheduler.dao.entity.User;

import java.time.format.DateTimeFormatter;
import java.util.Date;

@Data
public class HtzhUser {
    private String id;
    private String createTime;
    private String isDelete;
    private String fullname;
    private String account;
    private String password;
    private String email;
    private String mobile;
    private String address;
    private String photo;
    private String sex;
    private String from;
    private String status;
    private String groupId;
    private String hasSyncToWx;
    private String pwdCreateTime;
//    private String attributes;
    private String enabled;
    private String username;
    private String userId;
    private String admin;
    private String identityType;
    private String enable;
    private String credentialsNonExpired;
    private String accountNonExpired;
    private String accountNonLocked;
    private String pkVal;

    public User toUser() {
        User user = new User();
        user.setId(id);
        user.setUserName(account);
        user.setEmail(email);
        user.setPhone(mobile);
        user.setUserType(UserType.ADMIN_USER);
        user.setTenantId(-1);
        user.setState(1);
        user.setCreateTime(this.getPwdCreateTime() != null ?
                DateUtils.parse(this.getPwdCreateTime(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"), "Asia/Shanghai")
                : new Date());
        user.setUpdateTime(new Date());
        return user;
    }

    public boolean isValid() {
        return account != null && !account.isEmpty() && id != null && !id.isEmpty();
    }
}
