package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkUdpDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
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
public class SinkUdpPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkUdpDTO sinkKafkaDTO = convertParams(task);
        seatunnelConfigDTO.getSink().add(sinkKafkaDTO);
        log.info("Success to add Source Jdbc Plugin");
    }

    private SinkUdpDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        SinkUdpDTO sinkPluginValue = new SinkUdpDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sinkPluginValue = mapper.readValue(jsonString, SinkUdpDTO.class);
                //处理datassource字段
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        sinkPluginValue.setPluginInput(taskNode.getPreTaskcode());
        sinkPluginValue.setPluginName(taskNode.getPluginName());
        sinkPluginValue.setParallelism(1);
        return sinkPluginValue;
    }


    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "UDP";
    }


    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}