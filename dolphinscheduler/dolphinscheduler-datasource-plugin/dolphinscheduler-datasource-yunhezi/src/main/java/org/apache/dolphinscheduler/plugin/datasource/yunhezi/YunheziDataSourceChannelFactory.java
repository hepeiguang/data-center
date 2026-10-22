package org.apache.dolphinscheduler.plugin.datasource.yunhezi;

import com.google.auto.service.AutoService;
import org.apache.dolphinscheduler.spi.datasource.DataSourceChannel;
import org.apache.dolphinscheduler.spi.datasource.DataSourceChannelFactory;
import org.apache.dolphinscheduler.spi.enums.DbType;

@AutoService(DataSourceChannelFactory.class)
public class YunheziDataSourceChannelFactory implements DataSourceChannelFactory {
    @Override
    public DataSourceChannel create() {
        return new YunheziDataSourceChannel();
    }

    @Override
    public String getName() {
        return DbType.YUNHEZI.getName();
    }
}
