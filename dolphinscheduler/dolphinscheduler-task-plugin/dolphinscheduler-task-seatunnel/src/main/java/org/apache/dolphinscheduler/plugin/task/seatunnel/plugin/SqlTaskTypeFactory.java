package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin;

import org.apache.commons.collections4.MapUtils;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.ConnectParamDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.OutputFieldDTO;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.SeatunnelTaskNodeVO;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 18:59
 **/
public interface SqlTaskTypeFactory extends TaskTypeFactory {


    default String getSqlColumnStr(SeatunnelTaskNodeVO seatunnelTaskNodeVO, LinkedHashSet<String> outputFiles) {
        Map<String, String> colMap = outputFiles.stream()
                .collect(Collectors.toMap(
                        key -> key,       // Map 的键为集合中的元素
                        value -> "",       // Map 的值统一为空字符串
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new
                ));
        processSqlSelectColumn(seatunnelTaskNodeVO, colMap);
        StringBuilder sqlColumns = new StringBuilder();

        for (Map.Entry<String, String> entry : colMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();

            if (value == null || value.isEmpty()) {
                sqlColumns.append(key);
            } else {
                sqlColumns.append(value).append(" as ").append(key);
            }

            sqlColumns.append(", ");
        }

        if (sqlColumns.length() > 0) {
            sqlColumns.setLength(sqlColumns.length() - 2); // 去掉最后一个逗号和空格
        }

        return sqlColumns.toString();
    }

/**
 * 获取所有字段，用逗号拼接
 *
 * @param vo
 * @return
 */
    default String getAllColumnStr(SeatunnelTaskNodeVO vo) {
        String allColumnStr = "";
        List<OutputFieldDTO> outFields = vo.getOutputFields();
        if (outFields != null && outFields.size() > 0) {
            for (OutputFieldDTO outputFieldDTO : outFields) {
                allColumnStr = outputFieldDTO.getFields().stream().collect(Collectors.joining(","));
            }
        }
        return allColumnStr;
    }

    void processSqlSelectColumn(SeatunnelTaskNodeVO seatunnelTaskNodeVO, Map<String, String> colMap);


}
