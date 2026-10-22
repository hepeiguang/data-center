package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.HdfsFileParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceHdfsFileDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;


@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceHdfsPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceHdfsFileDTO sourceHdfsDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sourceHdfsDTO);
        log.info("Success to add Source HDFS File Plugin");
    }

    private SourceHdfsFileDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String, Object> params = taskNode.getParams();
        SourceHdfsFileDTO sourceHdfsDTO = null;
        if (params == null) {
            sourceHdfsDTO = new SourceHdfsFileDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sourceHdfsDTO = mapper.readValue(jsonString, SourceHdfsFileDTO.class);
                convertDatasource(taskExecutionContext, sourceHdfsDTO);
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            }
        }
        //转化schema字段
        if (ObjectUtils.isNotEmpty(sourceHdfsDTO.getSchemaVo())) {
            JsonNode schemaVo = JSONUtils.parseObject(sourceHdfsDTO.getSchemaVo());
            Map<String, Object> schemaMap = new HashMap<>();
            schemaMap.put("fields", schemaVo);
            sourceHdfsDTO.setSchema(schemaMap);
            sourceHdfsDTO.setSchemaVo(null);
        }

        sourceHdfsDTO.setPluginOutput(taskNode.getCode());
        sourceHdfsDTO.setPluginName(taskNode.getPluginName());
        return sourceHdfsDTO;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SourceHdfsFileDTO sourceHdfsFileDTO) throws JsonProcessingException {
        if (sourceHdfsFileDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sourceHdfsFileDTO.getDatasourceId());
        log.info("source hdfs file datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        HdfsFileParamDTO hdfsFileParamDTO = JSONUtils.parseObject(connectionParams, HdfsFileParamDTO.class);
        if (hdfsFileParamDTO != null) {
            sourceHdfsFileDTO.setFsDefaultFS(hdfsFileParamDTO.getFsDefault());
            sourceHdfsFileDTO.setHdfsSitePath(hdfsFileParamDTO.getHdfsSitePath());
            sourceHdfsFileDTO.setRemoteUser(hdfsFileParamDTO.getRemoteUser());
            if (hdfsFileParamDTO.getOther() != null) {
                sourceHdfsFileDTO.setKrb5Path(hdfsFileParamDTO.getOther().getKrb5Path());
                sourceHdfsFileDTO.setKerberosPrincipal(hdfsFileParamDTO.getOther().getKerberosPrincipal());
                sourceHdfsFileDTO.setKerberosKeytabPath(hdfsFileParamDTO.getOther().getKerberosKeytabPath());
            }
        }
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "HdfsFile";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}