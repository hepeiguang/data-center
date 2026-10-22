package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.transform;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto.BaseSeatunneTransformDTO;

/**
 * @program: 20250425
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-25
 **/
@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TransFormSqlDTO extends BaseSeatunneTransformDTO {
    /**
     * 页面传过来的参数
     */

    /**
     * 源表名称，查询 SQL 表名称必须与此字段匹配。
     */
    @JsonProperty("plugin_input")
    private String pluginInput;

    @JsonProperty("plugin_output")
    private String pluginOutput;

    /**
     * 查询 SQL，它是一个简单的 SQL，支持基本的函数和条件过滤操作。
     * 但是，复杂的 SQL 尚不支持，包括：多源表/行连接和聚合操作等。
     *  ---
     * 查询表达式可以是select [table_name.]column_a，这时会去查询列为column_a的列，
     * table_name为可选项 也可以是select c_row.c_inner_row.column_b，这时会去
     * 查询列c_row下的c_inner_row的column_b。嵌套结构查询中，不能存在table_name
     */
    @JsonProperty("query")
    private String query;
}

