package org.apache.dolphinscheduler.api.utils;

import org.apache.dolphinscheduler.api.dto.taskType.TaskConfig;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdvancedTaskYamlParser {

    public static List<TaskConfig> parseYaml(InputStream inputStream) {
        Yaml yaml = new Yaml();
        List<Object> yamlData = yaml.load(inputStream);
        List<TaskConfig> result = new ArrayList<>();

        for (Object item : yamlData) {
            if (item instanceof Map) {
                result.add(parseTaskConfig((Map<String, Object>) item));
            }
        }

        return result;
    }

    private static TaskConfig parseTaskConfig(Map<String, Object> map) {
        TaskConfig config = new TaskConfig();
        config.setTaskType((String) map.get("taskType"));
        config.setTaskName((String) map.get("taskName"));

        if (map.containsKey("jobMode")) {
            config.setJobMode((String) map.get("jobMode"));
        }

        if (map.containsKey("children")) {
            List<Map<String, Object>> children = (List<Map<String, Object>>) map.get("children");
            List<TaskConfig> childConfigs = new ArrayList<>();

            for (Map<String, Object> childMap : children) {
                childConfigs.add(parseTaskConfig(childMap));
            }

            config.setChildren(childConfigs);
        }

        return config;
    }
}
