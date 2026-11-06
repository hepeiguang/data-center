package com.lhyf.dataasset.whitelist;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.lhyf.dataasset.util.TtlCache;

@Service
public class WhitelistService {

    private final WhitelistRepository repository;
    private final TtlCache<String, List<BrowseEntry>> activeCache = new TtlCache<>(10_000);

    public WhitelistService(WhitelistRepository repository) {
        this.repository = repository;
    }

    public List<BrowseEntry> list() {
        return repository.findAll();
    }

    public BrowseEntry find(String id) {
        return repository.findById(id);
    }

    public BrowseEntry create(BrowseEntry entry, String applicant) {
        validate(entry);
        entry.setId(UUID.randomUUID().toString().replace("-", ""));
        entry.setApplicant(applicant);
        entry.setStatus(BrowseEntry.PENDING);
        repository.insert(entry);
        invalidate();
        return entry;
    }

    public void approve(String id, String approver) {
        BrowseEntry entry = find(id);
        if (entry == null) {
            throw new IllegalArgumentException("entry not found: " + id);
        }
        if (approver != null && approver.equals(entry.getApplicant())) {
            throw new IllegalArgumentException("applicant cannot approve own entry");
        }
        repository.updateStatus(id, BrowseEntry.ACTIVE, approver);
        repository.insertApprovalEvent(id, "APPROVE", approver, null);
        invalidate();
    }

    public void reject(String id, String approver, String comment) {
        if (!StringUtils.hasText(comment)) {
            throw new IllegalArgumentException("reject reason is required");
        }
        repository.updateStatus(id, BrowseEntry.REJECTED, approver);
        repository.insertApprovalEvent(id, "REJECT", approver, comment);
        invalidate();
    }

    public void disable(String id, String actor) {
        repository.updateStatusOnly(id, BrowseEntry.DISABLED);
        repository.insertApprovalEvent(id, "DISABLE", actor, null);
        invalidate();
    }

    public List<String> approvals(String id) {
        return repository.approvalLines(id);
    }

    /** Finds an active entry granting this user access to the table fqn (service.database.schema.table). */
    public BrowseEntry findActiveFor(String user, boolean admin, String fqn) {
        String[] parts = fqn.split("\\.");
        if (parts.length < 4) {
            return null;
        }
        for (BrowseEntry entry : activeEntries()) {
            if (entry.getValidUntil() != null && entry.getValidUntil().isBefore(LocalDate.now())) {
                continue;
            }
            if (!eq(entry.getDatasource(), parts[0]) || !eq(entry.getDatabase(), parts[1])
                    || !eq(entry.getSchema(), parts[2])) {
                continue;
            }
            if (!matchesTable(entry, fqn, parts[3])) {
                continue;
            }
            if (matchesSubject(entry, user, admin)) {
                return entry;
            }
        }
        return null;
    }

    private List<BrowseEntry> activeEntries() {
        return activeCache.get("active", repository::findActive);
    }

    private static boolean matchesTable(BrowseEntry entry, String fqn, String table) {
        for (String configured : entry.getTables()) {
            if (fqn.equalsIgnoreCase(configured) || table.equalsIgnoreCase(configured)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesSubject(BrowseEntry entry, String user, boolean admin) {
        BrowseEntry.Subjects subjects = entry.getSubjects();
        if (subjects.isEmpty()) {
            return admin;
        }
        for (String candidate : subjects.getUsers()) {
            if (candidate.equalsIgnoreCase(user)) {
                return true;
            }
        }
        for (String role : subjects.getRoles()) {
            if ("ADMIN".equalsIgnoreCase(role) && admin) {
                return true;
            }
        }
        return false;
    }

    private static boolean eq(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }

    private static void validate(BrowseEntry entry) {
        if (!StringUtils.hasText(entry.getDatasource()) || !StringUtils.hasText(entry.getDatabase())
                || !StringUtils.hasText(entry.getSchema()) || entry.getTables().isEmpty()) {
            throw new IllegalArgumentException("datasource, database, schema and tables are required");
        }
        for (String table : entry.getTables()) {
            if ("*".equals(table) || table.contains("*")) {
                throw new IllegalArgumentException("wildcard tables are not allowed: " + table);
            }
        }
        if (entry.getPageSize() <= 0 || entry.getPageSize() > 1000) {
            throw new IllegalArgumentException("pageSize must be between 1 and 1000");
        }
        if (entry.getMaxRows() <= 0 || entry.getMaxRows() > 100_000) {
            throw new IllegalArgumentException("maxRows must be between 1 and 100000");
        }
    }

    private void invalidate() {
        activeCache.clear();
    }
}
