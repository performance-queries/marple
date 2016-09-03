import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.RuntimeException;

/// Generate dependencies for each stream or relational query
/// , i.e., for each query print out the streams written to and read from
public class DepXtractor extends perf_queryBaseListener {
  private int id_ttype_;
  private HashMap<String, IdentifierType> symbol_table_;

  public DepXtractor(int identifier_ttype, HashMap<String, IdentifierType> symbol_table) {
    id_ttype_ = identifier_ttype;
    symbol_table_ = symbol_table;
  }

  @Override public void exitRelational_stmt(perf_queryParser.Relational_stmtContext ctx) {
    ParseTree relation = ctx.getChild(0);
    assert(relation instanceof perf_queryParser.RelationContext);

    ParseTree query = ctx.getChild(2);
    assert(query instanceof perf_queryParser.Relational_queryContext);

    ArrayList<String> input_streams = getInputStreams((perf_queryParser.Relational_queryContext)query);
    for (int i = 0; i < input_streams.size(); i++) {
      if (symbol_table_.get(input_streams.get(i)) != IdentifierType.STREAM) {
        throw new RuntimeException("Type mismatch, only STREAMS can be input to a query");
      }
    }
    System.out.println(relation.getText() + " <- " + input_streams);
  }

  @Override public void exitStream_stmt(perf_queryParser.Stream_stmtContext ctx) {
    ParseTree stream = ctx.getChild(0);
    assert(stream instanceof perf_queryParser.StreamContext);

    ParseTree query = ctx.getChild(2);
    assert(query instanceof perf_queryParser.Stream_queryContext);

    ArrayList<String> input_streams = getInputStreams((perf_queryParser.Stream_queryContext)query);
    for (int i = 0; i < input_streams.size(); i++) {
      if (symbol_table_.get(input_streams.get(i)) != IdentifierType.STREAM) {
        throw new RuntimeException("Type mismatch, only STREAMS can be input to a query");
      }
    }
    System.out.println(stream.getText() + " <- " + input_streams);
  }

  /// Get streams that are required for the given query
  ArrayList<String> getInputStreams(perf_queryParser.Stream_queryContext query) {
    assert(query.getChildCount() == 1);
    ParseTree op = query.getChild(0);
    if (op instanceof perf_queryParser.FilterContext) {
      // SELECT * FROM stream, so stream is at location 3
      return getAllTokens(op.getChild(3), id_ttype_);
    } else if (op instanceof perf_queryParser.SfoldContext) {
      // SELECT agg_func FROM stream SGROUPBY ...
      return getAllTokens(op.getChild(3), id_ttype_);
    } else if (op instanceof perf_queryParser.ProjectContext) {
      // SELECT expre_list FROM stream
      return getAllTokens(op.getChild(3), id_ttype_);
    } else if (op instanceof perf_queryParser.JoinContext) {
      // stream JOIN stream
      ArrayList<String> ret = getAllTokens(op.getChild(0), id_ttype_);
      ret.addAll(getAllTokens(op.getChild(2), id_ttype_));
      return ret;
    } else {
      assert(false);
      return new ArrayList<String>();
    }
  }

  /// Same as above, but do it for a relational query
  ArrayList<String> getInputStreams(perf_queryParser.Relational_queryContext query) {
    assert(query.getChildCount() == 1);
    ParseTree op = query.getChild(0);
    if (op instanceof perf_queryParser.RfoldContext) {
      // SELECT agg_func FROM stream RGROUPBY ...
      return getAllTokens(op.getChild(3), id_ttype_);
    } else {
      assert(false);
      return new ArrayList<String>();
    }
  }

  private ArrayList<String> getAllTokens(ParseTree node, int ttype) {
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
}
