package com.lhyf.dataasset;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * AC-4: in ds mode the central {@code @RequireAdmin} gate rejects non-admin DS users with a 403
 * JSON body ({code,message}) before the handler runs, while reads and admins still pass.
 * DolphinScheduler's get-user-info is served by a local JDK HttpServer: GENERAL session -> normal
 * user, ADMIN session -> admin.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:asset-gate;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
        "data-asset.auth.mode=ds"
})
@AutoConfigureMockMvc
class AdminGateTest {

    private static HttpServer ds;

    @BeforeAll
    static void startDs() throws IOException {
        ds = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        ds.createContext("/users/get-user-info", exchange -> {
            String session = exchange.getRequestHeaders().getFirst("sessionId");
            String userType = "ADMIN".equals(session) ? "ADMINULATOR" : "GENERAL";
            byte[] body = ("{\"success\":true,\"data\":{\"userName\":\""
                    + ("ADMIN".equals(session) ? "boss" : "bob") + "\",\"userType\":\"" + userType + "\"}}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        ds.start();
    }

    @AfterAll
    static void stopDs() {
        if (ds != null) {
            ds.stop(0);
        }
    }

    @DynamicPropertySource
    static void dsUrl(DynamicPropertyRegistry registry) {
        registry.add("data-asset.auth.ds-base-url", () -> "http://127.0.0.1:" + ds.getAddress().getPort());
    }

    @Autowired
    private MockMvc mvc;

    @Test
    void missingCredentialsIsUnauthorized() throws Exception {
        mvc.perform(post("/data-asset/governance/tag"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void nonAdminCannotWriteGovernance() throws Exception {
        mvc.perform(post("/data-asset/governance/tag")
                        .header("sessionId", "GENERAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fqn\":\"a.b.c.t\",\"tagFqn\":\"X.Y\",\"action\":\"add\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.message").value("只有管理员可以维护治理信息"));
    }

    @Test
    void nonAdminCannotWriteLineage() throws Exception {
        mvc.perform(put("/data-asset/lineage/row-switch")
                        .header("sessionId", "GENERAL")
                        .param("tableFqn", "a.b.c.t").param("enabled", "true"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只有管理员可以维护血缘"));
        mvc.perform(post("/data-asset/lineage/keys")
                        .header("sessionId", "GENERAL")
                        .param("targetFqn", "a.b.c.t").param("keyColumn", "id")
                        .param("sourceFqn", "a.b.c.s").param("sourceKeyColumn", "id"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只有管理员可以维护血缘"));
    }

    @Test
    void nonAdminCannotWriteWhitelist() throws Exception {
        mvc.perform(post("/data-asset/browse/entries")
                        .header("sessionId", "GENERAL")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("只有管理员可以管理浏览白名单"));
    }

    @Test
    void nonAdminCanStillRead() throws Exception {
        mvc.perform(get("/data-asset/lineage/keys").header("sessionId", "GENERAL"))
                .andExpect(status().isOk());
        mvc.perform(get("/data-asset/browse/entries").header("sessionId", "GENERAL"))
                .andExpect(status().isOk());
    }

    @Test
    void adminSessionPassesTheGate() throws Exception {
        // Passes the gate; OM is unreachable in tests, so a 4xx/5xx from downstream is fine — 403 is not.
        mvc.perform(put("/data-asset/lineage/row-switch")
                        .header("sessionId", "ADMIN")
                        .param("tableFqn", "a.b.c.t").param("enabled", "true"))
                .andExpect(result -> org.junit.jupiter.api.Assertions.assertNotEquals(403,
                        result.getResponse().getStatus(),
                        "admin session must not be blocked by the gate"));
    }
}
