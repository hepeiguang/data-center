/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.plugin.task.openmetadata;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.Join;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectBody;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.select.SelectItem;

/**
 * Best-effort column lineage from INSERT ... SELECT or SELECT queries used by SeaTunnel SQL transforms.
 * Table FQNs are supplied by the caller; only column pairs are derived here.
 */
public final class SqlLineageParser {

    private SqlLineageParser() {
    }

    public static ParsedLineage parse(String sql, String sourceFqn, String targetFqn) {
        if (StringUtils.isBlank(sql)) {
            return ParsedLineage.empty();
        }
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);
            Table target = null;
            List<String> targetColumns = Collections.emptyList();
            Select select = null;
            if (statement instanceof Insert) {
                Insert insert = (Insert) statement;
                target = insert.getTable();
                if (insert.getColumns() != null) {
                    targetColumns = new ArrayList<>();
                    for (Column column : insert.getColumns()) {
                        targetColumns.add(column.getColumnName());
                    }
                }
                select = insert.getSelect();
            } else if (statement instanceof Select) {
                select = (Select) statement;
            }
            if (select == null) {
                return ParsedLineage.empty();
            }
            SelectBody selectBody = select.getSelectBody();
            if (!(selectBody instanceof PlainSelect)) {
                return ParsedLineage.empty();
            }
            PlainSelect plain = (PlainSelect) selectBody;

            Set<String> tables = new LinkedHashSet<>();
            addTable(tables, plain.getFromItem());
            if (plain.getJoins() != null) {
                for (Join join : plain.getJoins()) {
                    addTable(tables, join.getRightItem());
                }
            }

            List<ColumnLineageParam> columns = new ArrayList<>();
            if (StringUtils.isNotBlank(sourceFqn) && StringUtils.isNotBlank(targetFqn)) {
                int index = 0;
                for (SelectItem item : plain.getSelectItems()) {
                    if (!(item instanceof SelectExpressionItem)) {
                        continue;
                    }
                    SelectExpressionItem expressionItem = (SelectExpressionItem) item;
                    if (!(expressionItem.getExpression() instanceof Column)) {
                        continue;
                    }
                    String sourceColumn = ((Column) expressionItem.getExpression()).getColumnName();
                    String alias = expressionItem.getAlias() == null
                            ? sourceColumn
                            : expressionItem.getAlias().getName();
                    String targetColumn = index < targetColumns.size() ? targetColumns.get(index) : alias;
                    ColumnLineageParam mapping = new ColumnLineageParam();
                    mapping.setFromColumns(Collections.singletonList(sourceFqn + "." + sourceColumn));
                    mapping.setToColumn(targetFqn + "." + targetColumn);
                    columns.add(mapping);
                    index++;
                }
            }
            return new ParsedLineage(tables, target == null ? null : target.getName(), columns);
        } catch (Exception e) {
            return ParsedLineage.empty();
        }
    }

    private static void addTable(Set<String> tables, FromItem fromItem) {
        if (fromItem instanceof Table) {
            tables.add(((Table) fromItem).getName());
        }
    }

    public static final class ParsedLineage {

        private final Set<String> sourceTables;
        private final String targetTable;
        private final List<ColumnLineageParam> columnLineage;

        private ParsedLineage(Set<String> sourceTables, String targetTable, List<ColumnLineageParam> columnLineage) {
            this.sourceTables = sourceTables;
            this.targetTable = targetTable;
            this.columnLineage = columnLineage;
        }

        static ParsedLineage empty() {
            return new ParsedLineage(Collections.emptySet(), null, Collections.emptyList());
        }

        public Set<String> getSourceTables() {
            return sourceTables;
        }

        public String getTargetTable() {
            return targetTable;
        }

        public List<ColumnLineageParam> getColumnLineage() {
            return columnLineage;
        }
    }
}
