package org.apache.dolphinscheduler.spi.enums;

public enum DbTypeRegex {

    MYSQL(DbType.MYSQL, " %s REGEXP '%s'"),
    MariaDB(DbType.MYSQL, " %s REGEXP '%s'"),
    POSTGRESQL(DbType.POSTGRESQL, " %s ~* '%s'"),
    ORACLE(DbType.ORACLE, " %s REGEXP '%s'"),
    SQLSERVER(DbType.SQLSERVER, " %s LIKE '%s'"),
    DB2(DbType.DB2, " %s REGEXP '%s'"),
    H2(DbType.H2, " %s REGEXP '%s'"),
    REDSHIFT(DbType.REDSHIFT, " %s ~ '%s'"),
    TRINO(DbType.TRINO, " %s ~ '%s'"),
    PRESTO(DbType.PRESTO, " %s ~ '%s'"),
    DAMENG(DbType.DAMENG, " %s REGEXP '%s'"),
    OCEANBASE(DbType.OCEANBASE, " %s REGEXP '%s'"),
    OPENGAUSS(DbType.OPENGAUSS, " %s ~* '%s'"),
    CLICKHOUSE(DbType.CLICKHOUSE, " match(%s LIKE '%s')"),
    HIVE(DbType.HIVE, " %s RLIKE '%s'"),
    SPARK(DbType.SPARK, " %s RLIKE '%s'"),
    STARROCKS(DbType.STARROCKS, " %s REGEXP '%s'"),
    DORIS(DbType.DORIS, " %s REGEXP '%s'"),
    ;

    private DbType dbType;
    private String regexCond;

    DbTypeRegex(DbType dbType, String regexCond) {
        this.dbType = dbType;
        this.regexCond = regexCond;
    }

    public String toCond(String columnName, String regex) {
        return String.format(regexCond, columnName, regex);
    }
}
