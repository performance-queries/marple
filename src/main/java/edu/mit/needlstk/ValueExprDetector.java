package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.RuntimeException;

/// Class implementation to determine if an expression is a "value" expression
public class ValueExprDetector extends PerfQueryBaseVisitor<Boolean> {
  /// expr : column. A column is not a value expression
  @Override public Boolean visitExprCol(PerfQueryParser.ExprColContext ctx) {
    return new Boolean("false");
  }
  /// expr : value. A value is a value!
  @Override public Boolean visitExprVal(PerfQueryParser.ExprValContext ctx) {
    return ctx.getText().equals("infinity") ? new Boolean("false") : new Boolean("true");
  }

  /// expr : expr <combinator> expr. Only a value expression if both
  /// sub-expressions are value expressions.
  @Override public Boolean visitExprComb(PerfQueryParser.ExprCombContext ctx) {
    return visit(ctx.expr(0)) && visit(ctx.expr(1));
  }

  /// expr: ( expr ). Value expression iff internal expr is also one.
  @Override public Boolean visitExprParen(PerfQueryParser.ExprParenContext ctx) {
    return visit(ctx.expr());
  }
}
