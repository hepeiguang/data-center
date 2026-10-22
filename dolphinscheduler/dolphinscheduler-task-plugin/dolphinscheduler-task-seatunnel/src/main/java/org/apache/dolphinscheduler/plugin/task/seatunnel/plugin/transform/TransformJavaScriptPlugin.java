package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormJavaDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.CompileLanguageEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.CompilePatternEnum;
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
public class TransformJavaScriptPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormJavaDTO transFormJavaDTO = convertParams(task);
        seatunnelConfigDTO.getTransform().add(transFormJavaDTO);
        log.info("Success to add Transform JavaScript Plugin");
    }

    private TransFormJavaDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        TransFormJavaDTO transFormJavaDTO = new TransFormJavaDTO();
        Map<String,Object> params = taskNode.getParams();
        try {
            transFormJavaDTO.setSourceCode(params.get("source_code").toString());
            transFormJavaDTO.setPluginInput(taskNode.getPreTaskcode());
            transFormJavaDTO.setPluginOutput(taskNode.getCode());
            transFormJavaDTO.setPluginName("JavaScript");
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return transFormJavaDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "JavaScript";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
