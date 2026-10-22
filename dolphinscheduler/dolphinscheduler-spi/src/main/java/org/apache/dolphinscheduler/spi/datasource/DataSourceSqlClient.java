package org.apache.dolphinscheduler.spi.datasource;

import java.util.List;

public interface DataSourceSqlClient {
    List<String> getNotEmptyTables(String database, String schema);

    List<String> getNoPrimaryKeyTables(String database, String schema);


}
