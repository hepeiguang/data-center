package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.source;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.source.SourceUdpDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.springframework.util.CollectionUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

@Slf4j
@AutoService(TaskTypeFactory.class)
public class SourceUdpPlugin implements TaskTypeFactory {
    private static final String DEFAULT_COL = "content";

    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task,
                                   TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getSource())) {
            seatunnelConfigDTO.setSource(new ArrayList<>());
        }
        SourceUdpDTO sourceUdpDTO = convertParams(task);
        seatunnelConfigDTO.getSource().add(sourceUdpDTO);
        log.info("Success to add Source UDP Plugin");
    }

    private SourceUdpDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        SourceUdpDTO pluginValue = new SourceUdpDTO();
        Map<String, Object> params = taskNode.getParams();
        if (params != null) {
            try {
                String jsonString = mapper.writeValueAsString(params);
                pluginValue = mapper.readValue(jsonString, SourceUdpDTO.class);
                convertSchema(pluginValue);
                resolveUdfCodecPath(pluginValue);
                // WRITE_ONLY 已避免写出；仍清空，避免后续逻辑误用
                pluginValue.setUdfName(null);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }
        pluginValue.setPluginOutput(taskNode.getCode());
        pluginValue.setPluginName(taskNode.getPluginName());
        pluginValue.setParallelism(1);
        return pluginValue;
    }

    /**
     * 若配置了 udfName，按 parseMode 解析为 scriptPath / offsetTablePath：
     * $SEATUNNEL_HOME/udf/udp-codec/{udfName}.js|json
     */
    private void resolveUdfCodecPath(SourceUdpDTO dto) {
        if (StringUtils.isBlank(dto.getUdfName())) {
            return;
        }
        String mode = dto.getParseMode() == null ? "none" : dto.getParseMode().trim().toLowerCase();
        Path codecDir = resolveCodecDir();
        if (codecDir == null) {
            log.warn("Cannot resolve UDP codec dir for udfName={}", dto.getUdfName());
            return;
        }
        String base = dto.getUdfName().trim();
        if ("js".equals(mode)) {
            Path js = pickExisting(codecDir, base, ".js");
            if (js != null) {
                dto.setScriptPath(js.toAbsolutePath().toString().replace('\\', '/'));
            } else {
                throw new RuntimeException("UDP JS UDF not found under " + codecDir + ": " + base + ".js");
            }
        } else if ("offset_table".equals(mode)) {
            Path json = pickExisting(codecDir, base, ".json");
            if (json != null) {
                dto.setOffsetTablePath(json.toAbsolutePath().toString().replace('\\', '/'));
            } else {
                throw new RuntimeException(
                        "UDP offset table UDF not found under " + codecDir + ": " + base + ".json");
            }
        }
    }

    private static Path pickExisting(Path dir, String base, String ext) {
        Path exact = dir.resolve(base.endsWith(ext) ? base : base + ext);
        if (Files.isRegularFile(exact)) {
            return exact;
        }
        return null;
    }

    private static Path resolveCodecDir() {
        String home = System.getenv("SEATUNNEL_HOME");
        if (StringUtils.isBlank(home)) {
            home = System.getProperty("seatunnel.home");
        }
        if (StringUtils.isBlank(home)) {
            return null;
        }
        return Paths.get(home.trim(), "udf", "udp-codec");
    }

    private void convertSchema(SourceUdpDTO sourceUdpDTO) {
        if (StringUtils.isEmpty(sourceUdpDTO.getSchemaVo())) {
            return;
        }
        Map<String, String> schemaVoMap = JSONUtils.toMap(sourceUdpDTO.getSchemaVo());
        Map<String, Object> seatunnelSchemaMap = new HashMap<>();
        seatunnelSchemaMap.put("fields", schemaVoMap);
        sourceUdpDTO.setSchema(seatunnelSchemaMap);
        sourceUdpDTO.setSchemaVo(null);
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.SOURCE + "_" + "UDP";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }

    @Override
    public OutputFieldDTO getOutputField(SeatunnelTaskNodeVO vo) {
        SourceUdpDTO sourceUdpDTO = convertParams(vo);
        if (sourceUdpDTO.isTextFormat()) {
            return new OutputFieldDTO(OutputFieldDTO.Type.OTHER).addField(DEFAULT_COL);
        }
        if (sourceUdpDTO.getSchema() == null) {
            throw new RuntimeException("请定义schema");
        }
        Map<String, String> schemaVoMap = (Map<String, String>) sourceUdpDTO.getSchema().get("fields");
        if (schemaVoMap == null) {
            throw new RuntimeException("请输入正确的schema");
        }
        return new OutputFieldDTO(OutputFieldDTO.Type.OTHER).setFields(new LinkedHashSet<>(schemaVoMap.keySet()));
    }
}
