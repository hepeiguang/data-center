package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.FtpParamDto;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceFtpFileDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 源传输对象-Ftp文件插件
 *
 * @author ZhaoYuan
 * @date 2025/5/7 21:29
 * @since JDK1.8
 */
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceFtpFilePlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceFtpFileDTO ftpFileDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(ftpFileDTO);
        log.info("Success to add Source FtpFile Plugin");
    }

    private SourceFtpFileDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String,Object> params = taskNode.getParams();
        SourceFtpFileDTO ftpFileDTO = null;
        if (params == null) {
            ftpFileDTO = new SourceFtpFileDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                ftpFileDTO = mapper.readValue(jsonString, SourceFtpFileDTO.class);

                //数据源处理
                DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                        .getResourceParameters(ResourceType.DATASOURCE, ftpFileDTO.getDatasourceId());
                log.info("source Ftp datasource:{}", JSONUtils.toJsonString(dbSource));
                String connectionParams = dbSource.getConnectionParams();
                FtpParamDto paramDto = mapper.readValue(connectionParams, FtpParamDto.class);
                ftpFileDTO.setHost(paramDto.getHost());
                ftpFileDTO.setPort(paramDto.getPort());
                ftpFileDTO.setUser(paramDto.getUser());
                ftpFileDTO.setPassword(paramDto.getPassword());
                //置空
                ftpFileDTO.setDatasourceId(null);
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            }
        }
        //转化schema字段
        if (ObjectUtils.isNotEmpty(ftpFileDTO.getSchemaVo())) {
            Map<String, Object> schemaVoMap = JSONUtils.json2Map(ftpFileDTO.getSchemaVo());
            Map<String, Object> schemaMap = new HashMap<>();
            schemaMap.put("fields", schemaVoMap);
            ftpFileDTO.setSchema(schemaMap);
            ftpFileDTO.setSchemaVo(null);
        }

        ftpFileDTO.setPluginOutput(taskNode.getCode());
        ftpFileDTO.setPluginName(taskNode.getPluginName());
        return ftpFileDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "FtpFile";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}