package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-19 11:30
 **/
@Data
@Slf4j
public abstract class BaseSeatunnelSinkDTO {
    @JsonProperty("plugin_name")
    private String pluginName;

    /**
     * 当不指定 plugin_input 时，当前插件处理配置文件中上一个插件输出的数据集 dataset
     * <p>
     * 当指定了 plugin_input 时，当前插件正在处理该参数对应的数据集
     */
    @JsonProperty("plugin_input")
    private String pluginInput;
    /**
     * 当没有指定parallelism时，默认使用 env 中的 parallelism。
     * <p>
     * 当指定 parallelism 时，它将覆盖 env 中的 parallelism。
     */
    private Integer parallelism;

    /**
     * 页面传过来的参数，从数据库获取 url，driver，user，password
     */
    private Integer datasourceId;
}