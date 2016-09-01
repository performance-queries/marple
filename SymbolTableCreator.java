import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.TreeSet;

public class SymbolTableCreator extends perf_queryBaseListener {
  perf_queryLexer lexer_;
  TreeSet<String> identifiers_ = new TreeSet<String>();
  TreeSet<String> streams_     = new TreeSet<String>();
  TreeSet<String> columns_     = new TreeSet<String>();
  TreeSet<String> agg_funcs_   = new TreeSet<String>();

  public SymbolTableCreator(perf_queryLexer lexer) { this.lexer_ = lexer; }
  @Override public void visitTerminal(TerminalNode node) {
    if (lexer_.getVocabulary().getSymbolicName(node.getSymbol().getType()) == "ID") {
      identifiers_.add(node.getSymbol().getText());
    }
  }

  @Override public void exitStream(perf_queryParser.StreamContext ctx) {
    assert(ctx.getChildCount() == 1);
    streams_.add(ctx.getStart().getText());
  }

  @Override public void exitColumn(perf_queryParser.ColumnContext ctx) {
    assert(ctx.getChildCount() == 1);
    columns_.add(ctx.getStart().getText());
  }

  @Override public void exitAgg_func(perf_queryParser.Agg_funcContext ctx) {
    assert(ctx.getChildCount() == 1);
    agg_funcs_.add(ctx.getStart().getText());
  }

  @Override public void exitProg(perf_queryParser.ProgContext ctx) {
    System.out.println("identifiers_: " + identifiers_.toString() + "\n");
    System.out.println("streams_: " + streams_.toString());
    System.out.println("columns_: " + columns_.toString());
    System.out.println("agg_funcs_: " + agg_funcs_.toString());
    assert(identifiers_.size() == streams_.size() + columns_.size() + agg_funcs_.size());
  }
}
