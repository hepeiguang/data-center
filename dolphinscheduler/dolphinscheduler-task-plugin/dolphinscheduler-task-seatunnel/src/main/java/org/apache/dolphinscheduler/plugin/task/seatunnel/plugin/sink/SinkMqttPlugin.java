package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkMqttDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkMqttDTO;
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
public class SinkMqttPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkMqttDTO SinkMqttDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSink().add(SinkMqttDTO);
        log.info("Success to add Source Jdbc Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "Mqtt";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

    private SinkMqttDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SinkMqttDTO sinkPluginValue = new SinkMqttDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                String jsonString = mapper.writeValueAsString(params);
                log.info("转化前的数据：{}", jsonString);
                sinkPluginValue = mapper.readValue(jsonString, SinkMqttDTO.class);
                log.info("转化后的数据：{}", JSONUtils.toJsonString(sinkPluginValue));
                //处理datassource字段
                convertDatasource(taskExecutionContext, sinkPluginValue);
                convertTopics(sinkPluginValue);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        sinkPluginValue.setPluginInput(taskNode.getPreTaskcode());
        sinkPluginValue.setPluginName(taskNode.getPluginName());
        log.info("sink转化后的数据：{}", JSONUtils.toJsonString(sinkPluginValue));
        return sinkPluginValue;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SinkMqttDTO sinkMqttDTO) throws JsonProcessingException {
        if (sinkMqttDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sinkMqttDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        JsonNode connParamsJson = JSONUtils.parseObject(connectionParams);
        sinkMqttDTO.setBrokenUrls(connParamsJson.get("brokenUrls").asText());
        if(null != connParamsJson.get("userName")){
            sinkMqttDTO.setUsername(connParamsJson.get("userName").asText());
        }
        if(null != connParamsJson.get("password")){
            sinkMqttDTO.setPassword(connParamsJson.get("password").asText());
        }
        sinkMqttDTO.setUseSsl(connParamsJson.get("useSsl").asBoolean());
        if (connParamsJson.has("useSsl") && connParamsJson.get("useSsl").asBoolean()) {
            sinkMqttDTO.setSslKeyStore(connParamsJson.get("sslKeyStore").asText());
            sinkMqttDTO.setSslKeyStorePassword(connParamsJson.get("sslKeyStorePassword").asText());
            sinkMqttDTO.setSslTrustStore(connParamsJson.get("sslTrustStore").asText());
            sinkMqttDTO.setSslTrustStorePassword(connParamsJson.get("sslTrustStorePassword").asText());
        }
    }

    private void convertTopics(SinkMqttDTO mqttDTO) {
        List<MqttTopicDTO> topicList = mqttDTO.getTopicList();
        if (topicList == null || topicList.isEmpty()) {
            throw new RuntimeException("topicList is empty");
        }

        Map<String, String> topicMap =topicList.stream()
                .filter(Objects::nonNull)  // 过滤null元素
                .collect(Collectors.toMap(
                        MqttTopicDTO::getTopic,
                        MqttTopicDTO::getQos,
                        (existing, replacement) -> existing  // 处理重复key
                ));
        mqttDTO.setTopics(topicMap);
        mqttDTO.setTopicList( null);
    }
}