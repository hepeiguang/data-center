package com.lhyf.dataasset.audit;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class AuditService {

    private final AuditRepository repository;

    public AuditService(AuditRepository repository) {
        this.repository = repository;
    }

    public void record(String user, String action, String target, Object detail, int resultCode,
                       long durationMs, String clientIp) {
        repository.insert(user, action, target, detail, resultCode, durationMs, clientIp);
    }

    public List<Map<String, Object>> list(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return repository.recent(safeLimit);
    }
}
