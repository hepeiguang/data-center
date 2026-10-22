package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin;

import org.apache.commons.collections4.MapUtils;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.ConnectParamDTO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 18:59
 **/
public interface JdbcTaskTypeFactory extends TaskTypeFactory {
    default String getJdbcUrl(ConnectParamDTO connectParamDTO) {
        if (MapUtils.isEmpty(connectParamDTO.getOther())) {
            return connectParamDTO.getJdbcUrl();

        }
        return String.format("%s?%s", connectParamDTO.getJdbcUrl(),
                transformOther(connectParamDTO.getOther()));
    }

    default String transformOther(Map<String, String> otherMap) {
        if (MapUtils.isEmpty(otherMap)) {
            return null;
        }
        List<String> otherList = new ArrayList<>();
        otherMap.forEach((key, value) -> otherList.add(String.format("%s=%s", key, value)));
        return String.join("&", otherList);
    }


}
