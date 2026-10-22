package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlcondition;

import org.apache.dolphinscheduler.spi.enums.DbType;

public class MysqlHandler implements FieldTypeHandler {

    @Override
    public String handleValue(String columnValue, String columnType) {
        if (columnValue == null) {
            return "NULL";
        }
        FieldTypeEnum fieldTypeEnum = ColumnTypeEnum.getFiledTypeEnum(DbType.MYSQL,columnType);
        if (fieldTypeEnum == null) {
            fieldTypeEnum = ColumnTypeEnum.getFiledTypeEnum(DbType.MYSQL,"default");
        }
        String temp;
        switch (fieldTypeEnum) {
            case STRING:
                temp =  " '" +columnValue + "' ";
                break;
            case INT:
            case FLOAT:
//                temp =  columnValue + " + 0 ";
                temp =  columnValue;
                break;

            case DATE:
            case DATETIME:
                temp = " '" + columnValue + "' ";
                break;
            default:
                temp = " CAST('" + columnValue + "' AS CHAR) ";
                break;
        }
        return temp;
    }
}
