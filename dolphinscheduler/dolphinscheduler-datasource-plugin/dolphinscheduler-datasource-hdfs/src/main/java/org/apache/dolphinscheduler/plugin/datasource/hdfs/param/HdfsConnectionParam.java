package org.apache.dolphinscheduler.plugin.datasource.hdfs.param;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.apache.dolphinscheduler.spi.datasource.ConnectionParam;

import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HdfsConnectionParam implements ConnectionParam {
    private String fsDefault;
    private Map<String, String> kbsConfig;
    private String remoteUser;
    private String hdfsSitePath;

}
