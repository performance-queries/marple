import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.HashMap;
import java.lang.RuntimeException;

public class SymbolTableCreator extends perf_queryBaseListener {
  /// Hash table for storing identifiers with their type
  /// Maybe add other attributes as required
  private HashMap<String, IdentifierType> identifiers_ = new HashMap<String, IdentifierType>();

  /// Return symbol table once this compiler pass is over
  public HashMap<String, IdentifierType> symbol_table() { return identifiers_; }

  /// Listener for relation productions
  @Override public void exitRelation(perf_queryParser.RelationContext ctx) {
    add_to_symbol_table(ctx, IdentifierType.RELATION);
  }

  /// Listener for stream productions
  @Override public void exitStream(perf_queryParser.StreamContext ctx) {
    add_to_symbol_table(ctx, IdentifierType.STREAM);
  }

  /// Listener for column productions
  @Override public void exitColumn(perf_queryParser.ColumnContext ctx) {
    add_to_symbol_table(ctx, IdentifierType.COLUMN);
  }

  /// Listener for aggregate function productions
  @Override public void exitAgg_func(perf_queryParser.Agg_funcContext ctx) {
    add_to_symbol_table(ctx, IdentifierType.AGG_FUNC);
  }

  /// Listener for the top-level, i.e., prog productions
  /// Prints out prepopulated symbol table
  @Override public void exitProg(perf_queryParser.ProgContext ctx) {
    System.out.println("identifiers_: " + identifiers_);
  }

  /// Add a new identifier to the symbol table
  private void add_to_symbol_table(ParserRuleContext ctx, IdentifierType id_type) {
    /// Make sure we are processing one of the right contexts
    assert(ctx instanceof perf_queryParser.RelationContext ||
           ctx instanceof perf_queryParser.StreamContext   ||
           ctx instanceof perf_queryParser.ColumnContext   ||
           ctx instanceof perf_queryParser.Agg_funcContext);
    assert(ctx.getChildCount() == 1);

    /// Get identifier's name. These productions have a fan-out of 1,
    /// so the identifier is right under the production
    String id_name = ctx.getStart().getText();

    /// Check that the identifier isn't already in the symbol table with a different type
    check_for_type_change(id_name, id_type);

    /// Add it to the symbol table
    identifiers_.put(id_name, id_type);
  }

  private void check_for_type_change(String id_name, IdentifierType new_type) {
    /// Does id_name exist in symbol table
    if (identifiers_.get(id_name) != null) {
      /// If so, is id_name's type different from new_type?
      if (identifiers_.get(id_name) != new_type) {
        throw new RuntimeException("Trying to change type of " + id_name + " from " +
                               identifiers_.get(id_name) +
                               " to " + new_type);
      }
    }
  }
}
