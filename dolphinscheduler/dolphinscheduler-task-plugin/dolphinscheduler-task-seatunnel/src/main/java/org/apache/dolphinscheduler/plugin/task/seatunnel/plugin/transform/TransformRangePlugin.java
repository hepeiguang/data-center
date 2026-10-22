package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormRangeDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormRangeItemDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Map;

/**
 * @program: 20250430
 * @author: 这里是你要写开发者姓名
 * @create: 20250430
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformRangePlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormRangeDTO transFormRangeDTO = convertParams(task);
        seatunnelConfigDTO.getTransform().add(transFormRangeDTO);
        log.info("Success to add Transform Range Plugin");
    }

    private TransFormRangeDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        TransFormRangeDTO transFormRangeDTO = null;
        Map<String,Object> params = taskNode.getParams();
        System.out.println("TransFormSqlDTO=====params=====:"+params);
        if (params != null) {
            try{
                transFormRangeDTO = new TransFormRangeDTO();
                transFormRangeDTO.setRangeType(params.get("range_type").toString());
                transFormRangeDTO.setRangeValueDefault(params.get("range_value_default").toString());
                transFormRangeDTO.setRangeField(params.get("range_field").toString());
                transFormRangeDTO.setRangeToField(params.get("range_to_field").toString());
                transFormRangeDTO.setRangeToFieldType(params.get("range_to_field_type").toString());
                transFormRangeDTO.setRangeMappings(JSONUtils.toList(JSONUtils.toJsonString(params.get("range_mappings")), TransFormRangeItemDTO.class));
                transFormRangeDTO.setPluginName(taskNode.getPluginName());
                transFormRangeDTO.setPluginInput(taskNode.getPreTaskcode());
                transFormRangeDTO.setPluginOutput(taskNode.getCode());
                log.info("Success to convertParams");
            }catch (Exception e){
                log.error("failed to convertParams", e);
            }
        }
        return transFormRangeDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "Range";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
