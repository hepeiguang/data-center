package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.dolphinscheduler.spi.enums.DbType;

import java.util.Map;

/**
 * @program: data-center-platform-20250414
 * @author: 这里是你要写开发者姓名
 * @create: 2025-04-24 11:55
 **/
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConnectParamDTO {
    protected String user;

    protected String password;

    protected String address;

    protected String database;

    protected String jdbcUrl;

    private DbType type;

    protected String driverLocation;

    protected String driverClassName;

    protected String validationQuery;

    protected Map<String, String> other;
    protected String queryPort;

    protected Integer dorisRequestRetries;

    protected Integer dorisRequestReadTimeoutMs;

    protected Integer dorisRequestConnectTimeoutMs;
    protected String sinkEnableDelete;;
}