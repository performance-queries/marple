package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

public class DefineBeforeUse {
  private HashMap<String, HashMap<String, AggFunVarType>> symTable;
  private HashMap<String, ThreeOpCode> code;
  private HashMap<String, List<String>> stateVars;
  private HashMap<String, List<String>> fieldVars;

  public DefineBeforeUse(HashMap<String, ThreeOpCode> code,
                         HashMap<String, List<String>> stateVars,
                         HashMap<String, List<String>> fieldVars) {
    this.code = code;
    this.stateVars = stateVars;
    this.fieldVars = fieldVars;
    this.symTable = new HashMap<String, HashMap<String, AggFunVarType>>();
  }

  public boolean check() {
    Iterator it = code.keySet().iterator();
    while(it.hasNext()) {
      String aggFun = (String)it.next();
      assert(stateVars.containsKey(aggFun));
      assert(fieldVars.containsKey(aggFun));
      ThreeOpCode toc = code.get(aggFun);
      symTable.put(aggFun, new HashMap<String, AggFunVarType>());
      initSymTable(aggFun);
      checkToc(aggFun, toc);
    }
    return true;
  }

  private void initSymTable(String aggFun) {
    /// Bootstrap the agg-fun-specific symbol table with formal parameters
    for (String state: stateVars.get(aggFun)) {
      symTable.get(aggFun).put(state, AggFunVarType.STATE);
    }
    for (String field: fieldVars.get(aggFun)) {
      symTable.get(aggFun).put(field, AggFunVarType.FIELD);
    }
  }

  public boolean checkToc(String aggFun, ThreeOpCode toc) {
    /// Go through function body to check define before use
    for (ThreeOpStmt stmt: toc.stmts) {
      for(String usedVar: stmt.getUsedVars()) {
        /// Check if each is available in symbol table
        if(! symTable.get(aggFun).containsKey(usedVar)) {
          /// TODO: Error condition. Flag the error to user.
          throw new RuntimeException("Using variable " + usedVar + " in function " +
                                     aggFun + " without prior definition");
        }
      }
      String definedVar = stmt.getDefinedVar();
      /// Add to symbol table if not a field var already in the symbol table
      if(definedVar != null &&
         symTable.get(aggFun).containsKey(definedVar) &&
         symTable.get(aggFun).get(definedVar) == AggFunVarType.FIELD) {
        throw new RuntimeException("Can't set a packet field" + definedVar
                                   + " directly in function " + aggFun);
      } else if(definedVar != null &&
                ! symTable.get(aggFun).containsKey(definedVar)) {
        symTable.get(aggFun).put(definedVar, AggFunVarType.FN_VAR);
      }
    }
    return true;
  }
  
  /// identify state from defined variables
  /// then build symbol table for use-before-define internally
  /// packet inputs might either be from:
  /// (1) language keywords (e.g., tin) --> check with static list
  /// (2) results of previous queries: can only detect with pipeline
  /// how is state initialized?
}
