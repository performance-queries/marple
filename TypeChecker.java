import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;

public class TypeChecker extends perf_queryBaseListener {
  private int id_ttype_;

  public TypeChecker(int identifier_ttype) { id_ttype_ = identifier_ttype; }

  @Override public void exitRelational_stmt(perf_queryParser.Relational_stmtContext ctx) {
    ParseTree relation = ctx.getChild(0);
    assert(relation instanceof perf_queryParser.RelationContext);
    System.out.println("Assigning to relation " + relation.getText());

    // Get all identifiers within the query
    ParseTree query = ctx.getChild(2);
    assert(query instanceof perf_queryParser.Relational_queryContext);
    System.out.println("Query has " + getAllTokens(query, id_ttype_).size() + " identifiers");
  }

  @Override public void exitStream_stmt(perf_queryParser.Stream_stmtContext ctx) {
    ParseTree stream = ctx.getChild(0);
    assert(stream instanceof perf_queryParser.StreamContext);
    System.out.println("Assigning to stream " + stream.getText());

    // Get all identifiers within the query
    ParseTree query = ctx.getChild(2);
    assert(query instanceof perf_queryParser.Stream_queryContext);
    System.out.println("Query has " + getAllTokens(query, id_ttype_).size() + " identifiers");
  }

  ArrayList<Token> getAllTokens(ParseTree node, int ttype) {
    if (node instanceof TerminalNode) {
      ArrayList<Token> token = new ArrayList<Token>();
      if (((TerminalNode)node).getSymbol().getType() == ttype) {
        token.add(((TerminalNode)node).getSymbol());
      }
      return token;
    } else {
      assert(node instanceof ParserRuleContext);
      ArrayList<Token> tokens = new ArrayList<Token>();
      // get all tokens of children
      for (int i = 0; i < node.getChildCount(); i++) {
        tokens.addAll(getAllTokens(node.getChild(i), ttype));
      }
      return tokens;
    }
  }
}
