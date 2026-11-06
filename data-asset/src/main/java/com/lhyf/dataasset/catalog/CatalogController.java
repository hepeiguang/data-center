package com.lhyf.dataasset.catalog;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lhyf.dataasset.auth.AuthInterceptor;

/** Catalog endpoints: routing only, orchestration lives in CatalogService. */
@RestController
@RequestMapping("/data-asset")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/search")
    public Map<String, Object> search(@RequestParam(defaultValue = "*") String q,
                                      @RequestParam(defaultValue = "20") int size,
                                      @RequestParam(defaultValue = "0") int from) {
        return catalog.search(q, size, from);
    }

    @GetMapping("/tree")
    public List<Map<String, Object>> tree(@RequestParam(defaultValue = "500") int size) {
        return catalog.tree(size);
    }

    @GetMapping("/detail")
    public Map<String, Object> detail(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                      @RequestAttribute(AuthInterceptor.ATTR_ADMIN) boolean admin,
                                      @RequestParam String fqn) {
        return catalog.detail(user, admin, fqn);
    }

    @GetMapping("/columns")
    public List<Map<String, Object>> columns(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                             @RequestAttribute(AuthInterceptor.ATTR_ADMIN) boolean admin,
                                             @RequestParam String fqn) {
        return catalog.columns(user, admin, fqn);
    }
}
