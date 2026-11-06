package com.lhyf.dataasset.browse;

import java.util.regex.Pattern;

/** Shared identifier validation; only the quoting character varies between supported dialects. */
abstract class IdentifierDialect implements SqlDialect {

    private static final Pattern IDENTIFIER = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]{0,127}$");

    private final String quoteChar;

    IdentifierDialect(char quoteChar) {
        this.quoteChar = String.valueOf(quoteChar);
    }

    @Override
    public String quote(String identifier) {
        if (identifier == null || !IDENTIFIER.matcher(identifier).matches()) {
            throw new IllegalArgumentException("invalid identifier: " + identifier);
        }
        return quoteChar + identifier + quoteChar;
    }

    @Override
    public String limitOffset() {
        return " limit ? offset ?";
    }
}
