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
import org.apache.dolphinscheduler.spi.enums.DbType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Sink对象-Minio文件单元测试
 *
 * @author ZhaoYuan
 * @date 2025/5/11 18:26
 * @since JDK1.8
 */
public class SinkMinioFilePluginTest extends BaseTest {
    private static final String EXECUTE_PATH = "C:\\Users\\ZY\\Desktop\\package";
    private static final String TASK_APPID = "3752";

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
        params.put("datasourceId", 7);
        params.put("bucket", "zhaoyuan");
        params.put("path", "/202505");
        params.put("file_format_type", "csv");
        params.put("have_partition", true);
        params.put("partitionByVo", "[\"class\"]");
        params.put("custom_filename", true);
        params.put("file_name_expression", "minio_${now}");
        params.put("filename_time_format", "yyyyMMddHHmmss");
        params.put("sinkColumnsVo", "[\"name\",\"age\"]");
        params.put("batch_size", 1000000);
        params.put("csv_string_quote_mode", "MINIMAL");
        params.put("create_empty_file_when_no_data", true);
        params.put("schema_save_mode", "CREATE_SCHEMA_WHEN_NOT_EXIST");
        params.put("data_save_mode", "APPEND_DATA");
        params.put("enable_header_write", true);
        params.put("encoding", "UTF-8");
        seatunnelTaskNodeVO.setParams(params);

        seatunnelTaskNodeVO.setPluginName("MinioFile");
        seatunnelTaskNodeVO.setPreTaskcode("123");
        seatunnelTaskNodeVO.setPostTaskcode("345");

        List<SeatunnelTaskNodeVO> seatunnelTaskNodeVOS = new ArrayList<>();
        seatunnelTaskNodeVOS.add(seatunnelTaskNodeVO);
        return seatunnelTaskNodeVOS;
    }

    /**
     * 重写数据源设置
     *
     * @author ZhaoYuan
     * @date 2025/5/11 16:20
     * @return 返回值
     */
    public ResourceParametersHelper setDataSource() {
        DataSourceParameters dataSourceParameters = new DataSourceParameters();
        dataSourceParameters.setType(DbType.MINIO);

        Map<String,Object> connectionParams = new HashMap<>();
        connectionParams.put("endpoint", "139.198.13.185");
        connectionParams.put("port", 9000);
        connectionParams.put("accessKey", "admin");
        connectionParams.put("secretKey", "Your@Strong!Password123");
        connectionParams.put("secure", false);
        try {
            dataSourceParameters.setConnectionParams(mapper.writeValueAsString(connectionParams));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ResourceParametersHelper resourceParametersHelper = new ResourceParametersHelper();
        resourceParametersHelper.put(ResourceType.DATASOURCE, 7, dataSourceParameters);
        return resourceParametersHelper;
    }

    @Override
    public void setTaskFactory() {
        TaskTypeFactory plugin = new SinkMinioFilePlugin();
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