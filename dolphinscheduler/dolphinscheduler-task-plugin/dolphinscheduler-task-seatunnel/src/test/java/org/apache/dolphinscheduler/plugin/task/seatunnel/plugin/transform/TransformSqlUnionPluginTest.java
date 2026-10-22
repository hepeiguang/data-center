package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.BaseTest;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SeatunnelPluginManager;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransformSqlUnionDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Map.*;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-21 16:59
 **/
public class TransformSqlUnionPluginTest extends BaseTest {
    private static final String EXECUTE_PATH = "D:\\资料\\数据资产治理平台\\";
    private static final String TASK_APPID = "9527";

    @Test
    public void convertParams() throws Exception {
        super.convertParams();
    }

    @Override
    public List<SeatunnelTaskNodeVO> getTaskNodeList() {
        SeatunnelTaskNodeVO seatunnelTaskNodeVO = new SeatunnelTaskNodeVO();
        seatunnelTaskNodeVO.setType(SeatunnelTaskTypeEnum.TRANSFORM.getTypeName());
        seatunnelTaskNodeVO.setCode("123");
        Map<String, Object> params = new HashMap<>();
        params.put("sourceTable", "table1");


        List<String> targetTables = new ArrayList<>();
        targetTables.add("table2");
        targetTables.add("table3");
        params.put("targetTables", targetTables);


        List<Map<String, String>> fields = new ArrayList<>();
        Map<String, String> field1 = new HashMap<>();
        field1.put("table1", "id");
        field1.put("table2", "user_id");
        field1.put("table3", "client_id");
        field1.put("aliasName", "id");
        fields.add(field1);

        Map<String, String> field2 = new HashMap<>();
        field2.put("table1", "name");
        field2.put("table2", "username");
        field2.put("table3", "client_name");
        field2.put("aliasName", "name");
        fields.add(field2);

        Map<String, String> field3 = new HashMap<>();
        field3.put("table1", "age");
        field3.put("table2", "user_age");
        field3.put("table3", null);
        field3.put("aliasName", "age");
        fields.add(field3);
        params.put("fields", fields);
        seatunnelTaskNodeVO.setParams(params);
        seatunnelTaskNodeVO.setPluginName("sqlunion");
        seatunnelTaskNodeVO.setPreTaskcode("123");
        seatunnelTaskNodeVO.setPostTaskcode("345");

        List<SeatunnelTaskNodeVO> seatunnelTaskNodeVOS = new ArrayList<>();
        seatunnelTaskNodeVOS.add(seatunnelTaskNodeVO);
        return seatunnelTaskNodeVOS;
    }

    @Override
    public void setTaskFactory() {
        TaskTypeFactory plugin = new TransformSqlUnionPlugin();
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