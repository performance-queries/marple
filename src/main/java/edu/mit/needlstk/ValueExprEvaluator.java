package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.RuntimeException;

/// Class implementation to evaluate a value expression.
public class ValueExprEvaluator extends PerfQueryBaseVisitor<Integer> {
  /// expr : column. A column is not a value expression; just return -1.
  @Override public Integer visitExprCol(PerfQueryParser.ExprColContext ctx) {
    return -1;
  }
  /// expr : value. A value is a value, return it.
  @Override public Integer visitExprVal(PerfQueryParser.ExprValContext ctx) {
    return ctx.getText().equals("infinity") ? -1 : Integer.valueOf(ctx.getText());
  }

  /// expr : expr <combinator> expr. Result is valid only if both
  /// sub-expressions are value expressions.
  @Override public Integer visitExprComb(PerfQueryParser.ExprCombContext ctx) {
    Integer e1 = visit(ctx.expr(0));
    Integer e2 = visit(ctx.expr(1));
    Integer result;
    switch (ctx.op.getText()) {
      case "+": result = e1 + e2; break;
      case "-": result = e1 - e2; break;
      case "*": result = e1 * e2; break;
      case "/": result = e1 / e2; break;
      default: result = -1; assert(false);
    }
    return result;
  }

  /// expr: ( expr ). Value expression iff internal expr is also one.
  @Override public Integer visitExprParen(PerfQueryParser.ExprParenContext ctx) {
    return visit(ctx.expr());
  }
}
