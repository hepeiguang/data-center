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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransformSqlFunctionDTO;
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
public class TransformSqlFunctionPlugin implements SqlTaskTypeFactory {

    private Object e;

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransformSqlFunctionDTO transformSqlFunctionDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getTransform().add(transformSqlFunctionDTO);
        log.info("Success to add transform Filter Plugin");
    }

    private TransformSqlFunctionDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        TransformSqlFunctionDTO transformSqlFunctionDTO = new TransformSqlFunctionDTO();
        transformSqlFunctionDTO.setPluginIn(taskNode.getPreTaskcode());
        transformSqlFunctionDTO.setPluginOutput(taskNode.getCode());
        transformSqlFunctionDTO.setPluginName("Sql");

        Map<String, Object> params = taskNode.getParams();
        log.debug("TransformSqlFunctionDTO params: {}", params);
        LinkedHashSet<String> outputFiles = new LinkedHashSet<>();
        if(taskNode.getOutputFields()!=null&&taskNode.getOutputFields().size()>0){
            outputFiles = taskNode.getOutputFields().get(0).getFields();
        }
        try {
            String columns = getSqlColumnStr(taskNode, CollectionUtils.isEmpty(outputFiles)? getOutputField(taskNode).getFields() : outputFiles);
            // 移除最后一个逗号并完成SQL构建
            transformSqlFunctionDTO.setQuery("select " + columns + " from dual");
        } catch (Exception e) {
            log.error("Failed to convert params for task node: {}", taskNode.getPluginName(), e);
        }
        return transformSqlFunctionDTO;
    }

    @Override
    public void processSqlSelectColumn(SeatunnelTaskNodeVO seatunnelTaskNodeVO, Map<String, String> colMap) {

        List<LinkedHashMap<String, Object>> columnLists = (List<LinkedHashMap<String, Object>>) seatunnelTaskNodeVO.getParams().get("columns");
        if (columnLists == null || columnLists.isEmpty()) {
            log.error("Parameter columns is empty. parameter: {}", JSONUtils.toJsonString(seatunnelTaskNodeVO));
            throw new RuntimeException("Parameter columns is empty.");
        }
        for (LinkedHashMap<String, Object> columnMap : columnLists) {
            String columnName = columnMap.get("column_name").toString();
            String newName = columnMap.get("new_name").toString();
            String functionCode = columnMap.get("function_code").toString();
            Object functionParam = columnMap.get("function_param");

            Map<String, Object> paramMap = new ObjectMapper().convertValue(functionParam, Map.class);
            StringBuilder paramBuilder = new StringBuilder();
            if(paramMap!=null&&!paramMap.isEmpty()) {
                for (Map.Entry<String, Object> entry : paramMap.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof String) {
                        if ("bDateAndTime".equals(entry.getKey())) {
                            paramBuilder.append("TO_DATE('").append(value).append("','yyyy-MM-dd HH:mm:ss'),");
                        } else {
                            paramBuilder.append("'").append(value).append("',");
                        }

                    } else {
                        paramBuilder.append(value).append(",");
                    }
                }
                colMap.put(newName, String.format("%s(%s,%s)", functionCode, columnName, paramBuilder.substring(0, paramBuilder.length() - 1)));
            }else{
                colMap.put(newName, String.format("%s(%s)", functionCode, columnName));
            }

        }
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
                String newName = (String) columnMap.get("new_name");
                files.add(newName);
            }
            outputFieldDTO.setFields(files);
        }

        return outputFieldDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "SqlFunction";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}
