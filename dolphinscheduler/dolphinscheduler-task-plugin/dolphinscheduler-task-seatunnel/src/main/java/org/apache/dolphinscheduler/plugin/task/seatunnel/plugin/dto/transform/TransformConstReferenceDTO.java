package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-23 10:01
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TransformConstReferenceDTO extends BaseSeatunneTransformDTO {


    /**
     *	最终需要的sql。
     */
    @JsonProperty("query")
    private String query;

    /**
     * 当未指定 plugin_in 时，此插件处理的数据将不会被注册为可由其他插件直接访问的数据集 (dataStream/dataset)，或称为临时表 (table)。
     * 当指定了 plugin_in 时，此插件处理的数据将被注册为可由其他插件直接访问的数据集 (dataStream/dataset)。
     */
    @JsonProperty("plugin_input")
    private String pluginIn;

    /**
     * 当未指定 plugin_output 时，此插件处理的数据将不会被注册为可由其他插件直接访问的数据集 (dataStream/dataset)，或称为临时表 (table)。
     * 当指定了 plugin_output 时，此插件处理的数据将被注册为可由其他插件直接访问的数据集 (dataStream/dataset)，或称为临时表 (table)。此处注册的数据集 (dataStream/dataset) 可通过指定 plugin_input 直接被其他插件访问。
     */
    @JsonProperty("plugin_output")
    private String pluginOutput;

    /**
     * 当未指定 parallelism 时，默认使用环境中的 parallelism。
     * 当指定了 parallelism 时，将覆盖环境中的 parallelism 设置。
     */
    private Integer parallelism;

}

