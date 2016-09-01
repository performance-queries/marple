import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;

public class SymbolTableCreator extends perf_queryBaseListener {
  @Override public void enterStream(perf_queryParser.StreamContext ctx) { System.out.println("Entering stream context\n"); }
}
