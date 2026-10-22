package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SqlTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.params.TransformDataDistinctDto;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.OrderByColumn;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserBuilder;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserUtil;
import org.apache.dolphinscheduler.spi.enums.DbTypeRegex;

import java.util.*;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformDataDistinctPlugin implements SqlTaskTypeFactory {

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        log.info("Success to add transform Filter Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "DataDistinct";
    }


    @Override
    public OutputFieldDTO getOutputField(SeatunnelTaskNodeVO seatunnelTaskNodeVO) {
        List<OutputFieldDTO> outputFields = seatunnelTaskNodeVO.getInputFields();
        OutputFieldDTO outputFieldDTO = new OutputFieldDTO(OutputFieldDTO.Type.OTHER);
        if (outputFields != null && !outputFields.isEmpty()) {
            outputFieldDTO = outputFields.get(0);
        }
        TransformDataDistinctDto dto = JSONUtils.parseObject(JSONUtils.toJsonString(seatunnelTaskNodeVO.getParams()), TransformDataDistinctDto.class);
        outputFieldDTO.setFields(dto.getFieldList());
        return outputFieldDTO;
    }

    @Override
    public void processSqlSelectColumn(SeatunnelTaskNodeVO seatunnelTaskNodeVO, Map<String, String> colMap) {

    }

    @Override
    public Select getCustomerSqlSelect(Select select, SeatunnelTaskNodeVO seatunnelTaskNodeVO,TaskExecutionContext taskExecutionContext) {

        Map<String, Object> params = seatunnelTaskNodeVO.getParams();
        TransformDataDistinctDto dto = JSONUtils.parseObject(JSONUtils.toJsonString(params), TransformDataDistinctDto.class);
        if (dto == null || dto.getFieldList() == null || dto.getFieldList().isEmpty()) {
            log.error("Parameter fieldList is empty. parameter: {}", JSONUtils.toJsonString(seatunnelTaskNodeVO));
            throw new RuntimeException("Parameter fieldList is empty.");
        }
        // 解析SQL
        try {
            SqlParserUtil.buildDeduplicateSelectWithDistinct(select, dto.getFieldList());
            PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
            log.info("-----------------------------plainSelect: {}", plainSelect);
            switch (dto.getCondType()) {
                case "keywords":
                    for (TransformDataDistinctDto.Cond cond : dto.getConds()) {
                        SqlParserBuilder.withSelect(plainSelect).addWhereConditions(null,
                                cond.getColumn().concat(" = '").concat(cond.getValue()).concat("'"));
                    }
                    break;
                case "NOTNULL":
                    for (TransformDataDistinctDto.Cond cond : dto.getConds()) {
                        SqlParserBuilder.withSelect(plainSelect).addWhereConditions(null,
                                cond.getColumn().concat(" IS NOT NULL"));
                    }
                    break;
                case "complateMatch":
                    SqlParserBuilder.withSelect(plainSelect);
                    break;
                default:
                    DbTypeRegex dbTypeRegex = DbTypeRegex.valueOf(dto.getDbType());
                    for (TransformDataDistinctDto.Cond cond : dto.getConds()) {
                        SqlParserBuilder.withSelect(plainSelect).addWhereConditions(null,
                                dbTypeRegex.toCond(cond.getColumn(), cond.getValue()));
                    }
                    break;
            }
//            SqlParserBuilder.withSelect(plainSelect).addWhereConditions("t", dto.getConds());
            log.info("SeaTunnel task custom sql selectStatement : {}", select.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return select;
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.TRANSFORM_DATADISTINCT;
    }

}
