package com.lhyf.dataasset.browse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** Builds parameterized, whitelist-constrained SELECT statements. Never concatenates user values. */
public final class QueryBuilder {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{user\\.([A-Za-z0-9_]+)}");
    private static final List<String> OPS = List.of("=", "!=", ">", ">=", "<", "<=", "like");

    private QueryBuilder() {
    }

    public static final class Filter {
        public final String column;
        public final String op;
        public final String value;

        public Filter(String column, String op, String value) {
            this.column = column;
            this.op = op == null ? "=" : op.trim().toLowerCase();
            this.value = value;
        }
    }

    public static final class SqlQuery {
        public final String sql;
        public final List<Object> params;

        SqlQuery(String sql, List<Object> params) {
            this.sql = sql;
            this.params = params;
        }
    }

    public static SqlQuery build(SqlDialect dialect, String schema, String table, List<String> columns,
                                 List<Filter> filters, String orderBy, boolean ascending, int limit, int offset,
                                 String rowPolicy, Map<String, String> userAttributes) {
        if (columns == null || columns.isEmpty()) {
            throw new IllegalArgumentException("no visible columns");
        }
        StringBuilder sql = new StringBuilder("select ");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            sql.append(dialect.quote(columns.get(i)));
        }
        sql.append(" from ").append(dialect.quote(schema)).append('.').append(dialect.quote(table));

        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        if (rowPolicy != null && !rowPolicy.isBlank()) {
            conditions.add(expandPolicy(rowPolicy, userAttributes, params));
        }
        if (filters != null) {
            for (Filter filter : filters) {
                if (filter == null || filter.column == null) {
                    continue;
                }
                if (!columns.contains(filter.column)) {
                    throw new IllegalArgumentException("filter column not visible: " + filter.column);
                }
                if (!OPS.contains(filter.op)) {
                    throw new IllegalArgumentException("unsupported operator: " + filter.op);
                }
                conditions.add(dialect.quote(filter.column) + " " + filter.op + " ?");
                params.add(filter.value);
            }
        }
        if (!conditions.isEmpty()) {
            sql.append(" where ").append(String.join(" and ", conditions));
        }
        if (orderBy != null && !orderBy.isBlank()) {
            if (!columns.contains(orderBy)) {
                throw new IllegalArgumentException("order column not visible: " + orderBy);
            }
            sql.append(" order by ").append(dialect.quote(orderBy)).append(ascending ? " asc" : " desc");
        }
        sql.append(dialect.limitOffset());
        params.add(limit);
        params.add(offset);
        return new SqlQuery(sql.toString(), params);
    }

    private static String expandPolicy(String policy, Map<String, String> attributes, List<Object> params) {
        java.util.regex.Matcher matcher = PLACEHOLDER.matcher(policy);
        StringBuffer out = new StringBuffer();
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = attributes == null ? null : attributes.get(key);
            if (value == null && "name".equals(key) && attributes != null) {
                value = attributes.get("user");
            }
            if (value == null) {
                throw new IllegalArgumentException("row policy attribute missing: " + key);
            }
            params.add(value);
            matcher.appendReplacement(out, "?");
        }
        matcher.appendTail(out);
        return out.toString();
    }
}
