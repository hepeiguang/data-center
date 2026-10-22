package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.HttpConnectParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkHttpDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.utils.ParamUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Map;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SinkHttpPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkHttpDTO sinkhttpDTO = convertParams(task);
        seatunnelConfigDTO.getSink().add(sinkhttpDTO);
        log.info("Success to add Sink Http Plugin");
    }

    private SinkHttpDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        SinkHttpDTO sinkPluginValue = new SinkHttpDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sinkPluginValue = mapper.readValue(jsonString, SinkHttpDTO.class);
                // 处理请求头headers
                ParamUtils.extractMapByFieldName(params, "httpHeaders", sinkPluginValue::setHeaders);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
//        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
//                .getResourceParameters(ResourceType.DATASOURCE, taskNode.getDatasourceId());
//        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
//        String connectionParams = dbSource.getConnectionParams();
//        HttpConnectParamDTO httpParameters = JSONUtils.parseObject(connectionParams, HttpConnectParamDTO.class);
//        // 构建完整URL
//        String path = sinkPluginValue.getUrl().startsWith("/") ? sinkPluginValue.getUrl() : "/" + sinkPluginValue.getUrl();
//        // todo http算子应支持http / https 两种协议
//        if (taskNode != null) {
//            sinkPluginValue.setUrl(taskNode.getProtocol() + "://" + taskNode.getHost() + ":" + taskNode.getPort() + path);
//        }
        sinkPluginValue.setPluginInput(taskNode.getPreTaskcode());
        sinkPluginValue.setPluginName(taskNode.getPluginName());
        sinkPluginValue.setParallelism(1);
        return sinkPluginValue;
    }


    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "Http";
    }


    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}