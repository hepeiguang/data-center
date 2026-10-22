package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SqlParserUtil {

    /**
     * 向 SELECT 语句中添加 LEFT JOIN，并支持设置表别名
     *
     * @param selectStatement 原始的 SELECT 语句对象
     * @param leftTableName   左表名称（例如 "sys_users"）
     * @param rightTableName  右表名称（例如 "sys_dept"）
     * @param leftColumn      左表关联字段（例如 "dept_id"）
     * @param rightColumn     右表关联字段（例如 "id"）
     * @param leftTableAlias  左表别名（例如 "u"），可为 null
     * @param rightTableAlias 右表别名（例如 "d"），可为 null
     */
    public static void addLeftJoin(Select selectStatement,
                                   String leftTableName,
                                   String rightTableName,
                                   String leftColumn,
                                   String rightColumn,
                                   String leftTableAlias,
                                   String rightTableAlias) {
        if (!(selectStatement.getSelectBody() instanceof PlainSelect)) {
            return;
        }

        PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
        FromItem fromItem = plainSelect.getFromItem();

        if (fromItem instanceof FromItemWrapper) {
            FromItemWrapper wrapper = (FromItemWrapper) fromItem;
            if (wrapper.getLeftTable() instanceof Table &&
                    ((Table) wrapper.getLeftTable()).getName().equals(leftTableName)) {
                // 已经存在 LEFT JOIN，继续添加新的 JOIN 到右侧
                addJoinToNested(wrapper.getJoin().getRightItem(), rightTableName, leftColumn, rightColumn, leftTableAlias, rightTableAlias, plainSelect);
                return;
            }
        }

        Table leftTable = (Table) fromItem;

        // 设置左表别名（如果传入了）
        if (leftTableAlias != null) {
            leftTable.setAlias(new Alias(leftTableAlias));
        }

        // 创建JOIN条件表达式：u.dept_id = d.id
        Column leftExpr = new Column(new Table(leftTableAlias != null ? leftTableAlias : leftTableName), leftColumn);
        Column rightExpr = new Column(new Table(rightTableAlias != null ? rightTableAlias : rightTableName), rightColumn);

        Expression joinCondition = new EqualsTo(leftExpr, rightExpr);

        // 创建右表并设置别名
        Table rightTable = new Table(rightTableName);
        if (rightTableAlias != null) {
            rightTable.setAlias(new Alias(rightTableAlias));
        }

        // 创建JOIN对象
        Join join = new Join();
        join.setRightItem(rightTable);
        join.setOnExpression(joinCondition);
        join.setLeft(true); // LEFT JOIN

        // 构建新的FROM结构（包含JOIN）
        FromItemWrapper fromItemWrapper = new FromItemWrapper(leftTable);
        fromItemWrapper.setJoin(join);

        // 设置回PlainSelect
        plainSelect.setFromItem(fromItemWrapper);
    }

    private static void addJoinToNested(FromItem fromItem, String rightTableName, String leftColumn, String rightColumn, String leftTableAlias, String rightTableAlias, PlainSelect plainSelect) {
        if (fromItem instanceof Join) {
            Join join = (Join) fromItem;
            FromItem rightItem = join.getRightItem();

            if (rightItem instanceof FromItem) {
                addJoinToNested(rightItem, rightTableName, leftColumn, rightColumn, leftTableAlias, rightTableAlias, plainSelect);
                join.setRightItem((FromItem) rightItem);
                return;
            }
        } else if (fromItem instanceof Table) {
            Table leftTable = (Table) fromItem;

            // ✅ 正确创建左右表达式
            Column leftExpr = new Column(leftTable, leftColumn);
            Column rightExpr = new Column(new Table(rightTableName), rightColumn);

            Expression joinCondition = new EqualsTo(leftExpr, rightExpr); // ✅ 使用两个 Expression 参数

            Join newJoin = new Join();
            newJoin.setRightItem(new Table(rightTableName));
            newJoin.setOnExpression(joinCondition);
            newJoin.setLeft(true); // LEFT JOIN

            FromItemWrapper wrapper = new FromItemWrapper(leftTable);
            wrapper.setJoin(newJoin);

            if (plainSelect.getFromItem() instanceof Join) {
                ((Join) plainSelect.getFromItem()).setRightItem(wrapper);
            } else {
                plainSelect.setFromItem(wrapper);
            }
        }
    }



    /**
     * 向 SELECT 子句中添加指定列，并自动使用表别名（如果存在）
     *
     * @param selectStatement SELECT 语句对象
     * @param tableName       表名（例如 "sys_dept"）
     * @param columnName      列名（例如 "dept_name"）
     */
    public static void addSelectColumn(Select selectStatement, String tableName, String columnName) {
        if (!(selectStatement.getSelectBody() instanceof PlainSelect)) {
            return;
        }

        PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();

        // 查找主表是否有别名
        FromItem fromItem = plainSelect.getFromItem();
        // 构造 Column 使用别名或原表名
        String tablePrefix = getTableAliasOrName(tableName, fromItem); // 默认使用原表名

        // ✅ 关键修改：Column 使用别名前缀
        Column column = new net.sf.jsqlparser.schema.Column();
        column.setColumnName(columnName);
        column.setTable(new Table(tablePrefix)); // 这里设置别名前缀

        SelectExpressionItem selectExpressionItem = new SelectExpressionItem(column);
        plainSelect.getSelectItems().add(selectExpressionItem);
    }

    @Nullable
    public static Table getTargetTable(String tableName, FromItem fromItem) {
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            if (table.getName().equals(tableName)) {
                return table;
            }
        } else if (fromItem instanceof FromItemWrapper) {
            // 从 FromItemWrapper 中提取主表
            FromItemWrapper wrapper = (FromItemWrapper) fromItem;
            Table leftTable = (Table) wrapper.getLeftTable(); // 获取左表
            if (leftTable.getName().equals(tableName)) {
                return leftTable;
            }

            // 查找嵌套的 JOIN 表
            if (wrapper.getJoin() != null && wrapper.getJoin().getRightItem() instanceof Table) {
                Table rightTable = (Table) wrapper.getJoin().getRightItem();
                if (rightTable.getName().equals(tableName)) {
                    return rightTable;
                }
            }

            // 递归查找更深层的 JOIN
            if (wrapper.getJoin() != null && wrapper.getJoin().getRightItem() instanceof FromItem) {
                return getTargetTable(tableName, wrapper.getJoin().getRightItem());
            }
        }

        // 如果是普通的 Join 类型，继续查找
        if (fromItem instanceof Join) {
            Join join = (Join) fromItem;
            if (join.getRightItem() instanceof FromItem) {
                return getTargetTable(tableName, join.getRightItem());
            }
        }

        return null;
    }

    /**
     * 获取 SELECT 中的所有 JOIN（包括 FromItemWrapper 包装的 JOIN）
     *
     * @param fromItem 当前 FromItem
     * @return 所有 JOIN 的列表
     */
    public static List<Join> getAllJoins(FromItem fromItem) {
        List<Join> joins = new ArrayList<>();

        // ✅ 直接判断 fromItem 是否是 FromItemWrapper 类型
        if (fromItem instanceof FromItemWrapper) {
            FromItemWrapper wrapper = (FromItemWrapper) fromItem;

            Join currentJoin = wrapper.getJoin();
            if (currentJoin != null) {
                joins.add(currentJoin);

                // 递归处理嵌套的 JOIN
                if (currentJoin.getRightItem() instanceof FromItem) {
                    joins.addAll(getAllJoins(currentJoin.getRightItem()));
                }
            }
        }

        return joins;
    }

    /**
     * 向 SELECT 语句中添加 ORDER BY 排序字段，自动使用表别名（如果存在）
     *
     * @param selectStatement SELECT 语句对象
     * @param orderByColumns  排序字段列表
     */
    public static void addOrderByColumns(Select selectStatement, List<OrderByColumn> orderByColumns) {
        if (!(selectStatement.getSelectBody() instanceof PlainSelect)) {
            return;
        }

        PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();

        if (plainSelect.getOrderByElements() == null) {
            plainSelect.setOrderByElements(new ArrayList<>());
        }

        FromItem fromItem = plainSelect.getFromItem();

        for (OrderByColumn item : orderByColumns) {
            String tableName = item.getTableName();
            String columnName = item.getColumnName();
            boolean isAsc = item.isAsc();

            // ✅ 使用 getTableAliasOrName 获取别名

            String tablePrefix = getTableAliasOrName(tableName, fromItem);

            Column column = new Column(new Table(tablePrefix), columnName);

            OrderByElement orderByElement = new OrderByElement();
            orderByElement.setExpression(column);
            orderByElement.setAsc(isAsc);

            plainSelect.getOrderByElements().add(orderByElement);
        }
    }
    /**
     * 构建去重 SQL，基于 SELECT 语句和排序字段列表
     *
     * @param selectStatement 原始 SELECT 语句对象
     * @param distinctColumn  踢重字段列表
     * @return 去重后的完整 SELECT 语句对象
     */
    /**
     * 使用 DISTINCT 去重 SELECT 查询的结果（指定字段）
     *
     * @param selectStatement 原始 SELECT 语句对象
     * @param distinctColumns 指定去重字段列表
     * @return 修改后的 SELECT 对象
     */
    public static Select buildDeduplicateSelectWithDistinct(Select selectStatement, Set<String> distinctColumns) throws Exception {
        if (!(selectStatement.getSelectBody() instanceof PlainSelect)) {
            throw new IllegalArgumentException("Only support PlainSelect for deduplication");
        }
        PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();
        List<SelectItem> selectItems = plainSelect.getSelectItems();
        List<SelectItem> newSelectItems = new ArrayList<>();
        for (SelectItem item : selectItems) {
            if (item instanceof SelectExpressionItem) {
                Column column = (Column) ((SelectExpressionItem) item).getExpression();
                String fullColName = column.getColumnName();
                if (distinctColumns.contains(fullColName)) {
                    newSelectItems.add(item);
                }
            }
        }
        if (!newSelectItems.isEmpty()) {
            plainSelect.setSelectItems(newSelectItems);
        }
        // 设置 DISTINCT
        Distinct distinct = new Distinct();
        plainSelect.setDistinct(distinct);
        return selectStatement;
    }

    /**
     * 获取表的别名（如果没有别名则返回原表名）
     *
     * @param fromItem  当前 FromItem（主表或 JOIN 表）
     * @param tableName 县要查找的原始表名
     * @return 表的别名或原始表名
     */
    public static String getTableAliasOrName(String tableName, FromItem fromItem) {
        Table table = getTargetTable(tableName, fromItem);
        if (table == null) return tableName;

        return Optional.ofNullable(table.getAlias())
                .map(Alias::getName)
                .orElse(tableName);
    }

    /**
     * 替换 SELECT 子句中的字段表达式（支持加前缀 + 别名）
     *
     * @param selectStatement  SELECT 语句对象
     * @param oldColumnName    要替换的原字段名（如 "id"）
     * @param newQualifiedName 新字段全名（如 "u.id"）
     * @param aliasName        别名（如 "u_id"），可为 null
     */
    public static void renameSelectColumn(Select selectStatement, String oldColumnName, String newQualifiedName, String aliasName) {
        SelectBody selectBody = selectStatement.getSelectBody();
        if (!(selectBody instanceof PlainSelect)) {
            return;
        }

        PlainSelect plainSelect = (PlainSelect) selectBody;
        List<SelectItem> selectItems = plainSelect.getSelectItems();

        List<SelectItem> newSelectItems = new ArrayList<>();

        for (SelectItem item : selectItems) {
            if (item instanceof SelectExpressionItem) {
                SelectExpressionItem exprItem = (SelectExpressionItem) item;
                if (exprItem.getExpression() instanceof Column) {
                    Column column = (Column) exprItem.getExpression();
                    if (column.getColumnName().equals(oldColumnName)) {
                        // 创建新的 Column
                        Column newColumn = parseQualifiedColumn(newQualifiedName);

                        // 构造新 SelectExpressionItem
                        SelectExpressionItem newItem = new SelectExpressionItem(newColumn);
                        if (aliasName != null) {
                            newItem.setAlias(new Alias(aliasName));
                        }

                        newSelectItems.add(newItem);
                        continue;
                    }
                }
            }
            newSelectItems.add(item);
        }

        plainSelect.setSelectItems(newSelectItems);
    }


    /**
     * 为 SQL 中所有未带表名的字段添加指定前缀，并设置别名（适用于 SELECT / WHERE / ORDER BY 等）
     *
     * @param selectStatement SELECT 语句对象
     * @param tableName       字段所属表名（如 "sys_users"）
     * @param prefix          要添加的前缀（如 "u"）
     */
    public static void addColumnPrefix(Select selectStatement, String tableName, String prefix) {
        ColumnPrefixVisitor visitor = new ColumnPrefixVisitor(tableName, prefix);

        if (!(selectStatement.getSelectBody() instanceof PlainSelect)) {
            return;
        }

        PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();

        // 处理 SELECT 字段 + 设置别名
        List<SelectItem> newSelectItems = new ArrayList<>();
        for (SelectItem item : plainSelect.getSelectItems()) {
            if (item instanceof SelectExpressionItem) {
                SelectExpressionItem exprItem = (SelectExpressionItem) item;
                exprItem.getExpression().accept(visitor);

                Column column = (Column) exprItem.getExpression();
                if ((column.getTable() == null || tableName.equals(column.getTable().getName()))) {
                    Column newColumn = new Column(new Table(prefix), column.getColumnName());
                    SelectExpressionItem newItem = new SelectExpressionItem(newColumn);
                    newItem.setAlias(new Alias(prefix + "_" + column.getColumnName()));
                    newSelectItems.add(newItem);
                } else {
                    newSelectItems.add(item);
                }
            } else {
                newSelectItems.add(item);
            }
        }
        plainSelect.setSelectItems(newSelectItems);

        // 处理 WHERE 条件
        if (plainSelect.getWhere() != null) {
            plainSelect.getWhere().accept(visitor);
        }

        // ✅ 使用 getGroupBy() 替代 getGroupByItems() 或 getGroupByColumnReferences()
        if (plainSelect.getGroupBy() != null) {
            for (Expression expr : plainSelect.getGroupBy().getGroupByExpressionList().getExpressions()) {
                expr.accept(visitor);
            }
        }

        // 处理 ORDER BY
        if (plainSelect.getOrderByElements() != null) {
            plainSelect.getOrderByElements().forEach(orderByElement ->
                    orderByElement.getExpression().accept(visitor)
            );
        }

        // 处理 FROM 表别名
        FromItem fromItem = plainSelect.getFromItem();
        if (fromItem instanceof Table && ((Table) fromItem).getName().equals(tableName)) {
            ((Table) fromItem).setAlias(new Alias(prefix));
        }

        // 处理 JOIN ON 条件
        if (plainSelect.getJoins() != null) {
            for (Join join : plainSelect.getJoins()) {
                if (join.getOnExpression() != null) {
                    join.getOnExpression().accept(visitor);
                }
            }
        }
    }
    /**
     * 向 SELECT 语句中添加多个 GROUP BY 字段和多个聚合函数
     *
     * @param selectStatement 原始 SELECT 语句对象
     * @param tableName       表名（如 "orders"）
     * @param groupByColumns  要分组的字段列表（如 ["customer_id", "order_date"]）
     * @param aggregations    聚合函数配置项（如 [{func: "SUM", column: "amount", alias: "total"}]）
     */
    public static void addGroupByWithMultipleAggregations(Select selectStatement,
                                                          String tableName,
                                                          List<String> groupByColumns,
                                                          List<AggregationColumn> aggregations) throws Exception {
        if (!(selectStatement.getSelectBody() instanceof PlainSelect)) {
            throw new IllegalArgumentException("Only support PlainSelect");
        }

        PlainSelect plainSelect = (PlainSelect) selectStatement.getSelectBody();

        //  添加多个 GROUP BY 字段
        GroupByElement groupByElement = plainSelect.getGroupBy();
        if (groupByElement == null) {
            groupByElement = new GroupByElement();
            plainSelect.setGroupByElement(groupByElement);
        }

        Table table = new Table(tableName);
        if (groupByElement.getGroupByExpressionList().addExpressions() == null) {
            groupByElement.setGroupByExpressionList(new ExpressionList());
        }
        for (String colName : groupByColumns) {
            Column column = new Column(table, colName);
            groupByElement.getGroupByExpressionList().getExpressions().add(column);
        }

        //  添加多个聚合函数表达式
        for (AggregationColumn agg : aggregations) {
            Function function = new Function();
            function.setName(agg.getFunction().toUpperCase());

            List<Expression> parameters = new ArrayList<>();
            parameters.add(new Column(table, agg.getColumnName()));
            function.setParameters(new ExpressionList(parameters));

            SelectExpressionItem item = new SelectExpressionItem(function);
            if (agg.getAlias() != null && !agg.getAlias().isEmpty()) {
                item.setAlias(new Alias(agg.getAlias()));
            }

            plainSelect.getSelectItems().add(item);
        }
    }
    /**
     * 将形如 "u.id" 的字符串解析为带表名的 Column 对象
     */
    private static Column parseQualifiedColumn(String qualifiedName) {
        String[] parts = qualifiedName.split("\\.");
        Table table = new Table(parts[0]);
        return new Column(table, parts[1]);
    }

    public static void main(String[] args) throws Exception {
        // 示例 SQL 查询
        String sql = "SELECT t0.menu_sort, t0.del_flag, t0.menu_icon, t0.menu_hidden, t0.create_time, t0.menu_type, t0.remark, t0.menu_redirect, t0.create_by, t0.update_time, t0.parent_id, t0.menu_component, t0.menu_perms, t0.menu_name, t0.menu_path, t0.menu_code, t0.id, t0.update_by, t0.status FROM dam_test.dam_sys_menu t0";
        Select select = (Select) CCJSqlParserUtil.parse(sql);
        PlainSelect plainSelect = (PlainSelect) select.getSelectBody();
        // 构建去重字段列表
        List<OrderByColumn> ordertColumns = Arrays.asList(
                new OrderByColumn("dam_sys_menu", "id", true),
                new OrderByColumn("dam_sys_menu", "menu_name", false)
        );
//        String  actualTableName = "dam_test.dam_sys_menu";
//        if(actualTableName.contains(".")){
//            System.out.println(actualTableName.split("\\.")[1]);
//        }

        // 生成去重后的 SQL
        PlainSelect deduplicatedSelect = SqlParserBuilder.withSelect(plainSelect).addOrderBy( ordertColumns).build();
        System.out.println(deduplicatedSelect.toString());
    }

}
