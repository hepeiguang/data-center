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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.MinioParamDto;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceMinioFileDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * 源传输对象-Minio文件插件
 *
 * @author ZhaoYuan
 * @date 2025/5/11 9:12
 * @since JDK1.8
 */
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceMinioFilePlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceMinioFileDTO minioFileDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(minioFileDTO);
        log.info("Success to add Source MinioFile Plugin");
    }

    private SourceMinioFileDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String,Object> params = taskNode.getParams();
        SourceMinioFileDTO minioFileDTO = null;
        if (params == null) {
            minioFileDTO = new SourceMinioFileDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                minioFileDTO = mapper.readValue(jsonString, SourceMinioFileDTO.class);

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
                log.error("failed to convertParams", e);
            }
        }

        //转化schema字段
        if (ObjectUtils.isNotEmpty(minioFileDTO.getSchemaVo())) {
            Map<String, Object> schemaVoMap = JSONUtils.json2Map(minioFileDTO.getSchemaVo());
            Map<String, Object> schemaMap = new HashMap<>();
            schemaMap.put("fields", schemaVoMap);
            minioFileDTO.setSchema(schemaMap);
            minioFileDTO.setSchemaVo(null);
        }

        minioFileDTO.setPluginOutput(taskNode.getCode());
        minioFileDTO.setPluginName("S3File");
        return minioFileDTO;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "MinioFile";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}