package org.apache.dolphinscheduler.api.security.impl.htzh;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Data
@Component
public class HtzhAuthConfig {
    @Value("${security.authentication.htzh.mode:jwt}")
    private String mode;
    @Value("${security.authentication.htzh.org-code:}")
    private String orgCode;
    @Value("${security.authentication.htzh.base-url:}")
    private String baseUrl;
    @Value("${security.authentication.htzh.urls.sso-auth-path:}")
    private String ssoAuthPath;
    @Value("${security.authentication.htzh.urls.user-path:}")
    private String userPath;
    @Value("${security.authentication.htzh.urls.log-path:}")
    private String logPath;
    @Value("${security.authentication.htzh.log-url:}")
    private String logUrl;


    /**
     * 获取oauth请求token的地址
     *
     * @return
     */
    public String getSsoAuthUrl(String token, String service) {
        String suffix = String.format("%s?service=%s&ssoMode=%s&code=%s&ticket=%s", this.getSsoAuthPath(), service, mode, token, token);
        return this.getBaseUrl().concat(suffix);
    }

    public String getUserUrl(String account) {
        String suffix = String.format("%s?orgCode=%s&account=%s", this.getUserPath(), this.getOrgCode(), account);
        return this.getBaseUrl() + suffix;
    }

    public String getLoggerUrl() {
        return this.getLogUrl().concat(this.getLogPath());
    }
}
