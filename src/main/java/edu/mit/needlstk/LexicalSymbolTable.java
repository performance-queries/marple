package edu.mit.needlstk;
import java.util.HashMap;
import java.util.List;

public class LexicalSymbolTable extends PerfQueryBaseVisitor<Boolean> {
  /// A member that always represents the symbol table for the current context
  private HashMap<String, AggFunVarType> outerSymTable;

  /// Global symbol table containing all variables in the code
  private HashMap<String, HashMap<String, AggFunVarType>> globalSymTable;

  /// A member that always represents the agg fun for the current context
  private String currAggFun;

  /// A map from aggregation functions to their list of state arguments
  private HashMap<String, List<String>> stateVars;

  /// A map from aggregation functions to their list of field arguments
  private HashMap<String, List<String>> fieldVars;

  /// Constructor
  public LexicalSymbolTable(HashMap<String, List<String>> stateVars,
                            HashMap<String, List<String>> fieldVars) {
    this.stateVars = stateVars;
    this.fieldVars = fieldVars;
    this.globalSymTable = new HashMap<String, HashMap<String, AggFunVarType>>();
  }

  /// Initialize symbol table for an aggregation function with state and field formal arguments
  private void initSymTable(String aggFun, HashMap<String, AggFunVarType> symTable) {
    /// Bootstrap the agg-fun-specific symbol table with formal parameters
    for (String state: stateVars.get(aggFun)) {
      symTable.put(state, AggFunVarType.STATE);
      globalSymTable.get(aggFun).put(state, AggFunVarType.STATE);
    }
    for (String field: fieldVars.get(aggFun)) {
      if (symTable.containsKey(field)) {
        throw new RuntimeException("Identifier " + field + " cannot appear both as accumulated " +
                                   "state and packet argument in function " + aggFun);
      }
      symTable.put(field, AggFunVarType.FIELD);
      globalSymTable.get(aggFun).put(field, AggFunVarType.FIELD);
    }
  }

  /// ANTLR visitor for `primitive.` Using the symbol table for the current lexical context,
  /// determine whether expressions use variables that are not yet defined.
  @Override public Boolean visitPrimitive(PerfQueryParser.PrimitiveContext ctx) {
    if (ctx.ID() != null) {
      AugExpr assignedExpr = new AugExpr(ctx.expr());
      for (String usedVar: assignedExpr.getUsedVars()) {
        if (! outerSymTable.containsKey(usedVar)) {
          throw new RuntimeException("Using variable " + usedVar + " in function " +
                                     currAggFun + " without prior definition\n" +
                                     ctx.getText());
        }
      }
      String definedVar = ctx.ID().getText();
      assert (definedVar != null);
      if (! outerSymTable.containsKey(definedVar)) {
        outerSymTable.put(definedVar, AggFunVarType.FN_VAR);
        globalSymTable.get(currAggFun).put(definedVar, AggFunVarType.FN_VAR);
      } else if (outerSymTable.get(definedVar) == AggFunVarType.FIELD) {
          throw new RuntimeException("Can't set a packet field" + definedVar
                                     + " directly in function " + currAggFun);
      } // Nothing more to be done if it's already defined to be a local variable.
    } // Nothing else to be done for emit and ; primitive statements
    return true;
  }

  /// ANTLR visitor for predicate. Use the symbol table to check if all variables used in the
  /// predicate have been defined earlier.
  /// Note: This has been defined not as the `predicate` visitor because of the sub-casing of ANTLR
  /// rules, which would require a separate visitor method for each production from `predicate`.
  public Boolean checkPred(PerfQueryParser.PredicateContext ctx) {
    AugPred pred = new AugPred(ctx);
    for (String usedVar: pred.getUsedVars()) {
      if (! outerSymTable.containsKey(usedVar)) {
        throw new RuntimeException("Using variable " + usedVar + " in function " +
                                   currAggFun + " without prior definition\n" +
                                   ctx.getText());
      }
    }
    return true;
  }

  @Override public Boolean visitIfConstruct(PerfQueryParser.IfConstructContext ctx) {
    // start a new symbol table for the new lexical context
    HashMap<String, AggFunVarType> oldOuterSymTable = new HashMap<>(outerSymTable);
    // check the predicate, if branch, and else branch.
    checkPred(ctx.predicate());
    visit(ctx.ifCodeBlock());
    if (ctx.elseCodeBlock() != null) {
      visit(ctx.elseCodeBlock());
    }
    outerSymTable = oldOuterSymTable;
    return true;
  }

  @Override public Boolean visitAggFun(PerfQueryParser.AggFunContext ctx) {
    // Initialize an outer symbol table using stateVars and fieldVars
    currAggFun = ctx.aggFunc().getText();
    outerSymTable = new HashMap<String, AggFunVarType>();
    globalSymTable.put(currAggFun, new HashMap<String, AggFunVarType>());
    initSymTable(currAggFun, outerSymTable);
    // Check define-before-use for every statement in the function
    visit(ctx.codeBlock());
    return true;
  }

  public HashMap<String, HashMap<String, AggFunVarType>> getGlobalSymTable() {
    return this.globalSymTable;
  }
}
