package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.aliyun.oss.common.utils.StringUtils;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SqlTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransformConstReferenceDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.OrderByColumn;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserBuilder;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserUtil;
import org.springframework.util.CollectionUtils;

import java.util.*;

import static org.apache.dolphinscheduler.common.constants.Constants.LOCAL_PARAMS;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformSortPlugin implements SqlTaskTypeFactory {

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        log.info("Success to add transform Filter Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "Sort";
    }




    @Override
    public OutputFieldDTO getOutputField(SeatunnelTaskNodeVO seatunnelTaskNodeVO) {
        List<OutputFieldDTO> outputFields = seatunnelTaskNodeVO.getInputFields();
        OutputFieldDTO outputFieldDTO = new OutputFieldDTO(OutputFieldDTO.Type.OTHER);
        if(outputFields!=null&&outputFields.size()>0){
            outputFieldDTO = outputFields.get(0);
        }

        return outputFieldDTO;
    }

    @Override
    public void processSqlSelectColumn(SeatunnelTaskNodeVO seatunnelTaskNodeVO, Map<String, String> colMap) {

    }

    @Override
    public Select getCustomerSqlSelect(Select select, SeatunnelTaskNodeVO seatunnelTaskNodeVO,TaskExecutionContext taskExecutionContext) {
        log.info("SeaTunnel task custom sql selectSort : {}", select.toString());
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        log.info("-----------------------------plainSelect: {}", plainSelect);
        Map<String, Object> params = seatunnelTaskNodeVO.getParams();
        String tableName = params.get("table_name").toString();
        List<LinkedHashMap<String, Object>> fieldList = (List<LinkedHashMap<String, Object>>) seatunnelTaskNodeVO.getParams().get("sort_fields");
        if (fieldList == null || fieldList.isEmpty()) {
            log.error("Parameter fieldList is empty. parameter: {}", JSONUtils.toJsonString(seatunnelTaskNodeVO));
            throw new RuntimeException("Parameter fieldList is empty.");
        }
        // 解析SQL
        try {
            List<OrderByColumn> orderBys = new ArrayList<>();
            for (LinkedHashMap<String, Object> filed : fieldList) {
                String fieldName = filed.get("field_name").toString();
                String order = filed.get("order").toString();
                OrderByColumn orderByColumn = new OrderByColumn(tableName, fieldName, "asc".equals(order));
                orderBys.add(orderByColumn);
            }
            PlainSelect plainSelects = SqlParserBuilder.withSelect(plainSelect).addOrderBy(orderBys).build();
//                        plainSelect  = SqlParserBuilder.withSelect(plainSelect).addOrderBy(orderBys).build();
            select.setSelectBody(plainSelects);
            log.info("SeaTunnel task custom sql select2 : {}", select.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return select;
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.TRANSFORM_SORT;
    }

}
