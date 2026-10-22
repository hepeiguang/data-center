package org.apache.dolphinscheduler.api.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 工作流参数替换器 - 将固定值替换为变量占位符
 */
@Slf4j
public class ParamReplacer {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 替换工作流参数中的固定值为变量占位符
     * @param params 工作流参数Map
     * @param replaceRules 替换规则 {原始值: 占位符}
     */
    public static void replaceFixedValues(Map<String, Object> params,
                                          Map<String, String> replaceRules) {
        if (params == null || replaceRules == null || replaceRules.isEmpty()) {
            return;
        }

        // 1. 替换Query参数
        replaceQueryParams(params, replaceRules);

        // 2. 替换JSON Body参数
        replaceJsonBodyParams(params, replaceRules);
    }

    /**
     * 替换Query参数
     */
    private static void replaceQueryParams(Map<String, Object> params,
                                           Map<String, String> replaceRules) {
        try {
            List<Map<String, Object>> httpParams = (List<Map<String, Object>>) params.get("httpParams");
            if (CollectionUtils.isNotEmpty(httpParams)) {
                for (Map<String, Object> param : httpParams) {
                    String originalValue = (String) param.get("value");
                    if (StringUtils.isNotBlank(originalValue)) {
                        // 检查是否需要替换
                        String replacedValue = applyReplaceRules(originalValue, replaceRules);
                        if (!originalValue.equals(replacedValue)) {
                            param.put("value", replacedValue);
                            log.debug("替换Query参数: {} -> {}", originalValue, replacedValue);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("替换Query参数失败", e);
        }
    }

    /**
     * 替换JSON Body参数
     */
    private static void replaceJsonBodyParams(Map<String, Object> params,
                                              Map<String, String> replaceRules) {
        try {
            String body = (String) params.get("body");
            if (StringUtils.isBlank(body)) {
                return;
            }

            // 尝试解析为JSON
            JsonNode rootNode = objectMapper.readTree(body);
            boolean modified = replaceJsonNodeValues(rootNode, replaceRules);

            if (modified) {
                String newBody = objectMapper.writeValueAsString(rootNode);
                params.put("body", newBody);
                log.debug("JSON Body参数已替换");
            }
        } catch (Exception e) {
            // 如果不是JSON格式，按普通文本处理
            try {
                String body = (String) params.get("body");
                if (StringUtils.isNotBlank(body)) {
                    String newBody = applyReplaceRules(body, replaceRules);
                    if (!body.equals(newBody)) {
                        params.put("body", newBody);
                    }
                }
            } catch (Exception ex) {
                log.warn("替换Body参数失败", ex);
            }
        }
    }

    /**
     * 递归替换JSON节点中的值
     */
    private static boolean replaceJsonNodeValues(JsonNode node,
                                                 Map<String, String> replaceRules) {
        boolean modified = false;

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();

            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                JsonNode childNode = entry.getValue();

                if (childNode.isTextual()) {
                    // 替换文本值
                    String originalValue = childNode.asText();
                    String replacedValue = applyReplaceRules(originalValue, replaceRules);

                    if (!originalValue.equals(replacedValue)) {
                        objectNode.set(entry.getKey(), new TextNode(replacedValue));
                        modified = true;
                        log.debug("替换JSON字段[{}]: {} -> {}",
                                entry.getKey(), originalValue, replacedValue);
                    }
                } else if (childNode.isObject() || childNode.isArray()) {
                    // 递归处理嵌套结构
                    boolean childModified = replaceJsonNodeValues(childNode, replaceRules);
                    modified = modified || childModified;
                }
            }
        } else if (node.isArray()) {
            // 处理数组
            for (JsonNode arrayItem : node) {
                boolean childModified = replaceJsonNodeValues(arrayItem, replaceRules);
                modified = modified || childModified;
            }
        }

        return modified;
    }

    /**
     * 应用替换规则
     */
    private static String applyReplaceRules(String value, Map<String, String> rules) {
        if (StringUtils.isBlank(value) || rules == null) {
            return value;
        }

        String result = value;
        for (Map.Entry<String, String> rule : rules.entrySet()) {
            String original = rule.getKey();
            String placeholder = rule.getValue();

            // 精确匹配替换
            if (original.equals(result)) {
                return placeholder;
            }

            // 包含匹配替换（避免误替换）
            if (result.contains(original) && !isPartOfWord(result, original)) {
                result = result.replace(original, placeholder);
            }
        }

        return result;
    }

    /**
     * 检查是否是单词的一部分（避免误替换）
     */
    private static boolean isPartOfWord(String text, String part) {
        if (text == null || part == null) return false;

        int index = text.indexOf(part);
        if (index == -1) return false;

        // 检查前后字符
        boolean prevCharValid = index == 0 ||
                !Character.isLetterOrDigit(text.charAt(index - 1));
        boolean nextCharValid = (index + part.length() >= text.length()) ||
                !Character.isLetterOrDigit(text.charAt(index + part.length()));

        return !(prevCharValid && nextCharValid);
    }
}