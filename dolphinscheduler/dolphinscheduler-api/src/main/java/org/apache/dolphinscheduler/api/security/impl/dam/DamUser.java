package org.apache.dolphinscheduler.api.security.impl.dam;

import java.util.Map;

public class DamUser {
    private String id;
    private String username;
    private String nickname;
    private Map<String, Object> dept;
    private String email;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public Map<String, Object> getDept() {
        return dept;
    }

    public void setDept(Map<String, Object> dept) {
        this.dept = dept;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
}
