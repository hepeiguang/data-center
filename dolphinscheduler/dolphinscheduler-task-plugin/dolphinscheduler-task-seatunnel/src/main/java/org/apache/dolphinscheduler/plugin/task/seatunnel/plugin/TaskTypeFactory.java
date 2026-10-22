package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.commons.collections4.MapUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.ConnectParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkJdbcDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlcondition.FieldTypeHandler;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlcondition.HandlerFactory;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.apache.dolphinscheduler.spi.plugin.PrioritySPI;
import org.apache.dolphinscheduler.spi.plugin.SPIIdentify;
import com.fasterxml.jackson.databind.DeserializationFeature;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 18:59
 **/
public interface TaskTypeFactory extends PrioritySPI {
    ObjectMapper mapper = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            // 忽略null值字段
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            // 忽略空数组/集合
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY)
            ;

    /**
     * 组装data
     *
     * @param seatunnelConfigDTO  配置文件的内容
     * @param seatunnelTaskNodeVO 页面配置的参数
     */
    void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO seatunnelTaskNodeVO, TaskExecutionContext taskExecutionContext);

    /**
     * 定义插件的唯一标识符
     *
     * @return
     */
    String getName();

    DsSeatunnelTaskEnum getDsSeatunnelTaskEnum();

    default SPIIdentify getIdentify() {
        return SPIIdentify.builder().name(getName()).build();
    }

    default String getJdbcUrl(ConnectParamDTO connectParamDTO) {
        if (MapUtils.isEmpty(connectParamDTO.getOther())) {
            return connectParamDTO.getJdbcUrl();

        }
        return String.format("%s?%s", connectParamDTO.getJdbcUrl(),
                transformOther(connectParamDTO.getOther()));
    }

    default String transformOther(Map<String, String> otherMap) {
        if (MapUtils.isEmpty(otherMap)) {
            return null;
        }
        List<String> otherList = new ArrayList<>();
        otherMap.forEach((key, value) -> otherList.add(String.format("%s=%s", key, value)));
        return String.join("&", otherList);
    }

    default List<OutputFieldDTO> getOutputFields(SeatunnelTaskNodeVO vo) {
        List<OutputFieldDTO> outputFields = new ArrayList<>();
        if (vo.getInputFields() != null && !vo.getInputFields().isEmpty()) {
            outputFields.addAll(vo.getInputFields());
        }
        OutputFieldDTO output = getOutputField(vo);
        if (output != null && output.getFields() != null && !output.getFields().isEmpty()) {
            output.setTaskName(vo.getName());
            String name = getName(output);
            if (name != null) {
                //过滤掉指定 tableName 的对象
                outputFields = outputFields.stream().filter(outputField -> !name.equals(getName(outputField)))
                        .collect(Collectors.toList());
                outputFields.add(output);
            }
        }
        return outputFields;
    }

    static String getName(OutputFieldDTO output) {
        return output.getTableName() == null ? output.getTaskName() : output.getTableName();
    }

    default Select getCustomerSqlSelect(Select select, SeatunnelTaskNodeVO seatunnelTaskNodeVO, TaskExecutionContext taskExecutionContext) {
        return select;
    }

    /**
     * 当前 算子新增列信息
     * 如果没有则返回null
     *
     * @param vo
     * @return
     */
    default OutputFieldDTO getOutputField(SeatunnelTaskNodeVO vo) {
        return null;
    }


    /**
     * 处理当前表的前缀信息（schema/database . table）
     * 如果存在
     *
     * @return
     */
    default String getTablePrefix(TaskExecutionContext taskExecutionContext, SeatunnelTaskNodeVO task) {
        Integer datasourceId = (Integer) task.getParams().get("datasourceId");
        if (datasourceId == null) {
            throw new RuntimeException("datasourceId must be not null");
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, datasourceId);
        String connectionParams = dbSource.getConnectionParams();
        String tableNamePrefix = null;
        try {
            ConnectParamDTO connectParamDTO = mapper.readValue(connectionParams, ConnectParamDTO.class);
            // todo 需要支持 数据库资源类型 MYSQL POSTGRESQL OPENGAUSS DORIS DAMENG
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
                    if (connectParamDTO.getOther() != null) {
                        for (Map.Entry<String, String> entry : connectParamDTO.getOther().entrySet()) {
                            String k = entry.getKey().toLowerCase();
                            if ("currentschema".equals(k) || "schema".equals(k)) {
                                tableNamePrefix = entry.getValue() + ".";
                                break;
                            }
                        }
                    } else {
                        // 如果前缀拿不到 则默认用前缀public
                        tableNamePrefix = "public.";
                    }
                    break;
                default:
                    break;
            }

        } catch (JsonProcessingException jsonex) {
            throw new RuntimeException("json parse error, cannot parse connection params");
        } catch (Exception ex) {
            throw new RuntimeException("exception caused by a unknow error, {}", ex);
        }
        return tableNamePrefix;
    }

    // 获取数据库字段的引号
    // mysql 反引号``  pg/gauss ""
    default String getColumnQuotation(TaskExecutionContext taskExecutionContext, SeatunnelTaskNodeVO task) {
        Integer datasourceId = (Integer) task.getParams().get("datasourceId");
        if (datasourceId == null) {
            throw new RuntimeException("datasourceId must be not null");
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, datasourceId);
        String quotation = null;
        try {
            // todo 需要支持 数据库资源类型 MYSQL POSTGRESQL OPENGAUSS DORIS DAMENG
            DbType type = dbSource.getType();
            switch (type) {
                case MYSQL:
                    // 如果是mysql 前缀是数据库名
                    quotation = "`";
                    break;
//                case POSTGRESQL:
                case OPENGAUSS:
                    quotation = "\"";
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            throw new RuntimeException("exception caused by a unknow error, {}", ex);
        }
        return quotation;
    }

    default String getColumnQuotationByDbType(String type) {
        String quotation = null;
        try {
            // todo 需要支持 数据库资源类型 MYSQL POSTGRESQL OPENGAUSS DORIS DAMENG
            switch (type) {
                case "MYSQL":
                    // 如果是mysql 前缀是数据库名
                    quotation = "`";
                    break;
//                case "POSTGRESQL":
                case "OPENGAUSS":
                    quotation = "\"";
                    break;
                default:
                    break;
            }
        } catch (Exception ex) {
            throw new RuntimeException("exception caused by a unknow error, {}", ex);
        }
        return quotation;
    }

    // 兼容 casewhen sqlfilter swaitch
    // 整数型（整形） 字符串型(varchar text) 布尔值型(boolean) 日期型(data datetime)
    default String getColumnCastResult (String databaseType, String columnValue, String columnType) {
        String result;
        try {
            // todo 需要支持 数据库资源类型 MYSQL POSTGRESQL OPENGAUSS DORIS DAMENG
            DbType type = DbType.valueOf(databaseType);
            FieldTypeHandler handler = null;
            switch (type) {
                case MYSQL:
                    handler = HandlerFactory.getHandler(DbType.MYSQL);
                    result = handler.handleValue(columnValue, columnType);
                    break;
                case POSTGRESQL:
                    handler = HandlerFactory.getHandler(DbType.POSTGRESQL);
                    result = handler.handleValue(columnValue, columnType);
                    break;
                case OPENGAUSS:
                    handler = HandlerFactory.getHandler(DbType.OPENGAUSS);
                    result = handler.handleValue(columnValue, columnType);
                    break;
                default:
                    result = columnValue;
                    break;
            }
        } catch (Exception ex) {
            throw new RuntimeException("exception caused by a unknow error, {}", ex);
        }
        return result;
    }
}
