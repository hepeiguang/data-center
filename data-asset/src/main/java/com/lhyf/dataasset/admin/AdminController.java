package com.lhyf.dataasset.admin;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lhyf.dataasset.config.BusinessDataSources;
import com.lhyf.dataasset.config.DataAssetProperties;
import com.lhyf.dataasset.om.OmClient;

@RestController
@RequestMapping("/data-asset/admin")
public class AdminController {

    private final JdbcTemplate jdbc;
    private final OmClient omClient;
    private final BusinessDataSources dataSources;
    private final DataAssetProperties properties;

    public AdminController(JdbcTemplate jdbc, OmClient omClient, BusinessDataSources dataSources,
                           DataAssetProperties properties) {
        this.jdbc = jdbc;
        this.omClient = omClient;
        this.dataSources = dataSources;
        this.properties = properties;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> configDb = new LinkedHashMap<>();
        try {
            Integer value = jdbc.queryForObject("select 1", Integer.class);
            configDb.put("status", value != null && value == 1 ? "ok" : "error");
        } catch (Exception e) {
            configDb.put("status", "error");
            configDb.put("error", e.getClass().getSimpleName());
        }
        result.put("configDb", configDb);

        Map<String, Object> om = new LinkedHashMap<>();
        try {
            om.put("status", "ok");
            om.put("version", omClient.version());
        } catch (Exception e) {
            om.put("status", "error");
            om.put("error", e.getClass().getSimpleName());
        }
        result.put("openMetadata", om);
        result.put("datasources", dataSources.health());
        result.put("authMode", properties.getAuth().getMode());
        return result;
    }
}
