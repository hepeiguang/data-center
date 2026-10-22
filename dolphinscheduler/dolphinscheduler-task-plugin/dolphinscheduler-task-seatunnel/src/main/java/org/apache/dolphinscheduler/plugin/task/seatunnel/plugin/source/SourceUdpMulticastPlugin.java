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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.UdpMulticastParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceUdpMulticastDTO;
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
public class SourceUdpMulticastPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceUdpMulticastDTO sourceUdpMulticastDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sourceUdpMulticastDTO);
        log.info("Success to add Source UDP Multicast Plugin");
    }

    private SourceUdpMulticastDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SourceUdpMulticastDTO pluginValue = new SourceUdpMulticastDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                // 1.处理附件参数
                ParamUtils.processMapField(params, "otherConfig", pluginValue::setOtherConfig);
                Optional.ofNullable(pluginValue.getOtherConfig())
                        .ifPresent(params::putAll);
                params.remove("otherConfig");

                String jsonString = mapper.writeValueAsString(params);
                pluginValue = mapper.readValue(jsonString, SourceUdpMulticastDTO.class);
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

    private void convertSchema(SourceUdpMulticastDTO sourceUdpMulticastDTO) {
        if (StringUtils.isEmpty(sourceUdpMulticastDTO.getSchemaVo())) {
            return;
        }
        JsonNode schemaVo = JSONUtils.toJsonNode(sourceUdpMulticastDTO.getSchemaVo());
        Map<String, Object> seatunnelSchemaMap = new HashMap<>();
        seatunnelSchemaMap.put("fields", schemaVo);
        sourceUdpMulticastDTO.setSchema(seatunnelSchemaMap);
        sourceUdpMulticastDTO.setSchemaVo(null);
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SourceUdpMulticastDTO sourceUdpMulticastDTO) throws JsonProcessingException {
        if (sourceUdpMulticastDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sourceUdpMulticastDTO.getDatasourceId());
        log.info("source udp multicast datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        UdpMulticastParamDTO udpMulticastParamDTO = JSONUtils.parseObject(connectionParams, UdpMulticastParamDTO.class);
        if (udpMulticastParamDTO != null) {
            sourceUdpMulticastDTO.setGroupAddress(udpMulticastParamDTO.getMulticastAddress());
            sourceUdpMulticastDTO.setPort(udpMulticastParamDTO.getPort());
            sourceUdpMulticastDTO.setSoTimeout(udpMulticastParamDTO.getConnectTimeout());
            sourceUdpMulticastDTO.setReceiveBufferSize(udpMulticastParamDTO.getBufferSize());
            if (udpMulticastParamDTO.getOther() != null) {
                if (udpMulticastParamDTO.getOther().getMaxPacketSize() != null) {
                    sourceUdpMulticastDTO.setMaxPacketSize(udpMulticastParamDTO.getOther().getMaxPacketSize());
                }
                if (udpMulticastParamDTO.getOther().getTtl() != null) {
                    sourceUdpMulticastDTO.setTtl(udpMulticastParamDTO.getOther().getTtl());
                }
                if (udpMulticastParamDTO.getOther().getReuseAddress() != null) {
                    sourceUdpMulticastDTO.setReuseAddress(udpMulticastParamDTO.getOther().getReuseAddress());
                }
                if (udpMulticastParamDTO.getOther().getReplyEnabled() != null) {
                    sourceUdpMulticastDTO.setReplyEnabled(udpMulticastParamDTO.getOther().getReplyEnabled());
                }
                if (udpMulticastParamDTO.getOther().getReplyMessage() != null) {
                    sourceUdpMulticastDTO.setReplyMessage(udpMulticastParamDTO.getOther().getReplyMessage());
                }
                if (udpMulticastParamDTO.getOther().getCharsetName() != null) {
                    sourceUdpMulticastDTO.setCharsetName(udpMulticastParamDTO.getOther().getCharsetName());
                }
            }
        }
    }


    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "UDPMULTICAST";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}