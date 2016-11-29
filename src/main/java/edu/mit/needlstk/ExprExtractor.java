package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.List;
import java.util.ArrayList;

public class ExprExtractor {
  public static List<PerfQueryParser.ExprContext> getExprs(PerfQueryParser.ExprListContext ctx)
  {
    List<PerfQueryParser.ExprContext> exprs = new ArrayList<>();
    exprs.add(ctx.expr());
    for (PerfQueryParser.ExprWithCommaContext ectx: ctx.exprWithComma()) {
      exprs.add(ectx.expr());
    }
    return exprs;
  }
}
