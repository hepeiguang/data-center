package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.sf.jsqlparser.statement.select.Select;

import java.util.List;
import java.util.Map;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-16 18:16
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SeatunnelTaskNodeVO {
    private String type;
    private String code;
    private String pluginName;
    private String preTaskcode;
    private String postTaskcode;
    private Map<String, Object> params;
    /**
     * todo 多余的，等前台修改好了，去掉
     */
    private String name;

    private List<OutputFieldDTO> inputFields;
    private List<OutputFieldDTO> outputFields;

    private Select select;
}
