package com.lhyf.dataasset.browse;

/** H2 in PostgreSQL compatibility mode: same syntax as PostgreSQL for the fragments we emit. */
public final class H2SqlDialect extends IdentifierDialect {

    public static final H2SqlDialect INSTANCE = new H2SqlDialect();

    private H2SqlDialect() {
        super('"');
    }
}
