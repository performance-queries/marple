package edu.mit.needlstk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DivisorChecker {
  /// Pre-generated threeOpCode object that is checked for division restrictions.
  private ArrayList<PipeStage> pipes;
  
  public DivisorChecker(ArrayList<PipeStage> pipes) {
    this.pipes = pipes;
  }

  public void checkDivisor() {
    for (PipeStage pipe: this.pipes) {
      checkSingleFun(pipe.getConfigInfo().code);
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
