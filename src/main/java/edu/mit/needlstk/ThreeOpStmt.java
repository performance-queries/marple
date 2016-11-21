package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

/// Template for three operand instructions
/// These are statements of the form:
/// result = pred ? expr_if : expr_else
public class ThreeOpStmt {
  private boolean isTernary;
  public String result;
  public String predVar;
  public AugPred pred;
  public AugExpr exprIf;
  public AugExpr exprElse;
  
  /// Constructor with a predefined predicate variable
  public ThreeOpStmt(String result,
                     String predVar,
                     AugExpr exprIf,
                     AugExpr exprElse) {
    this.isTernary = true;
    this.result = result;
    this.predVar = predVar;
    this.exprIf = exprIf;
    this.exprElse = exprElse;
  }

  /// Constructor that defines a predicate condition
  public ThreeOpStmt(String result,
                     AugPred pred) {
    this.isTernary = false;
    this.result = result;
    this.pred = pred;
  }

  /// Printing for visual inspection on console
  public String print() {
    String res;
    if(isTernary) {
      res = (result + " = " + predVar + " ? ("
             + exprIf.print() + ") : ("
             + exprElse.print() + ");");
    } else {
      res = result + " = " + pred.print() + ";";
    }
    return res;
  }
}
