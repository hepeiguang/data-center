package com.lhyf.dataasset.admin;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/data-asset/admin")
public class AdminController {

    private final AdminHealthService healthService;

    public AdminController(AdminHealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        return healthService.health();
    }
}
