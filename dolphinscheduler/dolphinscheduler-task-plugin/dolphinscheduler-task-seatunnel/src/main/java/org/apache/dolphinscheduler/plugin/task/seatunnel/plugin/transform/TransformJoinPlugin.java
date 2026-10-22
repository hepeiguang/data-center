package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.aliyun.oss.common.utils.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SqlTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.ConnectParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.JoinInfo;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.JoinType;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserBuilder;
import org.apache.dolphinscheduler.spi.enums.DbType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformJoinPlugin implements SqlTaskTypeFactory {

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        log.info("Success to add transform Filter Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "Join";
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
        try {
            String tablePrefix = getTablePrefix(taskExecutionContext, seatunnelTaskNodeVO);
            String quotation = getColumnQuotation(taskExecutionContext, seatunnelTaskNodeVO);
            log.error("----------------------tablePrefix: {}", tablePrefix);
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
            Map<String, Object> params = seatunnelTaskNodeVO.getParams();
            String leftTable = params.get("left_table").toString();
            String rightTable = params.get("right_table").toString();
            if (!StringUtils.isNullOrEmpty(tablePrefix)) {
                log.info("__tablePrefix: {}", tablePrefix);
                leftTable = leftTable.contains(".") ? leftTable.substring(leftTable.indexOf(".") + 1) : leftTable;
                leftTable = tablePrefix + leftTable;
                rightTable = rightTable.contains(".") ? rightTable.substring(rightTable.indexOf(".") + 1) : rightTable;
                rightTable = tablePrefix + rightTable;
            }
            log.info("leftTable : {} , rightTable : {}", leftTable , rightTable);

            Integer datasourceId = (Integer) seatunnelTaskNodeVO.getParams().get("datasourceId");
            if (datasourceId == null) {
                throw new RuntimeException("datasourceId must be not null");
            }
            DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                    .getResourceParameters(ResourceType.DATASOURCE, datasourceId);
            String connectionParams = dbSource.getConnectionParams();
            String tableNamePrefix = null;
            try {
                ConnectParamDTO connectParamDTO = mapper.readValue(connectionParams, ConnectParamDTO.class);
                // 数据库资源类型 MYSQL POSTGRESQL OPENGAUSS DORIS DAMENG
                DbType type = dbSource.getType();
                switch (type) {
                    case MYSQL:
                        // 如果是mysql 前缀是数据库名
                        String database = connectParamDTO.getDatabase();
                        tableNamePrefix = database + ".";
                        break;
                    case POSTGRESQL:
                    case OPENGAUSS:
                        // 如果是postgresql gauss 前缀是 schema
                        if (connectParamDTO.getOther() != null ) {
                            for (Map.Entry<String, String> entry : connectParamDTO.getOther().entrySet()) {
                                String k = entry.getKey().toLowerCase();
                                if ("currentschema".equals(k) || "schema".equals(k)) {
                                    tableNamePrefix = entry.getValue() + ".";
                                    break;
                                }
                            }
                        }else {
                            // 如果前缀拿不到 则默认用前缀public
                            tableNamePrefix = "public.";
                        }
                        break;
                    default:
                        break;
                }

            } catch (JsonProcessingException jsonex) {
                throw new RuntimeException("json parse error, cannot parse connection params");
            }catch (Exception ex) {
                throw new RuntimeException("exception caused by a unknow error, {}",ex);
            }

            String joinType = params.get("join_type").toString();
            List<Map<String, String>> relation = (List<Map<String, String>>) params.get("relation");
            List<Map<String, String>> outputFields = (List<Map<String, String>>) params.get("select_fields");
            List<String> rightColumnList = new ArrayList<>();
            for (Map<String, String> map : outputFields) {
                String fieldName = map.get("column_name");
                String newName = map.get("new_name");
                log.info("fieldName : {} , newName : {}", fieldName, newName);
                if (StringUtils.isNullOrEmpty(newName)) {
                    rightColumnList.add(fieldName);
                } else {
//                    rightColumnList.add(fieldName+" as '"+newName+"("+fieldName+")'");
                    rightColumnList.add(
                            quotation + fieldName + quotation + " as "
                                    + quotation + newName + quotation );
                }
            }

            List<String> leftColumns = relation.stream()
                    .map(map -> map.get("left_column"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            List<String> rightColumns = relation.stream()
                    .map(map -> map.get("right_column"))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            if (!leftColumns.isEmpty()) {
                for (int i = 0; i < leftColumns.size(); i++) {
                    String field = (String) leftColumns.get(i);
                    if (!field.startsWith(quotation) && !field.endsWith(quotation)) {
                        leftColumns.set(i, quotation + field + quotation);
                    }
                }
            }
            if (!rightColumns.isEmpty()) {
                for (int i = 0; i < rightColumns.size(); i++) {
                    String field = rightColumns.get(i);
                    if (!field.startsWith(quotation) && !field.endsWith(quotation)) {
                        rightColumns.set(i, quotation + field + quotation);
                    }
                }
            }
            if (!rightColumnList.isEmpty()) {
                for (int i = 0; i < rightColumnList.size(); i++) {
                    String field = rightColumnList.get(i);
                    if (!field.startsWith(quotation) && !field.endsWith(quotation)) {
                        rightColumnList.set(i, quotation + field + quotation);
                    }
                }
            }



            //取上个节点的输出字段
//            List<OutputFieldDTO> outputFieldsList = seatunnelTaskNodeVO.getOutputFields();
//            List<String> allFields = outputFieldsList.stream()
//                    .filter(dto -> dto.getFields() != null)
//                    .flatMap(dto -> dto.getFields().stream())
//                    .collect(Collectors.toList());


//            Set<String> leftTableColumn = seatunnelTaskNodeVO.getOutputFields().get(0).getFields();
//            List<String> leftColumnList = new ArrayList<>(leftTableColumn);
            //将两个连接的表字段进行合并
//            leftColumnList.addAll(rightColumnList);
            JoinInfo userJoinDept = new JoinInfo(
                    leftTable,
                    rightTable,
                    leftColumns,
                    rightColumns,
                    JoinType.valueOf(joinType)
            ).setColumns(rightColumnList);

            String sql = SqlParserBuilder.withSelect(plainSelect).addJoin(userJoinDept).toSQL();
            log.info("SeaTunnel task join sql selectStatement : {}", select.toString());
            select = (Select) CCJSqlParserUtil.parse(sql.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        log.info("SeaTunnel task join sql selectStatement : {}", select.toString());
        return select;
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.TRANSFORM_JOIN;
    }

}
