package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkHiveDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Map;

/**
 * Sink对象-Hive插件
 *
 * @author ZhaoYuan
 * @date 2025/5/14 21:47
 * @since JDK1.8
 */
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SinkHivePlugin implements TaskTypeFactory {

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkHiveDTO hiveDTO = convertParams(task);
        seatunnelConfigDTO.getSink().add(hiveDTO);
        log.info("Success to add Sink Hive Plugin");
    }

    private SinkHiveDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        SinkHiveDTO hiveDTO = new SinkHiveDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                String jsonString = mapper.writeValueAsString(params);
                hiveDTO = mapper.readValue(jsonString, SinkHiveDTO.class);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        //处理List类数据字段
        if (ObjectUtils.isNotEmpty(hiveDTO.getHiveHadoopConfVo())) {
            hiveDTO.setHiveHadoopConf(JSONUtils.parseObject(hiveDTO.getHiveHadoopConfVo(), new TypeReference<Map<String, Object>>() {}));
            hiveDTO.setHiveHadoopConfVo(null);
        }

        hiveDTO.setPluginInput(taskNode.getPreTaskcode());
        hiveDTO.setPluginName("Hive");
        hiveDTO.setParallelism(1);
        return hiveDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "Hive";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}