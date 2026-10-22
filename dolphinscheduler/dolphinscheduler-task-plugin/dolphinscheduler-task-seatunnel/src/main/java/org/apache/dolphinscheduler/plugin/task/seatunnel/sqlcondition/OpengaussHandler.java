package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlcondition;

import org.apache.dolphinscheduler.spi.enums.DbType;

public class OpengaussHandler implements FieldTypeHandler {

    @Override
    public String handleValue(String columnValue, String columnType) {
        if (columnValue == null) {
            return "NULL";
        }
        FieldTypeEnum fieldTypeEnum = ColumnTypeEnum.getFiledTypeEnum(DbType.OPENGAUSS,columnType);
        if (fieldTypeEnum == null) {
            fieldTypeEnum = ColumnTypeEnum.getFiledTypeEnum(DbType.OPENGAUSS,"default");
        }
        String temp;
        switch (fieldTypeEnum) {
            case STRING:
                temp =  " '" +columnValue + "' ";
                break;
            case INT:
            case FLOAT:
                temp =  columnValue;
                break;

            case DATE:
                temp = "TO_DATE('" + columnValue + "', 'yyyy-MM-dd')";
                break;
            case DATETIME:
                temp = "TO_TIMESTAMP('" + columnValue + "', 'yyyy-MM-dd HH:mm:ss')";
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
            default: //其他类型
                temp =  " TO_CHAR ( '" + columnValue + "' ) ";
                break;
        }
        return temp;
    }
}
