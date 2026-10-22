package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SqlTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
//import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransformDataNormalizeDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransformDataNormalizeDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformDataNormalizePlugin implements TaskTypeFactory {

    private Object e;

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransformDataNormalizeDTO transformDataNormalizeDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getTransform().add(transformDataNormalizeDTO);
        log.info("Success to add transform Filter Plugin");
    }

    private TransformDataNormalizeDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        String apiUrl = "";
        TransformDataNormalizeDTO transformDataNormalizeDTO = new TransformDataNormalizeDTO();
        transformDataNormalizeDTO.setPluginInput(taskNode.getPreTaskcode());
        transformDataNormalizeDTO.setPluginOutput(taskNode.getCode());
        transformDataNormalizeDTO.setPluginName("Dict");

//        transformDataNormalizeDTO.setApiUrl("http://192.168.1.80:9528/api/data/standard/dataItem/page?current=1&size=20&postName=&refTopicId=&orders%5B0%5D.asc=false&orders%5B0%5D.column=id");
//        transformDataNormalizeDTO.setApiKey("Bearer eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJhZG1pbiIsInRlbmFudElkIjoiLTEiLCJleHAiOjE3NTAyNDc3NDksImlhdCI6MTc1MDE2MTM0OX0.QMcCmwsyw09OEZHKf1fhrd36fnJx-2YehVMTObx-DL2CcxyK339M1X2amCMF_koFgdVtrsEwRAPPJ1I1mA7-Zw");
        Map<String, Object> params = taskNode.getParams();
        log.debug("TransformDataNormalizeDTO columnLists: {}", params);
        Set<String> outputFiles = new HashSet<>();
        if(taskNode.getOutputFields()!=null&&taskNode.getOutputFields().size()>0){
            outputFiles = taskNode.getOutputFields().get(0).getFields();
        }
        try {
            List<LinkedHashMap<String, Object>> columnLists = (List<LinkedHashMap<String, Object>>) taskNode.getParams().get("columns");
            List<LinkedHashMap<String, Object>> columnNewLists = new ArrayList<>();
            log.info("TransformDataNormalizeDTO columnLists: {}", columnLists);
            if (columnLists == null || columnLists.isEmpty()) {
                log.error("Parameter columns is empty. parameter: {}", JSONUtils.toJsonString(taskNode));
                throw new RuntimeException("Parameter columns is empty.");
            }
            for (LinkedHashMap<String, Object> columnMap : columnLists) {
                LinkedHashMap<String, Object> columnNewMap = new LinkedHashMap<>();
                columnNewMap.put("source_field",columnMap.get("column_name")!=null?columnMap.get("column_name"):"");
                columnNewMap.put("max_num",columnMap.get("max_num")!=null?columnMap.get("max_num"):"");
                columnNewMap.put("min_num",columnMap.get("min_num")!=null?columnMap.get("min_num"):"");
                columnNewMap.put("num_accuracy",columnMap.get("num_accuracy")!="0"?columnMap.get("num_accuracy"):"");
                columnNewMap.put("time_type",columnMap.get("time_type")!=null?columnMap.get("time_type"):"");
                columnNewMap.put("dict_code",columnMap.get("dict_code")!=null?columnMap.get("dict_code"):"");
                columnNewMap.put("dict_name",columnMap.get("dict_name")!=null?columnMap.get("dict_name"):"");
                columnNewMap.put("dict_type",columnMap.get("dict_type")!=null?columnMap.get("dict_type"):"");
                apiUrl =columnMap.get("api_url")!=null?columnMap.get("api_url").toString():"";
                columnNewLists.add(columnNewMap);
            }
            transformDataNormalizeDTO.setApiUrl(apiUrl);
            transformDataNormalizeDTO.setSourceFields(columnNewLists);
        } catch (Exception e) {
            log.error("Failed to convert params for task node: {}", taskNode.getPluginName(), e);
        }
        return transformDataNormalizeDTO;
    }



    @Override
    public OutputFieldDTO getOutputField(SeatunnelTaskNodeVO seatunnelTaskNodeVO) {
        List<LinkedHashMap<String, Object>> columnLists = (List<LinkedHashMap<String, Object>>) seatunnelTaskNodeVO.getParams().get("columns");
        OutputFieldDTO outputFieldDTO = new OutputFieldDTO(OutputFieldDTO.Type.OTHER);
        List<OutputFieldDTO> outputFields = seatunnelTaskNodeVO.getInputFields();
        if(outputFields!=null&&outputFields.size()>0){
            outputFieldDTO =  outputFields.get(0);
            LinkedHashSet<String> files = outputFieldDTO.getFields();
            for (LinkedHashMap<String, Object> columnMap : columnLists) {
                String newName = (String) columnMap.get("column_name");
                files.add(newName);
            }
            outputFieldDTO.setFields(files);
        }

        return outputFieldDTO;
    }


    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "DataNormalize";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }


}
