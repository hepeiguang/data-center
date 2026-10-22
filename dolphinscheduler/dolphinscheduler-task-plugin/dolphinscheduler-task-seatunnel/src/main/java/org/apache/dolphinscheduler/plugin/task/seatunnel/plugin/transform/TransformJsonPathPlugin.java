package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormJsonPathColumnDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormJsonPathDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.RowErrorHandleWayEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformJsonPathPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        //
        TransFormJsonPathDTO transFormJsonPathDTO = convertParams(task);
        seatunnelConfigDTO.getTransform().add(transFormJsonPathDTO);
        log.info("Success to add Transform JsonPath Plugin");
    }

    private TransFormJsonPathDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        TransFormJsonPathDTO transFormJsonPathDTO = new TransFormJsonPathDTO();
        Map<String,Object> params = taskNode.getParams();
        try {
            transFormJsonPathDTO.setRowErrorHandleWay(RowErrorHandleWayEnum.fromString(params.get("row_error_handle_way").toString()));
            String jsonString = JSONUtils.toJsonString(params.get("columns"));
            List<TransFormJsonPathColumnDTO> list = JSONUtils.toList(jsonString, TransFormJsonPathColumnDTO.class);
            transFormJsonPathDTO.setColumns(list);
            transFormJsonPathDTO.setPluginInput(taskNode.getPreTaskcode());
            transFormJsonPathDTO.setPluginOutput(taskNode.getCode());
            transFormJsonPathDTO.setPluginName(taskNode.getPluginName());
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return transFormJsonPathDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "JSONPATH";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
