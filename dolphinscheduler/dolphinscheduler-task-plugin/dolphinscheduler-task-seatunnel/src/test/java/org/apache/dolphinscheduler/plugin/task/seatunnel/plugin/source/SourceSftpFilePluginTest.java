package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

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
 * 源传输对象-SftpFile文件单元测试
 *
 * @author ZhaoYuan
 * @date 2025/5/7 21:44
 * @since JDK1.8
 */
public class SourceSftpFilePluginTest extends BaseTest {
    private static final String EXECUTE_PATH = "C:\\Users\\ZY\\Desktop\\package";
    private static final String TASK_APPID = "3704";

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
        params.put("datasourceId", 9);
        params.put("path", "/data/test");
        params.put("file_format_type", "csv");
        params.put("schemaVo", "{\"name\":\"string\",\"age\":\"int\",\"classes\":\"string\"}");
        params.put("skip_header_row_number", 1);
        params.put("csv_use_header_line", false);
        params.put("filename_extension", "csv");
        params.put("compress_codec", "lzo");
        params.put("encoding", "UTF-8");
        seatunnelTaskNodeVO.setParams(params);
        seatunnelTaskNodeVO.setPluginName("SftpFile");
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
     * @date 2025/5/13 23:17
     * @return 返回值
     */
    public ResourceParametersHelper setDataSource() {
        DataSourceParameters dataSourceParameters = new DataSourceParameters();
        dataSourceParameters.setType(DbType.SFTP);

        Map<String,Object> connectionParams = new HashMap<>();
        connectionParams.put("host", "139.198.13.185");
        connectionParams.put("port", 22);
        connectionParams.put("user", "zhaoyuan");
        connectionParams.put("password", "123456");
        try {
            dataSourceParameters.setConnectionParams(mapper.writeValueAsString(connectionParams));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        ResourceParametersHelper resourceParametersHelper = new ResourceParametersHelper();
        resourceParametersHelper.put(ResourceType.DATASOURCE, 9, dataSourceParameters);
        return resourceParametersHelper;
    }

    @Override
    public void setTaskFactory() {
        TaskTypeFactory plugin = new SourceSftpFilePlugin();
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