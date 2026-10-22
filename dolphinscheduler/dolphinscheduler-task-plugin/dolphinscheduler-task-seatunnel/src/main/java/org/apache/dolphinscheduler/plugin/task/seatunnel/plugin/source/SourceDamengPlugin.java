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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceDamengDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.TablePro;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.utils.ParamUtils;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserBuilder;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * SourceDamengPlugin
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceDamengPlugin implements JdbcTaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceDamengDTO sourceDamengDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sourceDamengDTO);
        log.info("Success to add Source Dameng Plugin");
    }

    private SourceDamengDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String,Object> params = taskNode.getParams();
        SourceDamengDTO sourceDamengDTO = null;
        if (params == null) {
            sourceDamengDTO = new SourceDamengDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sourceDamengDTO = mapper.readValue(jsonString, SourceDamengDTO.class);
                ParamUtils.extractMapByFieldName(params, "properties", sourceDamengDTO::setProperties);
                convertDatasource(taskExecutionContext, sourceDamengDTO);
                if (taskNode.getSelect() != null) {
                    sourceDamengDTO.setQuery(taskNode.getSelect().toString());
                    sourceDamengDTO.setTablePath(null);
                    sourceDamengDTO.setWhereCondition(null);
                } else {
                    convertTableList(sourceDamengDTO);
                }
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            }
        }
        assert sourceDamengDTO != null;
        sourceDamengDTO.setPluginOutput(taskNode.getCode());
        sourceDamengDTO.setPluginName("Jdbc");
        return sourceDamengDTO;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SourceDamengDTO sourceDamengDTO) throws JsonProcessingException {
        if (sourceDamengDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sourceDamengDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        BaseConnectionParam connectionParam = (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                dbSource.getType(), dbSource.getConnectionParams());
        sourceDamengDTO.setUrl(DataSourceUtils.getJdbcUrl(dbSource.getType(), connectionParam));
        sourceDamengDTO.setUser(connectionParam.getUser());
        sourceDamengDTO.setDriver(connectionParam.getDriverClassName());
        sourceDamengDTO.setPassword(connectionParam.getPassword());

        if(connectionParam.getOther() != null){
            connectionParam.getOther().forEach((k,v) -> {
                // 兼容大小 写
                k = k.toLowerCase();
                if("currentschema".equals(k) || "schema".equals(k)) {
                    String tablePath = sourceDamengDTO.getTablePath();
                    if (!StringUtils.isNullOrEmpty(tablePath)) {
                        if (tablePath.contains(".")) {
                            String[] parts = tablePath.split("\\.", 2);
                            sourceDamengDTO.setTablePath(v + "." + parts[1]);
                        } else {
                            sourceDamengDTO.setTablePath(v + "." + tablePath);
                        }
                    }
                }
            });
        }else{
            log.warn("数据源未设置currentSchema 或 schema 默认使用数据库名");
        }
    }



    private void convertTableList(SourceDamengDTO sourceDamengDTO) {
        if (sourceDamengDTO.getTableList() == null || CollectionUtils.isEmpty(sourceDamengDTO.getTableList())) {
            return;
        }
        List<String> tableList = sourceDamengDTO.getTableList();
        List<TablePro> tables = new ArrayList<>();
        for (String tablePath : tableList) {
            TablePro tablePro = new TablePro();
            tablePro.setTablePath(tablePath);
        }
        sourceDamengDTO.setTableList(tables);
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "Dameng";
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
        SourceDamengDTO sourceDamengDTO = null;
        if (params == null) {
            return select;
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sourceDamengDTO = mapper.readValue(jsonString, SourceDamengDTO.class);
                String query = sourceDamengDTO.getQuery();
                StringBuilder sql = new StringBuilder();
                if (!StringUtils.isNullOrEmpty(query)) {
                     sql.append(query);
                     select = (Select) CCJSqlParserUtil.parse(sql.toString());
                } else {
                    String tableName = sourceDamengDTO.getTablePath();
                    String tablePrefix = getTablePrefix(taskExecutionContext, taskNode);
                    if (!StringUtils.isNullOrEmpty(tablePrefix)) {
                        tableName = tableName.contains(".") ? tableName.substring(tableName.indexOf(".") + 1) : tableName;
                        tableName = tablePrefix + tableName;
                    }
                    log.info("--------------------select tableName: {}", tableName);
                    String whereCondition = sourceDamengDTO.getWhereCondition();
                    log.info("SeaTunnel taskSourceDamengPlugin whereCondition: {}", whereCondition);
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
                    log.info("SeaTunnel taskSourceDamengPlugin sql: {}", sql.toString());
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
