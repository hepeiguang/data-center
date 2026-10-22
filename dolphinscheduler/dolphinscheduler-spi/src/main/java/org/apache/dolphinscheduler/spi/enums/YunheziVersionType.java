package org.apache.dolphinscheduler.spi.enums;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 版本类型枚举
 */
public enum YunheziVersionType {

    /**
     * Git版本控制
     */
    V1("1", "V1", "云盒子8版本",
            "/server/services2/login/getAccessTokenByAppKey",
            "$.tokenInfo.accessToken",
            "$.result.result",
            "/server/services/json/query/getSubItemList/%s",
            Stream.of(
                    new Param("-1", "公司文档"),
                    new Param("-2", "我的文档")
//                    ,new Param("-3", "同事文档"),
//        new Param("-17", "日志文档")
            ).collect(Collectors.toMap(Param::getId, param -> param.getName())));

    /**
     * 版本类型ID
     */
    private final String versionType;

    /**
     * 版本类型名称
     */
    private final String name;

    /**
     * 版本类型描述
     */
    private final String descp;

    /**
     * Token获取URL
     */
    private final String tokenUrl;

    /**
     * Token匹配模式（正则表达式）
     */
    private final String tokenPattern;

    private final String resultCondition;

    private final String getDirListUrl;

    /**
     * 收集参数Map（key: 参数ID, value: 参数名称）
     */
    private final Map<String, String> getDirListParamMap;

    /**
     * 内部参数类
     */
    public static class Param {
        private final String id;
        private final String name;

        public Param(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }
    }

    /**
     * 构造函数
     */
    YunheziVersionType(String versionType, String name, String descp,
                       String tokenUrl, String tokenPattern, String resultCondition,
                       String getDirListUrl, Map<String, String> getDirListParamMap) {
        this.versionType = versionType;
        this.name = name;
        this.descp = descp;
        this.tokenUrl = tokenUrl;
        this.tokenPattern = tokenPattern;
        this.resultCondition = resultCondition;
        this.getDirListParamMap = Collections.unmodifiableMap(getDirListParamMap);
        this.getDirListUrl = getDirListUrl;
    }

    // ========== Getter方法 ==========

    public String getVersionType() {
        return versionType;
    }

    public String getName() {
        return name;
    }

    public String getDescp() {
        return descp;
    }

    public String getTokenUrl() {
        return tokenUrl;
    }

    public String getTokenPattern() {
        return tokenPattern;
    }

    public Map<String, String> getGetDirListParamMap() {
        return getDirListParamMap;
    }

    public String getGetDirListUrl() {
        return getDirListUrl;
    }

    public String getResultCondition() {
		return resultCondition;
	}

    // ========== 工具方法 ==========

    /**
     * 根据版本类型ID获取枚举
     */
    public static YunheziVersionType getByType(Integer versionType) {
        if (versionType == null) {
            return null;
        }
        for (YunheziVersionType type : values()) {
            if (type.getVersionType().equals(versionType)) {
                return type;
            }
        }
        return null;
    }

    /**
     * 根据名称获取枚举
     */
    public static YunheziVersionType getByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return null;
        }
        for (YunheziVersionType type : values()) {
            if (type.getName().equalsIgnoreCase(name.trim())) {
                return type;
            }
        }
        return null;
    }

    /**
     * 检查版本类型是否存在
     */
    public static boolean containsType(Integer versionType) {
        return getByType(versionType) != null;
    }

    /**
     * 获取所有版本类型的ID列表
     */
    public static java.util.List<String> getAllTypeIds() {
        return Stream.of(values())
                .map(YunheziVersionType::getName)
                .collect(Collectors.toList());
    }

    /**
     * 获取枚举名称和描述映射
     */
    public static Map<String, String> getTypeNameMap() {
        Map<String, String> map = new HashMap<>();
        for (YunheziVersionType type : values()) {
            map.put(type.getVersionType(), type.getName());
        }
        return map;
    }
}
