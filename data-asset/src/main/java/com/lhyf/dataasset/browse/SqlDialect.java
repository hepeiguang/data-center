package com.lhyf.dataasset.browse;

/** Dialect-specific SQL fragments for the read-only business queries built by {@link QueryBuilder}. */
public interface SqlDialect {

    /** Validates the identifier and quotes it for this dialect. */
    String quote(String identifier);

    /** Trailing pagination clause with two placeholders (limit, offset). */
    String limitOffset();

    static SqlDialect forJdbcUrl(String jdbcUrl) {
        if (jdbcUrl != null && jdbcUrl.toLowerCase().startsWith("jdbc:h2:")) {
            return H2SqlDialect.INSTANCE;
        }
        return PostgresSqlDialect.INSTANCE;
    }
}
