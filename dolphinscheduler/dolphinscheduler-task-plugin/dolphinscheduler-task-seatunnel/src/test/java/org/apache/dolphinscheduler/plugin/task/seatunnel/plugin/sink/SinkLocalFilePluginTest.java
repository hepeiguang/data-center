package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

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
 * Sink对象-本地文件单元测试
 *
 * @author ZhaoYuan
 * @date 2025/5/6 19:49
 * @since JDK1.8
 */
public class SinkLocalFilePluginTest extends BaseTest {
    private static final String EXECUTE_PATH = "C:\\Users\\ZY\\Desktop\\package";
    private static final String TASK_APPID = "3751";

    @Test
    public void convertParams() throws Exception {
        super.convertParams();
    }

    @Override
    public List<SeatunnelTaskNodeVO> getTaskNodeList() {
        SeatunnelTaskNodeVO seatunnelTaskNodeVO = new SeatunnelTaskNodeVO();
        seatunnelTaskNodeVO.setType(SeatunnelTaskTypeEnum.SINK.name());
        seatunnelTaskNodeVO.setCode("123");
        //注入参数
        Map<String, Object> params = new HashMap<>();
        params.put("path", "/data/zhaoyuan");
        params.put("file_format_type", "text");
        params.put("sinkColumnsVo", "[\"name\",\"age\"]");
        params.put("batch_size", 100);
        params.put("create_empty_file_when_no_data", true);
        params.put("enable_header_write", true);
        params.put("encoding", "UTF-8");
        seatunnelTaskNodeVO.setParams(params);

        seatunnelTaskNodeVO.setPluginName("LocalFile");
        seatunnelTaskNodeVO.setPreTaskcode("123");
        seatunnelTaskNodeVO.setPostTaskcode("345");

        List<SeatunnelTaskNodeVO> seatunnelTaskNodeVOS = new ArrayList<>();
        seatunnelTaskNodeVOS.add(seatunnelTaskNodeVO);
        return seatunnelTaskNodeVOS;
    }


    @Override
    public void setTaskFactory() {
        TaskTypeFactory plugin = new SinkLocalFilePlugin();
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