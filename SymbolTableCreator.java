import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.HashMap;
import java.lang.RuntimeException;
import java.util.ArrayList;
import java.util.stream.Stream;

public class SymbolTableCreator extends perf_queryBaseListener {
  /// Hash table for storing identifiers with their type
  /// Maybe add other attributes as required
  private HashMap<String, IdentifierType> identifiers_ = new HashMap<>();

  /// Return symbol table once this compiler pass is over
  public HashMap<String, IdentifierType> symbol_table() { return identifiers_; }

  /// Maintain mapping from name of aggregation function to
  /// the type of GROUPBY it is used for (streaming or relational)
  /// The heuristic is simple: anything with an emit is a streaming groupby
  private HashMap<String, GroupbyType> agg_fun_map_ = new HashMap<>();
  
  /// Listener for stream productions
  @Override public void exitStream(perf_queryParser.StreamContext ctx) {
    add_to_symbol_table(ctx.getText(), IdentifierType.STREAM);
  }

  /// Listern for state productions
  @Override public void exitState(perf_queryParser.StateContext ctx) {
    add_to_symbol_table(ctx.getText(), IdentifierType.STATE);
  }

  /// Listener for column productions
  @Override public void exitColumn(perf_queryParser.ColumnContext ctx) {
    add_to_symbol_table(ctx.getText(), IdentifierType.COLUMN);
  }

  /// Listener for "name of aggregation function" productions
  @Override public void exitAgg_func(perf_queryParser.Agg_funcContext ctx) {
    add_to_symbol_table(ctx.getText(), IdentifierType.AGG_FUNC);
  }

  /// Listener for "body of aggregation function" productions
  @Override public void exitAgg_fun(perf_queryParser.Agg_funContext ctx) {
    ArrayList<String> all_tokens = Utility.getAllTokens(ctx);
    assert(! all_tokens.isEmpty());
    Boolean has_emit = all_tokens.stream().anyMatch(token -> token.equals("emit()"));
    if (has_emit) {
      // An emit makes all state accessible as columns
      Utility.getAllTokens(ctx.state_list(), perf_queryParser.ID)
             .forEach(state -> add_to_symbol_table(state, IdentifierType.COLUMN));

      // It also makes this aggregation function a streaming group by
      agg_fun_map_.put(ctx.agg_func().getText(), GroupbyType.STREAMING);
    } else {
      agg_fun_map_.put(ctx.agg_func().getText(), GroupbyType.RELATIONAL);
    }
  }

  /// Listener for groupby operations/queries
  @Override public void exitStream_stmt(perf_queryParser.Stream_stmtContext ctx) {
    perf_queryParser.StreamContext stream = ctx.stream();

    perf_queryParser.Stream_queryContext query = ctx.stream_query();

    OperationType operation = Utility.getOperationType((perf_queryParser.Stream_queryContext)query);

    if (operation == OperationType.GROUPBY) {
      perf_queryParser.GroupbyContext groupby = (perf_queryParser.GroupbyContext) query.getChild(0);
      if (agg_fun_map_.get(groupby.agg_func().getText()) == GroupbyType.RELATIONAL) {
        add_to_symbol_table(stream.getText(), IdentifierType.RELATION);
      } else {
        add_to_symbol_table(stream.getText(), IdentifierType.STREAM);
      }
    }
  }

  /// Listener for the top-level, i.e., prog productions
  /// Prints out prepopulated symbol table
  @Override public void exitProg(perf_queryParser.ProgContext ctx) {
    System.out.println("identifiers_: " + identifiers_);
  }

  /// Add a new identifier name to the symbol table
  private void add_to_symbol_table(String id_name, IdentifierType new_type) {
    /// Check if the type changed?
    Boolean type_changed = check_for_type_change(id_name, new_type);

    /// Now, check old and new types
    /// TODO: The part below is the equivalent of our type inference algorithm.
    /// Although it's a poor apology for one. Maybe we should think about implementing
    /// type inference in a more principled manner below.
    /// The good thing is this rather unhygienic coding style is localized.
    if (type_changed) {
      IdentifierType old_type = identifiers_.get(id_name);
      // Make an exception for some type changes alone
      if ((old_type == IdentifierType.STATE) && (new_type == IdentifierType.COLUMN)) {
        // STATE can go to COLUMN because we implicitly type cast when we see an emit()
        new_type = IdentifierType.STATE_OR_COLUMN;
      } else if ((old_type == IdentifierType.STATE_OR_COLUMN) && (new_type == IdentifierType.COLUMN)) {
        // Something that became a STATE_OR_COLUMN can be further used as a COLUMN
        new_type = IdentifierType.STATE_OR_COLUMN;
      } else if ((old_type == IdentifierType.STREAM) && (new_type == IdentifierType.RELATION)) {
        // Stream to relation changes are allowed, as in RELATIONAL GROUPBYs
        new_type = IdentifierType.RELATION;
      } else {
        // Everything else is a type error including going from STATE_OR_COLUMN back to STATE.
        throw new RuntimeException("Trying to change type of " +
                                   id_name +
                                   " from " + old_type +
                                   " to " + new_type);
      }
    }

    /// Add it to the symbol table
    identifiers_.put(id_name, new_type);
  }

  /// Does id_name exist in symbol table? and is its old type different from new_type?
  private Boolean check_for_type_change(String id_name, IdentifierType new_type) {
    return ((identifiers_.get(id_name) != null) && (identifiers_.get(id_name) != new_type));
  }
}
