package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormDesensitizeDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormDesensitizeItemDTO;
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
public class TransformDesensitizePlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormDesensitizeDTO transFormDesensitizeDTO = convertParams(task);
        seatunnelConfigDTO.getTransform().add(transFormDesensitizeDTO);
        log.info("Success to add Transform Desensitize Plugin");
    }

    private TransFormDesensitizeDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        TransFormDesensitizeDTO transFormDesensitizeDTO = new TransFormDesensitizeDTO();
        Map<String,Object> params = taskNode.getParams();
        try {
            String jsonString = JSONUtils.toJsonString(params.get("custom_list"));
            transFormDesensitizeDTO.setCustomList(JSONUtils.toList(jsonString, TransFormDesensitizeItemDTO.class));
            transFormDesensitizeDTO.setPluginInput(taskNode.getPreTaskcode());
            transFormDesensitizeDTO.setPluginOutput(taskNode.getCode());
            transFormDesensitizeDTO.setPluginName(taskNode.getPluginName());
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return transFormDesensitizeDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "DESENSITIZE";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
