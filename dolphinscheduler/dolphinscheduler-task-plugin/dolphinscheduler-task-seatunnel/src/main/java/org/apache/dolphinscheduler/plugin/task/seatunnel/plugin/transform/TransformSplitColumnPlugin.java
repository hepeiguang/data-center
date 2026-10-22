package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormSplitColumnDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;

import java.util.ArrayList;
import java.util.Map;

/**
 * @Author： 王涛
 * @DATE： 2025/5/14 10:09
 */
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformSplitColumnPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO seatunnelTaskNodeVO, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormSplitColumnDTO transFormSplitColumnsDTO = convertParams(seatunnelTaskNodeVO, taskExecutionContext);
        seatunnelConfigDTO.getTransform().add(transFormSplitColumnsDTO);
        log.info("Success to add TransformSqlFunctionsPlugin Sql Plugin");
    }

    public TransFormSplitColumnDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String, Object> params = taskNode.getParams();
        TransFormSplitColumnDTO transFormSplitColumnsDTO = new TransFormSplitColumnDTO();
        try {
            String jsonString = JSONUtils.toJsonString(params);
            log.info("jsonString:{}", jsonString);
            transFormSplitColumnsDTO = JSONUtils.parseObject(jsonString, TransFormSplitColumnDTO.class);
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        if (transFormSplitColumnsDTO != null) {
            transFormSplitColumnsDTO.setPluginInput(taskNode.getPreTaskcode());
            transFormSplitColumnsDTO.setPluginOutput(taskNode.getCode());
            transFormSplitColumnsDTO.setPluginName("SplitColumn");
        }
        return transFormSplitColumnsDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "SPLITCOLUMNS";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
