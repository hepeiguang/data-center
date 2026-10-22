package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormSequenceDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;

import java.util.ArrayList;
import java.util.Map;

@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformSequencePlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO seatunnelTaskNodeVO, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormSequenceDTO transFormSequenceDTO = convertParams(seatunnelTaskNodeVO, taskExecutionContext);
        seatunnelConfigDTO.getTransform().add(transFormSequenceDTO);
        log.info("Success to add TransformSequencePlugin");
    }

    public TransFormSequenceDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        TransFormSequenceDTO transFormSequenceDTO = new TransFormSequenceDTO();
        Map<String,Object> params = taskNode.getParams();
        try {
            transFormSequenceDTO.setFieldName(params.get("field_name").toString());
            if(params.get("start_value") == null){
                log.info("start_value is null,default value 1");
            }else{
                transFormSequenceDTO.setStartValue(params.get("start_value").toString());
            }
            if(params.get("add_value") == null){
                log.info("add_value is null,default value 1");
            }else{
                transFormSequenceDTO.setAddValue(params.get("add_value").toString());
            }
            if(params.get("max_value") == null){
                log.info("max_value is null,Default unlimited");
            }else{
                transFormSequenceDTO.setMaxValue(params.get("max_value").toString());
            }
            transFormSequenceDTO.setPluginName(taskNode.getPluginName());
            transFormSequenceDTO.setPluginInput(taskNode.getPreTaskcode());
            transFormSequenceDTO.setPluginOutput(taskNode.getCode());
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return transFormSequenceDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "SEQUENCE";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
