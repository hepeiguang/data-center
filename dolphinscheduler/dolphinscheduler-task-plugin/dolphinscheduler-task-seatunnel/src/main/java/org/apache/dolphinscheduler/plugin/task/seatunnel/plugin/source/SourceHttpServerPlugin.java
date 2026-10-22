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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.HttpServerParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceHttpServerDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceHttpServerPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceHttpServerDTO sourceHttpServerDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sourceHttpServerDTO);
        log.info("Success to add Source Http Server Plugin");
    }

    private SourceHttpServerDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SourceHttpServerDTO pluginValue = new SourceHttpServerDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                // 1.JSON转对象
                mapper.updateValue(pluginValue, params);
                // 2.参数转换
                //处理datassource字段
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

    private void convertSchema(SourceHttpServerDTO sourceHttpServerDTO) {
        if (!StringUtils.isEmpty(sourceHttpServerDTO.getSchemaVo())) {
            JsonNode schemaNode = JSONUtils.toJsonNode(sourceHttpServerDTO.getSchemaVo());
            Map<String, Object> seatunnelSchemaMap = new HashMap<>();
            seatunnelSchemaMap.put("fields", schemaNode);
            sourceHttpServerDTO.setSchema(seatunnelSchemaMap);
            sourceHttpServerDTO.setSchemaVo(null);
        }
        if (!StringUtils.isEmpty(sourceHttpServerDTO.getHttpAuthConfig())) {
            // 设置认证方式
            Map<String, String> authMap = JSONUtils.toMap(sourceHttpServerDTO.getHttpAuthConfig());
            sourceHttpServerDTO.setAuthConfig(authMap);
            sourceHttpServerDTO.setHttpAuthConfig(null);
        }
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SourceHttpServerDTO sourceHttpServerDTO) throws JsonProcessingException {
        if (sourceHttpServerDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sourceHttpServerDTO.getDatasourceId());
        log.info("source http server datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        HttpServerParamDTO httpServerParamDTO = JSONUtils.parseObject(connectionParams, HttpServerParamDTO.class);
        if (httpServerParamDTO != null) {
            sourceHttpServerDTO.setProtocol(httpServerParamDTO.getProtocol());
            sourceHttpServerDTO.setHost(httpServerParamDTO.getHost());
            sourceHttpServerDTO.setPort(httpServerParamDTO.getPort());
//            sourceHttpServerDTO.setEndpoint(httpServerParamDTO.getHttpEndpoint());
//            sourceHttpServerDTO.setMethod(httpServerParamDTO.getHttpMethod());
//            sourceHttpServerDTO.setAuthType(httpServerParamDTO.getHttpAuthType());
            sourceHttpServerDTO.setConnectTimeout(httpServerParamDTO.getConnectTimeout());
            if (httpServerParamDTO.getHttpAuthConfig() != null && !httpServerParamDTO.getHttpAuthType().equalsIgnoreCase("NONE")) {
                Map<String, String> authMap = JSONUtils.toMap(httpServerParamDTO.getHttpAuthConfig());
                sourceHttpServerDTO.setAuthConfig(authMap);
            }
        }
    }


    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "HTTPSERVER";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}
