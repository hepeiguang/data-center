package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SqlTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransformConstReferenceDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;

import java.util.*;

import static org.apache.dolphinscheduler.common.constants.Constants.LOCAL_PARAMS;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformConstReferencePlugin implements SqlTaskTypeFactory {

    private Object e;
    private final String GLOBAL_PARAMETERS = "1";//全局参数
    private final String LOCAL_PARAMETERS = "2";//局部参数
    private final String DEFAULT_PARAMETERS = "3";//系统参数

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransformConstReferenceDTO transformConstReferenceDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getTransform().add(transformConstReferenceDTO);
        log.info("Success to add transform Filter Plugin");
    }

private TransformConstReferenceDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
    TransformConstReferenceDTO transformConstReferenceDTO = new TransformConstReferenceDTO();
    transformConstReferenceDTO.setPluginIn(taskNode.getPostTaskcode());
    transformConstReferenceDTO.setPluginOutput(taskNode.getCode());
    transformConstReferenceDTO.setPluginName("Sql");
    Map<String, Object> params = taskNode.getParams();
    String fieldName = params.get("field_name").toString();
    LinkedHashSet<String> outputFiles = new LinkedHashSet<>();
    if(taskNode.getOutputFields()!=null&&taskNode.getOutputFields().size()>0){
        outputFiles = taskNode.getOutputFields().get(0).getFields();
    }
    try {
        String columns = getSqlColumnStr(taskNode, CollectionUtils.isEmpty(taskNode.getOutputFields()) ? getOutputField(taskNode).getFields() : outputFiles);
        String values = getSetValue(taskNode, taskExecutionContext);
        StringBuilder sqlBuilder = new StringBuilder("select ");
        sqlBuilder.append(columns +",'" + values + "' as " + fieldName);
        String sql = sqlBuilder + " from dual ";
        log.info("-------------------------------Sql: {}", sql);
        transformConstReferenceDTO.setQuery(sql);
    } catch (Exception e) {
        log.error("Failed to convert params for transformConstReference node: {}", taskNode.getPluginName(), e);
    }
    return transformConstReferenceDTO;
}
    // 提取公共查找逻辑
    private String findPropertyValue(List<Property> propertyList, String name, String defaultValue) {
        for (Property property : propertyList) {
            if (property.getProp().equals(name)) {
                return property.getValue();
            }
        }
        return defaultValue;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "ConstReference";
    }

    private String getSetValue(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext){
        Map<String, Object> params = taskNode.getParams();
        Object variableObj = params.get("variable");
        Map<String, Object> variables = (Map<String, Object>) variableObj;
        String typeStr = variables.get("type").toString();
        String name = variables.get("name").toString();
        String values = variables.get("value").toString();
        if (GLOBAL_PARAMETERS.equals(typeStr)) {
            List<Property> propertyList = JSONUtils.toList(taskExecutionContext.getGlobalParams(), Property.class);
            values = findPropertyValue(propertyList, name, values);
        } else if (LOCAL_PARAMETERS.equals(typeStr)) {
            String definedParams = taskExecutionContext.getTaskParams();
            String localParams = JSONUtils.getNodeString(definedParams, LOCAL_PARAMS);
            if (!StringUtils.isEmpty(localParams)) {
                List<Property> localParamsList = JSONUtils.toList(localParams, Property.class);
                values = findPropertyValue(localParamsList, name, values);
            }
        } else if (DEFAULT_PARAMETERS.equals(typeStr)) {
            Map<String, Property> prepareParamsMap = taskExecutionContext.getPrepareParamsMap();
            for (Map.Entry<String, Property> entry : prepareParamsMap.entrySet()) {
                if (entry.getKey().equals(values)) {
                    values = entry.getValue().getValue();
                    break;
                }
            }
        }
        return values;
    }


    @Override
    public OutputFieldDTO getOutputField(SeatunnelTaskNodeVO seatunnelTaskNodeVO) {
        List<OutputFieldDTO> outputFields = seatunnelTaskNodeVO.getInputFields();
        OutputFieldDTO outputFieldDTO = new OutputFieldDTO(OutputFieldDTO.Type.OTHER);
        if (outputFields != null && outputFields.size() > 0) {
            outputFieldDTO = outputFields.get(0);
        }
        Map<String, Object> params = seatunnelTaskNodeVO.getParams();
        String fieldName = params.get("field_name").toString();
        LinkedHashSet<String> fields = outputFieldDTO.getFields();
        if (fields != null) {
            fields.add(fieldName);
        }
        outputFieldDTO.setFields(fields);
        return outputFieldDTO;
    }

    @Override
    public void processSqlSelectColumn(SeatunnelTaskNodeVO seatunnelTaskNodeVO, Map<String, String> colMap) {
        Map<String, Object> params = seatunnelTaskNodeVO.getParams();
        String fieldName = params.get("field_name").toString();
        colMap.remove(fieldName);
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}
