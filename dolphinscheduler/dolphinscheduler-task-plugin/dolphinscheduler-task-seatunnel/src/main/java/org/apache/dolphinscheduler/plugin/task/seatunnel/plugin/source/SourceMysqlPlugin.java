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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceMysqlDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.TablePro;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.utils.ParamUtils;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.SqlParserBuilder;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceMysqlPlugin implements JdbcTaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceMysqlDTO sourceMysqlDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sourceMysqlDTO);
        log.info("Success to add Source MySQL Plugin");
    }

    private SourceMysqlDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String,Object> params = taskNode.getParams();
        SourceMysqlDTO sourceMysqlDTO = null;
        if (params == null) {
            sourceMysqlDTO = new SourceMysqlDTO();
        } else {
            try {
//                if (params.containsKey("whereCondition")) {
//                    params.put("where_condition", params.get("whereCondition").toString());
//                }
                String jsonString = mapper.writeValueAsString(params);
                sourceMysqlDTO = mapper.readValue(jsonString, SourceMysqlDTO.class);
                ParamUtils.extractMapByFieldName(params, "properties", sourceMysqlDTO::setProperties);
                convertDatasource(taskExecutionContext, sourceMysqlDTO);
                if (taskNode.getSelect() != null) {
                    sourceMysqlDTO.setQuery(taskNode.getSelect().toString());
                    sourceMysqlDTO.setTablePath(null);
                    sourceMysqlDTO.setWhereCondition(null);
                } else {
                    convertTableList(sourceMysqlDTO);
                }
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            }
        }
        assert sourceMysqlDTO != null;
        sourceMysqlDTO.setPluginOutput(taskNode.getCode());
        sourceMysqlDTO.setPluginName("Jdbc");
        return sourceMysqlDTO;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SourceMysqlDTO sourceMysqlDTO) throws JsonProcessingException {
        if (sourceMysqlDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sourceMysqlDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        ConnectParamDTO connectParamDTO = mapper.readValue(connectionParams,ConnectParamDTO.class);
        sourceMysqlDTO.setUrl(getJdbcUrl(connectParamDTO));
        sourceMysqlDTO.setUser(connectParamDTO.getUser());
        sourceMysqlDTO.setDriver(connectParamDTO.getDriverClassName());
        sourceMysqlDTO.setPassword(connectParamDTO.getPassword());
    }



    private void convertTableList(SourceMysqlDTO sourceMysqlDTO) {
        if (sourceMysqlDTO.getTableList() == null || CollectionUtils.isEmpty(sourceMysqlDTO.getTableList())) {
            return;
        }
        List<String> tableList = sourceMysqlDTO.getTableList();
        List<TablePro> tables = new ArrayList<>();
        for (String tablePath : tableList) {
            TablePro tablePro = new TablePro();
            tablePro.setTablePath(tablePath);
        }
        sourceMysqlDTO.setTableList(tables);
    }

    @Override
    public String getName() {
        //todo
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "MySQL";
    }

    @Override
    //todo
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
        SourceMysqlDTO sourceMysqlDTO = null;
        if (params == null) {
            return select;
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sourceMysqlDTO = mapper.readValue(jsonString, SourceMysqlDTO.class);
                String query = sourceMysqlDTO.getQuery();
                StringBuilder sql = new StringBuilder();
                if (!StringUtils.isNullOrEmpty(query)) {
                     sql.append(query);
                     select = (Select) CCJSqlParserUtil.parse(sql.toString());
                } else {
                    String tableName = sourceMysqlDTO.getTablePath();
                    String tablePrefix = getTablePrefix(taskExecutionContext, taskNode);
                    if (!StringUtils.isNullOrEmpty(tablePrefix)) {
                        tableName = tableName.contains(".") ? tableName.substring(tableName.indexOf(".") + 1) : tableName;
                        tableName = tablePrefix + tableName;
                    }
                    log.info("--------------------select tableName: {}", tableName);
                    String whereCondition = sourceMysqlDTO.getWhereCondition();
                    log.info("SeaTunnel taskSourceJdbcPlugin whereCondition: {}", whereCondition);
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
