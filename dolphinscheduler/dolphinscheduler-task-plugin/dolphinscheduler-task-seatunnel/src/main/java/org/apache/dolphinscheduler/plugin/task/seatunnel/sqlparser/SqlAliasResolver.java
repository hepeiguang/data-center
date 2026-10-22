package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SubSelect;
import org.apache.dolphinscheduler.common.utils.JSONUtils;

import java.util.concurrent.atomic.AtomicInteger;
@Slf4j
public class SqlAliasResolver {

    private static final AtomicInteger TABLE_ALIAS_COUNTER = new AtomicInteger(0);

    /**
     * 从 PlainSelect 中提取主表别名，如果没有则自动生成一个
     */
    public static String resolveBaseAlias(PlainSelect plainSelect) {
        FromItem fromItem = plainSelect.getFromItem();

        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            if (table.getAlias() != null && table.getAlias().getName() != null) {
                return table.getAlias().getName();
            } else {
                // 自动生成别名 t0, t1...
                String generatedAlias = "t" + TABLE_ALIAS_COUNTER.getAndIncrement();
                table.setAlias(new Alias(generatedAlias, false));
                return generatedAlias;
            }
        }

        // 如果是 JOIN 结构，尝试获取最左侧的别名
        if (fromItem instanceof Join) {
            Join join = (Join) fromItem;
            FromItem leftItem = join.withLeft(true).getRightItem();
            if (leftItem instanceof Table) {
                Table leftTable = (Table) leftItem;
                if (leftTable.getAlias() != null) {
                    return leftTable.getAlias().getName();
                } else {
                    String generatedAlias = "t" + TABLE_ALIAS_COUNTER.getAndIncrement();
                    leftTable.setAlias(new Alias(generatedAlias, false));
                    return generatedAlias;
                }
            }
        }

        if (fromItem instanceof SubSelect) {
            return fromItem.getAlias().getName();
        }
        throw new IllegalArgumentException("Unsupported FROM item type: " + fromItem.getClass().getName());
    }

    public static String getTableAlias(PlainSelect plainSelect, String targetTableName) {
        FromItem fromItem = plainSelect.getFromItem();
        log.info("SeaTunnel task custom sql targetTableName : {}", targetTableName);
        // 处理普通表的情况
        if (fromItem instanceof Table) {
            Table table = (Table) fromItem;
            String actualTableName = table.getName();
            log.info("SeaTunnel task custom sql actualTableName : {}", actualTableName);
            if(actualTableName.contains(".")&&!targetTableName.contains(".")){
                actualTableName =actualTableName.split("\\.")[1];
            }
            log.info("SeaTunnel task custom sql actualTableName1 : {}", actualTableName);
            if (targetTableName.equalsIgnoreCase(actualTableName)) {
                Alias alias = table.getAlias();
                log.info("SeaTunnel task custom sql alias : {}", JSONUtils.toJsonString(alias));
                return alias != null ? alias.getName() : actualTableName;
            }
        }

        // 处理 JOIN 的情况，遍历所有 FROM 项
        for (Join join : plainSelect.getJoins()) {
            FromItem joinedFromItem = join.getRightItem();

            if (joinedFromItem instanceof Table) {
                Table joinedTable = (Table) joinedFromItem;
                String actualTableName = joinedTable.getName();

                if (targetTableName.equalsIgnoreCase(actualTableName)) {
                    Alias alias = joinedTable.getAlias();
                    return alias != null ? alias.getName() : actualTableName;
                }
            }
        }

        throw new IllegalArgumentException("Table '" + targetTableName + "' not found in the query.");
    }

    public static String getTableAlias() {
        return "t" + TABLE_ALIAS_COUNTER.getAndIncrement();
    }

    /**
     * 重置别名计数器（可选）
     */
    public static void resetAliasCounter() {
        TABLE_ALIAS_COUNTER.set(0);
    }
}
