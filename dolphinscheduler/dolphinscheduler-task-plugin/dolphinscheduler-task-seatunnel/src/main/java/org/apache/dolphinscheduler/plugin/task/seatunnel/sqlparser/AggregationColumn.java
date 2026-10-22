package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AggregationColumn {
    private String function;     // 聚合函数，如 "COUNT", "SUM", "MAX"
    private String tableName;    // 表名，如 "orders"
    private String columnName;   // 字段名，如 "amount"
    private String alias;        // 别名，如 "total_amount"
}
