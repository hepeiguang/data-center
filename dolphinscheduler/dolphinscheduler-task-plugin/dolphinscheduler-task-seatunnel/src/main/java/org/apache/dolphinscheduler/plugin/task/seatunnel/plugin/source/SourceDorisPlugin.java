package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.ResourceType;
import org.apache.dolphinscheduler.plugin.task.api.parameters.resource.DataSourceParameters;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceDorisDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.TablePro;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @program: data-center-platform-20250414
 * @author: wembo
 * @create: 2025-05-27 11:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceDorisPlugin implements TaskTypeFactory {
    private static final Pattern IP_PATTERN = Pattern.compile(
            "\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b|" + // IPv4
                    "\\[([0-9a-fA-F:]+)\\]" // IPv6 (方括号内)
    );
    private static final Pattern PORT_PATTERN = Pattern.compile("(?:@|//)(?:\\[[^\\]]+\\]|[^:/]+):(\\d+)");



    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceDorisDTO sourceDorisDTO = convertParams(task, taskExecutionContext);
        seatunnelConfigDTO.getSource().add(sourceDorisDTO);
        log.info("Success to add Source Doris Plugin");
    }

    private SourceDorisDTO convertParams(SeatunnelTaskNodeVO taskNode, TaskExecutionContext taskExecutionContext) {
        Map<String, Object> params = taskNode.getParams();
        SourceDorisDTO sourceDorisDTO = null;
        if (params == null) {
            sourceDorisDTO = new SourceDorisDTO();
        } else {
            try {
                String jsonString = mapper.writeValueAsString(params);
                sourceDorisDTO = mapper.readValue(jsonString, SourceDorisDTO.class);
                convertDatasource(taskExecutionContext, sourceDorisDTO,params);
               // convertTableList(sourceDorisDTO);
            } catch (JsonProcessingException e) {
                log.error("failed to convertParams", e);
            }

        }
        sourceDorisDTO.setPluginOutput(taskNode.getCode());
        String pluginName = taskNode.getPluginName().substring(0, 1).toUpperCase() +
                taskNode.getPluginName().substring(1).toLowerCase();
        sourceDorisDTO.setPluginName(pluginName);
        return sourceDorisDTO;
    }

    private void convertDatasource(TaskExecutionContext taskExecutionContext, SourceDorisDTO sourceDorisDTO ,Map<String, Object> params) throws JsonProcessingException {
        if (sourceDorisDTO.getDatasourceId() == null) {
            return;
        }
        DataSourceParameters dbSource = (DataSourceParameters) taskExecutionContext.getResourceParametersHelper()
                .getResourceParameters(ResourceType.DATASOURCE, sourceDorisDTO.getDatasourceId());
        log.info("source doris datasource:{}", JSONUtils.toJsonString(dbSource));
        BaseConnectionParam connectionParam = (BaseConnectionParam) DataSourceUtils.buildConnectionParams(
                dbSource.getType(), dbSource.getConnectionParams());
        String feHttpPort = connectionParam.getOther() == null || connectionParam.getOther().isEmpty() ? "8030" :
                connectionParam.getOther().get("feHttpPort");
        sourceDorisDTO.setQueryPort(connectionParam.getPort());
        sourceDorisDTO.setFenodes(connectionParam.getHost() + ":" + feHttpPort);
        sourceDorisDTO.setUsername(connectionParam.getUser());
        sourceDorisDTO.setPassword(connectionParam.getPassword());
        sourceDorisDTO.setDatabase(connectionParam.getDatabase());
        int length = connectionParam.getDatabase().length();
        sourceDorisDTO.setTable(skipFirstNChars(sourceDorisDTO.getTablePath(),length+1));
        //sourceDorisDTO.setTable(sourceDorisDTO.getTablePath());
        sourceDorisDTO.setDorisRequestQueryTimeouts((Integer)params.get("dorisRequestQueryTimeouts"));
        sourceDorisDTO.setDorisRequestReadTimeoutMs((Integer)params.get("dorisRequestReadTimeoutMs"));
        sourceDorisDTO.setDorisRequestConnectTimeoutMs((Integer)params.get("dorisRequestConnectTimeoutMs"));
        sourceDorisDTO.setDorisRequestRetries((Integer)params.get("dorisRequestRetries"));
        sourceDorisDTO.setDorisBatchSize((Integer)params.get("dorisBatchSize"));
        Object memLimitObj = params.get("dorisExecMemLimit");
        sourceDorisDTO.setDorisExecMemLimit(((Number) memLimitObj).longValue());
        List<String> readFields = (List<String>) params.getOrDefault("dorisReadField", Collections.emptyList());
        sourceDorisDTO.setDorisReadField(String.join(",", readFields));
        System.out.println(sourceDorisDTO.toString()+"sourceDorisDTO");
    }

    private void convertTableList(SourceDorisDTO sourceDorisDTO) {
        if (sourceDorisDTO.getTableList() == null || CollectionUtils.isEmpty(sourceDorisDTO.getTableList())) {
            return;
        }
        List<String> tableList = sourceDorisDTO.getTableList();
        List<TablePro> tables = new ArrayList<>();
        for (String tablePath : tableList) {
            TablePro tablePro = new TablePro();
            tablePro.setTablePath(tablePath);
        }
        sourceDorisDTO.setTableList(tables);
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "DORIS";
    }

    //获取Doris的fenodes的fe_host
    public static String getDoridHost(String jdbcUrl) {
        Matcher matcher = IP_PATTERN.matcher(jdbcUrl);
        if (matcher.find()) {
            return matcher.group();
        } else {
            return "未检测到ip";
        }
    }
    public static Integer getDorisPort(String jdbcUrl) {
        Matcher matcher = PORT_PATTERN.matcher(jdbcUrl);
        if (matcher.find()) {
            return Integer.valueOf(matcher.group(1));
        } else {
            return 9030;
        }
    }
    public static String skipFirstNChars(String input, int n) {
        if (n <= 0) return input;
        if (input == null || input.isEmpty()) return "";
        if (n >= input.length()) return "";

        return input.substring(n);
    }

}