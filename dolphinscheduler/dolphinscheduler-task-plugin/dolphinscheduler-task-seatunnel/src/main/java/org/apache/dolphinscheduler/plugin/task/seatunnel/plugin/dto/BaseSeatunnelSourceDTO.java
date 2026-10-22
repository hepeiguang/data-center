package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-19 11:31
 **/
@Data
@Slf4j
public abstract class BaseSeatunnelSourceDTO {
    @JsonProperty("plugin_name")
    private String pluginName;

    /**
     * 	当未指定 plugin_output 时，此插件处理的数据将不会被注册为可由其他插件直接访问的数据集 (dataStream/dataset)，或称为临时表 (table)。
     * 当指定了 plugin_output 时，此插件处理的数据将被注册为可由其他插件直接访问的数据集 (dataStream/dataset)，或称为临时表 (table)。此处注册的数据集 (dataStream/dataset) 可通过指定 plugin_input 直接被其他插件访问。
     */
    @JsonProperty("plugin_output")
    private String pluginOutput;
    /**
     * 当未指定 parallelism 时，默认使用环境中的 parallelism。
     * 当指定了 parallelism 时，将覆盖环境中的 parallelism 设置。
     */
    private Integer parallelism;
    /**
     * 页面传过来的参数，从数据库获取 url，driver，user，password
     */
    private Integer datasourceId;

}