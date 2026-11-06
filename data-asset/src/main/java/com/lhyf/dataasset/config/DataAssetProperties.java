package com.lhyf.dataasset.config;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "data-asset")
public class DataAssetProperties {

    private Om om = new Om();
    private Auth auth = new Auth();
    private Query query = new Query();
    private Map<String, Datasource> datasources = new LinkedHashMap<>();

    public Om getOm() {
        return om;
    }

    public void setOm(Om om) {
        this.om = om;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    public Query getQuery() {
        return query;
    }

    public void setQuery(Query query) {
        this.query = query;
    }

    public Map<String, Datasource> getDatasources() {
        return datasources;
    }

    public void setDatasources(Map<String, Datasource> datasources) {
        this.datasources = datasources;
    }

    public static class Om {
        private String baseUrl = "http://127.0.0.1:8585";
        private String token = "";
        private String adminEmail = "admin@open-metadata.org";
        private String adminPassword = "admin";
        private int timeoutSeconds = 5;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }

        public String getAdminEmail() {
            return adminEmail;
        }

        public void setAdminEmail(String adminEmail) {
            this.adminEmail = adminEmail;
        }

        public String getAdminPassword() {
            return adminPassword;
        }

        public void setAdminPassword(String adminPassword) {
            this.adminPassword = adminPassword;
        }

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }
    }

    public static class Auth {
        /** local: trust X-Debug-User header (dev only). ds: validate sessionId against DolphinScheduler. */
        private String mode = "local";
        private String debugUser = "admin";
        private String dsBaseUrl = "http://127.0.0.1:12345/dolphinscheduler";

        public String getMode() {
            return mode;
        }

        public void setMode(String mode) {
            this.mode = mode;
        }

        public String getDebugUser() {
            return debugUser;
        }

        public void setDebugUser(String debugUser) {
            this.debugUser = debugUser;
        }

        public String getDsBaseUrl() {
            return dsBaseUrl;
        }

        public void setDsBaseUrl(String dsBaseUrl) {
            this.dsBaseUrl = dsBaseUrl;
        }
    }

    public static class Query {
        private int timeoutSeconds = 60;
        private int defaultPageSize = 200;

        public int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        public void setTimeoutSeconds(int timeoutSeconds) {
            this.timeoutSeconds = timeoutSeconds;
        }

        public int getDefaultPageSize() {
            return defaultPageSize;
        }

        public void setDefaultPageSize(int defaultPageSize) {
            this.defaultPageSize = defaultPageSize;
        }
    }

    public static class Datasource {
        private String url;
        private String username;
        private String password;
        private int poolSize = 3;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public int getPoolSize() {
            return poolSize;
        }

        public void setPoolSize(int poolSize) {
            this.poolSize = poolSize;
        }
    }
}
