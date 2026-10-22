package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

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
 * 源传输对象-虚拟数据源单元测试
 *
 * @author ZhaoYuan
 * @date 2025/5/5 19:04
 * @since JDK1.8
 */
public class SourceFakeSourcePluginTest extends BaseTest {
    private static final String EXECUTE_PATH = "C:\\Users\\ZY\\Desktop\\package";
    private static final String TASK_APPID = "3702";

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
        params.put("schemaVo", "{\"name\":\"string\",\"age\":\"int\",\"classes\":\"string\"}");
        params.put("row.num", 10);
        params.put("split.num", 1);
        params.put("string.fake.mode", "template");
        params.put("stringTemplateVo", "[\"1班\",\"2班\",\"3班\",\"4班\"]");
        params.put("intTemplateVo", "[1,2,3,5]");
        params.put("int.fake.mode", "range");
        params.put("int.min", 8);
        params.put("int.max", 10);
        seatunnelTaskNodeVO.setParams(params);
        seatunnelTaskNodeVO.setPluginName("FakeSource");
        seatunnelTaskNodeVO.setPreTaskcode("123");
        seatunnelTaskNodeVO.setPostTaskcode("345");

        List<SeatunnelTaskNodeVO> seatunnelTaskNodeVOS = new ArrayList<>();
        seatunnelTaskNodeVOS.add(seatunnelTaskNodeVO);
        return seatunnelTaskNodeVOS;
    }

    @Override
    public void setTaskFactory() {
        TaskTypeFactory plugin = new SourceFakeSourcePlugin();
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