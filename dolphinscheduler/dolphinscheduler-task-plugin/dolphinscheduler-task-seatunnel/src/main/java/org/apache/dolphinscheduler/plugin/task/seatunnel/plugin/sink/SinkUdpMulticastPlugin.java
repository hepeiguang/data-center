package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkUdpMulticastDTO;
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
public class SinkUdpMulticastPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkUdpMulticastDTO sinkUdpMulticastDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSink().add(sinkUdpMulticastDTO);
        log.info("Success to add Sink UDP Multicast Plugin");
    }

    private SinkUdpMulticastDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SinkUdpMulticastDTO sinkPluginValue = new SinkUdpMulticastDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                // 1.处理附件参数
                ParamUtils.processMapField(params, "otherConfig", sinkPluginValue::setOtherConfig);
                Optional.ofNullable(sinkPluginValue.getOtherConfig())
                        .ifPresent(params::putAll);
                params.remove("otherConfig");

                String jsonString = mapper.writeValueAsString(params);
                sinkPluginValue = mapper.readValue(jsonString, SinkUdpMulticastDTO.class);
                //处理datassource字段
                convertDatasource(taskExecutionContext, sinkPluginValue);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        sinkPluginValue.setPluginInput(taskNode.getPreTaskcode());
        sinkPluginValue.setPluginName(taskNode.getPluginName());
        sinkPluginValue.setParallelism(1);
        return sinkPluginValue;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SinkUdpMulticastDTO sinkUdpMulticastDTO) throws JsonProcessingException {
        if (sinkUdpMulticastDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sinkUdpMulticastDTO.getDatasourceId());
        log.info("sink udp multicast datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        UdpMulticastParamDTO udpMulticastParamDTO = JSONUtils.parseObject(connectionParams, UdpMulticastParamDTO.class);
        if (udpMulticastParamDTO != null) {
            sinkUdpMulticastDTO.setGroupAddress(udpMulticastParamDTO.getMulticastAddress());
            sinkUdpMulticastDTO.setPort(udpMulticastParamDTO.getPort());
            sinkUdpMulticastDTO.setSoTimeout(udpMulticastParamDTO.getConnectTimeout());
            sinkUdpMulticastDTO.setSendBufferSize(udpMulticastParamDTO.getBufferSize());
            if (udpMulticastParamDTO.getOther() != null) {
                if (udpMulticastParamDTO.getOther().getAsyncEnabled() != null) {
                    sinkUdpMulticastDTO.setAsyncEnabled(udpMulticastParamDTO.getOther().getAsyncEnabled());
                }
                if (udpMulticastParamDTO.getOther().getMaxPacketSize() != null) {
                    sinkUdpMulticastDTO.setMaxPacketSize(udpMulticastParamDTO.getOther().getMaxPacketSize());
                }
                if (udpMulticastParamDTO.getOther().getMaxRetries() != null) {
                    sinkUdpMulticastDTO.setMaxRetries(udpMulticastParamDTO.getOther().getMaxRetries());
                }
                if (udpMulticastParamDTO.getOther().getReplyTimeoutMs() != null) {
                    sinkUdpMulticastDTO.setReplyTimeoutMs(udpMulticastParamDTO.getOther().getReplyTimeoutMs());
                }
                if (udpMulticastParamDTO.getOther().getWaitForReply() != null) {
                    sinkUdpMulticastDTO.setWaitForReply(udpMulticastParamDTO.getOther().getWaitForReply());
                }
                if (udpMulticastParamDTO.getOther().getRetryIntervalMs() != null) {
                    sinkUdpMulticastDTO.setRetryIntervalMs(udpMulticastParamDTO.getOther().getRetryIntervalMs());
                }
                if (udpMulticastParamDTO.getOther().getBatchSize() != null) {
                    sinkUdpMulticastDTO.setBatchSize(udpMulticastParamDTO.getOther().getBatchSize());
                }
                if (udpMulticastParamDTO.getOther().getBatchIntervalMs() != null) {
                    sinkUdpMulticastDTO.setBatchIntervalMs(udpMulticastParamDTO.getOther().getBatchIntervalMs());
                }
                if (udpMulticastParamDTO.getOther().getBatchQueueSize() != null) {
                    sinkUdpMulticastDTO.setBatchQueueSize(udpMulticastParamDTO.getOther().getBatchQueueSize());
                }
                if (udpMulticastParamDTO.getOther().getAsyncThreads() != null) {
                    sinkUdpMulticastDTO.setAsyncThreads(udpMulticastParamDTO.getOther().getAsyncThreads());
                }
                if (udpMulticastParamDTO.getOther().getAsyncQueueSize() != null) {
                    sinkUdpMulticastDTO.setAsyncQueueSize(udpMulticastParamDTO.getOther().getAsyncQueueSize());
                }
                if (udpMulticastParamDTO.getOther().getAsyncWaitNanos() != null) {
                    sinkUdpMulticastDTO.setAsyncWaitNanos(udpMulticastParamDTO.getOther().getAsyncWaitNanos());
                }
                if (udpMulticastParamDTO.getOther().getCharsetName() != null) {
                    sinkUdpMulticastDTO.setCharsetName(udpMulticastParamDTO.getOther().getCharsetName());
                }
            }
        }
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "UDPMULTICAST";
    }


    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}