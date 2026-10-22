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
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.JdbcTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.ConnectParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourcePostgreSQLDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.TablePro;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.utils.ParamUtils;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserBuilder;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * SourcePostgreSQLPlugin
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourcePostgreSQLPlugin implements JdbcTaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourcePostgreSQLDTO sourcePostgreSQLDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sourcePostgreSQLDTO);
        log.info("Success to add Source PostgreSQL Plugin");
    }

    private SourcePostgreSQLDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String,Object> params = taskNode.getParams();
        SourcePostgreSQLDTO sourcePostgreSQLDTO = null;
        if (params == null) {
            sourcePostgreSQLDTO = new SourcePostgreSQLDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sourcePostgreSQLDTO = mapper.readValue(jsonString, SourcePostgreSQLDTO.class);
                ParamUtils.extractMapByFieldName(params, "properties", sourcePostgreSQLDTO::setProperties);
                convertDatasource(taskExecutionContext, sourcePostgreSQLDTO);
                if (taskNode.getSelect() != null) {
                    sourcePostgreSQLDTO.setQuery(taskNode.getSelect().toString());
                    sourcePostgreSQLDTO.setTablePath(null);
                    sourcePostgreSQLDTO.setWhereCondition(null);
                } else {
                    convertTableList(sourcePostgreSQLDTO);
                }
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            }
        }
        assert sourcePostgreSQLDTO != null;
        sourcePostgreSQLDTO.setPluginOutput(taskNode.getCode());
        sourcePostgreSQLDTO.setPluginName("Jdbc");
        return sourcePostgreSQLDTO;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SourcePostgreSQLDTO sourcePostgreSQLDTO) throws JsonProcessingException {
        if (sourcePostgreSQLDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sourcePostgreSQLDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        ConnectParamDTO connectParamDTO = mapper.readValue(connectionParams,ConnectParamDTO.class);
        sourcePostgreSQLDTO.setUrl(getJdbcUrl(connectParamDTO));
        sourcePostgreSQLDTO.setUser(connectParamDTO.getUser());
        sourcePostgreSQLDTO.setDriver(connectParamDTO.getDriverClassName());
        sourcePostgreSQLDTO.setPassword(connectParamDTO.getPassword());
        if(connectParamDTO.getOther() != null){
            connectParamDTO.getOther().forEach((k,v) -> {
                // 兼容大小写
                k = k.toLowerCase();
                if("currentschema".equals(k) || "schema".equals(k)) {
                    String tablePath = sourcePostgreSQLDTO.getTablePath();
                    if (!StringUtils.isNullOrEmpty(tablePath)) {
                        if (tablePath.contains(".")) {
                            // db.table -> db.schema.table
                            String[] parts = tablePath.split("\\.", 2);
                            sourcePostgreSQLDTO.setTablePath(parts[0] + "." + v + "." + parts[1]);
                        } else {
                            // table -> schema.table （适用于默认情况）
                            sourcePostgreSQLDTO.setTablePath(v + "." + tablePath);
                        }
                    }
                }
            });
        }else{
            log.warn("数据源未设置currentSchema 或 schema 默认使用数据库名");
        }
    }



    private void convertTableList(SourcePostgreSQLDTO sourcePostgreSQLDTO) {
        if (sourcePostgreSQLDTO.getTableList() == null || CollectionUtils.isEmpty(sourcePostgreSQLDTO.getTableList())) {
            return;
        }
        List<String> tableList = sourcePostgreSQLDTO.getTableList();
        List<TablePro> tables = new ArrayList<>();
        for (String tablePath : tableList) {
            TablePro tablePro = new TablePro();
            tablePro.setTablePath(tablePath);
        }
        sourcePostgreSQLDTO.setTableList(tables);
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "PostgreSQL";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.SOURCE_JDBC;
    }
    @Override
    public Select getCustomerSqlSelect(Select select, SeatunnelTaskNodeVO taskNode,TaskExecutionContext taskExecutionContext) {
        Map<String, Object> params = taskNode.getParams();
        if(select == null){
            select = new Select();
        }
        PlainSelect plainSelect = new PlainSelect();
        SourcePostgreSQLDTO sourcePostgreSQLDTO = null;
        if (params == null) {
            return select;
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sourcePostgreSQLDTO = mapper.readValue(jsonString, SourcePostgreSQLDTO.class);
                String query = sourcePostgreSQLDTO.getQuery();
                StringBuilder sql = new StringBuilder();
                if (!StringUtils.isNullOrEmpty(query)) {
                     sql.append(query);
                     select = (Select) CCJSqlParserUtil.parse(sql.toString());
                } else {
                    String tableName = sourcePostgreSQLDTO.getTablePath();
                    String tablePrefix = getTablePrefix(taskExecutionContext, taskNode);
                    if (!StringUtils.isNullOrEmpty(tablePrefix)) {
                        tableName = tableName.contains(".") ? tableName.substring(tableName.indexOf(".") + 1) : tableName;
                        tableName = tablePrefix + tableName;
                    }
                    log.info("--------------------select tableName: {}", tableName);
                    String whereCondition = sourcePostgreSQLDTO.getWhereCondition();
                    log.info("SeaTunnel taskSourcePostgreSQLPlugin whereCondition: {}", whereCondition);
                    List<OutputFieldDTO> outputFields = taskNode.getOutputFields();
                    Set fiels = new HashSet<>();
                    if(outputFields !=  null&& outputFields.size() > 0){
                        fiels = outputFields.get(0).getFields();
                    }
                    if(!StringUtils.isNullOrEmpty(whereCondition)&&!"null".equals(whereCondition)) {
                        if(whereCondition.trim().toLowerCase().startsWith("where")){
                            whereCondition = whereCondition.toLowerCase().replaceFirst("where","");
                        }
                        plainSelect = SqlParserBuilder.create(tableName, fiels, whereCondition)
                                .build();
                    }else{
                        plainSelect = SqlParserBuilder.create(tableName, fiels, "")
                                .build();
                    }
                    select.setSelectBody(plainSelect);
                    sql.append(SqlParserBuilder.withSelect(plainSelect).toSQL());
                    log.info("SeaTunnel taskSourcePostgreSQLPlugin sql: {}", sql.toString());
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
