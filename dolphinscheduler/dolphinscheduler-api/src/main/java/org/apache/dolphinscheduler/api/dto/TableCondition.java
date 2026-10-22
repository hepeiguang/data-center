package org.apache.dolphinscheduler.api.dto;

import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.dolphinscheduler.spi.enums.DbType;

import java.util.Arrays;
import java.util.List;

@Data
@Accessors(chain = true)
public class TableCondition {
    private DbType dsType;
    private Integer dsId;
    private String database;
    private String schemaName;
    private String tableName;
    private String condition = "";
    private String partitionColumn;
    private String primaryKey = "";
    private String querySql;
    private boolean isNoPrimaryKeyTable;
    private String dupKey;

    public String getTablePath() {
//        if (this.getSchemaName() == null || this.getSchemaName().isEmpty()) {
//            return this.getDatabase().concat(".").concat(this.getTableName());
//        } else {
//            return this.getDatabase().concat(".").concat(this.getSchemaName()).concat(".").concat(this.getTableName());
//        }
        return this.getDatabase().concat(".").concat(this.getTableName());
    }

    public List<String> getPrimaryKeys() {
        if (this.primaryKey == null || this.primaryKey.isEmpty()) {
            return null;
        }
        return Arrays.asList(this.primaryKey.split(","));
    }

    public List<String> getDupKeys() {
        if (this.dupKey == null || this.dupKey.isEmpty()) {
            return null;
        }
        return Arrays.asList(this.dupKey.split(","));
    }

}
