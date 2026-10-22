package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkMysqlDTO;
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
public class SinkMysqlPlugin implements JdbcTaskTypeFactory {

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkMysqlDTO sinkMysqlDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSink().add(sinkMysqlDTO);
        log.info("Success to add Sink MySQL Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "MySQL";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.SINK_JDBC;
    }

    private SinkMysqlDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SinkMysqlDTO sinkPluginValue = null;

        Map<String, Object> params = taskNode.getParams();
        if (params == null) {
            sinkPluginValue = new SinkMysqlDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sinkPluginValue = mapper.readValue(jsonString, SinkMysqlDTO.class);
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

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SinkMysqlDTO sinkMysqlDTO) throws JsonProcessingException {
        if (sinkMysqlDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sinkMysqlDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        ConnectParamDTO connectParamDTO = mapper.readValue(connectionParams, ConnectParamDTO.class);
        sinkMysqlDTO.setUrl(getJdbcUrl(connectParamDTO));
        sinkMysqlDTO.setUser(connectParamDTO.getUser());
        sinkMysqlDTO.setDriver(connectParamDTO.getDriverClassName());
        sinkMysqlDTO.setPassword(connectParamDTO.getPassword());
    }

    private void setGenerateSinkSql(SinkMysqlDTO sinkMysqlDTO) {
        if (sinkMysqlDTO.getDatabase() != null && sinkMysqlDTO.getTable() != null) {
            sinkMysqlDTO.setGenerateSinkSql(true);
        } else {
            sinkMysqlDTO.setGenerateSinkSql(false);
        }
    }
}
