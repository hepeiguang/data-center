package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlcondition;

public enum FieldTypeEnum {
    INT,       // 对应 MySQL: int, bigint; PostgreSQL: integer, bigint
    FLOAT,     // 对应 MySQL: float, double; PostgreSQL: real, double precision
    STRING,    // 对应 MySQL: varchar, text; PostgreSQL: varchar, text
    DATE,      // 对应 MySQL: date; PostgreSQL: date
    DATETIME,  // 对应 MySQL: datetime, timestamp; PostgreSQL: timestamp
    BOOLEAN,    // 对应 MySQL: tinyint(1); PostgreSQL: boolean ; ;
    DEFAULT    // 其他未匹配的值
}
