package com.lhyf.dataasset.auth;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lhyf.dataasset.config.DataAssetProperties;

/**
 * Resolves the calling user. In local mode a debug header is trusted; in ds mode the
 * sessionId/token is validated by DolphinScheduler, so no DS source code is touched.
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    public static final String ATTR_USER = "asset.user";
    public static final String ATTR_ADMIN = "asset.admin";

    private final DataAssetProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    public AuthInterceptor(DataAssetProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        if (request.getRequestURI().endsWith("/admin/health")) {
            return true;
        }
        String mode = properties.getAuth().getMode();
        if ("local".equalsIgnoreCase(mode)) {
            String user = header(request, "X-Debug-User");
            if (user == null || user.isBlank()) {
                user = properties.getAuth().getDebugUser();
            }
            request.setAttribute(ATTR_USER, user);
            request.setAttribute(ATTR_ADMIN, true);
            return true;
        }
        String sessionId = header(request, "sessionId");
        String token = header(request, "token");
        if ((sessionId == null || sessionId.isBlank()) && (token == null || token.isBlank())) {
            unauthorized(response, "sessionId or token required");
            return false;
        }
        try {
            String url = properties.getAuth().getDsBaseUrl() + "/users/get-user-info";
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            if (sessionId != null) {
                headers.add("sessionId", sessionId);
            }
            if (token != null) {
                headers.add("token", token);
            }
            String body = restTemplate
                    .exchange(url, org.springframework.http.HttpMethod.GET,
                            new org.springframework.http.HttpEntity<>(headers), String.class)
                    .getBody();
            JsonNode root = objectMapper.readTree(body == null ? "{}" : body);
            if (!root.path("success").asBoolean(false)) {
                unauthorized(response, "invalid DS session");
                return false;
            }
            JsonNode data = root.path("data");
            String userName = data.path("userName").asText("");
            String userType = data.path("userType").asText("");
            if (userName.isBlank()) {
                unauthorized(response, "DS session has no user");
                return false;
            }
            request.setAttribute(ATTR_USER, userName);
            request.setAttribute(ATTR_ADMIN, userType.toUpperCase().contains("ADMIN"));
            return true;
        } catch (Exception e) {
            unauthorized(response, "cannot validate DS session: " + e.getClass().getSimpleName());
            return false;
        }
    }

    private static String header(HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        return value == null ? null : value.trim();
    }

    private static void unauthorized(HttpServletResponse response, String message) throws Exception {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"code\":401,\"message\":\"" + message + "\"}");
    }
}
