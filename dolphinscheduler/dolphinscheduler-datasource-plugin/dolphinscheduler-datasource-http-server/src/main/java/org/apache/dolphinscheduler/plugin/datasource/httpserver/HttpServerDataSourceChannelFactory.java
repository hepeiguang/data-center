package org.apache.dolphinscheduler.plugin.datasource.httpserver;

import com.google.auto.service.AutoService;
import org.apache.dolphinscheduler.spi.datasource.DataSourceChannel;
import org.apache.dolphinscheduler.spi.datasource.DataSourceChannelFactory;
import org.apache.dolphinscheduler.spi.enums.DbType;

@AutoService(DataSourceChannelFactory.class)
public class HttpServerDataSourceChannelFactory implements DataSourceChannelFactory {
    @Override
    public DataSourceChannel create() {
        return new HttpServerDataSourceChannel();
    }

    @Override
    public String getName() {
        return DbType.HTTP.getName();
    }
}
