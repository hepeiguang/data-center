package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.collections.CollectionUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SqlTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.AggregationColumn;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.GroupByColumn;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserBuilder;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserUtil;

import java.util.*;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformGroupPlugin implements SqlTaskTypeFactory {

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        log.info("Success to add transform Filter Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "Group";
    }


    @Override
    public OutputFieldDTO getOutputField(SeatunnelTaskNodeVO seatunnelTaskNodeVO) {
        List<OutputFieldDTO> outputFields = seatunnelTaskNodeVO.getInputFields();
        OutputFieldDTO outputFieldDTO = new OutputFieldDTO(OutputFieldDTO.Type.OTHER);
        try {
            if (outputFields != null && outputFields.size() > 0) {
                outputFieldDTO = outputFields.get(0);
            }
            LinkedHashSet<String> fields = new LinkedHashSet<>();
            List<LinkedHashMap<String, Object>> groupFields = (List<LinkedHashMap<String, Object>>) seatunnelTaskNodeVO.getParams().get("aggregation_fields");
            if (!CollectionUtils.isEmpty(groupFields)) {
                for (LinkedHashMap<String, Object> filed : groupFields) {
                    String functionName = filed.get("aggregation_name").toString();
                    fields.add(functionName);
                }
                outputFieldDTO.setFields(fields);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return outputFieldDTO;
    }

    @Override
    public void processSqlSelectColumn(SeatunnelTaskNodeVO seatunnelTaskNodeVO, Map<String, String> colMap) {

    }

    @Override
    public Select getCustomerSqlSelect(Select select, SeatunnelTaskNodeVO seatunnelTaskNodeVO,TaskExecutionContext taskExecutionContext) {

        Map<String, Object> params = seatunnelTaskNodeVO.getParams();
        String tableName = params.get("table_name").toString();
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        log.info("-----------------------------plainSelect: {}", plainSelect);
        List<LinkedHashMap<String, Object>> groupFields = (List<LinkedHashMap<String, Object>>) seatunnelTaskNodeVO.getParams().get("aggregation_fields");
        if (groupFields == null || groupFields.isEmpty()) {
            log.error("Parameter groupFields is empty. parameter: {}", JSONUtils.toJsonString(seatunnelTaskNodeVO));
            throw new RuntimeException("Parameter groupFields is empty.");
        }
        // 解析SQL
        try {
            List<String> Columns = (List<String>) params.get("group_fields");
            List<GroupByColumn> groupByColumns =  new ArrayList<>();
            for (String column : Columns) {
                groupByColumns.add(new GroupByColumn(tableName, column));
            }
            List<AggregationColumn> aggColumns = new ArrayList<>();
            for (LinkedHashMap<String, Object> filed : groupFields) {
                String func = filed.get("aggregation_type").toString();
                String column = filed.get("field_name").toString();
                String alias = filed.get("aggregation_name").toString();
                aggColumns.add(new AggregationColumn(func,tableName,column,alias));
            }
            plainSelect = SqlParserBuilder.withSelect(plainSelect).addGroupBy(groupByColumns, aggColumns).build();
            log.info("SeaTunnel task custom sql selectStatement : {}", select.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        select.setSelectBody(plainSelect);
        return select;
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.TRANSFORM_GROUP;
    }

}
