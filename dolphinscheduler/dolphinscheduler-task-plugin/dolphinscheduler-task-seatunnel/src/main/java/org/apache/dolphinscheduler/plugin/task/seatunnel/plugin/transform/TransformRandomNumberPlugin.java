package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormRandomNumberDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;

import java.util.ArrayList;
import java.util.Map;

@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformRandomNumberPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO seatunnelTaskNodeVO, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormRandomNumberDTO transFormRandomNumberDTO = convertParams(seatunnelTaskNodeVO, taskExecutionContext);
        seatunnelConfigDTO.getTransform().add(transFormRandomNumberDTO);
        log.info("Success to add TransformRandomNumberPlugin Sql Plugin");
    }

    public TransFormRandomNumberDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        TransFormRandomNumberDTO transFormRandomNumberDTO = new TransFormRandomNumberDTO();
        Map<String,Object> params = taskNode.getParams();
        try {
            String uniqueField = params.get("field_name").toString();
            // max_number 大于 min_number 执行
            if (Integer.parseInt(params.get("max_number").toString()) < Integer.parseInt(params.get("min_number").toString())) {
                log.error("The maximum value is smaller than the minimum value");
            }

            String str = String.format(" CEIL(RAND() * (%s - %s) + %s) as %s,",
                    params.get("max_number").toString(),
                    params.get("min_number").toString(),
                    params.get("min_number").toString(),
                    uniqueField);
            transFormRandomNumberDTO.setQuery("select "+ str + " dual.* from dual");
            transFormRandomNumberDTO.setPluginName("SQL");
            transFormRandomNumberDTO.setPluginInput(taskNode.getPreTaskcode());
            transFormRandomNumberDTO.setPluginOutput(taskNode.getCode());
            log.error("------------------------sql:{} select dual.* from dual", str);
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return transFormRandomNumberDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "RANDOMNUMBER";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
