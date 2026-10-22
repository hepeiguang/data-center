package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.api.utils.ParameterUtils;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.HttpConnectParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceHttpDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransformHttpDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.utils.ParamUtils;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformHttpPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        // todo transform dto 定义
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransformHttpDTO transformHttpDTO = convertParams(task,taskExecutionContext);
        seatunnelConfigDTO.getTransform().add(transformHttpDTO);
        log.info("Success to add Transform Http Plugin");
    }

    private TransformHttpDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        TransformHttpDTO pluginValue = new TransformHttpDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                // 1.处理附件参数
//                ParamUtils.processMapField(params, "httpExtra", pluginValue::setHttpExtraParams);
//                Optional.ofNullable(pluginValue.getHttpExtraParams())
//                        .ifPresent(params::putAll);
//                params.remove("httpExtra");

                ParamUtils.processMapField(params, "httpExtra", extraParams -> {
                    if (extraParams != null && !extraParams.isEmpty()) {
                        // 直接将 extraParams 合并到主 params 中
                        params.putAll(extraParams);
                        log.info("Merged httpExtra params: {}", extraParams.keySet());
                    }
                });
                params.remove("httpExtra");

//                if (pluginValue.getHttpExtraParams() != null && !pluginValue.getHttpExtraParams().isEmpty()) {
//                    for (Map.Entry<String, Object> entry : pluginValue.getHttpExtraParams().entrySet()) {
//                        String extraKey = entry.getKey(); // 例如: "content_field"
//                        Object extraValue = entry.getValue(); // 例如: "$.data.totalList"
//                        if (extraValue != null) {
//                            params.put(extraKey, extraValue);
//                        }
//                    }
//                }

                // 2.JSON转对象
                mapper.updateValue(pluginValue, params);
                // 3. 处理请求头和请求参数
                ParamUtils.processMapField(params, "httpHeaders", pluginValue::setHeaders);
                ParamUtils.processMapField(params, "httpParams", pluginValue::setParams);

                // todo 对变量参数进行替换 将被${}包裹的数据替换成上一个任务拿到的数据
                Map<String, String> paramsMap = convertParamsMap(taskExecutionContext.getPrepareParamsMap());
                log.error("对比中，存在的字段： {}", paramsMap.toString());
                String originalUrl = pluginValue.getUrl();
                if (StringUtils.isNotEmpty(originalUrl) && containsPlaceholder(originalUrl)) {
                    String replacedUrl = ParameterUtils.convertParameterPlaceholders(originalUrl, paramsMap);
                    pluginValue.setUrl(replacedUrl);
                    log.info("URL replaced from {} to {}", originalUrl, replacedUrl);
                }

                String originalBody = pluginValue.getBody();
                if (StringUtils.isNotEmpty(originalBody) && containsPlaceholder(originalBody)) {
                    String replacedBody = ParameterUtils.convertParameterPlaceholders(originalBody, paramsMap);
                    pluginValue.setBody(replacedBody);
                    log.info("Body replaced from {} to {}", originalBody, replacedBody);
                }

                Map<String, Object> replacedHeaders = replaceMapPlaceholders(pluginValue.getHeaders(), paramsMap);
                pluginValue.setHeaders(replacedHeaders);

                Map<String, Object> replacedParams = replaceMapPlaceholders(pluginValue.getParams(), paramsMap);
                pluginValue.setParams(replacedParams);

                // 3.参数转换
                convertSchema(pluginValue);
                convertPageing(pluginValue);
                // chu处理url
                convertDatasource(pluginValue,taskExecutionContext);
                String url = pluginValue.getUrl();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        // transform  in out 需要特殊处理
        pluginValue.setPluginInput(taskNode.getPreTaskcode());
        pluginValue.setPluginOutput(taskNode.getCode());
        pluginValue.setPluginName(taskNode.getPluginName());
        return pluginValue;
    }


    private static Map<String, String> convertParamsMap(Map<String, Property> rawParamsMap) {
        Map<String, String> paramsMap = new HashMap<>();
        if (rawParamsMap != null) {
            for (Map.Entry<String, Property> entry : rawParamsMap.entrySet()) {
                if (entry.getValue() != null && entry.getValue().getValue() != null) {
                    paramsMap.put(entry.getKey(), entry.getValue().getValue());
                }
            }
        }
        return paramsMap;
    }

    // 替换 占位符变量
    private Map<String, Object> replaceMapPlaceholders(
            Map<String, Object> originalMap,
            Map<String, String> paramsMap) {
        if (CollectionUtils.isEmpty(originalMap)) {
            return originalMap;
        }
        Map<String, Object> replacedMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : originalMap.entrySet()) {
            String key = entry.getKey();
            String value = String.valueOf(entry.getValue());
            if (containsPlaceholder(value)) {
                String replacedValue = ParameterUtils.convertParameterPlaceholders(value, paramsMap);
                replacedMap.put(key, replacedValue);
                log.info("Map key '{}' replaced from '{}' to '{}'", key, value, replacedValue);
            } else {
                replacedMap.put(key, value);
            }
        }
        return replacedMap;
    }

    // 获取上游任务传递的字段 在当前算子中进行替换
//    private String resolvePlaceholders(String template, Map<String, Object> mapping) {
//        if (!containsPlaceholder(template)) {
//            return template;
//        }
//        String result = template;
//        List<String> placeholders = getPlaceholders(template);
//        log.error("--- [DEBUG] 打印原始模板和找到的占位符: '{}', found placeholders: {}", template, placeholders);
//        for (String placeholder : placeholders) {
//            String placeholderValue = mapping.get(placeholder).toString();
//            result = result.replace(placeholder, placeholderValue);
//        }
//        return result;
//    }
    private boolean containsPlaceholder(String template) {
        return template != null && template.contains("${") && template.contains("}");
    }
//    private List<String> getPlaceholders(String template) {
//        List<String> placeholders = new ArrayList<>();
//        if (template == null || template.isEmpty()) {
//            return placeholders;
//        }
//        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
//        Matcher matcher = pattern.matcher(template);
//        while (matcher.find()) {
//            placeholders.add(matcher.group(0)); // 添加完整的占位符，如${name}
//        }
//        return placeholders;
//    }

    private void convertDatasource(TransformHttpDTO pluginValue,TaskExecutionContext taskExecutionContext) throws JsonProcessingException {
        if (pluginValue.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, pluginValue.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        HttpConnectParamDTO httpParameters = JSONUtils.parseObject(connectionParams, HttpConnectParamDTO.class);
        // 构建完整URL
        String path = pluginValue.getUrl().startsWith("/") ? pluginValue.getUrl() : "/" + pluginValue.getUrl();
        // todo http算子应支持http / https 两种协议
        if (httpParameters != null) {
            pluginValue.setUrl(httpParameters.getProtocol() + "://" + httpParameters.getHost() + ":" + httpParameters.getPort() + path);
        }

    }


    private void convertSchema(TransformHttpDTO transformHttpDTO) {
        if (StringUtils.isEmpty(transformHttpDTO.getSchemaVo())) {
            return;
        }
        JsonNode schemaVo = JSONUtils.parseObject(transformHttpDTO.getSchemaVo());
        Map<String, Object> seatunnelSchemaMap = new HashMap<>();
        seatunnelSchemaMap.put("fields", schemaVo);
        transformHttpDTO.setSchema(seatunnelSchemaMap);
        transformHttpDTO.setSchemaVo(null);
        // 2 设置json_field
        Map<String, Object> jsonFiledVoMap = JSONUtils.json2Map(transformHttpDTO.getJsonFieldVo());
        transformHttpDTO.setJsonField(jsonFiledVoMap);
        transformHttpDTO.setJsonFieldVo(null);
    }

    private void convertPageing(TransformHttpDTO transformHttpDTO) {
        if (StringUtils.isEmpty(transformHttpDTO.getPageingVo())) {
            return;
        }
        Map<String, Object> pageingVoMap = JSONUtils.json2Map(transformHttpDTO.getPageingVo());
        transformHttpDTO.setPageing(pageingVoMap);
        transformHttpDTO.setPageingVo(null);
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "Http";
    }


    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}
