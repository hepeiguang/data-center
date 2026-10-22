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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceOpenGaussDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.TablePro;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.utils.ParamUtils;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserBuilder;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * SourceOpenGaussPlugin
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceOpenGaussPlugin implements JdbcTaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceOpenGaussDTO sourceOpenGaussDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sourceOpenGaussDTO);
        log.info("Success to add Source opengauss Plugin");
    }

    private SourceOpenGaussDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String,Object> params = taskNode.getParams();
        SourceOpenGaussDTO sourceOpenGaussDTO = null;
        if (params == null) {
            sourceOpenGaussDTO = new SourceOpenGaussDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sourceOpenGaussDTO = mapper.readValue(jsonString, SourceOpenGaussDTO.class);
                ParamUtils.extractMapByFieldName(params, "properties", sourceOpenGaussDTO::setProperties);
                convertDatasource(taskExecutionContext, sourceOpenGaussDTO);
                if (taskNode.getSelect() != null) {
                    sourceOpenGaussDTO.setQuery(taskNode.getSelect().toString());
                    sourceOpenGaussDTO.setTablePath(null);
                    sourceOpenGaussDTO.setWhereCondition(null);
                } else {
                    convertTableList(sourceOpenGaussDTO);
                }
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            }
        }
        assert sourceOpenGaussDTO != null;
        sourceOpenGaussDTO.setPluginOutput(taskNode.getCode());
        sourceOpenGaussDTO.setPluginName("Jdbc");
        return sourceOpenGaussDTO;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SourceOpenGaussDTO sourceOpenGaussDTO) throws JsonProcessingException {
        if (sourceOpenGaussDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sourceOpenGaussDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        ConnectParamDTO connectParamDTO = mapper.readValue(connectionParams,ConnectParamDTO.class);
        sourceOpenGaussDTO.setUrl(getJdbcUrl(connectParamDTO));
        sourceOpenGaussDTO.setUser(connectParamDTO.getUser());
        sourceOpenGaussDTO.setDriver(connectParamDTO.getDriverClassName());
        sourceOpenGaussDTO.setPassword(connectParamDTO.getPassword());
        if(connectParamDTO.getOther() != null){
            connectParamDTO.getOther().forEach((k,v) -> {
                if(k.equals("currentSchema") || k.equals("schema")) {
                    String tablePath = sourceOpenGaussDTO.getTablePath();
                    if (!StringUtils.isNullOrEmpty(tablePath)) {
                        if (tablePath.contains(".")) {
                            // db.table -> db.schema.table
                            String[] parts = tablePath.split("\\.", 2);
                            sourceOpenGaussDTO.setTablePath(parts[0] + "." + v + "." + parts[1]);
                        } else {
                            // table -> schema.table （适用于默认情况）
                            sourceOpenGaussDTO.setTablePath(v + "." + tablePath);
                        }
                    }
                }
            });
        }else{
            log.warn("数据源未设置currentSchema 或 schema 默认使用数据库名");
        }
    }



    private void convertTableList(SourceOpenGaussDTO sourceOpenGaussDTO) {
        if (sourceOpenGaussDTO.getTableList() == null || CollectionUtils.isEmpty(sourceOpenGaussDTO.getTableList())) {
            return;
        }
        List<String> tableList = sourceOpenGaussDTO.getTableList();
        List<TablePro> tables = new ArrayList<>();
        for (String tablePath : tableList) {
            TablePro tablePro = new TablePro();
            tablePro.setTablePath(tablePath);
        }
        sourceOpenGaussDTO.setTableList(tables);
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "GaussDB";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.SOURCE_POSTGRESQL;
    }
    @Override
    public Select getCustomerSqlSelect(Select select, SeatunnelTaskNodeVO taskNode,TaskExecutionContext taskExecutionContext) {
        Map<String, Object> params = taskNode.getParams();
        if(select == null){
            select = new Select();
        }
        PlainSelect plainSelect = new PlainSelect();
        SourceOpenGaussDTO sourceOpenGaussDTO = null;
        if (params == null) {
            return select;
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sourceOpenGaussDTO = mapper.readValue(jsonString, SourceOpenGaussDTO.class);
                String query = sourceOpenGaussDTO.getQuery();
                StringBuilder sql = new StringBuilder();
                if (!StringUtils.isNullOrEmpty(query)) {
                     sql.append(query);
                     select = (Select) CCJSqlParserUtil.parse(sql.toString());
                } else {
                    String tableName = sourceOpenGaussDTO.getTablePath();
                    String tablePrefix = getTablePrefix(taskExecutionContext, taskNode);
                    if (!StringUtils.isNullOrEmpty(tablePrefix)) {
                        tableName = tableName.contains(".") ? tableName.substring(tableName.indexOf(".") + 1) : tableName;
                        tableName = tablePrefix + tableName;
                    }
                    log.info("--------------------select tableName: {}", tableName);
                    String whereCondition = sourceOpenGaussDTO.getWhereCondition();
                    log.info("SeaTunnel taskSourceOpenGaussPlugin whereCondition: {}", whereCondition);
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
                    log.info("SeaTunnel taskSourceOpenGaussPlugin sql: {}", sql.toString());
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
