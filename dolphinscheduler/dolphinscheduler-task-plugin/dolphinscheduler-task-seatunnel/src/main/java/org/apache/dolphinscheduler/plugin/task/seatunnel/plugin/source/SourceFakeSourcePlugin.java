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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceFakeSourceDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 源传输对象-虚拟数据源插件
 *
 * @author ZhaoYuan
 * @date 2025/5/5 18:56
 * @since JDK1.8
 */
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceFakeSourcePlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceFakeSourceDTO fakeSourceDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(fakeSourceDTO);
        log.info("Success to add Source FakeSource Plugin");
    }

    private SourceFakeSourceDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String,Object> params = taskNode.getParams();
        SourceFakeSourceDTO fakeSourceDTO = null;
        if (params == null) {
            fakeSourceDTO = new SourceFakeSourceDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                fakeSourceDTO = mapper.readValue(jsonString, SourceFakeSourceDTO.class);
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            }
        }

        //转化schema字段，必填
        Map<String, Object> schemaVoMap = JSONUtils.json2Map(fakeSourceDTO.getSchemaVo());
        Map<String, Object> schemaMap = new HashMap<>();
        schemaMap.put("fields", schemaVoMap);
        fakeSourceDTO.setSchema(schemaMap);
        fakeSourceDTO.setSchemaVo(null);
        //数组非空处理
        if (ObjectUtils.isNotEmpty(fakeSourceDTO.getStringTemplateVo())) {
            fakeSourceDTO.setStringTemplate(JSONUtils.parseObject(fakeSourceDTO.getStringTemplateVo(), new TypeReference<List<String>>() {}));
            fakeSourceDTO.setStringTemplateVo(null);
        }
        if (ObjectUtils.isNotEmpty(fakeSourceDTO.getTinyintTemplateVo())) {
            fakeSourceDTO.setTinyintTemplate(JSONUtils.parseObject(fakeSourceDTO.getTinyintTemplateVo(), new TypeReference<List<Integer>>() {}));
            fakeSourceDTO.setTinyintTemplateVo(null);
        }
        if (ObjectUtils.isNotEmpty(fakeSourceDTO.getSmallintTemplateVo())) {
            fakeSourceDTO.setSmallintTemplate(JSONUtils.parseObject(fakeSourceDTO.getSmallintTemplateVo(), new TypeReference<List<Integer>>() {}));
            fakeSourceDTO.setSmallintTemplateVo(null);
        }
        if (ObjectUtils.isNotEmpty(fakeSourceDTO.getIntTemplateVo())) {
            fakeSourceDTO.setIntTemplate(JSONUtils.parseObject(fakeSourceDTO.getIntTemplateVo(), new TypeReference<List<Integer>>() {}));
            fakeSourceDTO.setIntTemplateVo(null);
        }
        if (ObjectUtils.isNotEmpty(fakeSourceDTO.getBigintTemplateVo())) {
            fakeSourceDTO.setBigintTemplate(JSONUtils.parseObject(fakeSourceDTO.getBigintTemplateVo(), new TypeReference<List<Long>>() {}));
            fakeSourceDTO.setBigintTemplateVo(null);
        }
        if (ObjectUtils.isNotEmpty(fakeSourceDTO.getFloatTemplateVo())) {
            fakeSourceDTO.setFloatTemplate(JSONUtils.parseObject(fakeSourceDTO.getFloatTemplateVo(), new TypeReference<List<Float>>() {}));
            fakeSourceDTO.setFloatTemplateVo(null);
        }
        if (ObjectUtils.isNotEmpty(fakeSourceDTO.getDoubleTemplateVo())) {
            fakeSourceDTO.setDoubleTemplate(JSONUtils.parseObject(fakeSourceDTO.getDoubleTemplateVo(), new TypeReference<List<Double>>() {}));
            fakeSourceDTO.setDoubleTemplateVo(null);
        }

        fakeSourceDTO.setPluginOutput(taskNode.getCode());
        fakeSourceDTO.setPluginName("FakeSource");
        return fakeSourceDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "FakeSource";
    }


    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}