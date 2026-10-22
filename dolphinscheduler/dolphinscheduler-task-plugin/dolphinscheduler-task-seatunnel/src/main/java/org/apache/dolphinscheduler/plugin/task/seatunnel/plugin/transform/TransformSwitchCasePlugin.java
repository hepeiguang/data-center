package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormSwitchCaseDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormSwitchCaseItemDTO;
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
public class TransformSwitchCasePlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormSwitchCaseDTO transFormSwitchCaseDTO = convertParams(task);
        seatunnelConfigDTO.getTransform().add(transFormSwitchCaseDTO);
        log.info("Success to add Transform SwitchCase Plugin");
    }

    private TransFormSwitchCaseDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        TransFormSwitchCaseDTO transFormSwitchCaseDTO = new TransFormSwitchCaseDTO();
        Map<String,Object> params = taskNode.getParams();
        try {
            transFormSwitchCaseDTO.setSwitchField(params.get("switch_field").toString());
            transFormSwitchCaseDTO.setSwitchTargetField(params.get("switch_target_field").toString());
            transFormSwitchCaseDTO.setSwitchTargetFieldType(params.get("switch_target_field_type").toString());
            transFormSwitchCaseDTO.setSwitchFieldValueDefault(params.get("switch_field_value_default").toString());
            String jsonString = JSONUtils.toJsonString(params.get("switch_case_mappings"));
            transFormSwitchCaseDTO.setSwitchCaseMappings(JSONUtils.toList(jsonString, TransFormSwitchCaseItemDTO.class));
            transFormSwitchCaseDTO.setPluginInput(taskNode.getPreTaskcode());
            transFormSwitchCaseDTO.setPluginOutput(taskNode.getCode());
            transFormSwitchCaseDTO.setPluginName("SwitchCase");
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return transFormSwitchCaseDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "SwitchCase";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
