package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceLocalfileDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 源传输对象-本地文件
 *
 * @author ZhaoYuan
 * @date 2025/5/1 17:00
 * @since JDK1.8
 */
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceLocalfilePlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceLocalfileDTO localFileDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(localFileDTO);
        log.info("Success to add Source LocalFile Plugin");
    }

    private SourceLocalfileDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String,Object> params = taskNode.getParams();
        SourceLocalfileDTO localFileDTO = null;
        if (params == null) {
            localFileDTO = new SourceLocalfileDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                localFileDTO = mapper.readValue(jsonString, SourceLocalfileDTO.class);
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            }
        }
        //转化schema字段
        if (ObjectUtils.isNotEmpty(localFileDTO.getSchemaVo())) {
            Map<String, Object> schemaVoMap = JSONUtils.json2Map(localFileDTO.getSchemaVo());
            Map<String, Object> schemaMap = new HashMap<>();
            schemaMap.put("fields", schemaVoMap);
            localFileDTO.setSchema(schemaMap);
            localFileDTO.setSchemaVo(null);
        }

        localFileDTO.setPluginOutput(taskNode.getCode());
        localFileDTO.setPluginName("LocalFile");
        return localFileDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "LocalFile";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}