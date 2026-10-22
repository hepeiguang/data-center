package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.MqttTopicDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceMqttDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceMqttPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceMqttDTO sourceJdbcDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sourceJdbcDTO);
        log.info("Success to add Source Jdbc Plugin");
    }

    private SourceMqttDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SourceMqttDTO pluginValue = new SourceMqttDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                String jsonString = mapper.writeValueAsString(params);
                pluginValue = mapper.readValue(jsonString, SourceMqttDTO.class);
                //处理datassource字段
                convertDatasource(taskExecutionContext, pluginValue);
                convertSchema(pluginValue);
                convertTopics(pluginValue);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        pluginValue.setPluginOutput(taskNode.getCode());
        pluginValue.setPluginName(taskNode.getPluginName());
        return pluginValue;
    }

    private void convertSchema(SourceMqttDTO mqttDTO) {
        if (mqttDTO.getSchemaVo() == null) {
            return;
        }
        Map<String, String> schemaVoMap = JSONUtils.toMap(mqttDTO.getSchemaVo());
        Map<String, Object> seatunnelSchemaMap = new HashMap<>();
        seatunnelSchemaMap.put("fields", schemaVoMap);
        mqttDTO.setSchema(seatunnelSchemaMap);
        mqttDTO.setSchemaVo(null);
    }

    private void convertTopics(SourceMqttDTO mqttDTO) {
        List<MqttTopicDTO> topicList = mqttDTO.getTopicList();
        if (topicList == null || topicList.isEmpty()) {
            throw new RuntimeException("topicList is empty");
        }

        Map<String, String> topicMap = topicList.stream()
                .filter(Objects::nonNull)  // 过滤null元素
                .collect(Collectors.toMap(
                        MqttTopicDTO::getTopic,
                        MqttTopicDTO::getQos,
                        (existing, replacement) -> existing  // 处理重复key
                ));
        mqttDTO.setTopics(topicMap);
        mqttDTO.setTopicList(null);
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SourceMqttDTO mqttDTO) throws JsonProcessingException {
        if (mqttDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, mqttDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        JsonNode connParamsJson = JSONUtils.parseObject(connectionParams);
        mqttDTO.setBrokenUrls(connParamsJson.get("brokenUrls").asText());
        if(null != connParamsJson.get("userName")){
            mqttDTO.setUsername(connParamsJson.get("userName").asText());
        }
        if(null != connParamsJson.get("password")){
            mqttDTO.setPassword(connParamsJson.get("password").asText());
        }
        mqttDTO.setUseSsl(connParamsJson.get("useSsl").asBoolean());
        if (connParamsJson.has("useSsl") && connParamsJson.get("useSsl").asBoolean()) {
            mqttDTO.setSslKeyStore(connParamsJson.get("sslKeyStore").asText());
            mqttDTO.setSslKeyStorePassword(connParamsJson.get("sslKeyStorePassword").asText());
            mqttDTO.setSslTrustStore(connParamsJson.get("sslTrustStore").asText());
            mqttDTO.setSslTrustStorePassword(connParamsJson.get("sslTrustStorePassword").asText());
        }
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "Mqtt";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.SOURCE_MQTT;
    }

}