package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.HashSet;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

/// Template for three operand instructions
/// These are statements of the form:
/// result = pred ? expr_if : expr_else
public class ThreeOpStmt {
  private enum StmtType {
    TERNARY,
    PRED_ASSIGN,
    EXPR_ASSIGN,
    EMIT
  };
  private StmtType type;
  private String result;
  private String predVar;
  private AugPred pred;
  private AugExpr expr;
  private AugExpr exprIf;
  private AugExpr exprElse;

  /// Constructor with a predefined predicate variable
  /// result = predVar ? exprIf : exprElse
  public ThreeOpStmt(String result,
                     String predVar,
                     AugExpr exprIf,
                     AugExpr exprElse) {
    this.type = StmtType.TERNARY;
    this.result = result;
    this.predVar = predVar;
    this.exprIf = exprIf;
    this.exprElse = exprElse;
  }

  /// Constructor that defines a predicate condition
  /// result = pred
  public ThreeOpStmt(String result,
                     AugPred pred) {
    this.type = StmtType.PRED_ASSIGN;
    this.result = result;
    this.pred = pred;
  }

  /// Constructor that defines an expression
  /// result = expr
  public ThreeOpStmt(String result,
                     AugExpr expr) {
    this.type = StmtType.EXPR_ASSIGN;
    this.result = result;
    this.expr = expr;
  }

  /// Constructor that emits internal state when a predicate is true.
  public ThreeOpStmt(String predVar) {
    this.type = StmtType.EMIT;
    this.predVar = predVar;
  }

  /// Get list of identifiers used in this statement
  public HashSet<String> getUsedVars() {
    if(type == StmtType.TERNARY) {
      HashSet<String> usedVars = new HashSet<>(Arrays.asList(predVar));
      usedVars.addAll(exprIf.getUsedVars());
      usedVars.addAll(exprElse.getUsedVars());
      return usedVars;
    } else if(type == StmtType.PRED_ASSIGN) {
      return pred.getUsedVars();
    } else if(type == StmtType.EXPR_ASSIGN) {
      return expr.getUsedVars();
    } else if(type == StmtType.EMIT) {
      return new HashSet<>();
    } else {
      assert(false); // Expecting a new statement type?
      return null;
    }
  }

  public String getDefinedVar() {
    if (type == StmtType.TERNARY || type == StmtType.PRED_ASSIGN || type == StmtType.EXPR_ASSIGN) {
      return result;
    } else if (type == StmtType.EMIT) {
      return null;
    } else {
      assert(false); // Expecting a new statement type?
      return null;
    }
  }

  /// Return expressions used to assign to the defined variables.
  public ArrayList<AugExpr> getUsedExprs() {
    if (type == StmtType.TERNARY) {
      return new ArrayList<AugExpr>(Arrays.asList(exprIf, exprElse));
    } else if (type == StmtType.EXPR_ASSIGN) {
      return new ArrayList<AugExpr>(Arrays.asList(expr));
    } else {
      assert (type == StmtType.PRED_ASSIGN || type == StmtType.EMIT);
      return new ArrayList<AugExpr>();
    }
  }

  /// Return expressions used to define either a predicate or an assigned variable.
  public ArrayList<AugExpr> getAllUsedExprs() {
    if (type == StmtType.TERNARY || type == StmtType.EXPR_ASSIGN) {
      return getUsedExprs();
    } else if (type == StmtType.PRED_ASSIGN) {
      return this.pred.getUsedExprs();
    } else {
      assert (type == StmtType.EMIT);
      return new ArrayList<AugExpr>();
    }
  }

  /// If the statement is ternary, return name of the enclosing predicate identifier.
  public String getPredVarOfTernary() {
    assert (type == StmtType.TERNARY);
    return predVar;
  }

  /// Printing for visual inspection on console
  public String print() {
    String res;
    if(type == StmtType.TERNARY) {
      res = (result + " = " + predVar + " ? ("
             + exprIf.print() + ") : ("
             + exprElse.print() + ");\n");
    } else if(type == StmtType.PRED_ASSIGN) {
      res = result + " = " + pred.print() + ";\n";
    } else if(type == StmtType.EXPR_ASSIGN) {
      res = result + " = " + expr.print() + ";\n";
    } else if(type == StmtType.EMIT) {
      res = "if (" + predVar + ") emit;\n";
    } else {
      assert(false); // Logic error. Expecting a new statement type?
      res = "";
    }
    return res;
  }

  /// Printing in P4 in the context of a symbol table
  public String getP4(HashMap<String, AggFunVarType> symTab) {
    String res = "";
    if(type == StmtType.TERNARY) {
      /// Symbol table should contain relevant identifiers
      assert (symTab.containsKey(result));
      assert (symTab.containsKey(predVar));
      /// Produce p4 line of code
      res = (P4Printer.p4Ident(result, symTab.get(result)) + " = " +
             P4Printer.p4Ident(predVar, symTab.get(predVar)) + " ? ("
             + exprIf.getP4(symTab) + ") : ("
             + exprElse.getP4(symTab) + ");\n");
    } else if(type == StmtType.PRED_ASSIGN) {
      assert (symTab.containsKey(result));
      res = (P4Printer.p4Ident(result, symTab.get(result)) + " = " +
             pred.getP4(symTab) + ";\n");
    } else if(type == StmtType.EXPR_ASSIGN) {
      assert (symTab.containsKey(result));
      res = (P4Printer.p4Ident(result, symTab.get(result)) + " = " +
             expr.getP4(symTab) + ";\n");
    } else if(type == StmtType.EMIT) {
      /// For each state variable in this context,
      /// copy the state to packet field of the same name.
      for (Map.Entry<String, AggFunVarType> entry: symTab.entrySet()) {
        if (entry.getValue() == AggFunVarType.STATE) {
          String ident = entry.getKey();
          res += (P4Printer.p4Ident(ident, AggFunVarType.FIELD) + " = " +
                  P4Printer.p4Ident(predVar, symTab.get(predVar)) + " ? (" +
                  P4Printer.p4Ident(ident, AggFunVarType.STATE) + ") : (" +
                  P4Printer.p4Ident(ident, AggFunVarType.FIELD) + ");\n");
        }
      }
    } else {
      assert(false); // Logic error. Expecting a new statement type?
      res = "";
    }
    return res;
  }

  public String getDomino(HashMap<String, AggFunVarType> symTab) {
    String res = "";
    if (type == StmtType.TERNARY) {
      /// Symbol table should contain relevant identifiers
      assert (symTab.containsKey(result));
      assert (symTab.containsKey(predVar));
      /// Produce p4 line of code
      res = "  " + (DominoPrinter.dominoIdent(result, symTab.get(result)) + " = " +
             DominoPrinter.dominoIdent(predVar, symTab.get(predVar)) + " ? ("
             + exprIf.getDomino(symTab) + ") : ("
             + exprElse.getDomino(symTab) + ");\n");
    } else if(type == StmtType.PRED_ASSIGN) {
      assert (symTab.containsKey(result));
      res = "  " + (DominoPrinter.dominoIdent(result, symTab.get(result)) + " = " +
             pred.getDomino(symTab) + ";\n");
    } else if(type == StmtType.EXPR_ASSIGN) {
      assert (symTab.containsKey(result));
      res = "  " + (DominoPrinter.dominoIdent(result, symTab.get(result)) + " = " +
             expr.getDomino(symTab) + ";\n");
    } else if(type == StmtType.EMIT) {
      /// For each state variable in this context,
      /// copy the state to packet field of the same name.
      for (Map.Entry<String, AggFunVarType> entry: symTab.entrySet()) {
        if (entry.getValue() == AggFunVarType.STATE) {
          String ident = entry.getKey();
          res += ("  " + DominoPrinter.dominoIdent(ident, AggFunVarType.FIELD) + " = " +
                  DominoPrinter.dominoIdent(predVar, symTab.get(predVar)) + " ? (" +
                  DominoPrinter.dominoIdent(ident, AggFunVarType.STATE) + ") : (" +
                  DominoPrinter.dominoIdent(ident, AggFunVarType.FIELD) + ");\n");
        }
      }
    } else {
      assert(false); // Logic error. Expecting a new statement type?
      res = "";
    }
    return res;
  }

  @Override public String toString() {
    return print();
  }

  public boolean isEmit() {
    return (type == StmtType.EMIT);
  }

  public boolean isPredAssign() {
    return (type == StmtType.PRED_ASSIGN);
  }

  public boolean isTernary() {
    return (type == StmtType.TERNARY);
  }

  public boolean isExprAssign() {
    return (type == StmtType.EXPR_ASSIGN);
  }

  public String getEmitPred() {
    assert (type == StmtType.EMIT);
    return predVar;
  }
}
