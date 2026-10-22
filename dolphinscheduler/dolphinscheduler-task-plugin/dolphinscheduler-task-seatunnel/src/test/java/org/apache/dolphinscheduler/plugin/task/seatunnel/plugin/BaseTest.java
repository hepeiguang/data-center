package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.ResourceParametersHelper;
import org.apache.dolphinscheduler.plugin.task.seatunnel.SeatunnelParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.SeatunnelTask;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.junit.jupiter.api.Assertions;

import java.util.List;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-21 18:15
 **/
public abstract class BaseTest {
    public static final ObjectMapper mapper = new ObjectMapper()
            // 忽略null值字段
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
            // 忽略空数组/集合
            .setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);

    public void convertParams() throws Exception {
        setTaskFactory();
        TaskExecutionContext taskExecutionContext = new TaskExecutionContext();
        taskExecutionContext.setExecutePath(getExecutePath());
        taskExecutionContext.setTaskAppId(getTaskAppid());
        taskExecutionContext.setResourceParametersHelper(setDataSource());


        SeatunnelTask seatunnelTask = new SeatunnelTask(taskExecutionContext);
        SeatunnelParameters seatunnelParameters = new SeatunnelParameters();
        seatunnelParameters.setScriptType("visualize");
        seatunnelParameters.setLocalParams(setLocalParams());


        List<SeatunnelTaskNodeVO> taskNodeVOS = getTaskNodeList();
        String RAW_SCRIPT = JSONUtils.toJsonString(taskNodeVOS);
        seatunnelParameters.setRawScript(RAW_SCRIPT);
        seatunnelTask.setSeatunnelParameters(seatunnelParameters);

        taskExecutionContext.setTaskParams(JSONUtils.toJsonString(seatunnelParameters));

        Assertions.assertEquals(getExecutePath()+"/seatunnel_"+getTaskAppid()+".json", seatunnelTask.buildCustomConfigCommand());
    }


    public ResourceParametersHelper setDataSource() {

        DataSourceParameters dataSourceParameters = new DataSourceParameters();
        dataSourceParameters.setType(DbType.MYSQL);
        String connectionParams = "{\"user\":\"root\",\"password\":\"zdkj0018\",\"address\":\"jdbc:mysql://192.168.0.201:3306\",\"database\":\"dam\",\"jdbcUrl\":\"jdbc:mysql://192.168.0.201:3306/dam\",\"driverClassName\":\"com.mysql.cj.jdbc.Driver\",\"validationQuery\":\"select 1\"}";
        dataSourceParameters.setConnectionParams(connectionParams);

        ResourceParametersHelper resourceParametersHelper = new ResourceParametersHelper();
        resourceParametersHelper.put(ResourceType.DATASOURCE, 2, dataSourceParameters);
        return resourceParametersHelper;

    }

    private List<Property> setLocalParams() {
        String paramstring = "[{\"prop\":\"parallelism\",\"direct\":\"IN\",\"type\":\"VARCHAR\",\"value\":\"1\"},\n" +
                "{\"prop\":\"job.mode\",\"direct\":\"IN\",\"type\":\"VARCHAR\",\"value\":\"BATCH\"},\n" +
                "{\"prop\":\"checkpoint.interval\",\"direct\":\"IN\",\"type\":\"VARCHAR\",\"value\":\"10000\"}]";

        List<Property> localParams = null;
        try {
            localParams = mapper.readValue(paramstring, new TypeReference<List<Property>>() {
            });
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return localParams;
    }

    public abstract List<SeatunnelTaskNodeVO> getTaskNodeList();

    public abstract void setTaskFactory();

    public abstract String getExecutePath();


    public abstract String getTaskAppid();
}