package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser;

import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;

public class ColumnPrefixVisitor extends ExpressionVisitorAdapter {

    private final String originalTableName;
    private final String prefix;

    public ColumnPrefixVisitor(String originalTableName, String prefix) {
        this.originalTableName = originalTableName;
        this.prefix = prefix;
    }

    @Override
    public void visit(Column column) {
        if (column.getTable() == null || originalTableName.equals(column.getTable().getName())) {
            column.setTable(new Table(prefix));
        }
    }
}
