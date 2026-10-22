package org.apache.dolphinscheduler.plugin.datasource.httpserver.param;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.spi.enums.DbType;

@Data
@EqualsAndHashCode(callSuper = true)
public class HttpServerDataSourceParamDTO extends BaseDataSourceParamDTO {
    private String httpEndpoint;
    private String httpMethod;
    private String httpAuthType;
    private String httpAuthConfig;
    private Integer connectTimeout;
    private Integer socketTimeout;
    private String protocol;

    @Override
    public DbType getType() {
        return DbType.HTTP;
    }
}
