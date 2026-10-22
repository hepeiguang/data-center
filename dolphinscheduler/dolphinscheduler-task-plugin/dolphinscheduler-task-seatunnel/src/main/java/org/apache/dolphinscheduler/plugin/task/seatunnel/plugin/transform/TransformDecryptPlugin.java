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
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormDecryptDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform.TransFormDecryptItemDTO;
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
public class TransformDecryptPlugin implements TaskTypeFactory {
    @Override
    public void assemblyConfigData(SeatunnelConfigDTO seatunnelConfigDTO, SeatunnelTaskNodeVO task, TaskExecutionContext taskExecutionContext) {
        if (CollectionUtils.isEmpty(seatunnelConfigDTO.getTransform())) {
            seatunnelConfigDTO.setTransform(new ArrayList<>());
        }
        TransFormDecryptDTO transFormDecryptDTO = convertParams(task);
        seatunnelConfigDTO.getTransform().add(transFormDecryptDTO);
        log.info("Success to add Transform Decrypt Plugin");
    }

    private TransFormDecryptDTO convertParams(SeatunnelTaskNodeVO taskNode) {
        TransFormDecryptDTO transFormDecryptDTO = new TransFormDecryptDTO();
        Map<String,Object> params = taskNode.getParams();
        try {
            String jsonString = JSONUtils.toJsonString(params.get("custom_list"));
            transFormDecryptDTO.setCustomList(convertSecretKey(jsonString));
            transFormDecryptDTO.setPluginInput(taskNode.getPreTaskcode());
            transFormDecryptDTO.setPluginOutput(taskNode.getCode());
            transFormDecryptDTO.setPluginName(taskNode.getPluginName());
        } catch (Exception e) {
            log.error("failed to convertParams", e);
        }
        return transFormDecryptDTO;
    }

    private List<TransFormDecryptItemDTO> convertSecretKey(String jsonString) {
        List<TransFormDecryptItemDTO> list = JSONUtils.toList(jsonString, TransFormDecryptItemDTO.class);
        if (list.isEmpty()) {
            return list;
        }
        Set<String> collect = list.stream().map(TransFormDecryptItemDTO::getDecryptType).collect(Collectors.toSet());
        String decryptJson = EncryptUtils.getSecretKey("decrypt", String.join(",", collect));
        ObjectNode decryptKeyRootNode;
        try {
            decryptKeyRootNode = (ObjectNode) mapper.readTree(decryptJson);
        } catch (JsonProcessingException e) {
            log.error("-------------------------------------获取获取密钥接口处理失败， 接口返回值为: {}", e.getMessage());
            throw new RuntimeException(e);
        }
        list.forEach(item -> item.setSecretKey(decryptKeyRootNode.get(item.getDecryptType()).get("secretKey").asText()));
        return list;
    }

    @Override
    public String getName() {
        return SeatunnelTaskTypeEnum.TRANSFORM + "_" + "DECRYPT";
    }

    @Override
    public DsSeatunnelTaskEnum getDsSeatunnelTaskEnum() {
        return null;
    }
}
