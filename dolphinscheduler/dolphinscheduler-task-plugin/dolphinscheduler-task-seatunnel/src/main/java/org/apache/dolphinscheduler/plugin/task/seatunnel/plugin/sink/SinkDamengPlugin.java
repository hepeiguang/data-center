package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkDamengDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.utils.ParamUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Map;

/**
 * SinkDamengPlugin
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SinkDamengPlugin implements JdbcTaskTypeFactory {

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkDamengDTO sinkDamengDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSink().add(sinkDamengDTO);
        log.info("Success to add Sink DAMENG Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "DAMENG";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.SINK_JDBC;
    }

    private SinkDamengDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SinkDamengDTO sinkPluginValue = null;

        Map<String, Object> params = taskNode.getParams();
        if (params == null) {
            sinkPluginValue = new SinkDamengDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sinkPluginValue = mapper.readValue(jsonString, SinkDamengDTO.class);
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

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SinkDamengDTO sinkDamengDTO) throws JsonProcessingException {
        if (sinkDamengDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sinkDamengDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        ConnectParamDTO connectParamDTO = mapper.readValue(connectionParams, ConnectParamDTO.class);
        sinkDamengDTO.setUrl(getJdbcUrl(connectParamDTO));
        sinkDamengDTO.setUser(connectParamDTO.getUser());
        sinkDamengDTO.setDriver(connectParamDTO.getDriverClassName());
        sinkDamengDTO.setPassword(connectParamDTO.getPassword());
        if (connectParamDTO.getOther() != null) {
            connectParamDTO.getOther().forEach((k, v) -> {
                if(k != null){
                    // 兼容大小写
                    k = k.toLowerCase();
                    if ("currentschema".equals(k) || "schema".equals(k)) {
                        sinkDamengDTO.setTable(v + "." + sinkDamengDTO.getTable());
                    }
                }
            });
        } else {
            sinkDamengDTO.setTable(connectParamDTO.getDatabase()+ "." + sinkDamengDTO.getTable());
            log.warn("数据源未设置currentSchema 或 schema 默认使用数据库名");
        }
    }

    private void setGenerateSinkSql(SinkDamengDTO sinkDamengDTO) {
        if (sinkDamengDTO.getDatabase() != null && sinkDamengDTO.getTable() != null) {
            sinkDamengDTO.setGenerateSinkSql(true);
        } else {
            sinkDamengDTO.setGenerateSinkSql(false);
        }
    }
}
