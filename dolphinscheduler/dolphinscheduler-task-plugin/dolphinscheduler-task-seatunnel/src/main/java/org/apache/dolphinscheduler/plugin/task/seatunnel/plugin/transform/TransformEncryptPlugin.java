package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.transform;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.auto.service.AutoService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.TaskTypeFactory;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelConfigDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormEncryptDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormEncryptItemDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.DsSeatunnelTaskEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.enums.SeatunnelTaskTypeEnum;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.utils.EncryptUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 19:01
 **/
@Slf4j
@AutoService(TaskTypeFactory.class)
public class TransformEncryptPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormEncryptDTO transFormEncryptDTO = convertParams(task);
        seatunnelConfigDTO.getTransform().add(transFormEncryptDTO);
        log.info("Success to add Transform Encrypt Plugin");
    }

    private TransFormEncryptDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        TransFormEncryptDTO transFormEncryptDTO = new TransFormEncryptDTO();
        Map<String,Object> params = taskNode.getParams();
        try {
            String jsonString = JSONUtils.toJsonString(params.get("custom_list"));
            transFormEncryptDTO.setCustomList(convertSecretKey(jsonString));
            transFormEncryptDTO.setPluginInput(taskNode.getPreTaskcode());
            transFormEncryptDTO.setPluginOutput(taskNode.getCode());
            transFormEncryptDTO.setPluginName(taskNode.getPluginName());
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return transFormEncryptDTO;
    }

    private List<TransFormEncryptItemDTO> convertSecretKey(String jsonString) {
        List<TransFormEncryptItemDTO> list = JSONUtils.toList(jsonString, TransFormEncryptItemDTO.class);
        if (list.isEmpty()) {
            return list;
        }
        Set<String> collect = list.stream().map(TransFormEncryptItemDTO::getEncryptType).collect(Collectors.toSet());
        String encryptJson = EncryptUtils.getSecretKey("encrypt", String.join(",", collect));
        ObjectNode encryptKeyRootNode;
        try {
            encryptKeyRootNode = (ObjectNode) mapper.readTree(encryptJson);
        } catch (JsonProcessingException e) {
            log.error("-------------------------------------获取获取密钥接口处理失败， 接口返回值为: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        list.forEach(item -> item.setSecretKey(encryptKeyRootNode.get(item.getEncryptType()).get("secretKey").asText()));
        return list;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "ENCRYPT";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
