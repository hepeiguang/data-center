package com.lhyf.dataasset.lineage;

import java.util.ArrayList;
import java.util.List;

/** Manual table/column lineage edge requested from the asset console. */
public class ManualLineageRequest {

    private String fromFqn;
    private String toFqn;
    private String sqlQuery;
    private String description;
    private List<ColumnMapping> columns = new ArrayList<>();

    public static class ColumnMapping {
        private String from;
        private String to;
        private String function;

        public String getFrom() {
            return from;
        }

        public void setFrom(String from) {
            this.from = from;
        }

        public String getTo() {
            return to;
        }

        public void setTo(String to) {
            this.to = to;
        }

        public String getFunction() {
            return function;
        }

        public void setFunction(String function) {
            this.function = function;
        }
    }

    public String getFromFqn() {
        return fromFqn;
    }

    public void setFromFqn(String fromFqn) {
        this.fromFqn = fromFqn;
    }

    public String getToFqn() {
        return toFqn;
    }

    public void setToFqn(String toFqn) {
        this.toFqn = toFqn;
    }

    public String getSqlQuery() {
        return sqlQuery;
    }

    public void setSqlQuery(String sqlQuery) {
        this.sqlQuery = sqlQuery;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<ColumnMapping> getColumns() {
        return columns;
    }

    public void setColumns(List<ColumnMapping> columns) {
        this.columns = columns == null ? new ArrayList<>() : columns;
    }
}
