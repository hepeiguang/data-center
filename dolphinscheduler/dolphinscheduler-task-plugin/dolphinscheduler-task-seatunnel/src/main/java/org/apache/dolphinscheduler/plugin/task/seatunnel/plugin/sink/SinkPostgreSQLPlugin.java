package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.JdbcTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.ConnectParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkPostgreSQLDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.utils.ParamUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Map;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SinkPostgreSQLPlugin implements JdbcTaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkPostgreSQLDTO sinkPostgreSQLDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSink().add(sinkPostgreSQLDTO);
        log.info("Success to add Sink PostgreSQL Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "PostgreSQL";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.SINK_JDBC;
    }

    private SinkPostgreSQLDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SinkPostgreSQLDTO sinkPluginValue = null;

        Map<String, Object> params = taskNode.getParams();
        if (params == null) {
            sinkPluginValue = new SinkPostgreSQLDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sinkPluginValue = mapper.readValue(jsonString, SinkPostgreSQLDTO.class);
                ParamUtils.extractMapByFieldName(params, "properties", sinkPluginValue::setProperties);
                //处理datassource字段
                convertDatasource(taskExecutionContext, sinkPluginValue);
                //处理generateSinkSql字段
                setGenerateSinkSql(sinkPluginValue);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        sinkPluginValue.setPluginInput(taskNode.getPreTaskcode());
        sinkPluginValue.setPluginName("Jdbc");
        return sinkPluginValue;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SinkPostgreSQLDTO sinkPostgreSQLDTO) throws JsonProcessingException {
        if (sinkPostgreSQLDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sinkPostgreSQLDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        ConnectParamDTO connectParamDTO = mapper.readValue(connectionParams, ConnectParamDTO.class);
        sinkPostgreSQLDTO.setUrl(getJdbcUrl(connectParamDTO));
        sinkPostgreSQLDTO.setUser(connectParamDTO.getUser());
        sinkPostgreSQLDTO.setDriver(connectParamDTO.getDriverClassName());
        sinkPostgreSQLDTO.setPassword(connectParamDTO.getPassword());
        if (connectParamDTO.getOther() != null) {
            connectParamDTO.getOther().forEach((k, v) -> {
                // 兼容大小写
                k = k.toLowerCase();
                if ("currentschema".equals(k) || "schema".equals(k)) {
                    sinkPostgreSQLDTO.setTable(v + "." + sinkPostgreSQLDTO.getTable());
                }
            });
        } else {
            sinkPostgreSQLDTO.setTable("public." + sinkPostgreSQLDTO.getTable());
            log.warn("数据源未设置currentSchema 或 schema 默认使用数据库名");
        }
    }

    private void setGenerateSinkSql(SinkPostgreSQLDTO sinkPostgreSQLDTO) {
        if (sinkPostgreSQLDTO.getDatabase() != null && sinkPostgreSQLDTO.getTable() != null) {
            sinkPostgreSQLDTO.setGenerateSinkSql(true);
        } else {
            sinkPostgreSQLDTO.setGenerateSinkSql(false);
        }
    }
}
