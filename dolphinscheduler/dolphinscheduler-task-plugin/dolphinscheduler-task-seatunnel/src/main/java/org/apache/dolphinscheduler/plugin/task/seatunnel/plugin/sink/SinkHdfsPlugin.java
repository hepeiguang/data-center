package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkHdfsFileDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


@Slf4j
@AutoService(TaskTypeFactory.class)
public class SinkHdfsPlugin implements TaskTypeFactory {

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkHdfsFileDTO sinkHdfsDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSink().add(sinkHdfsDTO);
        log.info("Success to add sink hdfs file Plugin");
    }

    private SinkHdfsFileDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SinkHdfsFileDTO sinkHdfsDTO = new SinkHdfsFileDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sinkHdfsDTO = mapper.readValue(jsonString, SinkHdfsFileDTO.class);
                convertDatasource(taskExecutionContext, sinkHdfsDTO);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        //处理List类数据字段
        if (ObjectUtils.isNotEmpty(sinkHdfsDTO.getPartitionByVo())) {
            sinkHdfsDTO.setPartitionBy(JSONUtils.parseObject(sinkHdfsDTO.getPartitionByVo(), new TypeReference<List<String>>() {
            }));
            sinkHdfsDTO.setPartitionByVo(null);
        }
        if (ObjectUtils.isNotEmpty(sinkHdfsDTO.getSinkColumnsVo())) {
            sinkHdfsDTO.setSinkColumns(JSONUtils.parseObject(sinkHdfsDTO.getSinkColumnsVo(), new TypeReference<List<String>>() {
            }));
            sinkHdfsDTO.setSinkColumnsVo(null);
        }

        sinkHdfsDTO.setPluginInput(taskNode.getPreTaskcode());
        sinkHdfsDTO.setPluginName(taskNode.getPluginName());
        sinkHdfsDTO.setParallelism(1);
        return sinkHdfsDTO;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SinkHdfsFileDTO sinkHdfsFileDTO) throws JsonProcessingException {
        if (sinkHdfsFileDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sinkHdfsFileDTO.getDatasourceId());
        log.info("sink hdfs file datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        HdfsFileParamDTO hdfsFileParamDTO = JSONUtils.parseObject(connectionParams, HdfsFileParamDTO.class);
        if (hdfsFileParamDTO != null) {
            sinkHdfsFileDTO.setFsDefaultFS(hdfsFileParamDTO.getFsDefault());
            sinkHdfsFileDTO.setHdfsSitePath(hdfsFileParamDTO.getHdfsSitePath());
            sinkHdfsFileDTO.setRemoteUser(hdfsFileParamDTO.getRemoteUser());
            if (hdfsFileParamDTO.getOther() != null) {
                sinkHdfsFileDTO.setKrb5Path(hdfsFileParamDTO.getOther().getKrb5Path());
                sinkHdfsFileDTO.setKerberosPrincipal(hdfsFileParamDTO.getOther().getKerberosPrincipal());
                sinkHdfsFileDTO.setKerberosKeytabPath(hdfsFileParamDTO.getOther().getKerberosKeytabPath());
            }
        }
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "HdfsFile";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}