package com.lhyf.dataasset.whitelist;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.lhyf.dataasset.auth.AuthInterceptor;
import com.lhyf.dataasset.auth.RequireAdmin;

@RestController
@RequestMapping("/data-asset/browse/entries")
public class WhitelistController {

    private final WhitelistService whitelist;

    public WhitelistController(WhitelistService whitelist) {
        this.whitelist = whitelist;
    }

    @GetMapping
    public List<BrowseEntry> list() {
        return whitelist.list();
    }

    @PostMapping
    @RequireAdmin("只有管理员可以管理浏览白名单")
    public BrowseEntry create(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                              @RequestBody BrowseEntry entry) {
        return whitelist.create(entry, user);
    }

    @PostMapping("/{id}/approve")
    @RequireAdmin("只有管理员可以管理浏览白名单")
    public Map<String, Object> approve(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                       @PathVariable String id) {
        whitelist.approve(id, user);
        return Map.of("id", id, "status", BrowseEntry.ACTIVE);
    }

    @PostMapping("/{id}/reject")
    @RequireAdmin("只有管理员可以管理浏览白名单")
    public Map<String, Object> reject(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                      @PathVariable String id,
                                      @RequestBody Map<String, String> body) {
        whitelist.reject(id, user, body.get("comment"));
        return Map.of("id", id, "status", BrowseEntry.REJECTED);
    }

    @PostMapping("/{id}/disable")
    @RequireAdmin("只有管理员可以管理浏览白名单")
    public Map<String, Object> disable(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                       @PathVariable String id) {
        whitelist.disable(id, user);
        return Map.of("id", id, "status", BrowseEntry.DISABLED);
    }

    @GetMapping("/{id}/approvals")
    public List<String> approvals(@PathVariable String id) {
        return whitelist.approvals(id);
    }
}
