package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HdfsFileParamDTO {

    // fs.DefaultFS
    private String fsDefault;
    // hdfs-sitepath
    private String hdfsSitePath;
    // hdfs remoteuser
    private String remoteUser;
    // other config
    private KbsParamDTO other;

    @Data
    public static class KbsParamDTO {
        @JsonProperty("krb5_path")
        private String krb5Path;
        @JsonProperty("kerberos_principal")
        private String kerberosPrincipal;
        @JsonProperty("kerberos_keytab_path")
        private String kerberosKeytabPath;
    }

}
