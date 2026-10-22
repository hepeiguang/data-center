package org.apache.dolphinscheduler.plugin.datasource.sftp;

import com.google.auto.service.AutoService;
import org.apache.dolphinscheduler.spi.datasource.DataSourceChannel;
import org.apache.dolphinscheduler.spi.datasource.DataSourceChannelFactory;
import org.apache.dolphinscheduler.spi.enums.DbType;

@AutoService(DataSourceChannelFactory.class)
public class SftpDataSourceChannelFactory implements DataSourceChannelFactory {

    @Override
    public String getName() {
        return DbType.SFTP.getName();
    }

    @Override
    public DataSourceChannel create() {
        return new SftpDataSourceChannel();
    }
}