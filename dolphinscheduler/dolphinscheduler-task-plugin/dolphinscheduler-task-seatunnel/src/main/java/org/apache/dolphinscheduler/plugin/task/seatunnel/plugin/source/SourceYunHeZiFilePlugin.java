package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.api.utils.ParameterUtils;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.MinioParamDto;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.YunHeZiParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceMinioFileDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceYunHeZiFileDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.spi.enums.YunheziVersionType;
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
public class SourceYunHeZiFilePlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceYunHeZiFileDTO minioFileDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(minioFileDTO);
        log.info("Success to add Source MinioFile Plugin");
    }

    private SourceYunHeZiFileDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String,Object> params = taskNode.getParams();
        SourceYunHeZiFileDTO yunHeZiFileDTO = null;
        if (params == null) {
            yunHeZiFileDTO = new SourceYunHeZiFileDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                yunHeZiFileDTO = mapper.readValue(jsonString, SourceYunHeZiFileDTO.class);
                DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                        .getResourceParameters(ResourceType.DATASOURCE, yunHeZiFileDTO.getDatasourceId());

                log.debug("Connect YUNHEZI datasource: {}", JSONUtils.toJsonString(dbSource));
                String connectionParams = dbSource.getConnectionParams();
                YunHeZiParamDTO connectParamDTO = JSONUtils.parseObject(connectionParams, YunHeZiParamDTO.class);
                yunHeZiFileDTO.setHost(connectParamDTO.getHost());
                yunHeZiFileDTO.setPort(connectParamDTO.getPort());
                yunHeZiFileDTO.setProtocol(connectParamDTO.getProtocol());
                yunHeZiFileDTO.setAppId(connectParamDTO.getAppId());
                yunHeZiFileDTO.setAppSecret(connectParamDTO.getAppSecret());
                yunHeZiFileDTO.setAccount(connectParamDTO.getAccount());
                yunHeZiFileDTO.setConnectTimeoutMs(connectParamDTO.getConnectTimeout());
                yunHeZiFileDTO.setSocketTimeoutMs(connectParamDTO.getSocketTimeout());
                String version = connectParamDTO.getVersion();
                YunheziVersionType yunheziVersionType = YunheziVersionType.getByName(version);
                yunHeZiFileDTO.setTokenPattern(yunheziVersionType.getTokenPattern());


                // 对时间参数变量进行替换
                Map<String, String> paramsMap = convertParamsMap(taskExecutionContext.getPrepareParamsMap());
                String fileFilterModifiedStart =
                        ParameterUtils.convertParameterPlaceholders(yunHeZiFileDTO.getFileFilterModifiedStart(), paramsMap);
                String fileFilterModifiedEnd =
                        ParameterUtils.convertParameterPlaceholders(yunHeZiFileDTO.getFileFilterModifiedEnd(), paramsMap);
                yunHeZiFileDTO.setFileFilterModifiedStart(fileFilterModifiedStart);
                yunHeZiFileDTO.setFileFilterModifiedEnd(fileFilterModifiedEnd);
                yunHeZiFileDTO.setToAutoAnalysis(yunHeZiFileDTO.getAutoAnalysis());
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            }
        }

        //转化schema字段
        if (ObjectUtils.isNotEmpty(yunHeZiFileDTO.getSchemaVo())) {
            Map<String, Object> schemaVoMap = JSONUtils.json2Map(yunHeZiFileDTO.getSchemaVo());
            Map<String, Object> schemaMap = new HashMap<>();
            schemaMap.put("fields", schemaVoMap);
            yunHeZiFileDTO.setSchema(schemaMap);
            yunHeZiFileDTO.setSchemaVo(null);
        }

        yunHeZiFileDTO.setPluginOutput(taskNode.getCode());
        yunHeZiFileDTO.setPluginName("YunHeZiFile");
        return yunHeZiFileDTO;
    }

    private static Map<String, String> convertParamsMap(Map<String, Property> rawParamsMap) {
        Map<String, String> paramsMap = new HashMap<>();
        if (rawParamsMap != null) {
            for (Map.Entry<String, Property> entry : rawParamsMap.entrySet()) {
                if (entry.getValue() != null && entry.getValue().getValue() != null) {
                    paramsMap.put(entry.getKey(), entry.getValue().getValue());
                }
            }
        }
        return paramsMap;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "YunHeZiFile";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

}