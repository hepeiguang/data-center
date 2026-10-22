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
 * Sink对象-Hive单元测试
 *
 * @author ZhaoYuan
 * @date 2025/5/14 21:50
 * @since JDK1.8
 */
public class SinkHivePluginTest extends BaseTest {
    private static final String EXECUTE_PATH = "C:\\Users\\ZY\\Desktop\\package";
    private static final String TASK_APPID = "3753";

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
        params.put("table_name", "default.test_hive_sink_on_hdfs_with_kerberos");
        params.put("metastore_uri", "thrift://metastore:9083");
        params.put("hive_site_path", "/tmp/hive-site.xml");
        params.put("kerberos_principal", "hive/metastore.seatunnel@EXAMPLE.COM");
        params.put("kerberos_keytab_path", "/tmp/hive.keytab");
        params.put("krb5_path", "/tmp/krb5.conf");
        params.put("hiveHadoopConfVo", "{\"bucket\":\"s3a://mybucket\",\"fs.s3a.aws.credentials.provider\":\"com.amazonaws.auth.InstanceProfileCredentialsProvider\"}");
        seatunnelTaskNodeVO.setParams(params);

        seatunnelTaskNodeVO.setPluginName("Hive");
        seatunnelTaskNodeVO.setPreTaskcode("123");
        seatunnelTaskNodeVO.setPostTaskcode("345");

        List<SeatunnelTaskNodeVO> seatunnelTaskNodeVOS = new ArrayList<>();
        seatunnelTaskNodeVOS.add(seatunnelTaskNodeVO);
        return seatunnelTaskNodeVOS;
    }


    @Override
    public void setTaskFactory() {
        TaskTypeFactory plugin = new SinkHivePlugin();
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