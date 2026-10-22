package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormDecryptDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormSplitDTO;
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
public class TransformSplitPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormSplitDTO transFormSplitDTO = convertParams(task);
        seatunnelConfigDTO.getTransform().add(transFormSplitDTO);
        log.info("Success to add Transform SPLIT Plugin");
    }

    private TransFormSplitDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        TransFormSplitDTO transFormSplitDTO = new TransFormSplitDTO();
        Map<String,Object> params = taskNode.getParams();
        try {
            transFormSplitDTO.setSplitField(params.get("split_field").toString());
            transFormSplitDTO.setSeparator(params.get("separator").toString());
            transFormSplitDTO.setOutputFields(JSONUtils.toList(JSONUtils.toJsonString(params.get("output_fields")), String.class));
            transFormSplitDTO.setPluginInput(taskNode.getPreTaskcode());
            transFormSplitDTO.setPluginOutput(taskNode.getCode());
            transFormSplitDTO.setPluginName(taskNode.getPluginName());
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return transFormSplitDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "SPLIT";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
