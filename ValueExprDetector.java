import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.RuntimeException;

/// Class implementation to determine if an expression is a "value" expression
public class ValueExprDetector extends perf_queryBaseVisitor<Boolean> {
    /// expr : column. A column is not a value expression
    @Override public Boolean visitExprCol(perf_queryParser.ExprColContext ctx) {
	return new Boolean("false");
    }
    /// expr : value. A value is a value!
    @Override public Boolean visitExprVal(perf_queryParser.ExprValContext ctx) {
	return new Boolean("true");
    }

    /// expr: infinity. Infinity is not a value for our purposes.
    @Override public Boolean visitExprInf(perf_queryParser.ExprInfContext ctx) {
	return new Boolean("false");
    }

    /// expr : expr <combinator> expr. Only a value expression if both
    /// sub-expressions are value expressions.
    @Override public Boolean visitExprComb(perf_queryParser.ExprCombContext ctx)
    {
	return visit(ctx.expr(0)) && visit(ctx.expr(1));
    }

    /// expr: ( expr ). Value expression iff internal expr is also one.
    @Override public Boolean visitExprParen(perf_queryParser.ExprParenContext ctx) {
	return visit(ctx.expr());
    }
}
