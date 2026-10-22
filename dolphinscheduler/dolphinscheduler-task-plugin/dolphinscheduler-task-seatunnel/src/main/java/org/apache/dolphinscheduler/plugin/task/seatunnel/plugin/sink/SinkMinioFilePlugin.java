package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.MinioParamDto;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkMinioFileDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sink对象-Minio文件插件
 *
 * @author ZhaoYuan
 * @date 2025/5/11 18:20
 * @since JDK1.8
 */
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SinkMinioFilePlugin implements TaskTypeFactory {

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkMinioFileDTO minioFileDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSink().add(minioFileDTO);
        log.info("Success to add Sink MinioFile Plugin");
    }

    private SinkMinioFileDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SinkMinioFileDTO minioFileDTO = new SinkMinioFileDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                String jsonString = mapper.writeValueAsString(params);
                minioFileDTO = mapper.readValue(jsonString, SinkMinioFileDTO.class);

                //数据源处理
                DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                        .getResourceParameters(ResourceType.DATASOURCE, minioFileDTO.getDatasourceId());
                log.info("source minio datasource:{}", JSONUtils.toJsonString(dbSource));
                String connectionParams = dbSource.getConnectionParams();
                MinioParamDto paramDto = mapper.readValue(connectionParams, MinioParamDto.class);
                minioFileDTO.setFsS3aEndpoint(paramDto.getSecure() ? String.format("https://%s:%s", paramDto.getEndpoint(), paramDto.getPort())
                        : String.format("http://%s:%s", paramDto.getEndpoint(), paramDto.getPort()));
                minioFileDTO.setAccessKey(paramDto.getAccessKey());
                minioFileDTO.setSecretKey(paramDto.getSecretKey());
                minioFileDTO.setBucket(String.format("s3a://%s", minioFileDTO.getBucket()));
                //置空
                minioFileDTO.setDatasourceId(null);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        //处理List类数据字段
        if (ObjectUtils.isNotEmpty(minioFileDTO.getPartitionByVo())) {
            minioFileDTO.setPartitionBy(JSONUtils.parseObject(minioFileDTO.getPartitionByVo(), new TypeReference<List<String>>() {}));
            minioFileDTO.setPartitionByVo(null);
        }
        if (ObjectUtils.isNotEmpty(minioFileDTO.getSinkColumnsVo())) {
            minioFileDTO.setSinkColumns(JSONUtils.parseObject(minioFileDTO.getSinkColumnsVo(), new TypeReference<List<String>>() {}));
            minioFileDTO.setSinkColumnsVo(null);
        }

        minioFileDTO.setPluginInput(taskNode.getPreTaskcode());
        minioFileDTO.setPluginName("S3File");
        minioFileDTO.setParallelism(1);
        return minioFileDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "MinioFile";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}