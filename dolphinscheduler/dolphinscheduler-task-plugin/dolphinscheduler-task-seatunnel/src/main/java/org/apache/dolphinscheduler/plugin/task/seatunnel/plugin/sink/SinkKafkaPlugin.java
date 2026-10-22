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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkKafkaDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceJdbcDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SinkKafkaPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkKafkaDTO sinkKafkaDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSink().add(sinkKafkaDTO);
        log.info("Success to add Source Jdbc Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "Kafka";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

    private SinkKafkaDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SinkKafkaDTO sinkPluginValue = new SinkKafkaDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sinkPluginValue = mapper.readValue(jsonString, SinkKafkaDTO.class);
                //处理datassource字段
                convertDatasource(taskExecutionContext, sinkPluginValue);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        sinkPluginValue.setPluginInput(taskNode.getPreTaskcode());
        sinkPluginValue.setPluginName(taskNode.getPluginName());
        return sinkPluginValue;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SinkKafkaDTO sinkKafkaDTO) throws JsonProcessingException {
        if (sinkKafkaDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sinkKafkaDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        JsonNode connParamsJson = JSONUtils.parseObject(connectionParams);
        sinkKafkaDTO.setBootstrapServers(connParamsJson.get("bootstrapServers").asText());
        //参数配置
        if (connParamsJson.get("kafkaConfig") == null || connParamsJson.get("kafkaConfig").isEmpty()) {
            return;
        }

        Map<String, String> kafkaConfig = JSONUtils.toMap(connParamsJson.get("kafkaConfig").toString());
        if (CollectionUtils.isEmpty(sinkKafkaDTO.getKafkaConfig())) {
            sinkKafkaDTO.setKafkaConfig(new HashMap<>());
        }
        kafkaConfig.forEach((key, value) -> sinkKafkaDTO.getKafkaConfig().put(key, value));
    }
}