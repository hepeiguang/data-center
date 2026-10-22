package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.aliyun.oss.common.utils.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SqlTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.ConnectParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkJdbcDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.JoinInfo;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.JoinType;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlAliasResolver;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserBuilder;
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
public class TransformSqlJoinPlugin implements SqlTaskTypeFactory {

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        log.info("Success to add transform SQLJoin Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "SQLJoin";
    }


    @Override
    public OutputFieldDTO getOutputField(SeatunnelTaskNodeVO seatunnelTaskNodeVO) {
        OutputFieldDTO outputFieldDTO = new OutputFieldDTO(OutputFieldDTO.Type.TABLE);
        log.info("SeaTunnel task join seatunnelTaskNodeVO : {}", seatunnelTaskNodeVO);
        LinkedHashSet<String> distinctFields = new LinkedHashSet<>();
        outputFieldDTO.setTaskName(seatunnelTaskNodeVO.getName());
        List<LinkedHashMap<String, String>> fieldList =
                (List<LinkedHashMap<String, String>>) seatunnelTaskNodeVO.getParams().get("select_fields");
        if (fieldList != null && !fieldList.isEmpty()) {
            for (LinkedHashMap<String, String> filed : fieldList) {
                // 拿到数据类型
                String dataType = filed.get("data_type");
                String tableName = filed.get("table_name");
                outputFieldDTO.setTableName(tableName);
                String fieldName = filed.get("column_name");
                String newName = filed.get("new_name");
                if (StringUtils.isNullOrEmpty(newName)) {
                    distinctFields.add(fieldName);
                } else {
                    distinctFields.add(newName);
                }
            }
        }
        outputFieldDTO.setFields(distinctFields);
        return outputFieldDTO;
    }

    @Override
    public void processSqlSelectColumn(SeatunnelTaskNodeVO seatunnelTaskNodeVO, Map<String, String> colMap) {

    }

    @Override
    public Select getCustomerSqlSelect(Select select, SeatunnelTaskNodeVO seatunnelTaskNodeVO,TaskExecutionContext taskExecutionContext) {
        log.info("select : {}", select);
        try {
            Map<String, Object> params = seatunnelTaskNodeVO.getParams();
            log.info("params : {}", params);
            String leftSql = params.get("left_sql").toString();
            String rightSql = params.get("right_sql").toString();
            String joinType = params.get("join_type").toString();
            List<Map<String, String>> relation = (List<Map<String, String>>) params.get("relation");
            List<Map<String, String>> leftOutputFields = (List<Map<String, String>>) params.get("select_fields_left");
            List<Map<String, String>> rightOutputFields = (List<Map<String, String>>) params.get("select_fields_right");

            if (leftOutputFields.isEmpty() && rightOutputFields.isEmpty()) {
                log.error("输出字段不能为空: {}", "select_fields_left and select_fields_right both empty");
                throw new RuntimeException("select_fields_left and select_fields_right both empty");
            }
            String leftAlias = SqlAliasResolver.getTableAlias();
            String rightAlias = SqlAliasResolver.getTableAlias();

            String whereSql = getWhereSql(relation, leftAlias, rightAlias);
            if (whereSql == null) {
                log.error("relation is invalid： {}","leftColumns size is not equal to rightColumns size");
                throw new RuntimeException("leftColumns size is not equal to rightColumns size");
            }
            String outColumns = getOutColumns(leftOutputFields, rightOutputFields, leftAlias, rightAlias);
            //将两个连接的表字段进行合并
            String sql = "select " + outColumns + " from (" + leftSql + ") " + leftAlias + " " + joinType + " join (" + rightSql + ") " + rightAlias + " on " + whereSql;
            log.info("SeaTunnel task join sql : {}", sql);
            select = (Select) CCJSqlParserUtil.parse(sql);
            log.info("-----------------------------plainSelect: {}", select);
        } catch (Exception e) {
            log.error("TransformSqlJoinPlugin task exception: {}", e);
            e.printStackTrace();
        }
        return select;
    }

    public String getWhereSql (List<Map<String, String>> relation, String leftAlias, String rightAlias) {
        List<String> leftColumns = relation.stream()
                .map(map -> map.get("left_column"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        List<String> rightColumns = relation.stream()
                .map(map -> map.get("right_column"))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (leftColumns.size() != rightColumns.size()) {
            return null;
        }
        StringBuilder whereSql = new StringBuilder();
        for (int i = 0; i < leftColumns.size(); i++) {
            if (i != 0) {
                whereSql.append(" and ");
            }
            whereSql
                    .append(leftAlias)
                    .append(".")
                    .append(leftColumns.get(i))
                    .append(" = ")
                    .append(rightAlias)
                    .append(".")
                    .append(rightColumns.get(i))
            ;
        }
        return whereSql.toString();
    }

    public String getOutColumns (List<Map<String, String>> leftOutputFields,
                                 List<Map<String, String>> rightOutputFields,
                                 String leftAlias,
                                 String rightAlias) {
        List<String> leftColumnList = new ArrayList<>();
        List<String> rightColumnList = new ArrayList<>();
        if (!leftOutputFields.isEmpty()) {
            leftOutputFields.forEach(map -> {
                String fieldName = map.get("column_name");
                String newName = map.get("new_name");
                if (StringUtils.isNullOrEmpty(newName)) {
                    leftColumnList.add(leftAlias + "." + fieldName);
                } else {
                    leftColumnList.add(leftAlias + "." + fieldName+" as \""+newName+"\"");
                }
            });
        }
        if (!rightOutputFields.isEmpty()) {
            rightOutputFields.forEach(map -> {
                String fieldName = map.get("column_name");
                String newName = map.get("new_name");
                if (StringUtils.isNullOrEmpty(newName)) {
                    rightColumnList.add(rightAlias + "." + fieldName);
                } else {
                    rightColumnList.add(rightAlias + "." + fieldName+" as \""+newName+"\"");
                }
            });
        }
        StringBuilder outColumns = new StringBuilder();
        if (leftColumnList.isEmpty()) {
            outColumns.append(String.join(",", rightColumnList));
        } else if (rightColumnList.isEmpty()) {
            outColumns.append(String.join(",", leftColumnList));
        } else {
            outColumns.append(String.join(",", leftColumnList)).append(",").append(String.join(",", rightColumnList));
        }
        return outColumns.toString();
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.TRANSFORM_SQLJOIN;
    }

}
