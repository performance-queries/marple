package edu.mit.needlstk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DivisorChecker {
  /// Pre-generated threeOpCode object that is checked for division restrictions.
  private HashMap<String, ThreeOpCode> code;
  /// Current aggregation function being tested
  private String currAggFun;
  
  public DivisorChecker(HashMap<String, ThreeOpCode> aggFunCode) {
    this.code = aggFunCode;
  }

  public void checkDivisor() {
    for (Map.Entry<String, ThreeOpCode> entry: this.code.entrySet()) {
      this.currAggFun = entry.getKey();
      ThreeOpCode toc = entry.getValue();
      checkSingleFun(toc);
    }
  }

  public void checkSingleFun(ThreeOpCode toc) {
    for (ThreeOpStmt stmt: toc.stmts) {
      ArrayList<AugExpr> exprs = stmt.getAllUsedExprs();
      for (AugExpr expr: exprs) {
        checkExpr(expr);
      }
    }
  }

  /// Transform each expression from division by power of 2 to lshift.
  public void checkExpr(AugExpr expr) {
    expr.transformDivision();
  }
}
