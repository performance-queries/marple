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
  }

  public boolean check() {
    Iterator it = code.keySet().iterator();
    while(it.hasNext()) {
      String aggFun = (String)it.next();
      assert(stateVars.containsKey(aggFun));
      assert(fieldVars.containsKey(aggFun));
      ThreeOpCode toc = code.get(aggFun);
      checkToc(aggFun, toc);
    }
    return true;
  }

  public boolean checkToc(String aggFun, ThreeOpCode toc) {
    /// Bootstrap the agg-fun-specific symbol table with formal parameters
    return true;
  }
  
  /// identify state from defined variables
  /// then build symbol table for use-before-define internally
  /// packet inputs might either be from:
  /// (1) language keywords (e.g., tin) --> check with static list
  /// (2) results of previous queries: can only detect with pipeline
  /// how is state initialized?
}
