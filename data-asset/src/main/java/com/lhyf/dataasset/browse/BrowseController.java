package com.lhyf.dataasset.browse;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.lhyf.dataasset.auth.AuthInterceptor;

@RestController
@RequestMapping("/data-asset")
public class BrowseController {

    private final BrowseService browseService;

    public BrowseController(BrowseService browseService) {
        this.browseService = browseService;
    }

    @GetMapping("/rows")
    public Map<String, Object> rows(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                    @RequestAttribute(AuthInterceptor.ATTR_ADMIN) boolean admin,
                                    @RequestParam String fqn,
                                    @RequestParam(defaultValue = "1") int page,
                                    @RequestParam(defaultValue = "100") int size,
                                    @RequestParam(required = false) String orderBy,
                                    @RequestParam(defaultValue = "true") boolean asc,
                                    @RequestParam(required = false) List<String> filter,
                                    HttpServletRequest request) {
        return browseService.browse(user, admin, fqn, page, size, orderBy, asc, parseFilters(filter),
                clientIp(request));
    }

    @PostMapping("/rows/export")
    public ResponseEntity<byte[]> export(@RequestAttribute(AuthInterceptor.ATTR_USER) String user,
                                         @RequestAttribute(AuthInterceptor.ATTR_ADMIN) boolean admin,
                                         @RequestBody Map<String, String> body,
                                         HttpServletRequest request) {
        String fqn = body.get("fqn");
        String csv = browseService.exportCsv(user, admin, fqn, clientIp(request));
        String fileName = fqn.replace('.', '_') + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .body(csv.getBytes(StandardCharsets.UTF_8));
    }

    private static List<QueryBuilder.Filter> parseFilters(List<String> raw) {
        List<QueryBuilder.Filter> filters = new ArrayList<>();
        if (raw == null) {
            return filters;
        }
        for (String item : raw) {
            String[] parts = item.split(":", 3);
            if (parts.length == 3 && !parts[0].isBlank()) {
                filters.add(new QueryBuilder.Filter(parts[0].trim(), parts[1].trim(), parts[2]));
            }
        }
        return filters;
    }

    private static String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
