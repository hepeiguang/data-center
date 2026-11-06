package com.lhyf.dataasset.lineage;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lhyf.dataasset.auth.AuthInterceptor;
import com.lhyf.dataasset.auth.RequireAdmin;

/**
 * Lineage facade. Table and column lineage stay in OpenMetadata; this service adds only the key mappings
 * and row-level switches that column lineage cannot express.
 */
@RestController
@RequestMapping("/data-asset/lineage")
public class LineageController {

    private final LineageService lineage;

    public LineageController(LineageService lineage) {
        this.lineage = lineage;
    }

    @GetMapping("/table")
    public Map<String, Object> table(@RequestParam String fqn,
                                     @RequestParam(defaultValue = "2") int upstreamDepth,
                                     @RequestParam(defaultValue = "2") int downstreamDepth) {
        return lineage.table(fqn, upstreamDepth, downstreamDepth);
    }

    /** Create or replace a manually maintained table edge, with column mappings, inside OpenMetadata. */
    @PostMapping("/manual")
    @RequireAdmin("只有管理员可以维护血缘")
    public Map<String, Object> manual(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                      @RequestBody ManualLineageRequest request) {
        return lineage.manual(user, request);
    }

    @DeleteMapping("/manual")
    @RequireAdmin("只有管理员可以维护血缘")
    public Map<String, Object> removeManual(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                            @RequestParam String fromFqn, @RequestParam String toFqn) {
        return lineage.removeManual(user, fromFqn, toFqn);
    }

    @GetMapping("/row/resolve")
    public Map<String, Object> rowResolve(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                          @RequestAttribute(AuthInterceptor.ATTR_ADMIN) boolean admin,
                                          @RequestParam String fqn,
                                          @RequestParam(required = false) List<String> key,
                                          @RequestParam(defaultValue = "3") int depth) {
        return lineage.rowResolve(user, admin, fqn, key, depth);
    }

    @GetMapping("/keys")
    public List<Map<String, Object>> keys(@RequestParam(required = false) String targetFqn) {
        return lineage.listKeys(targetFqn);
    }

    @PostMapping("/keys")
    @RequireAdmin("只有管理员可以维护血缘")
    public Map<String, Object> addKey(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                      @RequestParam String targetFqn, @RequestParam String keyColumn,
                                      @RequestParam String sourceFqn, @RequestParam String sourceKeyColumn) {
        return lineage.addKey(user, targetFqn, keyColumn, sourceFqn, sourceKeyColumn);
    }

    @DeleteMapping("/keys/{id}")
    @RequireAdmin("只有管理员可以维护血缘")
    public Map<String, Object> removeKey(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                         @PathVariable long id) {
        return lineage.removeKey(user, id);
    }

    @GetMapping("/row-switch")
    public List<Map<String, Object>> rowSwitch() {
        return lineage.listSwitches();
    }

    @PutMapping("/row-switch")
    @RequireAdmin("只有管理员可以维护血缘")
    public Map<String, Object> setRowSwitch(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                            @RequestParam String tableFqn, @RequestParam boolean enabled) {
        return lineage.setRowSwitch(user, tableFqn, enabled);
    }
}
