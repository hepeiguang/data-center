package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormSqlDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;

import java.util.ArrayList;
import java.util.Map;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformSqlPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO seatunnelTaskNodeVO, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormSqlDTO transFormSqlDTO = convertParams(seatunnelTaskNodeVO, taskExecutionContext);
        seatunnelConfigDTO.getTransform().add(transFormSqlDTO);
        log.info("Success to add Transform Sql Plugin");
    }

    public TransFormSqlDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        TransFormSqlDTO transFormSqlDTO = new TransFormSqlDTO();
        Map<String, Object> params = taskNode.getParams();
        System.out.println("TransFormSqlDTO=====params=====:"+params);
        transFormSqlDTO.setPluginName(taskNode.getPluginName());
        transFormSqlDTO.setPluginInput(taskNode.getPreTaskcode());
        transFormSqlDTO.setPluginOutput(taskNode.getCode());
        if (taskNode.getParams() != null) {
            try {
                //transFormSqlDTO.setQuery(taskNode.getParams().get("query").toString());
                // query 需要去除格式化
                transFormSqlDTO.setQuery(taskNode.getParams().get("query").toString().replaceAll("\n", " "));
                log.info("Success to convertParams");
            } catch (Exception e) {
                log.error("failed to convertParams", e);
                throw new RuntimeException(e);
            }
        }
        return transFormSqlDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "SQL";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
