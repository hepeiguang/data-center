package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.BaseTest;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SeatunnelPluginManager;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.RowErrorHandleWayEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform.TransformJsonPathPlugin;
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
public class TransformJsonPathPluginTest extends BaseTest {
    private static final String EXECUTE_PATH = "E:\\temp\\executor\\json\\";
    private static final String TASK_APPID = "9526";

    @Test
    public void convertParams() throws Exception {
        super.convertParams();
    }

    @Override
    public List<SeatunnelTaskNodeVO> getTaskNodeList() {
        SeatunnelTaskNodeVO seatunnelTaskNodeVO = new SeatunnelTaskNodeVO();
        seatunnelTaskNodeVO.setType(SeatunnelTaskTypeEnum.TRANSFORM.name());
        seatunnelTaskNodeVO.setCode("123");
        Map<String, Object> params = new HashMap<>();

        String str = "{\n" +
                "        \"plugin_input\": \"fake\",\n" +
                "        \"plugin_output\": \"fake1\",\n" +
                "        \"row_error_handle_way\": \"FAIL\",\n" +
                "        \"columns\": [\n" +
                "            {\n" +
                "                \"src_field\": \"col\",\n" +
                "                \"path\": \"$[0]\",\n" +
                "                \"dest_field\": \"name\",\n" +
                "                \"dest_type\": \"string\"\n" +
                "            },\n" +
                "            {\n" +
                "                \"src_field\": \"col\",\n" +
                "                \"path\": \"$[1]\",\n" +
                "                \"dest_field\": \"age\",\n" +
                "                \"dest_type\": \"int\"\n" +
                "            }\n" +
                "        ]\n" +
                "    }";
        params.put("JsonPath", str);
        seatunnelTaskNodeVO.setParams(params);
        seatunnelTaskNodeVO.setPluginName("JsonPath");
        seatunnelTaskNodeVO.setPreTaskcode("123");
        seatunnelTaskNodeVO.setPostTaskcode("345");

        List<SeatunnelTaskNodeVO> seatunnelTaskNodeVOS = new ArrayList<>();
        seatunnelTaskNodeVOS.add(seatunnelTaskNodeVO);
        return seatunnelTaskNodeVOS;
    }

    @Override
    public void setTaskFactory() {
        TaskTypeFactory plugin = new TransformJsonPathPlugin();
        SeatunnelPluginManager.getTaskTypeFactoryMaptest().put(plugin.getName(), plugin);
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