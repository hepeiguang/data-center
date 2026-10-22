package apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.BaseTest;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SeatunnelPluginManager;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform.TransformSqlFunctionPlugin;
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
public class TransformSqlFunctionPluginTest extends BaseTest {
    private static final String EXECUTE_PATH = "D:\\资料\\数据资产治理平台\\";
    private static final String TASK_APPID = "9521";

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
        Map<String, Object> f = new HashMap<>();
        Map<String, Object> ff = new HashMap<>();

        Map<String, Object> f2 = new HashMap<>();
        Map<String, Object> ff2 = new HashMap<>();
        List<Map<String, Object>> filedsMap = new ArrayList<Map<String, Object>>();
        f.put("table_name","table");
        f.put("column_name","str1");
        f.put("new_name","n1");
        f.put("function_code","subStr");
        ff.put("startInt",1);
        ff.put("length",1);
        f.put("function_param",ff);

        f2.put("table_name","table");
        f2.put("column_name","str2");
        f2.put("new_name","n2");
        f2.put("function_code","replace");
        ff2.put("startInt","1");
        ff2.put("length",2);
        ff2.put("tags",3);
        f2.put("function_param",ff2);
        filedsMap.add(f);
        filedsMap.add(f2);
        params.put("columns",filedsMap);

        seatunnelTaskNodeVO.setParams(params);
        seatunnelTaskNodeVO.setPluginName("Sql");
        seatunnelTaskNodeVO.setPreTaskcode("123");
        seatunnelTaskNodeVO.setPostTaskcode("345");

        List<SeatunnelTaskNodeVO> seatunnelTaskNodeVOS = new ArrayList<>();
        seatunnelTaskNodeVOS.add(seatunnelTaskNodeVO);
        return seatunnelTaskNodeVOS;
    }

    @Override    public void setTaskFactory() {
        TaskTypeFactory plugin = new TransformSqlFunctionPlugin();
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
