package org.apache.dolphinscheduler.plugin.datasource.yunhezi.param;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.spi.enums.DbType;

@Data
@EqualsAndHashCode(callSuper = true)
public class YunheziDataSourceParamDTO extends BaseDataSourceParamDTO {
    private Integer connectTimeout;
    private Integer socketTimeout;
    /**
     * 云盘分配给第三方系统的应用id
     */
    private String appId;
    /**
     * 用户登录账号
     */
    private String account;
    /**
     * 云盘分配给第三方系统的应用id
     */
    private String appSecret;
    /**
     * 对应的版本号
     */
    private String version;
    private String protocol;


    @Override
    public DbType getType() {
        return DbType.YUNHEZI;
    }
}
