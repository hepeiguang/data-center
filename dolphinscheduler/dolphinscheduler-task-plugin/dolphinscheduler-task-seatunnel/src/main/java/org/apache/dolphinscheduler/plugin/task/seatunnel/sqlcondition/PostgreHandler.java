package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlcondition;

import org.apache.dolphinscheduler.spi.enums.DbType;

public class PostgreHandler implements FieldTypeHandler {

    @Override
    public String handleValue(String columnValue, String columnType) {
        if (columnValue == null) {
            return "NULL";
        }
        FieldTypeEnum fieldTypeEnum = ColumnTypeEnum.getFiledTypeEnum(DbType.POSTGRESQL,columnType);
        if (fieldTypeEnum == null) {
            fieldTypeEnum = ColumnTypeEnum.getFiledTypeEnum(DbType.POSTGRESQL,"default");
        }
        String temp;
        switch (fieldTypeEnum) {
            case STRING:
                temp =  "'" +columnValue + "'";
                break;
            case INT:
            case FLOAT:
                // 不做任何处理 前端可以返回正确类型
                temp =  columnValue;
                break;

            case DATE:
                temp = "TO_DATE('" + columnValue + "', 'YYYY-MM-DD')";
                break;
            case DATETIME:
                temp = "TO_TIMESTAMP('" + columnValue + "', 'YYYY-MM-DD HH24:MI:SS')";
                break;
            case BOOLEAN: // 布尔值
                String lowerValue = columnValue.trim().toLowerCase();
                if ("true".equals(lowerValue) || "t".equals(lowerValue) || "1".equals(lowerValue) || "yes".equals(lowerValue) || "y".equals(lowerValue) || "on".equals(lowerValue)) {
                    temp = "true";
                } else if ("false".equals(lowerValue) || "f".equals(lowerValue) || "0".equals(lowerValue) || "no".equals(lowerValue) || "n".equals(lowerValue) || "off".equals(lowerValue)) {
                    temp = "false";
                } else {
                    throw new IllegalArgumentException("Invalid boolean value: " + columnValue);
                }
                break;
            default:
                temp = "'" + columnValue + "'";
                break;
        }
        return temp;
    }
}
