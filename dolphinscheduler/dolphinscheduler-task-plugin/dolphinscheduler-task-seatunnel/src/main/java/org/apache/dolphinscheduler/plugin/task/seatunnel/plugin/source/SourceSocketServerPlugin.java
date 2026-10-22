package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SocketServerParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceSocketServerDTO;
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
public class SourceSocketServerPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceSocketServerDTO sourceSocketServerDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sourceSocketServerDTO);
        log.info("Success to add Source Socket Server Plugin");
    }

    private SourceSocketServerDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SourceSocketServerDTO pluginValue = new SourceSocketServerDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                String jsonString = mapper.writeValueAsString(params);
                pluginValue = mapper.readValue(jsonString, SourceSocketServerDTO.class);
                convertDatasource(taskExecutionContext, pluginValue);
                convertSchema(pluginValue);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        pluginValue.setPluginOutput(taskNode.getCode());
        pluginValue.setPluginName(taskNode.getPluginName());
        pluginValue.setParallelism(1);
        return pluginValue;
    }

    private void convertSchema(SourceSocketServerDTO sourceSocketServerDTO) {
        if (StringUtils.isEmpty(sourceSocketServerDTO.getSchemaVo())) {
            return;
        }
        JsonNode schemaVo = JSONUtils.toJsonNode(sourceSocketServerDTO.getSchemaVo());
        Map<String, Object> seatunnelSchemaMap = new HashMap<>();
        seatunnelSchemaMap.put("fields", schemaVo);
        sourceSocketServerDTO.setSchema(seatunnelSchemaMap);
        sourceSocketServerDTO.setSchemaVo(null);
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SourceSocketServerDTO sourceSocketServerDTO) throws JsonProcessingException {
        if (sourceSocketServerDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sourceSocketServerDTO.getDatasourceId());
        log.info("source socket server datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        SocketServerParamDTO socketServerParamDTO = JSONUtils.parseObject(connectionParams, SocketServerParamDTO.class);
        if (socketServerParamDTO != null) {
            sourceSocketServerDTO.setHost(socketServerParamDTO.getHost());
            sourceSocketServerDTO.setPort(socketServerParamDTO.getPort());
            socketServerParamDTO.setBufferSize(socketServerParamDTO.getBufferSize());
            socketServerParamDTO.setSocketTimeout(socketServerParamDTO.getSocketTimeout());
            socketServerParamDTO.setMaxConnections(socketServerParamDTO.getMaxConnections());
        }
    }


    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "SOCKETSERVER";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}