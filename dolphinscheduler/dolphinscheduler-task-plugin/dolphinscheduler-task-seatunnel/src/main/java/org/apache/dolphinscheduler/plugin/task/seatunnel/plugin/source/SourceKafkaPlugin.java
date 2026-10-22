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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceKafkaDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
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
public class SourceKafkaPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceKafkaDTO sourceJdbcDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sourceJdbcDTO);
        log.info("Success to add Source Jdbc Plugin");
    }

    private SourceKafkaDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SourceKafkaDTO pluginValue = new SourceKafkaDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                String jsonString = mapper.writeValueAsString(params);
                pluginValue = mapper.readValue(jsonString, SourceKafkaDTO.class);
                //处理datassource字段
                convertDatasource(taskExecutionContext, pluginValue);
                convertSchema(pluginValue);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        if("debezium_json".equalsIgnoreCase(pluginValue.getFormat())){
            pluginValue.setDebeziumRecordIncludeSchema(false);
        }
        pluginValue.setPluginOutput(taskNode.getCode());
        pluginValue.setPluginName(taskNode.getPluginName());
        return pluginValue;
    }

    private void convertSchema(SourceKafkaDTO kafkaDTO) {
        if (kafkaDTO.getSchemaVo() == null) {
            return;
        }
        Map<String, String> schemaVoMap = JSONUtils.toMap(kafkaDTO.getSchemaVo());
        Map<String, Object> seatunnelSchemaMap = new HashMap<>();
        seatunnelSchemaMap.put("fields",schemaVoMap);
        kafkaDTO.setSchema(seatunnelSchemaMap);
        kafkaDTO.setSchemaVo(null);
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SourceKafkaDTO kafkaDTO) throws JsonProcessingException {
        if (kafkaDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, kafkaDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        JsonNode connParamsJson = JSONUtils.parseObject(connectionParams);
        kafkaDTO.setBootstrapServers(connParamsJson.get("bootstrapServers").asText());
        //参数配置
        if (connParamsJson.get("kafkaConfig") == null || connParamsJson.get("kafkaConfig").isEmpty()) {
            return;
        }

        Map<String, String> kafkaConfig = JSONUtils.toMap(connParamsJson.get("kafkaConfig").toString());
        if (CollectionUtils.isEmpty(kafkaDTO.getKafkaConfig())) {
            kafkaDTO.setKafkaConfig(new HashMap<>());
        }
        kafkaConfig.forEach((key, value) -> kafkaDTO.getKafkaConfig().put(key, value));
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "Kafka";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}