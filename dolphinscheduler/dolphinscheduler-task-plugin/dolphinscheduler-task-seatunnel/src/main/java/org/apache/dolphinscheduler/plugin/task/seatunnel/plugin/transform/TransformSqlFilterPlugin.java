package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormSqlDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlcondition.FieldTypeHandler;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlcondition.HandlerFactory;
import org.apache.dolphinscheduler.spi.enums.DbType;
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
public class TransformSqlFilterPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormSqlDTO transFormSqlDTO = convertParams(task);
        seatunnelConfigDTO.getTransform().add(transFormSqlDTO);
        log.info("Success to add Transform JsonPath Plugin");
    }

    private TransFormSqlDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        TransFormSqlDTO transFormSqlDTO = new TransFormSqlDTO();
        StringBuilder sql = new StringBuilder("SELECT * FROM dual WHERE ");

        Map<String,Object> params = taskNode.getParams();
        String databaseType = params.get("databaseType").toString();
        String quotation = getColumnQuotationByDbType(params.get("databaseType").toString());

        transFormSqlDTO.setPluginName("SQL");
        transFormSqlDTO.setPluginInput(taskNode.getPreTaskcode());
        transFormSqlDTO.setPluginOutput(taskNode.getCode());
        // propInit statusInit  valueInit  "id" ">=" "1985-08-22"
        String propInit = params.get("prop_init").toString();
        String statusInit = params.get("status_init").toString();
        String valueInit = params.get("value_init").toString();
        String typeInit = params.get("type_init").toString();

//        if ("boolean".equalsIgnoreCase(typeInit) || "bool".equalsIgnoreCase(typeInit)) {
//            statusInit = "IS";
//        }

        FieldTypeHandler handler = HandlerFactory.getHandler(DbType.valueOf(databaseType));
        String value1 = handler.handleValue(valueInit, typeInit.toLowerCase());

        sql.append(quotation).append(propInit).append(quotation).append(" ").append(statusInit).append(" ").append(value1);

        try {
            Object filterObj = params.get("field");
            if (filterObj instanceof List) {
                List<Map<String, Object>> filterList = (List<Map<String, Object>>) filterObj;
                for (int i = 0; i < filterList.size(); i++) {
                    Map<String, Object> item = filterList.get(i);
                    String prop = (String) item.get("prop");
                    prop = quotation + prop + quotation;
                    String status = (String) item.get("status");
                    String value = (String) item.get("value");
                    String type = (String) item.get("type");
                    // 连接符
                    String com = (String) item.get("com");

//                    if ("boolean".equalsIgnoreCase(type) || "bool".equalsIgnoreCase(type)) {
//                        status = "IS";
//                    }

                    // 处理值
                    String condition;
                    String value2 = handler.handleValue(value, type.toLowerCase());
//                    condition = String.format("%s %s '%s'", prop, status, value2);
                    condition = prop + " "+ status + " " + value2;
                    if (i >= 0 && com != null) {
                        sql.append(" ").append(com).append(" ");
                    }
                    sql.append(condition);
                    log.info("sql continue,now is :{} ",sql);
                }
            }
            log.info("-------------------------------------sql = {} ", sql.toString());
            transFormSqlDTO.setQuery(sql.toString());
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return transFormSqlDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "SQLFilter";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
