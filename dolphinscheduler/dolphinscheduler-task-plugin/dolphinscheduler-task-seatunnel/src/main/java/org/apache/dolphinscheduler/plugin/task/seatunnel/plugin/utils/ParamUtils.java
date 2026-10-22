package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * 参数转换工具类
 */
@Slf4j
public class ParamUtils {

    // 通用map处理工具方法
    public static void processMapField(Map<String, Object> params, String fieldName,
                                       Consumer<Map<String, Object>> setter) {
        Optional.ofNullable(params.get(fieldName))
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .map(ParamUtils::convertToMap)
                .filter(map -> !map.isEmpty())
                .ifPresent(setter);
    }

    // 类型安全转换
    public static Map<String, Object> convertToMap(List<?> list) {
        Map<String, Object> result = new HashMap<>(list.size());
        for (Object item : list) {
            if (item instanceof Map) {
                Map<?, ?> mapItem = (Map<?, ?>) item;
                Object prop = mapItem.get("prop");
                Object value = mapItem.get("value");

                if (prop instanceof String) {
                    result.put((String) prop, value);
                }
            }
        }
        return result;
    }

    // 根据给定字段名称，获取对应的Map
    public static void extractMapByFieldName(Map<String, Object> params, String fieldName, Consumer<Map<String, Object>> consumer) {
        Optional.ofNullable(params.get(fieldName))
                .filter(param -> param instanceof List)
                .map(param -> (List<Map<String, Object>>) param)
                .map(headerList -> headerList.stream()
                        .filter(item -> item.get("prop") instanceof String)
                        .collect(Collectors.toMap(
                                item -> (String) item.get("prop"),
                                item -> item.get("value"),
                                (existing, replacement) -> existing
                        )))
                .filter(map -> !map.isEmpty())
                .ifPresent(consumer);
    }

    // 根据给定字段名称，获取对应的Map
    public static void extractMap(Map<String, Object> params, Consumer<Map<String, Object>> consumer) {
        Optional.ofNullable(params)
                .filter(List.class::isInstance)
                .map(List.class::cast)
                .map(ParamUtils::convertToMap)
                .filter(map -> !map.isEmpty())
                .ifPresent(consumer);
    }

}
