package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser;

import lombok.Getter;

import java.util.Collection;
import java.util.List;

@Getter
public class JoinInfo {
    private final String leftTable;
    private final String rightTable;
    private final List<String> leftColumns;
    private final List<String> rightColumns;
    private final JoinType joinType;
    private String whereCondition;
    private String joinCondition;
    private Collection<String> columns;
    public JoinInfo(String leftTable,
                    String rightTable,
                    List<String> leftColumns,
                    List<String> rightColumns,
                    JoinType joinType) {
        this.leftTable = leftTable;
        this.rightTable = rightTable;
        this.leftColumns = leftColumns;
        this.rightColumns = rightColumns;
        this.joinType = joinType;
    }

    public JoinInfo setWhereCondition(String whereCondition) {
        this.whereCondition = whereCondition;
        return this;
    }

    public JoinInfo setColumns(Collection<String> columns) {
        this.columns = columns;
        return this;
    }

    public JoinInfo setJoinCondition(String joinCondition) {
        this.joinCondition = joinCondition;
        return this;
    }

}
