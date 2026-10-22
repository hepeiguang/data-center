package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlcondition;

import lombok.Getter;
import org.apache.dolphinscheduler.spi.enums.DbType;

@Getter
public enum ColumnTypeEnum {

//    DOUBLE(DbType.MYSQL, FieldTypeEnum.INT, "double");
    // --------------------------------------------------------
    MYSQL_SERIAL(DbType.MYSQL, FieldTypeEnum.INT, "serial"),
    MYSQL_INT(DbType.MYSQL, FieldTypeEnum.INT, "int"),
    MYSQL_BIGINT(DbType.MYSQL, FieldTypeEnum.INT, "bigint"),
    MYSQL_TINYINT(DbType.MYSQL, FieldTypeEnum.INT, "tinyint"),
    // --------------------------------------------------------
    MYSQL_DOUBLE(DbType.MYSQL, FieldTypeEnum.FLOAT, "double"),
    MYSQL_FLOAT(DbType.MYSQL, FieldTypeEnum.FLOAT, "float"),
    MYSQL_DECIMAL(DbType.MYSQL, FieldTypeEnum.FLOAT, "decimal"),
    // --------------------------------------------------------
    MYSQL_VARCHAR(DbType.MYSQL, FieldTypeEnum.STRING, "varchar"),
    MYSQL_CHAR(DbType.MYSQL, FieldTypeEnum.STRING, "char"),
    MYSQL_TEXT(DbType.MYSQL, FieldTypeEnum.STRING, "text"),
    MYSQL_LONGTEXT(DbType.MYSQL, FieldTypeEnum.STRING, "longtext"),
    // --------------------------------------------------------
    MYSQL_DATE(DbType.MYSQL, FieldTypeEnum.DATE, "date"),
    MYSQL_DATETIME(DbType.MYSQL, FieldTypeEnum.DATETIME, "datetime"),
    MYSQL_TIMESTAMP(DbType.MYSQL, FieldTypeEnum.DATETIME, "timestamp"),
    // --------------------------------------------------------
    MYSQL_DEFAULT(DbType.MYSQL,FieldTypeEnum.DEFAULT,"default"),



    POSTGRESQL_SERIAL(DbType.POSTGRESQL, FieldTypeEnum.INT, "serial"),
    POSTGRESQL_INTEGER(DbType.POSTGRESQL, FieldTypeEnum.INT, "integer"),
    POSTGRESQL_BIGINT(DbType.POSTGRESQL, FieldTypeEnum.INT, "bigint"),
    POSTGRESQL_SMALLINT(DbType.POSTGRESQL, FieldTypeEnum.INT, "smallint"),
    // --------------------------------------------------------
    POSTGRESQL_DOUBLE(DbType.POSTGRESQL, FieldTypeEnum.FLOAT, "double precision"),
    POSTGRESQL_REAL(DbType.POSTGRESQL, FieldTypeEnum.FLOAT, "real"),
    POSTGRESQL_NUMERIC(DbType.POSTGRESQL, FieldTypeEnum.FLOAT, "numeric"),
    POSTGRESQL_FLOAT8(DbType.POSTGRESQL, FieldTypeEnum.FLOAT, "float8"),
    // --------------------------------------------------------
    POSTGRESQL_VARCHAR(DbType.POSTGRESQL, FieldTypeEnum.STRING, "varchar"),
    POSTGRESQL_CHAR(DbType.POSTGRESQL, FieldTypeEnum.STRING, "char"),
    POSTGRESQL_TEXT(DbType.POSTGRESQL, FieldTypeEnum.STRING, "text"),
    // --------------------------------------------------------
    POSTGRESQL_DATE(DbType.POSTGRESQL, FieldTypeEnum.DATE, "date"),
    POSTGRESQL_TIMESTAMP(DbType.POSTGRESQL, FieldTypeEnum.DATETIME, "timestamp"),
    POSTGRESQL_TIMESTAMP_TZ(DbType.POSTGRESQL, FieldTypeEnum.DATETIME, "timestamp with time zone"),
    // --------------------------------------------------------
    POSTGRESQL_BOOLEAN(DbType.POSTGRESQL, FieldTypeEnum.BOOLEAN, "boolean"),
    POSTGRESQL_BOOL(DbType.POSTGRESQL, FieldTypeEnum.BOOLEAN, "bool"),
    // --------------------------------------------------------
    POSTGRESQL_DEFAULT(DbType.POSTGRESQL,FieldTypeEnum.DEFAULT,"default"),



    OPENGAUSS_SERIAL(DbType.OPENGAUSS, FieldTypeEnum.INT, "serial"),
    OPENGAUSS_INTEGER(DbType.OPENGAUSS, FieldTypeEnum.INT, "integer"),
    OPENGAUSS_BIGINT(DbType.OPENGAUSS, FieldTypeEnum.INT, "bigint"),
    OPENGAUSS_SMALLINT(DbType.OPENGAUSS, FieldTypeEnum.INT, "smallint"),
    // --------------------------------------------------------
    OPENGAUSS_DOUBLE(DbType.OPENGAUSS, FieldTypeEnum.FLOAT, "double precision"),
    OPENGAUSS_REAL(DbType.OPENGAUSS, FieldTypeEnum.FLOAT, "real"),
    OPENGAUSS_NUMERIC(DbType.OPENGAUSS, FieldTypeEnum.FLOAT, "numeric"),
    // --------------------------------------------------------
    OPENGAUSS_VARCHAR(DbType.OPENGAUSS, FieldTypeEnum.STRING, "varchar"),
    OPENGAUSS_CHAR(DbType.OPENGAUSS, FieldTypeEnum.STRING, "char"),
    OPENGAUSS_TEXT(DbType.OPENGAUSS, FieldTypeEnum.STRING, "text"),
    // --------------------------------------------------------
    OPENGAUSS_DATE(DbType.OPENGAUSS, FieldTypeEnum.DATE, "date"),
    OPENGAUSS_TIMESTAMP(DbType.OPENGAUSS, FieldTypeEnum.DATETIME, "timestamp"),
    OPENGAUSS_TIMESTAMP_TZ(DbType.OPENGAUSS, FieldTypeEnum.DATETIME, "timestamp with time zone"),
    // --------------------------------------------------------
    OPENGAUSS_BOOLEAN(DbType.OPENGAUSS, FieldTypeEnum.BOOLEAN, "boolean"),
    OPENGAUSS_BOOL(DbType.OPENGAUSS, FieldTypeEnum.BOOLEAN, "bool"),
    // --------------------------------------------------------
    OPENGAUSS_DEFAULT(DbType.OPENGAUSS,FieldTypeEnum.DEFAULT,"default");


    private DbType database;
    private FieldTypeEnum filedType;
    private String columnType;

    ColumnTypeEnum(DbType database, FieldTypeEnum filedType, String columnType) {
        this.database = database;
        this.filedType = filedType;
        this.columnType = columnType;
    }

    public static FieldTypeEnum getFiledTypeEnum(DbType database, String columnType) {
        ColumnTypeEnum[] enums = ColumnTypeEnum.values();
        for (ColumnTypeEnum columnTypeEnum : enums) {
            if (database.equals(columnTypeEnum.getDatabase()) && columnType.equals(columnTypeEnum.getColumnType())) {
                return columnTypeEnum.getFiledType();
            }
        }
        return null;
    }
}
