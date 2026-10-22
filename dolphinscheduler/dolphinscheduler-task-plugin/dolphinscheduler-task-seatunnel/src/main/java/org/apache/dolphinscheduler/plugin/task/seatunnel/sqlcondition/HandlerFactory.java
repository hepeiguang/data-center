
package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlcondition;

import org.apache.dolphinscheduler.spi.enums.DbType;

import java.util.HashMap;
import java.util.Map;

public class HandlerFactory {
    private static final Map<DbType, FieldTypeHandler> handlers = new HashMap<>();

    static {
        handlers.put(DbType.MYSQL,new MysqlHandler());
        handlers.put(DbType.POSTGRESQL,new PostgreHandler());
        handlers.put(DbType.OPENGAUSS,new OpengaussHandler());
    }

    public static FieldTypeHandler getHandler(DbType dbType) {
        return handlers.get(dbType);
    }
}
