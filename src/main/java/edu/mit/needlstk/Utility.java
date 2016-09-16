package edu.mit.needlstk;
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

  /// Get operation type for the given query
  public static OperationType getOperationType(ParserRuleContext query) {
    assert(query instanceof PerfQueryParser.StreamQueryContext);
    assert(query.getChildCount() == 1);
    ParseTree op = query.getChild(0);
    if (op instanceof PerfQueryParser.FilterContext) {
      // SELECT * FROM stream, so stream is at location 3
      return OperationType.FILTER;
    } else if (op instanceof PerfQueryParser.GroupbyContext) {
      // SELECT aggFunc FROM stream SGROUPBY ...
      return OperationType.GROUPBY;
    } else if (op instanceof PerfQueryParser.MapContext) {
      // SELECT exprList FROM stream
      return OperationType.PROJECT;
    } else if (op instanceof PerfQueryParser.ZipContext) {
      // stream JOIN stream
      return OperationType.JOIN;
    } else {
      assert(false);
      return OperationType.UNDEFINED;
    }
  }
}
