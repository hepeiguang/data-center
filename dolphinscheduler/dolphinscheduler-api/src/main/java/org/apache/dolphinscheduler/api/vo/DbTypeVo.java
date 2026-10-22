package org.apache.dolphinscheduler.api.vo;

import lombok.Data;

public class DbTypeVo {
    private int code;
    private String name;
    private String descp;
    private String serverType;

    // 构造方法
    public DbTypeVo(int code, String name, String descp, String serverType) {
        this.code = code;
        this.name = name;
        this.descp = descp;
        this.serverType = serverType;
    }

    // Getter 和 Setter 方法
    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescp() {
        return descp;
    }

    public void setDescp(String descp) {
        this.descp = descp;
    }

    public String getServerType() {
        return serverType;
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    @Override
    public String toString() {
        return "DbTypeVo{" +
                "code=" + code +
                ", name='" + name + '\'' +
                ", descp='" + descp + '\'' +
                ", serverType='" + serverType + '\'' +
                '}';
    }
}
