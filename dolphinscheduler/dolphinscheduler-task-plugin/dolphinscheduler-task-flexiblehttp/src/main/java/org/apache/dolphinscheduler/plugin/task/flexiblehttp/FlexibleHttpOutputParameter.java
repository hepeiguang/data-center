package org.apache.dolphinscheduler.plugin.task.flexiblehttp;

public class FlexibleHttpOutputParameter {

    private String name; // 输出参数名
    private String jsonPath; // JSON路径，用于从响应中提取值

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getJsonPath() {
        return jsonPath;
    }

    public void setJsonPath(String jsonPath) {
        this.jsonPath = jsonPath;
    }
}
