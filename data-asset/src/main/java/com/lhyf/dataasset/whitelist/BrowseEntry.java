package com.lhyf.dataasset.whitelist;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BrowseEntry {

    public static final String PENDING = "PENDING";
    public static final String ACTIVE = "ACTIVE";
    public static final String REJECTED = "REJECTED";
    public static final String DISABLED = "DISABLED";

    private String id;
    private String datasource;
    private String database;
    private String schema;
    private List<String> tables = new ArrayList<>();
    private List<String> includeColumns = new ArrayList<>();
    private List<String> excludeColumns = new ArrayList<>();
    private List<String> maskColumns = new ArrayList<>();
    private String rowPolicy;
    private Subjects subjects = new Subjects();
    private int pageSize = 200;
    private int maxRows = 5000;
    private boolean exportEnabled;
    private String status = PENDING;
    private LocalDate validUntil;
    private String applicant;
    private String approvedBy;

    public static class Subjects {
        private List<String> users = new ArrayList<>();
        private List<String> roles = new ArrayList<>();
        private List<String> projects = new ArrayList<>();

        public List<String> getUsers() {
            return users;
        }

        public void setUsers(List<String> users) {
            this.users = users;
        }

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }

        public List<String> getProjects() {
            return projects;
        }

        public void setProjects(List<String> projects) {
            this.projects = projects;
        }

        public boolean isEmpty() {
            return users.isEmpty() && roles.isEmpty() && projects.isEmpty();
        }
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public List<String> getTables() {
        return tables;
    }

    public void setTables(List<String> tables) {
        this.tables = tables == null ? new ArrayList<>() : tables;
    }

    public List<String> getIncludeColumns() {
        return includeColumns;
    }

    public void setIncludeColumns(List<String> includeColumns) {
        this.includeColumns = includeColumns == null ? new ArrayList<>() : includeColumns;
    }

    public List<String> getExcludeColumns() {
        return excludeColumns;
    }

    public void setExcludeColumns(List<String> excludeColumns) {
        this.excludeColumns = excludeColumns == null ? new ArrayList<>() : excludeColumns;
    }

    public List<String> getMaskColumns() {
        return maskColumns;
    }

    public void setMaskColumns(List<String> maskColumns) {
        this.maskColumns = maskColumns == null ? new ArrayList<>() : maskColumns;
    }

    public String getRowPolicy() {
        return rowPolicy;
    }

    public void setRowPolicy(String rowPolicy) {
        this.rowPolicy = rowPolicy;
    }

    public Subjects getSubjects() {
        return subjects;
    }

    public void setSubjects(Subjects subjects) {
        this.subjects = subjects == null ? new Subjects() : subjects;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getMaxRows() {
        return maxRows;
    }

    public void setMaxRows(int maxRows) {
        this.maxRows = maxRows;
    }

    public boolean isExportEnabled() {
        return exportEnabled;
    }

    public void setExportEnabled(boolean exportEnabled) {
        this.exportEnabled = exportEnabled;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public String getApplicant() {
        return applicant;
    }

    public void setApplicant(String applicant) {
        this.applicant = applicant;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }
}
