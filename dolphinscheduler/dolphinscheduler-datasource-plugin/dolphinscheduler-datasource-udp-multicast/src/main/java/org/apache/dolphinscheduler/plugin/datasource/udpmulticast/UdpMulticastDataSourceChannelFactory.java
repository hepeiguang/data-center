package org.apache.dolphinscheduler.plugin.datasource.udpmulticast;

import com.google.auto.service.AutoService;
import org.apache.dolphinscheduler.spi.datasource.DataSourceChannel;
import org.apache.dolphinscheduler.spi.datasource.DataSourceChannelFactory;
import org.apache.dolphinscheduler.spi.enums.DbType;

@AutoService(DataSourceChannelFactory.class)
public class UdpMulticastDataSourceChannelFactory implements DataSourceChannelFactory {
    @Override
    public DataSourceChannel create() {
        return new UdpMulticastDataSourceChannel();
    }

    @Override
    public String getName() {
        return DbType.UDP_MULTICAST.getName();
    }
}
