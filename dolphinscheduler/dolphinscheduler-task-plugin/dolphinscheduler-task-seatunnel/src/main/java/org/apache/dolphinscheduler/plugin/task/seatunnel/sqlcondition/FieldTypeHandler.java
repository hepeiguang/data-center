package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlcondition;

import org.apache.dolphinscheduler.spi.enums.DbType;
public interface FieldTypeHandler {

    // 根据字段大类型 和 数据库 类型 以及数据库字段类型  对数据值进行处理
    // 确保 casewhen switchcase sqlfilter 算子的功能正常
    String handleValue(String columnValue, String columnType);
}
