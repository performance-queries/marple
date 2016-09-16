package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.HashMap;
import java.lang.RuntimeException;
import java.util.ArrayList;
import java.util.stream.Stream;

public class SymbolTableCreator extends PerfQueryBaseListener {
  /// Hash table for storing identifiers with their type
  /// Maybe add other attributes as required
  private HashMap<String, IdentifierType> identifiers = new HashMap<>();

  /// Return symbol table once this compiler pass is over
  public HashMap<String, IdentifierType> symbolTable() { return identifiers; }

  /// Maintain mapping from name of aggregation function to
  /// the type of GROUPBY it is used for (streaming or relational)
  /// The heuristic is simple: anything with an emit is a streaming groupby
  private HashMap<String, GroupbyType> aggFunMap = new HashMap<>();
  /// Maintain mapping from name of aggregation function to whether it is
  /// annotated to be associative. This is part of the function definition.
  private HashMap<String, Boolean> aggFunAssocMap  = new HashMap<>();
  public HashMap<String, Boolean> getAggFunAssocMap() { return aggFunAssocMap; }
  
  /// Listener for stream productions
  @Override public void exitStream(PerfQueryParser.StreamContext ctx) {
    addToSymbolTable(ctx.getText(), IdentifierType.STREAM);
  }

  /// Listern for state productions
  @Override public void exitState(PerfQueryParser.StateContext ctx) {
    addToSymbolTable(ctx.getText(), IdentifierType.STATE);
  }

  /// Listener for column productions
  @Override public void exitColumn(PerfQueryParser.ColumnContext ctx) {
    addToSymbolTable(ctx.getText(), IdentifierType.COLUMN);
  }

  /// Listener for "name of aggregation function" productions
  @Override public void exitAggFunc(PerfQueryParser.AggFuncContext ctx) {
    addToSymbolTable(ctx.getText(), IdentifierType.AGG_FUNC);
  }

  /// Listener for "body of aggregation function" productions
  @Override public void exitAggFun(PerfQueryParser.AggFunContext ctx) {
    ArrayList<String> allTokens = Utility.getAllTokens(ctx);
    assert(! allTokens.isEmpty());
    Boolean hasEmit = allTokens.stream().anyMatch(token -> token.equals("emit()"));
    if (hasEmit) {
      // An emit makes all state accessible as columns
      Utility.getAllTokens(ctx.stateList(), PerfQueryParser.ID)
             .forEach(state -> addToSymbolTable(state, IdentifierType.COLUMN));

      // It also makes this aggregation function a streaming group by
      aggFunMap.put(ctx.aggFunc().getText(), GroupbyType.STREAMING);
    } else {
      aggFunMap.put(ctx.aggFunc().getText(), GroupbyType.RELATIONAL);
    }
    // Also recognize if the function is annotated associative
    aggFunAssocMap.put(ctx.aggFunc().getText(), ctx.ASSOC() != null);
  }

  /// Listener for groupby operations/queries
  @Override public void exitStreamStmt(PerfQueryParser.StreamStmtContext ctx) {
    PerfQueryParser.StreamContext stream = ctx.stream();

    PerfQueryParser.StreamQueryContext query = ctx.streamQuery();

    OperationType operation = Utility.getOperationType(query);

    if (operation == OperationType.GROUPBY) {
      PerfQueryParser.GroupbyContext groupby = (PerfQueryParser.GroupbyContext) query.getChild(0);
      if (aggFunMap.get(groupby.aggFunc().getText()) == GroupbyType.RELATIONAL) {
        addToSymbolTable(stream.getText(), IdentifierType.RELATION);
      } else {
        addToSymbolTable(stream.getText(), IdentifierType.STREAM);
      }
    }
  }

  /// Listener for the top-level, i.e., prog productions
  /// Prints out prepopulated symbol table
  @Override public void exitProg(PerfQueryParser.ProgContext ctx) {
    System.out.println("identifiers: " + identifiers);
  }

  /// Add a new identifier name to the symbol table
  private void addToSymbolTable(String idName, IdentifierType newType) {
    /// Check if the type changed?
    Boolean typeChanged = checkForTypeChange(idName, newType);

    /// Now, check old and new types
    /// TODO: The part below is the equivalent of our type inference algorithm.
    /// Although it's a poor apology for one. Maybe we should think about implementing
    /// type inference in a more principled manner below.
    /// The good thing is this rather unhygienic coding style is localized.
    if (typeChanged) {
      IdentifierType oldType = identifiers.get(idName);
      // Make an exception for some type changes alone
      if ((oldType == IdentifierType.STATE) && (newType == IdentifierType.COLUMN)) {
        // STATE can go to COLUMN because we implicitly type cast when we see an emit()
        newType = IdentifierType.STATE_OR_COLUMN;
      } else if ((oldType == IdentifierType.STATE_OR_COLUMN) && (newType == IdentifierType.COLUMN)) {
        // Something that became a STATE_OR_COLUMN can be further used as a COLUMN
        newType = IdentifierType.STATE_OR_COLUMN;
      } else if ((oldType == IdentifierType.STREAM) && (newType == IdentifierType.RELATION)) {
        // Stream to relation changes are allowed, as in RELATIONAL GROUPBYs
        newType = IdentifierType.RELATION;
      } else {
        // Everything else is a type error including going from STATE_OR_COLUMN back to STATE.
        throw new RuntimeException("Trying to change type of " +
                                   idName +
                                   " from " + oldType +
                                   " to " + newType);
      }
    }

    /// Add it to the symbol table
    identifiers.put(idName, newType);
  }

  /// Does idName exist in symbol table? and is its old type different from newType?
  private Boolean checkForTypeChange(String idName, IdentifierType newType) {
    return ((identifiers.get(idName) != null) && (identifiers.get(idName) != newType));
  }
}
