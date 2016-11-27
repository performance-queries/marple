package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.HashSet;
import java.util.Arrays;

/// Template for three operand instructions
/// These are statements of the form:
/// result = pred ? expr_if : expr_else
public class ThreeOpStmt {
  private boolean isTernary;
  private String result;
  private String predVar;
  private AugPred pred;
  private AugExpr exprIf;
  private AugExpr exprElse;

  /// Constructor with a predefined predicate variable
  /// result = predVar ? exprIf : exprElse
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
  /// result = pred
  public ThreeOpStmt(String result,
                     AugPred pred) {
    this.isTernary = false;
    this.result = result;
    this.pred = pred;
  }

  /// Get list of identifiers used in this statement
  public HashSet<String> getUsedVars() {
    if(isTernary) {
      HashSet<String> usedVars = new HashSet<>(Arrays.asList(predVar));
      usedVars.addAll(exprIf.getUsedVars());
      usedVars.addAll(exprElse.getUsedVars());
      return usedVars;
    } else {
      return pred.getUsedVars();
    }
  }

  public String getDefinedVar() {
    return result;
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
