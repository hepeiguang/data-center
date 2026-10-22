package org.apache.dolphinscheduler.plugin.datasource.kafka;

import org.apache.dolphinscheduler.spi.datasource.AdHocDataSourceClient;
import org.apache.dolphinscheduler.spi.datasource.BaseConnectionParam;
import org.apache.dolphinscheduler.spi.datasource.DataSourceChannel;
import org.apache.dolphinscheduler.spi.datasource.PooledDataSourceClient;
import org.apache.dolphinscheduler.spi.enums.DbType;

public class KafkaDataSourceChannel implements DataSourceChannel {

    @Override
    public AdHocDataSourceClient createAdHocDataSourceClient(BaseConnectionParam baseConnectionParam, DbType dbType) {
        throw new UnsupportedOperationException("Sagemaker AdHocDataSourceClient is not supported");
    }

    @Override
    public PooledDataSourceClient createPooledDataSourceClient(BaseConnectionParam baseConnectionParam, DbType dbType) {
        throw new UnsupportedOperationException("Sagemaker AdHocDataSourceClient is not supported");
    }

}