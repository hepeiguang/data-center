package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GroupByColumn {
    private String tableName;   // 表名，如 "users"
    private String columnName;  // 字段名，如 "name"
}
