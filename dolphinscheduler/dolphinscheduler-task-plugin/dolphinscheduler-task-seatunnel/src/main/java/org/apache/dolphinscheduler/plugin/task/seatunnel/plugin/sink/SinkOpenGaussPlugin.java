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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkOpenGaussDTO;
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
public class SinkOpenGaussPlugin implements JdbcTaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkOpenGaussDTO sinkOpenGaussDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSink().add(sinkOpenGaussDTO);
        log.info("Success to add Sink opengauss Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "GaussDB";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.SINK_JDBC;
    }

    private SinkOpenGaussDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SinkOpenGaussDTO sinkPluginValue = null;

        Map<String, Object> params = taskNode.getParams();
        if (params == null) {
            sinkPluginValue = new SinkOpenGaussDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sinkPluginValue = mapper.readValue(jsonString, SinkOpenGaussDTO.class);
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

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SinkOpenGaussDTO sinkOpenGaussDTO) throws JsonProcessingException {
        if (sinkOpenGaussDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sinkOpenGaussDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        ConnectParamDTO connectParamDTO = mapper.readValue(connectionParams, ConnectParamDTO.class);
        sinkOpenGaussDTO.setUrl(getJdbcUrl(connectParamDTO));
        sinkOpenGaussDTO.setUser(connectParamDTO.getUser());
        sinkOpenGaussDTO.setDriver(connectParamDTO.getDriverClassName());
        sinkOpenGaussDTO.setPassword(connectParamDTO.getPassword());
        if (connectParamDTO.getOther() != null) {
            connectParamDTO.getOther().forEach((k, v) -> {
                if (k.equals("currentSchema") || k.equals("schema")) {
                    sinkOpenGaussDTO.setTable(v + "." + sinkOpenGaussDTO.getTable());
                }
            });
        } else {
            sinkOpenGaussDTO.setTable("public." + sinkOpenGaussDTO.getTable());
            log.warn("数据源未设置currentSchema 或 schema 默认使用数据库名");
        }
    }

    private void setGenerateSinkSql(SinkOpenGaussDTO sinkOpenGaussDTO) {
        if (sinkOpenGaussDTO.getDatabase() != null && sinkOpenGaussDTO.getTable() != null) {
            sinkOpenGaussDTO.setGenerateSinkSql(true);
        } else {
            sinkOpenGaussDTO.setGenerateSinkSql(false);
        }
    }
}
