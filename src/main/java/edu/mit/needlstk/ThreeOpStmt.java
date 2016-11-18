package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

/// Template for three operand instructions
/// These are statements of the form:
/// result = pred ? expr_if : expr_else
public class ThreeOpStmt {
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
    this.result = result;
    this.predVar = predVar;
    this.exprIf = exprIf;
    this.exprElse = exprElse;
  }

  /// Constructor that defines a predicate condition
  public ThreeOpStmt(String result,
                     AugPred pred) {
    this.result = result;
    this.pred = pred;
  }

  /// TODO: printing ThreeOpStmt in various forms
}
