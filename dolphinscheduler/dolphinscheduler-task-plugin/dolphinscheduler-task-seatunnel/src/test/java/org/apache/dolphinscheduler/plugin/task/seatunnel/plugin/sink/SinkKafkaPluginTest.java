package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.ResourceParametersHelper;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.BaseTest;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SeatunnelPluginManager;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source.SourceJdbcPlugin;
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-21 16:59
 **/
public class SinkKafkaPluginTest extends BaseTest {
    private static final String EXECUTE_PATH = "D:\\资料\\数据资产治理平台\\";
    private static final String TASK_APPID = "9530";

    @Test
    public void convertParams() throws Exception {
        super.convertParams();
    }

    @Override
    public List<SeatunnelTaskNodeVO> getTaskNodeList() {
        SeatunnelTaskNodeVO seatunnelTaskNodeVO = new SeatunnelTaskNodeVO();
        seatunnelTaskNodeVO.setType(SeatunnelTaskTypeEnum.SINK.name());
        seatunnelTaskNodeVO.setCode("123");
        Map<String, Object> params = new HashMap<>();
        params.put("datasourceId", 2);
        seatunnelTaskNodeVO.setParams(params);
        seatunnelTaskNodeVO.setPluginName("Kafka");
        seatunnelTaskNodeVO.setPreTaskcode("123");
        seatunnelTaskNodeVO.setPostTaskcode("345");

        List<SeatunnelTaskNodeVO> seatunnelTaskNodeVOS = new ArrayList<>();
        seatunnelTaskNodeVOS.add(seatunnelTaskNodeVO);
        return seatunnelTaskNodeVOS;
    }

    public ResourceParametersHelper setDataSource() {

        DataSourceParameters dataSourceParameters = new DataSourceParameters();
        dataSourceParameters.setType(DbType.KAFKA);
        Map<String,String> kafka= new HashMap<>();
        kafka.put("timeout","20");
        Map<String,Object> connectionParams = new HashMap<>();
        connectionParams.put("host", "localhost:9092");
        connectionParams.put("other", kafka);
        try {
            dataSourceParameters.setConnectionParams(mapper.writeValueAsString(connectionParams));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ResourceParametersHelper resourceParametersHelper = new ResourceParametersHelper();
        resourceParametersHelper.put(ResourceType.DATASOURCE, 2, dataSourceParameters);
        return resourceParametersHelper;

    }

    @Override
    public void setTaskFactory() {
        TaskTypeFactory plugin = new SinkKafkaPlugin();
        SeatunnelPluginManager.getTaskTypeFactoryMaptest().put(plugin.getName().toLowerCase(), plugin);
    }

    @Override
    public String getExecutePath() {
        return EXECUTE_PATH;
    }

    @Override
    public String  getTaskAppid() {
        return TASK_APPID;

    }
}