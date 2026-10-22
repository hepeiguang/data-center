package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

import com.aliyun.oss.common.utils.StringUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.JdbcTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceJdbcDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.TablePro;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserBuilder;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.jetbrains.annotations.NotNull;
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
public class SourceJdbcPlugin implements JdbcTaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceJdbcDTO sourceJdbcDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sourceJdbcDTO);
        log.info("Success to add Source Jdbc Plugin");
    }

    private SourceJdbcDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String, Object> params = taskNode.getParams();
        SourceJdbcDTO sourceJdbcDTO = null;
        if (params == null) {
            throw new RuntimeException("params is null");
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sourceJdbcDTO = mapper.readValue(jsonString, SourceJdbcDTO.class);
                convertDatasource(taskExecutionContext, sourceJdbcDTO);
                if (taskNode.getSelect() != null) {
                    sourceJdbcDTO.setQuery(taskNode.getSelect().toString());
                    sourceJdbcDTO.setTablePath(null);
                    sourceJdbcDTO.setWhereCondition(null);
                } else {
                    convertTableList(sourceJdbcDTO);
                }
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            }
        }
        sourceJdbcDTO.setPluginOutput(taskNode.getCode());
        String pluginName = taskNode.getPluginName().substring(0, 1).toUpperCase() +
                taskNode.getPluginName().substring(1).toLowerCase();
        sourceJdbcDTO.setPluginName(pluginName);
        return sourceJdbcDTO;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SourceJdbcDTO sourceJdbcDTO) throws JsonProcessingException {
        if (sourceJdbcDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sourceJdbcDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        BaseConnectionParam connectionParam = (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                dbSource.getType(), dbSource.getConnectionParams());
        sourceJdbcDTO.setUrl(DataSourceUtils.getJdbcUrl(dbSource.getType(), connectionParam));
        sourceJdbcDTO.setUser(connectionParam.getUser());
        sourceJdbcDTO.setDriver(connectionParam.getDriverClassName());
        sourceJdbcDTO.setPassword(connectionParam.getPassword());
        if (!StringUtils.isNullOrEmpty(sourceJdbcDTO.getTablePath())) {
            String tablePath = getTablePath(sourceJdbcDTO, connectionParam);
            sourceJdbcDTO.setTablePath(tablePath);
        }
    }

    @NotNull
    private static String getTablePath(SourceJdbcDTO sourceJdbcDTO, BaseConnectionParam connectionParam) {
        String tableName;
        if (sourceJdbcDTO.getTablePath().contains(".")) {
            String[] parts = sourceJdbcDTO.getTablePath().split("\\.", 2);
            tableName = parts[1];
        } else {
            tableName = sourceJdbcDTO.getTablePath();
        }
        String schemaName = null;
        if (connectionParam.getOther() != null) {
            for (String key : connectionParam.getOther().keySet()) {
                if ("currentschema".equalsIgnoreCase(key) || "schema".equalsIgnoreCase(key)) {
                    schemaName = connectionParam.getOther().get(key);
                    break;
                }
            }
        } else {
            log.warn("数据源未设置currentSchema 或 schema 默认使用数据库名");
        }
        String tablePath = connectionParam.getDatabase() + "." + (schemaName == null ? "" : schemaName + ".") + tableName;
        return tablePath;
    }


    private void convertTableList(SourceJdbcDTO sourceJdbcDTO) {
        if (sourceJdbcDTO.getTableList() == null || CollectionUtils.isEmpty(sourceJdbcDTO.getTableList())) {
            return;
        }
        List<String> tableList = sourceJdbcDTO.getTableList();
        List<TablePro> tables = new ArrayList<>();
        for (String tablePath : tableList) {
            TablePro tablePro = new TablePro();
            tablePro.setTablePath(tablePath);
        }
        sourceJdbcDTO.setTableList(tables);
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "JDBC";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.SOURCE_JDBC;
    }

    @Override
    public Select getCustomerSqlSelect(Select select, SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String, Object> params = taskNode.getParams();
        if (select == null) {
            select = new Select();
        }
        PlainSelect plainSelect = new PlainSelect();
        SourceJdbcDTO sourceJdbcDTO = null;
        if (params == null) {
            return select;
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sourceJdbcDTO = mapper.readValue(jsonString, SourceJdbcDTO.class);
                String query = sourceJdbcDTO.getQuery();
                StringBuilder sql = new StringBuilder();
                if (!StringUtils.isNullOrEmpty(query)) {
                    sql.append(query);
                    select = (Select) CCJSqlParserUtil.parse(sql.toString());
                } else {
                    String tableName = sourceJdbcDTO.getTablePath();
                    String whereCondition = sourceJdbcDTO.getWhereCondition();
                    log.info("SeaTunnel taskSourceJdbcPlugin whereCondition: {}", whereCondition);
                    List<OutputFieldDTO> outputFields = taskNode.getOutputFields();
                    List fiels = new ArrayList();
                    if (outputFields != null && outputFields.size() > 0) {
                        //fiels = outputFields.get(0).getFields();
                        fiels = sourceJdbcDTO.getSelectedColumns();
                    }

                    // todo 这里需要对字段进行处理
                    // todo mysql ``  gauss/pg ""
                    String quotation = getColumnQuotation(taskExecutionContext, taskNode);
                    if (fiels != null && !fiels.isEmpty()) {
                        for (int i = 0; i < fiels.size(); i++) {
                            String field = (String) fiels.get(i);
                            if (!field.startsWith(quotation) && !field.endsWith(quotation)) {
                                fiels.set(i, quotation + field + quotation);
                            }
                        }
                    }

                    // 在这里进行实现
                    String tablePrefix = getTablePrefix(taskExecutionContext, taskNode);
                    if (!StringUtils.isNullOrEmpty(tablePrefix)) {
                        tableName = tableName.contains(".") ? tableName.substring(tableName.indexOf(".") + 1) : tableName;
                        tableName = tablePrefix + tableName;
                    }

                    log.info("--------------------select tableName: {}", tableName);
                    if (!StringUtils.isNullOrEmpty(whereCondition) && !"null".equals(whereCondition)) {
                        if (whereCondition.trim().toLowerCase().startsWith("where")) {
                            whereCondition = whereCondition.toLowerCase().replaceFirst("where", "");
                        }
                        plainSelect = SqlParserBuilder.create(tableName, fiels, whereCondition)
                                .build();
                    } else {
                        plainSelect = SqlParserBuilder.create(tableName, fiels, "")
                                .build();
                    }
                    select.setSelectBody(plainSelect);
                    sql.append(SqlParserBuilder.withSelect(plainSelect).toSQL());
                    log.info("SeaTunnel taskSourceJdbcPlugin sql: {}", sql.toString());
                }
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            } catch (JSQLParserException e) {
                throw new RuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return select;
    }
}
