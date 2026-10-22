package org.apache.dolphinscheduler.api.security.impl.dam;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Component
public class DamAuthConfig {

    @Value("${security.authentication.dam.user.admin:null}")
    private String adminUserId;

    @Value("${security.authentication.dam.base-url:}")
    private String baseUrl;

    @Value("${security.authentication.dam.urls.login:null}")
    private String loginUrl;

    @Value("${security.authentication.dam.urls.token-valid:null}")
    private String tokenValidUrl;

    @Value("${security.authentication.dam.urls.get-user:null}")
    private String getUserUrl;

    @Value("${security.authentication.dam.grant-type:password}")
    private String grantType;

    @Value("${security.authentication.dam.scope:all}")
    private String scope;

    @Value("${security.authentication.dam.token-key:Authorization}")
    private String tokenKey;

    @Value("${security.authentication.dam.login-authorization:null}")
    private String loginAuthorization;

    @Value("${security.authentication.dam.token-prefix:null}")
    private String tokenPrefix;

    public String getLoginUrl() {
        return baseUrl.concat(loginUrl);
    }

    public String getTokenValidUrl() {
        return baseUrl.concat(tokenValidUrl);
    }

    public String getGetUserUrl() {
        return baseUrl.concat(getUserUrl);
    }

}
