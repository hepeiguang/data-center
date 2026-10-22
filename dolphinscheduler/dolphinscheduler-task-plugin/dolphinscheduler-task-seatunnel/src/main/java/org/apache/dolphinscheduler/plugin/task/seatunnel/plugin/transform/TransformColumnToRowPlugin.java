package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SqlTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormColumnToRowDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormRowToColumnDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormSqlFunctionsDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformColumnToRowPlugin implements SqlTaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO seatunnelTaskNodeVO, TaskExecutionContext taskExecutionContext) {

        log.info("Success to add TransformColumnToRowPlugin Sql Plugin");
    }

    @Override
    public Select getCustomerSqlSelect(Select select, SeatunnelTaskNodeVO seatunnelTaskNodeVO,TaskExecutionContext taskExecutionContext) {
        log.info("列转行 sql select : {}", select.toString());
        /**
         * 获取上个节点的sql
         */
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        String tableAliasName = "table_as_" + RandomUtils.nextInt(1000, 9999);
        String plainSelectSql = "(" + plainSelect.toString() + ") as " + tableAliasName;

        Map<String, Object> params = seatunnelTaskNodeVO.getParams();

        try {
            String columns = JSONUtils.toJsonString(params.get("columns")); // 获取目标字段映射关系
            log.info("columns:{}", columns);
            List<TransFormColumnToRowDTO> columnsJsonList =
                    JSONUtils.toList(columns, TransFormColumnToRowDTO.class);
            if (columnsJsonList.isEmpty()) {
                throw new RuntimeException("columns is empty");
            }

            // 获取表的所有字段,并将制定字段替换成case when
            String allColumnStr = getAllColumnStr(seatunnelTaskNodeVO);
            if (StringUtils.isBlank(allColumnStr)) {
                throw new RuntimeException("allColumnStr is empty");
            }
            List<String> allColumnList = Arrays.stream(allColumnStr.split(",")).collect(Collectors.toList());

            String quotation = getColumnQuotation(taskExecutionContext, seatunnelTaskNodeVO);
            String sql = buildUnpivotSql(columnsJsonList, allColumnList, plainSelectSql, tableAliasName,quotation);
            log.info("----------------------最终拼接sql语句为 = {} ", sql);
            return (Select) CCJSqlParserUtil.parse(sql);
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return select;
    }

    /**
     * 构建行转列的sql
     *
     * @param configList
     * @param allColumnList
     * @param plainSelectSql source的sql
     * @param tableAliasName 表别名
     * @return
     */
    public String buildUnpivotSql(List<TransFormColumnToRowDTO> configList, List<String> allColumnList,
                                  String plainSelectSql, String tableAliasName,String quotation) {
        // 先移除configList已存在的字段
        for (TransFormColumnToRowDTO dto : configList) {
            allColumnList.remove(dto.getGroupKey());
            for (TransFormColumnToRowDTO.FieldMapping field : dto.getFields()) {
                allColumnList.remove(field.getFieldName());
                allColumnList.remove(field.getFieldValue());
            }
        }

        // 给原有字段加上别名
        StringBuilder columnStr = new StringBuilder();
        for (int i = 0; i < allColumnList.size(); i++) {
            String columnName = allColumnList.get(i);
            columnName = quotation + columnName + quotation;
            columnStr.append(tableAliasName).append(".").append(columnName);

            if (i != allColumnList.size() - 1) {
                columnStr.append(", ");
            }
        }

        StringBuilder sql = new StringBuilder();

        for (int i = 0; i < configList.size(); i++) {
            TransFormColumnToRowDTO config = configList.get(i);
            sql.append("SELECT ").append(columnStr).append(" ");

            // 添加 groupKey 字段
            sql.append(", '").append(config.getGroupValue()).append("' AS ").append(config.getGroupKey());

            // 添加 fields 中定义的字段
            for (TransFormColumnToRowDTO.FieldMapping field : config.getFields()) {
                sql.append(", ").append(field.getFieldValue()).append(" AS ").append(field.getFieldName());
            }

            sql.append(" FROM ").append(plainSelectSql);

            if (i < configList.size() - 1) {
                sql.append(" UNION ALL ");
            }
        }

        return sql.toString();
    }


    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "SQLCOLUMNTOROW";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.TRANSFORM_SQLCOLUMNTOROW;
    }

    @Override
    public void processSqlSelectColumn(SeatunnelTaskNodeVO seatunnelTaskNodeVO, Map<String, String> colMap) {

    }


}
