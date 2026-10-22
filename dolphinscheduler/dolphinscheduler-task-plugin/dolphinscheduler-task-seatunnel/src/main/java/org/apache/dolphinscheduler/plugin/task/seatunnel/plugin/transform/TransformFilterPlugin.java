package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransformFilterDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformFilterPlugin implements TaskTypeFactory {

    private Object e;

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransformFilterDTO transformFilterDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getTransform().add(transformFilterDTO);
        log.info("Success to add transform Filter Plugin");
    }

    private TransformFilterDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String, Object> params = taskNode.getParams();
        System.out.println("TransformFilterDTO：params===="+params);
        TransformFilterDTO transformFilterDTO = new TransformFilterDTO();

        try {
            List<LinkedHashMap<String, Object>> fieldLists = (List<LinkedHashMap<String, Object>>) params.get("include_fields");
            List<Object> fields = fieldLists.stream()
                    .map(map -> map.get("field")) // 提取指定键的值
                    .collect(Collectors.toList());
            transformFilterDTO.setIncludeFields(fields);
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        transformFilterDTO.setPluginIn(taskNode.getPreTaskcode());
        transformFilterDTO.setPluginOutput(taskNode.getCode());
        transformFilterDTO.setPluginName(taskNode.getPluginName());
        return transformFilterDTO;
    }
    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "Filter";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

    @Override
    public OutputFieldDTO getOutputField(SeatunnelTaskNodeVO taskNode) {
        List<OutputFieldDTO> inputFields = taskNode.getInputFields();
        OutputFieldDTO.Type type = OutputFieldDTO.Type.OTHER;
        String tableName="";
        if(inputFields!=null&&inputFields.size()==1){
            type = inputFields.get(0).getType();
            tableName = inputFields.get(0).getTableName();
        }
        OutputFieldDTO outputFieldDTO = new OutputFieldDTO();
        outputFieldDTO.setType(type);
        outputFieldDTO.setTableName(tableName);
        Map<String, Object> params = taskNode.getParams();
        log.info("TransformFilterDTO：params:{}",params);
        try {
            List<LinkedHashMap<String, Object>> fieldLists = (List<LinkedHashMap<String, Object>>) params.get("include_fields");
            log.info("TransformFilterDTO：fieldLists:{}",fieldLists);
            LinkedHashSet<String> fieldSet = fieldLists.stream()
                    .map(map -> map.get("field")) // 提取每个 map 中的 "field" 值
                    .filter(Objects::nonNull)     // 过滤掉 null 值
                    .map(Object::toString)        // 统一转为 String 类型
                    .collect(Collectors.toCollection(LinkedHashSet::new)); // 收集为 Set
            outputFieldDTO.setFields(fieldSet);
            log.info("TransformFilterDTO：fieldSet:{}",fieldSet);
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return outputFieldDTO;
    }
//    public static void main(String[] args) {
//        LinkedHashSet<String> outputFiles = new LinkedHashSet<>(Arrays.asList("id", "name", "age"));
//        Map<String, String> colMap = outputFiles.stream()
//                .collect(Collectors.toMap(
//                        key -> key,       // Map 的键为集合中的元素
//                        value -> ""       // Map 的值统一为空字符串
//                ));
//        System.out.println(colMap);
//    }
}
