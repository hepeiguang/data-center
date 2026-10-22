package org.apache.dolphinscheduler.plugin.task.seatunnel.plugin.sql;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;
import org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser.*;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SqlParserExample {

    @Test
    public void test() throws Exception {
        SqlParserBuilder withSql = SqlParserBuilder.createWithSql(" select id,name from user as user");

        PlainSelect plainSelect = SqlParserBuilder.create("sys_users", Arrays.asList("id", "dept_id", "tenant_id"), "tenant_id=1")
                .build();
        System.out.println("plainSelect = " + plainSelect.toString());
        System.out.println(plainSelect.getFromItem().getAlias());
        String whereSql = "(" + plainSelect.toString() + ") ";

        List<AggregationColumn> aggregationColumns = new ArrayList<>();
        aggregationColumns.add(new AggregationColumn("COUNT", "sys_users", "CASE  WHEN `product_name` = '手机'  THEN '手机' END", "sale_amount"));

        PlainSelect plainSelect2 = SqlParserBuilder.create(whereSql, Arrays.asList("product_name", "sale_amount", "date"))
                .addGroupBy(Arrays.asList(new GroupByColumn("sys_users", "name")),aggregationColumns)
                .build();

        String name = plainSelect2.getFromItem().getAlias().getName();
        //build.addGroupByColumnReference( new Column(new Table(name),"department"));
        System.out.println("plainSelect2 = " + plainSelect2.toString());

    }

    @Test
    public void sqlCreateTest() throws Exception {

        JoinInfo userJoinDept = new JoinInfo(
                "sys_users", "sys_dept",
                Arrays.asList("dept_id", "tenant_id"),
                Arrays.asList("id", "tenant_id"),
                JoinType.LEFT
        ).setWhereCondition("tenant_id=1").setColumns(Arrays.asList("dept_id", "tenant_id"));
        JoinInfo userJoinPost = new JoinInfo(
                "sys_users", "sys_user_post",
                Arrays.asList("id"),
                Arrays.asList("user_id"),
                JoinType.INNER
        );
        JoinInfo userJoinPost2 = new JoinInfo(
                "sys_user_post", "sys_post",
                Arrays.asList("post_id"),
                Arrays.asList("id"),
                JoinType.RIGHT
        ).setJoinCondition("tenant_id=1").setColumns(Arrays.asList("*"));

        PlainSelect plainSelect = SqlParserBuilder.create("sys_users", Arrays.asList("id", "dept_id", "tenant_id"), "tenant_id=1")
                .build();
        System.out.println(SqlParserBuilder.withSelect(plainSelect).toSQL());


        String sql = SqlParserBuilder.withSelect(plainSelect).addJoin(userJoinDept)
                .addJoin(userJoinPost).addJoin(userJoinPost2)
                .toSQL();

        System.out.println(sql);


        plainSelect = SqlParserBuilder.withSelect(plainSelect).addOrderBy(Arrays.asList(
                new OrderByColumn("sys_users", "id", true),
                new OrderByColumn("sys_dept", "dept_name", false))).build();

        System.out.println(SqlParserBuilder.withSelect(plainSelect).toSQL());

        List<GroupByColumn> groupByColumns = Arrays.asList(
                new GroupByColumn("sys_users", "name"),
                new GroupByColumn("sys_users", "age")
        );

        List<AggregationColumn> aggColumns = Arrays.asList(
                new AggregationColumn("COUNT", "sys_dept", "id", "order_count"),
                new AggregationColumn("SUM", "sys_dept", "amount", "total_amount")
        );

        plainSelect = SqlParserBuilder.withSelect(plainSelect).addGroupBy(groupByColumns, aggColumns).build();
        System.out.println(SqlParserBuilder.withSelect(plainSelect).toSQL());
    }

}

