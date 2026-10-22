package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.BaseTest;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.SeatunnelPluginManager;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform.TransformFilterPlugin;
import org.junit.Test;

import java.util.*;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-21 16:59
 **/
public class TransformFileterPluginTest extends BaseTest {
    private static final String EXECUTE_PATH = "D:\\资料\\数据资产治理平台\\";
    private static final String TASK_APPID = "9526";

    @Test
    public void convertParams() throws Exception {
        super.convertParams();
    }

    @Override
    public List<SeatunnelTaskNodeVO> getTaskNodeList() {
        ObjectMapper mapper = new ObjectMapper();
        SeatunnelTaskNodeVO seatunnelTaskNodeVO = new SeatunnelTaskNodeVO();
        seatunnelTaskNodeVO.setType(SeatunnelTaskTypeEnum.TRANSFORM.name());
        seatunnelTaskNodeVO.setCode("123");
        Map<String, Object> params = new HashMap<>();
        List<Object> lt = new ArrayList<>();
        String stl = "[\n" +
                "    {\n" +
                "        \"field\": \"ID\",\n" +
                "        \"fieldType\": \"VARCHAR\",\n" +
                "        \"fieldDes\": \"主键ID\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"NAME\",\n" +
                "        \"fieldType\": \"VARCHAR\",\n" +
                "        \"fieldDes\": \"应用名称\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"ENCY_KEY\",\n" +
                "        \"fieldType\": \"VARCHAR\",\n" +
                "        \"fieldDes\": \"加密key\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"ENCY_SALT\",\n" +
                "        \"fieldType\": \"VARCHAR\",\n" +
                "        \"fieldDes\": \"加密盐\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"APP_SECRET\",\n" +
                "        \"fieldType\": \"VARCHAR\",\n" +
                "        \"fieldDes\": \"应用认证信息\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"EXPIRATION_TIME\",\n" +
                "        \"fieldType\": \"DATETIME\",\n" +
                "        \"fieldDes\": \"应用认证过期时间\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"UPDATE_SECRET\",\n" +
                "        \"fieldType\": \"TINYINT\",\n" +
                "        \"fieldDes\": \"是否定时更新应用认证信息\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"UPDATE_CYCLE\",\n" +
                "        \"fieldType\": \"INT\",\n" +
                "        \"fieldDes\": \"定时更新认证周期\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"LAST_UPDATE_SECRET_TIME\",\n" +
                "        \"fieldType\": \"DATETIME\",\n" +
                "        \"fieldDes\": \"上次更新认证信息时间\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"STATUS\",\n" +
                "        \"fieldType\": \"TINYINT\",\n" +
                "        \"fieldDes\": \"状态（0不启用，1启用）\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"CREATE_BY\",\n" +
                "        \"fieldType\": \"VARCHAR\",\n" +
                "        \"fieldDes\": \"创建人\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"CREATE_TIME\",\n" +
                "        \"fieldType\": \"DATETIME\",\n" +
                "        \"fieldDes\": \"创建日期\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"CREATE_DEPT\",\n" +
                "        \"fieldType\": \"VARCHAR\",\n" +
                "        \"fieldDes\": \"创建人所属部门\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"UPDATE_BY\",\n" +
                "        \"fieldType\": \"VARCHAR\",\n" +
                "        \"fieldDes\": \"更新人\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"UPDATE_TIME\",\n" +
                "        \"fieldType\": \"DATETIME\",\n" +
                "        \"fieldDes\": \"更新日期\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ],\n" +
                "        \"hiddenDel\": true\n" +
                "    },\n" +
                "    {\n" +
                "        \"field\": \"REMARK\",\n" +
                "        \"fieldType\": \"VARCHAR\",\n" +
                "        \"fieldDes\": \"备注\",\n" +
                "        \"retain\": [\n" +
                "            true\n" +
                "        ]\n" +
                "    }\n" +
                "]";

        try {
            lt = mapper.readValue(stl, new TypeReference<List<Object>>() {});
        } catch (Exception e) {
//            log.error("parse list exception!", e);
        }
//        ObjectNode aa = JSONUtils.parseObject(stl);
//        ArrayNode a2 = (ArrayNode) aa.get("include_fields");
//        mapper.readValue(a2,new TypeReference<List<TablesInfo>>() {})
        params.put("include_fields",lt);
        seatunnelTaskNodeVO.setParams(params);
        seatunnelTaskNodeVO.setPluginName("Filter");
        seatunnelTaskNodeVO.setPreTaskcode("123");
        seatunnelTaskNodeVO.setPostTaskcode("345");

        List<SeatunnelTaskNodeVO> seatunnelTaskNodeVOS = new ArrayList<>();
        seatunnelTaskNodeVOS.add(seatunnelTaskNodeVO);
        return seatunnelTaskNodeVOS;
    }

    @Override
    public void setTaskFactory() {
        TaskTypeFactory plugin = new TransformFilterPlugin();
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
