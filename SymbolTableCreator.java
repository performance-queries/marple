import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.HashMap;
import java.lang.RuntimeException;
import java.util.ArrayList;

public class SymbolTableCreator extends perf_queryBaseListener {
  /// Hash table for storing identifiers with their type
  /// Maybe add other attributes as required
  private HashMap<String, IdentifierType> identifiers_ = new HashMap<String, IdentifierType>();

  /// Return symbol table once this compiler pass is over
  public HashMap<String, IdentifierType> symbol_table() { return identifiers_; }

  /// Listener for relation productions
  @Override public void exitRelation(perf_queryParser.RelationContext ctx) {
    add_to_symbol_table(ctx.getText(), IdentifierType.RELATION);
  }

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
    for (String token: all_tokens) {
      if (token.equals("emit()")) {
        // An emit makes all state accessible as columns
        for (String state_var: Utility.getAllTokens(ctx.state_list(), perf_queryParser.ID)) {
          add_to_symbol_table(state_var, IdentifierType.COLUMN);
        }
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
    if (type_changed) {
      IdentifierType old_type = identifiers_.get(id_name);
      // Make an exception for some type changes alone
      if ((old_type == IdentifierType.STATE) && (new_type == IdentifierType.COLUMN)) {
        // STATE can go to COLUMN because we implicitly type cast when we see an emit()
        new_type = IdentifierType.STATE_OR_COLUMN;
      } else if ((old_type == IdentifierType.STATE_OR_COLUMN) && (new_type == IdentifierType.COLUMN)) {
        // Something that became a STATE_OR_COLUMN can be further used as a COLUMN
        new_type = IdentifierType.STATE_OR_COLUMN;
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
