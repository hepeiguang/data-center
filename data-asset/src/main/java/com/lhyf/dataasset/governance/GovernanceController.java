package com.lhyf.dataasset.governance;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;

import com.lhyf.dataasset.auth.RequireAdmin;

/** Governance endpoints: routing only, orchestration lives in GovernanceService. */
@RestController
@RequestMapping("/data-asset/governance")
public class GovernanceController {

    private final GovernanceService governance;

    public GovernanceController(GovernanceService governance) {
        this.governance = governance;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return governance.overview();
    }

    @GetMapping("/assets")
    public List<Map<String, Object>> assets() {
        return governance.assets();
    }

    @GetMapping("/asset")
    public Map<String, Object> asset(@RequestParam String fqn) {
        return governance.asset(fqn);
    }

    /** Adds or removes a single tag. Body: {fqn, tagFqn, action: add|remove}. */
    @PostMapping("/asset/tag")
    @RequireAdmin("只有管理员可以维护治理信息")
    public Map<String, Object> tag(@RequestBody Map<String, String> body) {
        return governance.tag(body);
    }

    /** Replaces all tags of one classification. Body: {fqn, classification, tagFqn} (tagFqn empty clears). */
    @PostMapping("/asset/tag-set")
    @RequireAdmin("只有管理员可以维护治理信息")
    public Map<String, Object> tagSet(@RequestBody Map<String, String> body) {
        return governance.tagSet(body);
    }

    @PostMapping("/asset/domain")
    @RequireAdmin("只有管理员可以维护治理信息")
    public Map<String, Object> domain(@RequestBody Map<String, String> body) {
        return governance.domain(body);
    }

    @PostMapping("/asset/dataProduct")
    @RequireAdmin("只有管理员可以维护治理信息")
    public Map<String, Object> dataProduct(@RequestBody Map<String, String> body) {
        return governance.dataProduct(body);
    }

    @PostMapping("/asset/glossaryTerm")
    @RequireAdmin("只有管理员可以维护治理信息")
    public Map<String, Object> glossaryTerm(@RequestBody Map<String, String> body) {
        return governance.glossaryTerm(body);
    }

    @PostMapping("/classification")
    @RequireAdmin("只有管理员可以维护治理信息")
    public JsonNode createClassification(@RequestBody Map<String, String> body) {
        return governance.createClassification(body);
    }

    @PostMapping("/tag")
    @RequireAdmin("只有管理员可以维护治理信息")
    public JsonNode createTag(@RequestBody Map<String, String> body) {
        return governance.createTag(body);
    }

    @PostMapping("/domain")
    @RequireAdmin("只有管理员可以维护治理信息")
    public JsonNode createDomain(@RequestBody Map<String, String> body) {
        return governance.createDomain(body);
    }

    @PostMapping("/dataProduct")
    @RequireAdmin("只有管理员可以维护治理信息")
    public JsonNode createDataProduct(@RequestBody Map<String, String> body) {
        return governance.createDataProduct(body);
    }

    @PostMapping("/glossaryTerm")
    @RequireAdmin("只有管理员可以维护治理信息")
    public JsonNode createGlossaryTerm(@RequestBody Map<String, String> body) {
        return governance.createGlossaryTerm(body);
    }
}
