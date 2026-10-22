package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.JdbcTaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkJdbcDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
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
public class SinkJdbcPlugin implements JdbcTaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkJdbcDTO sinkJdbcDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSink().add(sinkJdbcDTO);
        log.info("Success to add Source Jdbc Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "JDBC";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return DsSeatunnelTaskEnum.SINK_JDBC;
    }

    private SinkJdbcDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SinkJdbcDTO sinkPluginValue = null;

        Map<String, Object> params = taskNode.getParams();
        if (params == null) {
            sinkPluginValue = new SinkJdbcDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sinkPluginValue = mapper.readValue(jsonString, SinkJdbcDTO.class);
                //处理datassource字段
                convertDatasource(taskExecutionContext, sinkPluginValue);
                //处理generateSinkSql字段
                setGenerateSinkSql(sinkPluginValue);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        sinkPluginValue.setPluginInput(taskNode.getPreTaskcode());
        sinkPluginValue.setPluginName(taskNode.getPluginName());
        return sinkPluginValue;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SinkJdbcDTO sinkJdbcDTO) throws JsonProcessingException {
        if (sinkJdbcDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sinkJdbcDTO.getDatasourceId());
        log.info("source jdbc datasource:{}", JSONUtils.toJsonString(dbSource));
        BaseConnectionParam connectionParam = (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                dbSource.getType(), dbSource.getConnectionParams());
        sinkJdbcDTO.setUrl(DataSourceUtils.getJdbcUrl(dbSource.getType(), connectionParam));
        sinkJdbcDTO.setUser(connectionParam.getUser());
        sinkJdbcDTO.setDriver(connectionParam.getDriverClassName());
        sinkJdbcDTO.setPassword(connectionParam.getPassword());
        if (connectionParam.getOther() != null) {
            connectionParam.getOther().forEach((k, v) -> {
                // 兼容大小写
                k = k.toLowerCase();
                if ("currentschema".equals(k) || "schema".equals(k)) {
                    sinkJdbcDTO.setTable(v + "." + sinkJdbcDTO.getTable());
                }
            });
        } else {
            sinkJdbcDTO.setTable("public." + sinkJdbcDTO.getTable());
            log.warn("数据源未设置currentSchema 或 schema 默认使用数据库名");
        }
    }

    private void setGenerateSinkSql(SinkJdbcDTO sinkJdbcDTO) {
        if (sinkJdbcDTO.getDatabase() != null && sinkJdbcDTO.getTable() != null) {
            sinkJdbcDTO.setGenerateSinkSql(true);
        } else {
            sinkJdbcDTO.setGenerateSinkSql(false);
        }
    }
}
