package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.ConnectParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.sink.SinkDorisDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.text.SimpleDateFormat;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: data-center-platform-20250414
 * @author: wembo
 * @create: 2025-05- 11:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SinkDorisPlugin implements TaskTypeFactory {
    private static final Pattern IP_PATTERN = Pattern.compile(
            "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b|" + // IPv4
                    "\\[([0-9a-fA-F:]+)\\]" // IPv6 (方括号内)
    );
    private static final Pattern PORT_PATTERN = Pattern.compile("(?:@|//)(?:\\[[^\\]]+\\]|[^:/]+):(\\d+)");// IPv6 (方括号内)

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSink())) {
            seatunnelConfigDTO.setSink(new ArrayList<>());
        }
        SinkDorisDTO sinkDorisDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSink().add(sinkDorisDTO);
        log.info("Success to add Sink Doris Plugin");
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SINK + "_" + "Doris";
    }

    private SinkDorisDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        SinkDorisDTO sinkPluginValue = null;

        Map<String, Object> params = taskNode.getParams();
        if (params == null) {
            sinkPluginValue = new SinkDorisDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sinkPluginValue = mapper.readValue(jsonString, SinkDorisDTO.class);
                //处理datassource字段
                convertDatasource(taskExecutionContext, sinkPluginValue, params);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        if (sinkPluginValue.getPrimaryKeys() != null ) {
            // 从 params 中获取 primary_keys
            if (params != null && params.containsKey("primary_keys")) {
                Object primaryKeys = params.get("primary_keys");
                if (primaryKeys instanceof List) {
                    sinkPluginValue.setPrimaryKeys((List<String>) primaryKeys);
                } else if (primaryKeys instanceof String) {
                    // 解析字符串格式的主键
                    String keysStr = (String) primaryKeys;
                    if (keysStr.startsWith("[") && keysStr.endsWith("]")) {
                        try {
                            List<String> keys = mapper.readValue(keysStr, List.class);
                            sinkPluginValue.setPrimaryKeys(keys);
                        } catch (JsonProcessingException e) {
                            log.warn("Failed to parse primary_keys as JSON: {}", keysStr, e);
                        }
                    } else {
                        // 逗号分隔
                        List<String> keys = Arrays.asList(keysStr.split(","));
                        sinkPluginValue.setPrimaryKeys(keys);
                    }
                }
            }
        }

        sinkPluginValue.setPluginInput(taskNode.getPreTaskcode());
        sinkPluginValue.setPluginName(taskNode.getPluginName());
        return sinkPluginValue;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SinkDorisDTO sinkDorisDTO, Map<String, Object> params) throws JsonProcessingException {
        if (sinkDorisDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sinkDorisDTO.getDatasourceId());
        log.info("source doris datasource:{}", JSONUtils.toJsonString(dbSource));
        String connectionParams = dbSource.getConnectionParams();
        ConnectParamDTO connectParamDTO = mapper.readValue(connectionParams, ConnectParamDTO.class);
        String ips = getDorisHost(connectParamDTO.getAddress());
        String feHttpPort = connectParamDTO.getOther() == null || connectParamDTO.getOther().isEmpty() ? "8030" :
                connectParamDTO.getOther().get("feHttpPort");
        sinkDorisDTO.setFenodes(ips + ":" + feHttpPort);
        Integer port = getDorisPort(connectParamDTO.getJdbcUrl());
        sinkDorisDTO.setQueryPort(port);
        sinkDorisDTO.setUsername(connectParamDTO.getUser());
        sinkDorisDTO.setPassword(connectParamDTO.getPassword());
        sinkDorisDTO.setDatabase(connectParamDTO.getDatabase());
        //唯一标识
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
        String formattedDate = sdf.format(new Date());
        sinkDorisDTO.setSinkLabelPrefix(formattedDate);
        sinkDorisDTO.setTableIdentifier(connectParamDTO.getDatabase() + "." + sinkDorisDTO.getTable());
        sinkDorisDTO.setSchemaSaveMode((String) params.getOrDefault("schemaSaveMode", "CREATE_SCHEMA_WHEN_NOT_EXIST"));
//        sinkDorisDTO.setDataSaveMode((String) params.getOrDefault("dataSaveMode", "APPEND_DATA"));
        if (sinkDorisDTO.getDataSaveMode() == null || sinkDorisDTO.getDataSaveMode().isEmpty()) {
            sinkDorisDTO.setDataSaveMode((String) params.getOrDefault("dataSaveMode", "APPEND_DATA"));
        }
        if ("CUSTOM_PROCESSING".equalsIgnoreCase((String) params.getOrDefault("dataSaveMode", "APPEND_DATA"))) {
            sinkDorisDTO.setCustomSql((String) params.getOrDefault("custom_sql", ""));
        }
        sinkDorisDTO.setNeedsUnsupportedTypeCasting(Boolean.parseBoolean(String.valueOf(params.getOrDefault("needsUnsupportedTypeCasting", "false"))));
        sinkDorisDTO.setSinkEnableDelete(Boolean.parseBoolean(String.valueOf(params.getOrDefault("sinkEnableDelete", "false"))));
        sinkDorisDTO.setSinkEnable2pc(Boolean.parseBoolean(String.valueOf(params.getOrDefault("sinkEnable2pc", "false"))));
        sinkDorisDTO.setSinkCheckInterval((Integer) params.getOrDefault("sinkCheckInterval", 10000));
        sinkDorisDTO.setSinkMaxRetries((Integer) params.getOrDefault("sinkMaxRetries", 3));
        sinkDorisDTO.setSinkBufferSize((Integer) params.getOrDefault("sinkBufferSize", 262144));
        sinkDorisDTO.setSinkBufferCount((Integer) params.getOrDefault("sinkBufferCount", 3));
        sinkDorisDTO.setDorisBatchSize((Integer) params.getOrDefault("dorisBatchSize", 1024));
        Map<String, String> formatMap = new HashMap<>();
        String format = (String) params.getOrDefault("format", "json");
        if ("json".equals(format)) {
            formatMap.put("format", "json");
            Object readJsonByLineObj = params.get("readJsonByLine");
            formatMap.put("read_json_by_line", (String) readJsonByLineObj);
        } else {
            formatMap.put("format", "csv");
            Object columnSeparatorObj = params.get("columnSeparator");
            formatMap.put("column_separator", (String) columnSeparatorObj);
        }
        sinkDorisDTO.setDorisConfig(formatMap);
    }

    //获取Doris的fenodes的fe_host
    public static String getDorisHost(String jdbcUrl) {
        Matcher matcher = IP_PATTERN.matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return "未检测到ip";
        }
    }

//    public static int getDorisPort(String jdbcUrl) {
//        Matcher matcher = IP_PATTERN.matcher(jdbcUrl);
//        if (matcher.find()) {
//            return matcher.group();
//        } else {
//            return "未检测到ip";
//        }
//    }

    public static Integer getDorisPort(String jdbcUrl) {
        Matcher matcher = PORT_PATTERN.matcher(jdbcUrl);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        } else {
            return 9030;
        }
    }
}