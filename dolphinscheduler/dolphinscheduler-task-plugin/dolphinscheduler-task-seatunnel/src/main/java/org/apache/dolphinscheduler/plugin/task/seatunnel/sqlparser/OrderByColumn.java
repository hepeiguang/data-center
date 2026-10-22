package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

@Accessors(chain = true)
@Getter
@AllArgsConstructor
public class OrderByColumn {
    private String tableName;   // 原始表名，例如 "users"
    private String columnName;  // 字段名，例如 "name"
    private boolean isAsc;  // true = ASC, false = DESC

}
