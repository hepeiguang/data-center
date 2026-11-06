package com.lhyf.dataasset.browse;

/** PostgreSQL: double-quoted identifiers, {@code limit ? offset ?} pagination. */
public final class PostgresSqlDialect extends IdentifierDialect {

    public static final PostgresSqlDialect INSTANCE = new PostgresSqlDialect();

    private PostgresSqlDialect() {
        super('"');
    }
}
