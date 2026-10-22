package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.BaseTest;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SeatunnelPluginManager;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
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
public class SourceUdpPluginTest extends BaseTest {
    private static final String EXECUTE_PATH = "D:\\资料\\数据资产治理平台\\";
    private static final String TASK_APPID = "9532";

    @Test
    public void convertParams() throws Exception {
        super.convertParams();
    }

    @Override
    public List<SeatunnelTaskNodeVO> getTaskNodeList() {
        SeatunnelTaskNodeVO seatunnelTaskNodeVO = new SeatunnelTaskNodeVO();
        seatunnelTaskNodeVO.setType(SeatunnelTaskTypeEnum.SOURCE.name());
        seatunnelTaskNodeVO.setCode("123");
        Map<String, Object> params = new HashMap<>();
        Map<String, String> filedsMap = new HashMap<>();
        filedsMap.put("id", "int");
        filedsMap.put("name", "String");
        filedsMap.put("value", "String");
        try {
            params.put("schemaVo", mapper.writeValueAsString(filedsMap));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        seatunnelTaskNodeVO.setParams(params);
        seatunnelTaskNodeVO.setPluginName("UDP");
        seatunnelTaskNodeVO.setPreTaskcode("123");
        seatunnelTaskNodeVO.setPostTaskcode("345");

        List<SeatunnelTaskNodeVO> seatunnelTaskNodeVOS = new ArrayList<>();
        seatunnelTaskNodeVOS.add(seatunnelTaskNodeVO);
        return seatunnelTaskNodeVOS;
    }

    @Override
    public void setTaskFactory() {
        TaskTypeFactory plugin = new SourceUdpPlugin();
        SeatunnelPluginManager.getTaskTypeFactoryMaptest().put(plugin.getName().toLowerCase(), plugin);
    }

    @Override
    public String getExecutePath() {
        return EXECUTE_PATH;
    }

    @Override
    public String getTaskAppid() {
        return TASK_APPID;

    }
}