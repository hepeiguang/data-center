package org.apache.dolphinscheduler.plugin.task.seatunnel.sqlparser;

import net.sf.jsqlparser.expression.*;
import net.sf.jsqlparser.expression.operators.arithmetic.*;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.conditional.XorExpression;
import net.sf.jsqlparser.expression.operators.relational.*;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.*;

public class FromItemWrapper implements FromItem {
    private final Table leftTable;
    private Join join;

    public FromItemWrapper(Table leftTable) {
        this.leftTable = leftTable;
    }

    public void setJoin(Join join) {
        this.join = join;
    }

    @Override
    public void accept(FromItemVisitor visitor) {
        // 访问左表
        leftTable.accept(visitor);

        // 如果有JOIN，访问JOIN结构
        if (join != null) {
            // 处理右表（如 dept）
            if (join.getRightItem() instanceof Table) {
                ((Table) join.getRightItem()).accept(visitor);
            }

            // 处理 JOIN 的 ON 表达式
            if (join.getOnExpression() != null) {
                join.getOnExpression().accept(new ExpressionVisitor() {
                    @Override
                    public void visit(BitwiseRightShift bitwiseRightShift) {

                    }

                    @Override
                    public void visit(BitwiseLeftShift bitwiseLeftShift) {

                    }

                    @Override
                    public void visit(NullValue nullValue) {}

                    @Override
                    public void visit(Function function) {}

                    @Override
                    public void visit(SignedExpression signedExpression) {}

                    @Override
                    public void visit(JdbcParameter jdbcParameter) {}

                    @Override
                    public void visit(JdbcNamedParameter jdbcNamedParameter) {}

                    @Override
                    public void visit(DoubleValue doubleValue) {}

                    @Override
                    public void visit(LongValue longValue) {}

                    @Override
                    public void visit(HexValue hexValue) {}

                    @Override
                    public void visit(DateValue dateValue) {}

                    @Override
                    public void visit(TimeValue timeValue) {}

                    @Override
                    public void visit(TimestampValue timestampValue) {}

                    @Override
                    public void visit(Parenthesis parenthesis) {}

                    @Override
                    public void visit(StringValue stringValue) {}

                    @Override
                    public void visit(Addition addition) {

                    }

                    @Override
                    public void visit(Division division) {

                    }

                    @Override
                    public void visit(IntegerDivision integerDivision) {}

                    @Override
                    public void visit(Multiplication multiplication) {

                    }

                    @Override
                    public void visit(Subtraction subtraction) {

                    }

                    @Override
                    public void visit(AndExpression andExpression) {}

                    @Override
                    public void visit(OrExpression orExpression) {}

                    @Override
                    public void visit(XorExpression xorExpression) {}

                    @Override
                    public void visit(Between between) {}

                    @Override
                    public void visit(OverlapsCondition overlapsCondition) {}

                    @Override
                    public void visit(EqualsTo equalsTo) {}

                    @Override
                    public void visit(GreaterThan greaterThan) {}

                    @Override
                    public void visit(GreaterThanEquals greaterThanEquals) {}

                    @Override
                    public void visit(InExpression inExpression) {}

                    @Override
                    public void visit(FullTextSearch fullTextSearch) {}

                    @Override
                    public void visit(IsNullExpression isNullExpression) {}

                    @Override
                    public void visit(IsBooleanExpression isBooleanExpression) {}

                    @Override
                    public void visit(LikeExpression likeExpression) {}

                    @Override
                    public void visit(MinorThan minorThan) {}

                    @Override
                    public void visit(MinorThanEquals minorThanEquals) {}

                    @Override
                    public void visit(NotEqualsTo notEqualsTo) {}

                    @Override
                    public void visit(Column column) {}

                    @Override
                    public void visit(SubSelect subSelect) {

                    }

                    @Override
                    public void visit(CaseExpression caseExpression) {

                    }

                    @Override
                    public void visit(WhenClause whenClause) {

                    }

                    @Override
                    public void visit(ExistsExpression existsExpression) {

                    }

                    @Override
                    public void visit(AnyComparisonExpression anyComparisonExpression) {

                    }

                    @Override
                    public void visit(Concat concat) {

                    }

                    @Override
                    public void visit(Matches matches) {

                    }

                    @Override
                    public void visit(BitwiseAnd bitwiseAnd) {

                    }

                    @Override
                    public void visit(BitwiseOr bitwiseOr) {

                    }

                    @Override
                    public void visit(BitwiseXor bitwiseXor) {

                    }

                    @Override
                    public void visit(CastExpression castExpression) {

                    }

                    @Override
                    public void visit(TryCastExpression tryCastExpression) {

                    }

                    @Override
                    public void visit(SafeCastExpression safeCastExpression) {

                    }

                    @Override
                    public void visit(Modulo modulo) {

                    }

                    @Override
                    public void visit(AnalyticExpression analyticExpression) {

                    }

                    @Override
                    public void visit(ExtractExpression extractExpression) {

                    }

                    @Override
                    public void visit(IntervalExpression intervalExpression) {

                    }

                    @Override
                    public void visit(OracleHierarchicalExpression oracleHierarchicalExpression) {

                    }

                    @Override
                    public void visit(RegExpMatchOperator regExpMatchOperator) {

                    }

                    @Override
                    public void visit(JsonExpression jsonExpression) {

                    }

                    @Override
                    public void visit(JsonOperator jsonOperator) {

                    }

                    @Override
                    public void visit(RegExpMySQLOperator regExpMySQLOperator) {

                    }

                    @Override
                    public void visit(UserVariable userVariable) {

                    }

                    @Override
                    public void visit(NumericBind numericBind) {

                    }

                    @Override
                    public void visit(KeepExpression keepExpression) {

                    }

                    @Override
                    public void visit(MySQLGroupConcat mySQLGroupConcat) {

                    }

                    @Override
                    public void visit(ValueListExpression valueListExpression) {

                    }

                    @Override
                    public void visit(RowConstructor rowConstructor) {

                    }

                    @Override
                    public void visit(RowGetExpression rowGetExpression) {

                    }

                    @Override
                    public void visit(OracleHint oracleHint) {

                    }

                    @Override
                    public void visit(TimeKeyExpression timeKeyExpression) {

                    }

                    @Override
                    public void visit(DateTimeLiteralExpression dateTimeLiteralExpression) {

                    }

                    @Override
                    public void visit(NotExpression notExpression) {

                    }

                    @Override
                    public void visit(NextValExpression nextValExpression) {

                    }

                    @Override
                    public void visit(CollateExpression collateExpression) {

                    }

                    @Override
                    public void visit(SimilarToExpression similarToExpression) {

                    }

                    @Override
                    public void visit(ArrayExpression arrayExpression) {

                    }

                    @Override
                    public void visit(ArrayConstructor arrayConstructor) {

                    }

                    @Override
                    public void visit(VariableAssignment variableAssignment) {

                    }

                    @Override
                    public void visit(XMLSerializeExpr xmlSerializeExpr) {

                    }

                    @Override
                    public void visit(TimezoneExpression timezoneExpression) {

                    }

                    @Override
                    public void visit(JsonAggregateFunction jsonAggregateFunction) {

                    }

                    @Override
                    public void visit(JsonFunction jsonFunction) {

                    }

                    @Override
                    public void visit(ConnectByRootOperator connectByRootOperator) {

                    }

                    @Override
                    public void visit(OracleNamedFunctionParameter oracleNamedFunctionParameter) {

                    }

                    @Override
                    public void visit(AllColumns allColumns) {

                    }

                    @Override
                    public void visit(AllTableColumns allTableColumns) {

                    }

                    @Override
                    public void visit(AllValue allValue) {

                    }

                    @Override
                    public void visit(IsDistinctExpression isDistinctExpression) {

                    }

                    @Override
                    public void visit(GeometryDistance geometryDistance) {

                    }
                });
            }
        }
    }

    @Override
    public String toString() {
        return leftTable.toString() + (join != null ? " " + join.toString() : "");
    }


    @Override
    public Alias getAlias() {
        return leftTable.getAlias();
    }

    @Override
    public void setAlias(Alias alias) {
        leftTable.setAlias(alias);
    }

    @Override
    public Pivot getPivot() {
        return null;
    }

    @Override
    public void setPivot(Pivot pivot) {

    }

    @Override
    public UnPivot getUnPivot() {
        return null;
    }

    @Override
    public void setUnPivot(UnPivot unPivot) {

    }
    // ✅ 添加 getJoin() 方法
    public Join getJoin() {
        return join;
    }

    public Object getLeftTable() {
        return leftTable;
    }
}
