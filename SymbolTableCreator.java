import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.HashMap;

public class SymbolTableCreator extends perf_queryBaseListener {
  HashMap<String, IdentifierType> identifiers_ = new HashMap<String, IdentifierType>();

  @Override public void exitRelation(perf_queryParser.RelationContext ctx) {
    assert(ctx.getChildCount() == 1);
    identifiers_.put(ctx.getStart().getText(), IdentifierType.RELATION);
  }

  @Override public void exitStream(perf_queryParser.StreamContext ctx) {
    assert(ctx.getChildCount() == 1);
    identifiers_.put(ctx.getStart().getText(), IdentifierType.STREAM);
  }

  @Override public void exitColumn(perf_queryParser.ColumnContext ctx) {
    assert(ctx.getChildCount() == 1);
    identifiers_.put(ctx.getStart().getText(), IdentifierType.COLUMN);
  }

  @Override public void exitAgg_func(perf_queryParser.Agg_funcContext ctx) {
    assert(ctx.getChildCount() == 1);
    identifiers_.put(ctx.getStart().getText(), IdentifierType.AGG_FUNC);
  }

  @Override public void exitProg(perf_queryParser.ProgContext ctx) {
    System.out.println("identifiers_: " + identifiers_.toString() + "\n");
  }
}
