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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceSftpFileDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 源传输对象-Sftp文件插件
 *
 * @author ZhaoYuan
 * @date 2025/5/7 21:29
 * @since JDK1.8
 */
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceSftpFilePlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceSftpFileDTO sftpFileDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sftpFileDTO);
        log.info("Success to add Source SftpFile Plugin");
    }

    private SourceSftpFileDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String,Object> params = taskNode.getParams();
        SourceSftpFileDTO sftpFileDTO = null;
        if (params == null) {
            sftpFileDTO = new SourceSftpFileDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sftpFileDTO = mapper.readValue(jsonString, SourceSftpFileDTO.class);

                //数据源处理
                DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                        .getResourceParameters(ResourceType.DATASOURCE, sftpFileDTO.getDatasourceId());
                log.info("source Sftp datasource:{}", JSONUtils.toJsonString(dbSource));
                String connectionParams = dbSource.getConnectionParams();
                FtpParamDto paramDto = mapper.readValue(connectionParams, FtpParamDto.class);
                sftpFileDTO.setHost(paramDto.getHost());
                sftpFileDTO.setPort(paramDto.getPort());
                sftpFileDTO.setUser(paramDto.getUser());
                sftpFileDTO.setPassword(paramDto.getPassword());
                //置空
                sftpFileDTO.setDatasourceId(null);
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            }
        }
        //转化schema字段
        if (ObjectUtils.isNotEmpty(sftpFileDTO.getSchemaVo())) {
            Map<String, Object> schemaVoMap = JSONUtils.json2Map(sftpFileDTO.getSchemaVo());
            Map<String, Object> schemaMap = new HashMap<>();
            schemaMap.put("fields", schemaVoMap);
            sftpFileDTO.setSchema(schemaMap);
            sftpFileDTO.setSchemaVo(null);
        }

        sftpFileDTO.setPluginOutput(taskNode.getCode());
        sftpFileDTO.setPluginName(taskNode.getPluginName());
        return sftpFileDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "SftpFile";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}