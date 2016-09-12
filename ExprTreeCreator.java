import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.RuntimeException;

/// First, generate dependencies for each stream or relational query
/// , i.e., for each query print out the streams written to and read from
/// Second, use this dependency table to build an expression tree
public class ExprTreeCreator extends PerfQueryBaseListener {
  /// The token type for identifiers. This is required to check if a given token
  /// is an identifier or not.
  private int id_ttype_;

  /// A reference to the symbol table created by the SymbolTableCreator pass
  private HashMap<String, IdentifierType> symbol_table_;

  /// The dependency table mapping from identifiers to operations, populated
  /// by this pass.
  private HashMap<String, Operation> dep_table_ = new HashMap<String, Operation>();

  /// A map from a stream/relation symbol to its context in the overall parse tree.
  private HashMap<String, ParserRuleContext> sym_tree_ = new HashMap<String, ParserRuleContext>();

  /// The last identifier assigned so far. Used to build an expression tree.
  private String last_assigned_id_ = "";

  /// Constructor
  public ExprTreeCreator(int identifier_ttype, HashMap<String, IdentifierType> symbol_table) {
    id_ttype_ = identifier_ttype;
    symbol_table_ = symbol_table;
  }

  @Override public void exitStreamStmt(PerfQueryParser.StreamStmtContext ctx) {
    PerfQueryParser.StreamContext stream = ctx.stream();

    PerfQueryParser.StreamQueryContext query = ctx.streamQuery();

    Operation operation = getOperation(query);
    for (int i = 0; i < operation.operands.size(); i++) {
      assert(symbol_table_.get(operation.operands.get(i)) == IdentifierType.STREAM);
    }
    dep_table_.put(stream.getText(), operation);
    last_assigned_id_ = stream.getText();
    sym_tree_.put(stream.getText(), (ParserRuleContext)query);
  }

  @Override public void exitProg(PerfQueryParser.ProgContext ctx) {
    System.out.println("dep_table_: " + dep_table_);
    System.out.println("expr_tree : " + build_expr_tree(last_assigned_id_));
    //System.err.println(build_expr_tree(last_assigned_id_).dot_output());
  }

  /// Get operands and operator that are required for the given query
  private Operation getOperation(PerfQueryParser.StreamQueryContext query) {
    assert(query.getChildCount() == 1);
    ParseTree op = query.getChild(0);
    if (op instanceof PerfQueryParser.FilterContext) {
      PerfQueryParser.FilterContext filter = (PerfQueryParser.FilterContext)op;
      return new Operation(OperationType.FILTER, Utility.getAllTokens(filter.stream(), id_ttype_));
    } else if (op instanceof PerfQueryParser.GroupbyContext) {
      PerfQueryParser.GroupbyContext groupby = (PerfQueryParser.GroupbyContext)op;
      return new Operation(OperationType.GROUPBY, Utility.getAllTokens(groupby.stream(), id_ttype_));
    } else if (op instanceof PerfQueryParser.MapContext) {
      PerfQueryParser.MapContext map = (PerfQueryParser.MapContext)op;
      return new Operation(OperationType.PROJECT, Utility.getAllTokens(map.stream(), id_ttype_));
    } else if (op instanceof PerfQueryParser.ZipContext) {
      PerfQueryParser.ZipContext zip = (PerfQueryParser.ZipContext)op;
      ArrayList<PerfQueryParser.StreamContext> stream_list = new ArrayList<>(zip.stream());
      ArrayList<String> ret = Utility.getAllTokens(stream_list.get(0), id_ttype_);
      ret.addAll(Utility.getAllTokens(stream_list.get(1), id_ttype_));
      return new Operation(OperationType.JOIN, ret);
    } else {
      assert(false);
      return new Operation();
    }
  }

  private ExprTree build_expr_tree(String id_name) {
   if (id_name.equals("T")) {
     return new ExprTree(OperationType.PKTLOG);
   } else {
     // Get operands using dep_table_
     Operation operation = dep_table_.get(id_name);
     if (operation == null) {
       throw new RuntimeException(id_name + " doesn't exist in dependency table. It was likely not defined before use.");
     }

     // Recursively build_expr_tree for each operand
     ArrayList<ExprTree> children = new ArrayList<ExprTree>();
     assert(operation.operands.size() >= 1);
     for (int i = 0; i < operation.operands.size(); i++) {
       children.add(build_expr_tree(operation.operands.get(i)));
     }
     return new ExprTree(operation.opcode, children);
   }
  }

  public HashMap<String, ParserRuleContext> getSymTree() {
    return sym_tree_;
  }

  public String getLastAssignedId() {
    return last_assigned_id_;
  }
}
