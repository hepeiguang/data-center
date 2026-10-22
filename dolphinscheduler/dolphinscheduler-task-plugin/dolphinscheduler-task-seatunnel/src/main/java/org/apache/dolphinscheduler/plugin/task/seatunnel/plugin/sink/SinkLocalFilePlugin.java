package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkLocalfileDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sink对象-本地文件插件
 *
 * @author ZhaoYuan
 * @date 2025/5/6 14:33
 * @since JDK1.8
 */
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SinkLocalFilePlugin implements TaskTypeFactory {

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkLocalfileDTO localfileDTO = convertParams(task);
        seatunnelConfigDTO.getSink().add(localfileDTO);
        log.info("Success to add Sink LocalFile Plugin");
    }

    private SinkLocalfileDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        SinkLocalfileDTO localfileDTO = new SinkLocalfileDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                String jsonString = mapper.writeValueAsString(params);
                localfileDTO = mapper.readValue(jsonString, SinkLocalfileDTO.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        //处理List类数据字段
        if (ObjectUtils.isNotEmpty(localfileDTO.getPartitionByVo())) {
            localfileDTO.setPartitionBy(JSONUtils.parseObject(localfileDTO.getPartitionByVo(), new TypeReference<List<String>>() {}));
            localfileDTO.setPartitionByVo(null);
        }
        if (ObjectUtils.isNotEmpty(localfileDTO.getSinkColumnsVo())) {
            localfileDTO.setSinkColumns(JSONUtils.parseObject(localfileDTO.getSinkColumnsVo(), new TypeReference<List<String>>() {}));
            localfileDTO.setSinkColumnsVo(null);
        }

        localfileDTO.setPluginInput(taskNode.getPreTaskcode());
        localfileDTO.setPluginName(taskNode.getPluginName());
        localfileDTO.setParallelism(1);
        return localfileDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "LocalFile";
    }


    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}