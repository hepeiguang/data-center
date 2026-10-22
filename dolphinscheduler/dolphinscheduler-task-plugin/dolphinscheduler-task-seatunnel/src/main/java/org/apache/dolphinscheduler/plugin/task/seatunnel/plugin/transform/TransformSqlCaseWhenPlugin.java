package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SqlTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormSqlCaseWhenColumnDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormSqlFunctionsDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformSqlCaseWhenPlugin implements SqlTaskTypeFactory {
    private final static List<String> SQL_NUMBER_FIELD_TYPE = Arrays.asList("TINYINT", "SMALLINT", "MEDIUMINT", "INT", "BIGINT", "DECIMAL", "NUMERIC", "FLOAT", "DOUBLE");
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO seatunnelTaskNodeVO, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormSqlFunctionsDTO transFormSqlFunctionsDTO = convertParams(seatunnelTaskNodeVO, taskExecutionContext);
        seatunnelConfigDTO.getTransform().add(transFormSqlFunctionsDTO);
        log.info("Success to add TransformSqlFunctionsPlugin Sql Plugin");
    }

    public TransFormSqlFunctionsDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        TransFormSqlFunctionsDTO transFormSqlFunctionsDTO = new TransFormSqlFunctionsDTO();
        Map<String, Object> params = taskNode.getParams();
        try {
            String tablePrefix = getTablePrefix(taskExecutionContext, taskNode);
            String quotation = getColumnQuotation(taskExecutionContext, taskNode);
            // 目标字段
            String targetField = params.getOrDefault("targetField", "").toString();
            if (StringUtils.isEmpty(targetField)) {
                throw new RuntimeException("targetField is empty");
            }
            String jsonString = JSONUtils.toJsonString(params.get("columns"));
//            log.info("jsonString:{}", jsonString);
            List<TransFormSqlCaseWhenColumnDTO> columnsJsonList = JSONUtils.toList(jsonString, TransFormSqlCaseWhenColumnDTO.class);
            if (columnsJsonList.isEmpty()) {
                throw new RuntimeException("columns is empty");
            }
            String useColumnName = columnsJsonList.get(0).getColumnName();
            StringBuilder caseStr = new StringBuilder(" CASE ");
            /**
             * CASE
             *     WHEN {0} {1} {2} THEN {3}
             *     WHEN sex  =  2   THEN '女'
             *   END
             */
            String caseWhenTemp = " when {0} {1} {2} then {3} ";

            // 字段类型 和 数据库类型
            String filedType = params.getOrDefault("fieldType", "").toString().toLowerCase();
            String databaseType = params.get("databaseType").toString();

            for (TransFormSqlCaseWhenColumnDTO columnObj : columnsJsonList) {
                String expression = columnObj.getExpression();
                String srcValue = columnObj.getSrcValue();
                String desValue = columnObj.getDesValue();

//                // 判定fieldType  是否是数字类型,如果不是数字，则需要前后加引号
//                log.info("-----------------FieldType:{}", columnObj.getFieldType());
//                if(!SQL_NUMBER_FIELD_TYPE.contains(columnObj.getFieldType())){
//                    srcValue = "'" + srcValue + "'";
//                }
//                caseStr += MessageFormat.format(caseWhenTemp, useColumnName, expression, srcValue, desValue);
                // todo 等待测试
                String srcValueCast = getColumnCastResult(databaseType, srcValue, filedType);
                String desValueCast = getColumnCastResult(databaseType, desValue, filedType);
                caseStr.append(MessageFormat.format(caseWhenTemp,
                        quotation+useColumnName+quotation,
                        expression,
                        srcValueCast,
                        desValueCast));
                // when {0} {1} {2} then {3}
                log.info("caseStr first part is {}",caseStr);
            }
            // todo 等待测试
            String defaultValue = taskNode.getParams().getOrDefault("defaultValue", "").toString();
//            String defaultValueResult = getColumnCastResult(databaseType, defaultValue, filedType);
            if (StringUtils.isNotBlank(defaultValue)) {
                String desValueCast = getColumnCastResult(databaseType, defaultValue, filedType);
                caseStr.append(" ELSE ").append(desValueCast).append(" ");
            }
            caseStr.append(" END as ").append(quotation).append(targetField).append(quotation).append(" ");
            // 获取表的所有字段,并将制定字段替换成case when
            String allColumnStr = getAllColumnStr(taskNode);
            if (StringUtils.isBlank(allColumnStr)) {
                throw new RuntimeException("allColumnStr is empty");
            }
            List<String> columnNameList = Arrays.stream(allColumnStr.split(",")).collect(Collectors.toList());
            columnNameList = columnNameList.stream()
                    .map(col -> {
                        if (col.equals(useColumnName)) {
                            return caseStr.toString(); // CASE WHEN不加引号
                        }
                        return quotation + col + quotation; // 其他列加引号
                    })
                    .collect(Collectors.toList());
            String sql = "select " + String.join(",", columnNameList) + " from dual ";
//            StringBuilder sb = new StringBuilder("select ");
//            for (int i = 0; i < columnNameList.size(); i++) {
//                sb.append(quotation).append(columnNameList.get(i)).append(quotation); // 添加双引号
//                if (i < columnNameList.size() - 1) {
//                    sb.append(",");
//                }
//            }
//            sb.append(" from dual");
//            String sql = sb.toString();
            transFormSqlFunctionsDTO.setQuery(sql);
            log.info("-------------------------------------sql = {} ", sql);
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }


        transFormSqlFunctionsDTO.setPluginInput(taskNode.getPreTaskcode());
        transFormSqlFunctionsDTO.setPluginOutput(taskNode.getCode());
        transFormSqlFunctionsDTO.setPluginName("SQL");

        return transFormSqlFunctionsDTO;
    }




    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "SQLCASEWHEN";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.TRANSFORM_SQLCASEWHEN;
    }

    @Override
    public void processSqlSelectColumn(SeatunnelTaskNodeVO seatunnelTaskNodeVO, Map<String, String> colMap) {

    }


}
