package org.apache.dolphinscheduler.plugin.datasource.socketserver.param;

import com.google.auto.service.AutoService;
import org.apache.dolphinscheduler.spi.datasource.DataSourceChannel;
import org.apache.dolphinscheduler.spi.datasource.DataSourceChannelFactory;
import org.apache.dolphinscheduler.spi.enums.DbType;

@AutoService(DataSourceChannelFactory.class)
public class SocketServerDataSourceChannelFactory implements DataSourceChannelFactory {
    @Override
    public DataSourceChannel create() {
        return new SocketServerDataSourceChannel();
    }

    @Override
    public String getName() {
        return DbType.SOCKET_SERVER.getName();
    }
}
