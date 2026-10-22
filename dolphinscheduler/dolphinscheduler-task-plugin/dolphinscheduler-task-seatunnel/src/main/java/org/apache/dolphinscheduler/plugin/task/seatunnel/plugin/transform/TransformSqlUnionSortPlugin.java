package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SqlTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransformSqlUnionDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransformSqlUnionSortDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserBuilder;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformSqlUnionSortPlugin implements SqlTaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO seatunnelTaskNodeVO, TaskExecutionContext taskExecutionContext) {

        log.info("Success to add TransformSqlUnionSortPlugin Sql Plugin");
    }

    @Override
    public Select getCustomerSqlSelect(Select select, SeatunnelTaskNodeVO seatunnelTaskNodeVO,TaskExecutionContext taskExecutionContext) {
        log.info("列转行 sql select : {}", select.toString());

        /**
         * 获取上个节点的sql
         */

//        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();

        try {
            String tablePrefix = getTablePrefix(taskExecutionContext, seatunnelTaskNodeVO);
            String quotation = getColumnQuotation(taskExecutionContext, seatunnelTaskNodeVO);

            TransformSqlUnionSortDTO sqlUnionDTO = JSONUtils.parseObject(JSONUtils.toJsonString(seatunnelTaskNodeVO.getParams()), TransformSqlUnionSortDTO.class);
            if (sqlUnionDTO == null ) {
                throw new RuntimeException("columnsJsonList is null");
            }
            //创建算子的sql
            String transformSql = generateUnionSqlWithAlias(sqlUnionDTO,tablePrefix,quotation);

            // 创建orderBySql
            String orderBySql = createTransformOrderBySql(sqlUnionDTO, quotation);

            // 拼接union
            String sql = MessageFormat.format("{0} order by {1}",transformSql,orderBySql);

            log.info("最终拼接sql语句为 = {} ", sql);
            return (Select) CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return select;
    }

    /**
     * 创建orderBySql
     * @param sqlUnionDTO
     * @return
     */
    private String createTransformOrderBySql(TransformSqlUnionSortDTO sqlUnionDTO, String quotation) {
        List<String> sqlOrderBy = new ArrayList<>();
        for (TransformSqlUnionSortDTO.FieldSortDTO sortField : sqlUnionDTO.getSortFields()) {
            sqlOrderBy.add(quotation + sortField.getFieldName() + quotation + " " + sortField.getSortType());
        }
        return String.join(",", sqlOrderBy);
    }

    /**
     * 生成带别名的UNION SQL语句
     * @return 完整的UNION SQL
     */
    public String generateUnionSqlWithAlias(TransformSqlUnionSortDTO sqlUnionDTO,String tablePrefix, String quotation) {
        // 1. 获取源表字段
        String sourceTable = sqlUnionDTO.getTableName();
        List<Map<String,String>> fields = sqlUnionDTO.getFields();
        // 1. 获取源表字段及其别名
        List<TransformSqlUnionSortPlugin.FieldWithAlias> sourceFields = getFieldsWithAliasForTable(sourceTable,fields);

        // 2. 为每个目标表生成SELECT部分
        List<String> unionParts = new ArrayList<>();

        // 首先添加源表SELECT
        sourceTable = tablePrefix + sqlUnionDTO.getTableName();
        unionParts.add(generateSelectSqlWithAlias(sourceTable, sourceFields, quotation));

        List<String> targetTables = sqlUnionDTO.getSelectedColumns();
        // 然后为每个目标表生成SELECT并UNION
        for (String targetTable : targetTables) {
            List<TransformSqlUnionSortPlugin.FieldWithAlias> targetFields = getFieldsWithAliasForTable(targetTable,fields);
            unionParts.add(generateSelectSqlWithAlias(tablePrefix + targetTable, targetFields, quotation));
        }

        // 3. 用UNION ALL连接所有部分
        return String.join(" UNION ", unionParts);
    }

    /**
     * 获取指定表对应的字段及别名列表
     * @param tableName 表名
     * @return 带别名的字段列表
     */
    private List<TransformSqlUnionSortPlugin.FieldWithAlias> getFieldsWithAliasForTable(String tableName, List<Map<String,String>> fields) {
        return fields.stream()
                .map(map -> new TransformSqlUnionSortPlugin.FieldWithAlias(
                        map.getOrDefault(tableName, null), // 字段名
                        map.get("aliasName") // 别名
                ))
                .collect(Collectors.toList());
    }

    /**
     * 生成带别名的单个表SELECT SQL
     * @param tableName 表名
     * @param fields 带别名字段列表
     * @return SELECT SQL语句
     */
    private String generateSelectSqlWithAlias(String tableName, List<TransformSqlUnionSortPlugin.FieldWithAlias> fields, String quotation) {
        if (fields == null || fields.isEmpty()) {
            return "";
        }

//        String fieldsStr = fields.stream()
//                .map(f -> String.format("%s AS %s", quotation+f.getFieldName()+quotation, quotation+f.getAliasName()+quotation))
//                .collect(Collectors.joining(", "));

        String fieldsStr = fields.stream()
                .map(f -> {
                    String fieldExpr = "NULL".equalsIgnoreCase(f.getFieldName())
                            ? "NULL"
                            : quotation + f.getFieldName() + quotation;
                    return String.format("%s AS %s",
                            fieldExpr,
                            quotation + f.getAliasName() + quotation);
                })
                .collect(Collectors.joining(", "));

        return String.format("SELECT %s FROM %s", fieldsStr, tableName);
    }

    /**
     * 内部类：字段和别名组合
     */
    @Data
    @AllArgsConstructor
    private static class FieldWithAlias {
        private String fieldName;
        private String aliasName;
    }


    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "SQLUNIONSORT";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.TRANSFORM_SQLUNIONSORT;
    }

    @Override
    public void processSqlSelectColumn(SeatunnelTaskNodeVO seatunnelTaskNodeVO, Map<String, String> colMap) {

    }

}
