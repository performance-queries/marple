import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.ArrayList;

class Utility {
  /// Get All Tokens of a specific type within a production
  public static ArrayList<String> getAllTokens(ParseTree node, int ttype) {
    if (node instanceof TerminalNode) {
      ArrayList<String> token = new ArrayList<String>();
      if (((TerminalNode)node).getSymbol().getType() == ttype) {
        token.add(((TerminalNode)node).getSymbol().getText());
      }
      return token;
    } else {
      assert(node instanceof ParserRuleContext);
      ArrayList<String> tokens = new ArrayList<String>();
      // get all tokens of children
      for (int i = 0; i < node.getChildCount(); i++) {
        tokens.addAll(getAllTokens(node.getChild(i), ttype));
      }
      return tokens;
    }
  }

  /// get all tokens regardless of token type
  public static ArrayList<String> getAllTokens(ParseTree node) {
    if (node instanceof TerminalNode) {
      ArrayList<String> token = new ArrayList<String>();
      token.add(((TerminalNode)node).getSymbol().getText());
      return token;
    } else {
      assert(node instanceof ParserRuleContext);
      ArrayList<String> tokens = new ArrayList<String>();
      // get all tokens of children
      for (int i = 0; i < node.getChildCount(); i++) {
        tokens.addAll(getAllTokens(node.getChild(i)));
      }
      return tokens;
    }
  }
}
