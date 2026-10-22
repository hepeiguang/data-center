package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
@Slf4j
public class SqlParserBuilder {

    private PlainSelect plainSelect;
    private String mainTableAlias;

    private SqlParserBuilder() {
        // 用于 withSelect 模式
    }

    private SqlParserBuilder(String baseTable, Collection<String> columns, String whereClause) throws JSQLParserException {
        this.mainTableAlias = SqlAliasResolver.getTableAlias();
        Table mainTable = new Table(baseTable).withAlias(new Alias(this.mainTableAlias, false));
        this.plainSelect = new PlainSelect().withFromItem(mainTable);
        this.addColumns(mainTableAlias, columns);
        this.addWhereConditions(mainTableAlias, whereClause);

    }

    /**
     * 静态工厂方法创建构建器实例（从头开始）
     */
    public static SqlParserBuilder create(String baseTable) throws JSQLParserException {
        return new SqlParserBuilder(baseTable, null, null);
    }

    /**
     * 静态工厂方法创建构建器实例（从头开始）
     */
    public static SqlParserBuilder create(String baseTable, Collection<String> columns) throws JSQLParserException {
        return new SqlParserBuilder(baseTable, columns, null);
    }

    /**
     * 静态工厂方法创建构建器实例（从头开始）
     */
    public static SqlParserBuilder create(String baseTable, Collection<String> columns, String whereClause) throws JSQLParserException {
        return new SqlParserBuilder(baseTable, columns, whereClause);
    }

    /**
     * 静态工厂方法创建构建器实例（从头开始）
     */
    public static SqlParserBuilder create(String baseTable, String whereClause) throws JSQLParserException {
        return new SqlParserBuilder(baseTable, null, whereClause);
    }

    /**
     * 静态工厂方法创建构建器实例（从头开始）
     */
    public static SqlParserBuilder createWithSql(String sql) throws JSQLParserException {
        // 解析传入的 SQL
        Select subSelect = (Select) CCJSqlParserUtil.parse(sql);
        Alias alias = ((PlainSelect) subSelect.getSelectBody()).getFromItem().getAlias();
        String aliasName =  alias != null ? alias.getName() : "";

        // 创建 SubSelect 作为 FROM 子句
        SubSelect subSelectAsFromItem = new SubSelect();
        subSelectAsFromItem.setSelectBody(subSelect.getSelectBody());
        subSelectAsFromItem.setAlias(new Alias(aliasName, false));

        // 构建新的 PlainSelect，把子查询作为 fromItem
        SqlParserBuilder builder = new SqlParserBuilder();
        builder.plainSelect = new PlainSelect();
        builder.plainSelect.setFromItem(subSelectAsFromItem);
        builder.plainSelect.addSelectItems(new AllTableColumns(new Table(aliasName))); // SELECT *

        // 自动解析 baseAlias
        builder.mainTableAlias = aliasName != null ? aliasName : SqlAliasResolver.resolveBaseAlias(builder.plainSelect);

        return builder;
    }

    /**
     * 静态工厂方法：基于已有 PlainSelect 创建构建器
     */
    public static SqlParserBuilder withSelect(PlainSelect plainSelect) {
        SqlParserBuilder builder = new SqlParserBuilder();
        builder.plainSelect = plainSelect;

        // 自动解析 baseAlias
        builder.mainTableAlias = SqlAliasResolver.resolveBaseAlias(plainSelect);
        return builder;
    }

    /**
     * 添加 JOIN 信息
     */
    public SqlParserBuilder addJoin(JoinInfo joinInfo) throws JSQLParserException {
        Join join = new Join();
        String leftAlias = SqlAliasResolver.getTableAlias(plainSelect, joinInfo.getLeftTable());
        String rightAlias = SqlAliasResolver.getTableAlias();

        Table rightTable = new Table(joinInfo.getRightTable()).withAlias(new Alias(rightAlias, false));

        List<String> leftColumns = joinInfo.getLeftColumns();
        List<String> rightColumns = joinInfo.getRightColumns();

        if (leftColumns.size() != rightColumns.size()) {
            throw new IllegalArgumentException("Left and right column lists must be the same size");
        }

        Expression onExpr = null;
        for (int i = 0; i < leftColumns.size(); i++) {
            String leftCol = leftColumns.get(i);
            String rightCol = rightColumns.get(i);

            EqualsTo equalsTo = new EqualsTo(
                    new Column(new Table(leftAlias), leftCol),
                    new Column(rightTable, rightCol)
            );

            if (onExpr == null) {
                onExpr = equalsTo;
            } else {
                onExpr = new AndExpression(onExpr, equalsTo);
            }
        }

        if (StringUtils.isNotEmpty(joinInfo.getJoinCondition())) {
            Expression whereExpression = CCJSqlParserUtil.parseCondExpression(joinInfo.getJoinCondition());

            // 自定义 Visitor 添加表别名
            whereExpression.accept(new ExpressionVisitorAdapter() {
                @Override
                public void visit(Column column) {
                    column.setTable(new Table(rightAlias));
                }
            });
            onExpr = new AndExpression(onExpr, whereExpression);
        }

        join.withRightItem(rightTable);
        join.withOnExpression(onExpr);

        switch (joinInfo.getJoinType()) {
            case LEFT:
                join.setLeft(true);
                break;
            case RIGHT:
                join.setRight(true);
                break;
            case FULL:
                join.setFull(true);
                break;
            case INNER:
            default:
                join.setInner(true);
                break;
        }

        plainSelect.addJoins(join);

        if (CollectionUtils.isNotEmpty(joinInfo.getColumns())) {
            this.addColumns(rightAlias, joinInfo.getColumns());
        }
        if (StringUtils.isNotEmpty(joinInfo.getWhereCondition())) {
            this.addWhereConditions(rightAlias, joinInfo.getWhereCondition());
        }
        return this;
    }

    /**
     * 添加列
     *
     * @param alias   表别名
     * @param columns 添加列集合
     * @return SqlParserBuilder
     */
    public SqlParserBuilder addColumns(String alias, Collection<String> columns) {
        if (CollectionUtils.isNotEmpty(columns)) {
            columns.forEach(column -> {
                plainSelect.addSelectItems(new SelectExpressionItem(new Column(new Table(alias), column)));
            });
        } else {
            plainSelect.addSelectItems(new AllTableColumns(new Table(alias)));
        }
        return this;
    }

    /**
     * 添加 WHERE 条件
     *
     * @param alias       表别名
     * @param whereClause where 条件
     * @return SqlParserBuilder
     */
    public SqlParserBuilder addWhereConditions(String alias, String whereClause) {
        if (StringUtils.isEmpty(whereClause)) {
            return this;
        }

        try {
            if (alias == null) {
                alias = SqlAliasResolver.resolveBaseAlias(plainSelect);
            }
            // 解析新增的 WHERE 条件
            Expression newWhere = CCJSqlParserUtil.parseCondExpression(whereClause);

            // 自定义 Visitor 添加表别名
            String finalAlias = alias;
            newWhere.accept(new ExpressionVisitorAdapter() {
                @Override
                public void visit(Column column) {
                    column.setTable(new Table(finalAlias));
                }
            });

            // 获取原 WHERE 条件
            Expression existingWhere = plainSelect.getWhere();

            // 如果已有条件，用 AND 拼接
            if (existingWhere != null) {
                plainSelect.setWhere(new AndExpression(existingWhere, newWhere));
            } else {
                // 否则设置为新条件
                plainSelect.setWhere(newWhere);
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse or modify WHERE clause", e);
        }

        return this;
    }
    /**
     * 添加 ORDER BY 条件，支持多表多字段排序（ASC/DESC）
     *
     * @param orderByColumns 排序字段列表（基于原始表名）
     * @return SqlParserBuilder
     */
    public SqlParserBuilder addOrderBy(List<OrderByColumn> orderByColumns) {
        if (CollectionUtils.isEmpty(orderByColumns)) {
            return this;
        }
        try {
            for (OrderByColumn column : orderByColumns) {
                // 根据原始表名获取当前查询中的别名
                String tableAlias = SqlAliasResolver.getTableAlias(plainSelect, column.getTableName());
                log.info("SeaTunnel task custom sql tableAlias : {}", tableAlias);
                // 创建 Column 对象，带上别名
                Column orderColumn = new Column(new Table(tableAlias), column.getColumnName());

                // 创建 OrderByElement 并设置排序方向
                OrderByElement orderByElement = new OrderByElement();
                orderByElement.setExpression(orderColumn);
                orderByElement.setAsc(column.isAsc());

                // 添加到 PlainSelect 的 ORDER BY 列表中
                plainSelect.addOrderByElements(orderByElement);
                log.info("SeaTunnel task custom sql addOrderByElements : {}", JSONUtils.toJsonString(plainSelect));
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
        return this;
    }

    /**
     * 将当前 SQL 包装为子查询，并在外层添加 GROUP BY 及聚合字段
     *
     * @param groupByColumns 分组字段列表（基于原始表名）
     * @param aggColumns     聚合字段列表
     * @return SqlParserBuilder
     */
    public SqlParserBuilder addGroupBy(
            List<GroupByColumn> groupByColumns,
            List<AggregationColumn> aggColumns) {

        if (CollectionUtils.isEmpty(groupByColumns) && CollectionUtils.isEmpty(aggColumns)) {
            return this;
        }

        // Step 1: 获取当前 PlainSelect 并创建子查询
        SubSelect subSelect = new SubSelect();
        subSelect.setSelectBody(plainSelect);
        String subQueryAlias = SqlAliasResolver.getTableAlias();  // 子查询别名
        subSelect.setAlias(new Alias(subQueryAlias, false));

        // Step 2: 创建新的 PlainSelect 用于外层查询
        PlainSelect outerSelect = new PlainSelect();
        outerSelect.setFromItem(subSelect);

        // Step 3: 添加分组字段（使用子查询别名）
        if (CollectionUtils.isNotEmpty(groupByColumns)) {
            for (GroupByColumn column : groupByColumns) {
                // 构建带子查询别名的字段表达式，如 t1.name
                Column groupByCol = new Column(new Table(subQueryAlias), column.getColumnName());

                // 添加到 SELECT 列表
                outerSelect.addSelectItems(new SelectExpressionItem(groupByCol));
            }

            // 设置 GROUP BY 条件
            GroupByElement groupBy = new GroupByElement();
            List<Expression> groupByExprs = groupByColumns.stream()
                    .map(column -> (Expression) new Column(new Table(subQueryAlias), column.getColumnName()))
                    .collect(Collectors.toList());
            groupBy.setGroupByExpressions(groupByExprs);
            outerSelect.setGroupByElement(groupBy);
        }

        // Step 4: 添加聚合字段
        if (CollectionUtils.isNotEmpty(aggColumns)) {
            for (AggregationColumn column : aggColumns) {
                // 构建聚合字段表达式，如 t1.age
                Column aggCol = new Column(new Table(subQueryAlias), column.getColumnName());

                // 构建聚合函数，如 COUNT(t1.age)
                net.sf.jsqlparser.expression.Function aggFunc = new net.sf.jsqlparser.expression.Function();
                aggFunc.setName(column.getFunction());

                // 设置参数
                aggFunc.setParameters(new ExpressionList(aggCol));
                aggFunc.setAllColumns(false);
                aggFunc.setDistinct(false);

                // 构建 SelectExpressionItem 并设置别名
                SelectExpressionItem selectItem = new SelectExpressionItem(aggFunc);
                if (column.getAlias() != null) {
                    selectItem.setAlias(new Alias(column.getAlias(), false));
                }

                // 添加到外层 SELECT 中
                outerSelect.addSelectItems(selectItem);
            }
        }

        // Step 5: 替换当前 plainSelect
        this.plainSelect = outerSelect;
        this.mainTableAlias = subQueryAlias;

        return this;
    }


    /**
     * 获取最终生成的 SQL 字符串
     */
    public String toSQL() throws Exception {
        Select select = (Select) CCJSqlParserUtil.parse("SELECT * FROM DUAL"); // dummy
        select.setSelectBody(plainSelect);
        return select.toString();
    }

    /**
     * 返回构建好的 PlainSelect 对象，便于后续操作
     */
    public PlainSelect build() {
        return plainSelect;
    }
}
