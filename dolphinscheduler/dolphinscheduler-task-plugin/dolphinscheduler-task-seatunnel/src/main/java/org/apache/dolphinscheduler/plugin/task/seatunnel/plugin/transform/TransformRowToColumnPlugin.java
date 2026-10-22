package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SqlTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormRowToColumnDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.jetbrains.annotations.NotNull;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformRowToColumnPlugin implements SqlTaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO seatunnelTaskNodeVO, TaskExecutionContext taskExecutionContext) {

        log.info("Success to add TransformRowToColumnPlugin Sql Plugin");
    }


    @Override
    public Select getCustomerSqlSelect(Select select, SeatunnelTaskNodeVO seatunnelTaskNodeVO,TaskExecutionContext taskExecutionContext) {
        log.info("SeaTunnel task custom sql select : {}", select.toString());

        /**
         * 获取上个节点的sql
         */
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        String tableAliasName =  "table_as_" + RandomUtils.nextInt(1000,9999);
        String plainSelectSql = "(" + plainSelect.toString() + ") as " + tableAliasName;
        log.info("-----------------------------plainSelect: {}", plainSelect);

        Map<String, Object> params = seatunnelTaskNodeVO.getParams();

        String groupField = params.getOrDefault("groupField", "").toString();//分组字段
        if (StringUtils.isBlank(groupField)) {
            throw new RuntimeException("groupField is empty");
        }
        String targetFields = JSONUtils.toJsonString(params.get("targetFields")); // 获取目标字段映射关系
        if(StringUtils.isBlank(targetFields)){
            throw new RuntimeException("目标字段映射关系字段为必填");
        }

        List<TransFormRowToColumnDTO> columnsJsonList = JSONUtils.toList(targetFields, TransFormRowToColumnDTO.class);
        if (columnsJsonList.isEmpty()) {
            throw new RuntimeException("columns is empty");
        }

        try {
            String quotation = getColumnQuotation(taskExecutionContext, seatunnelTaskNodeVO);
            // 拼接sql
            List<String> sqlCaseWhenList = getSqlCaseWhen(columnsJsonList, tableAliasName,quotation);

            String sqlTemp = " select {0},{1} from {2} GROUP BY {0}";
            String sql = MessageFormat.format(sqlTemp, groupField, String.join(",", sqlCaseWhenList), plainSelectSql);
            log.info("最终拼接sql语句为 : {}", sql);

            Select select1 = (Select) CCJSqlParserUtil.parse(sql);
            log.info("SeaTunnel task custom sql select1 : {}", select1);
            return select1;
        } catch (Exception e) {
            log.error("TransformRowToColumnPlugin task exception", e);
        }

        return select;
    }

    /**
     * 创建casewhen
     *
     * @param columnsJsonList
     * @param tableAliasName  表别名
     *                        MAX(CASE WHEN product_name = '手机' THEN sale_amount END ) AS 手机,
     *                        <p>
     *                        {0}(CASE WHEN {1}         {2} '{3}' THEN {4} END ) AS {5},
     * @return
     */
    @NotNull
    private static List<String> getSqlCaseWhen(List<TransFormRowToColumnDTO> columnsJsonList, String tableAliasName, String quotation) {
        List<String> sqlCaseWhenList = new ArrayList<>();
        String caseWhenTemp = " {0}(case when {6}.`{1}` {2} ''{3}'' then {4} end) as {5}  ";
        caseWhenTemp = caseWhenTemp.replace("`", quotation);
        for (TransFormRowToColumnDTO columnObj : columnsJsonList) {
            String aggregateFunctions = columnObj.getAggregateFunctions();
            String dataField = columnObj.getDataField();
            String expression = columnObj.getExpression();
            String srcValue = columnObj.getSrcValue();
            // 判定srcValue 是否是数字,如果不是数字，则需要前后加引号
            if (NumberUtils.isCreatable(srcValue)) {
                srcValue = "'" + columnObj.getSrcValue() + "'";
            }
            String targetField = columnObj.getTargetField();
            String targetFieldValue = columnObj.getTargetFieldValue();

            sqlCaseWhenList.add(MessageFormat.format(caseWhenTemp, aggregateFunctions, dataField, expression, srcValue, targetFieldValue, targetField,tableAliasName));
        }
        return sqlCaseWhenList;
    }


    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "SQLROWTOCOLUMN";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.TRANSFORM_SQLROWTOCOLUMN;
    }


    @Override
    public void processSqlSelectColumn(SeatunnelTaskNodeVO seatunnelTaskNodeVO, Map<String, String> colMap) {

    }
}
