package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;

/// A class for augmentable predicates, which predicates are mostly constructed from predicates
/// within aggregation function bodies. The class structure mirrors the structure of predicates in
/// the language grammar.

/// This code is unfortunate boilerplate resulting from an implicit design choice of ANTLR in
/// considering all ASTs as parse trees, i.e., ones generated from parsing. It appears they don't
/// consider usecases where ASTs are constructed and augmented in the code itself. There are other
/// custom mechanisms, like context-independent rewriting (using the => within the .g4 grammar
/// file), but this seems to tie the grammar to specific manipulations in the compiler application.
public class AugPred {
  /// Enum that defines types of predicates available
  public enum AugPredType {
    PRED_TRUE,  // True
    PRED_FALSE, // False
    PRED_ID,     // `identifier`, which is a pre-assigned predicate.
    PRED_EQ,    // expr == expr
    PRED_NE,    // expr != expr
    PRED_GT,    // expr >  expr
    PRED_LT,    // expr <  expr
    PRED_AND,   // pred && pred
    PRED_OR,    // pred || pred
    PRED_NOT   // ! pred
  };

  /// Type enum identifying the structure of the tree
  public AugPredType type;

  /// If this is a compound predicate (predAnd, etc.), this contains the child predicates. According
  /// to the grammar, it's enough to have at most two children, so this declaration allows for more
  /// general predicate trees than the grammar.
  public List<AugPred> childPreds;

  /// If this is a simple predicate (exprEq, etc.), this contains the child expressions.
  public List<AugExpr> childExprs;

  /// If this is an "identifier" predicate, store the identifier.
  public String predId;

  /// Default constructor with an input PredicateContext ctx
  public AugPred(PerfQueryParser.PredicateContext ctx) {
    if(ctx instanceof PerfQueryParser.TruePredContext) {
      this.type = AugPredType.PRED_TRUE;
    } else if(ctx instanceof PerfQueryParser.FalsePredContext) {
      this.type = AugPredType.PRED_FALSE;
    } else if(ctx instanceof PerfQueryParser.ExprEqContext) {
      this.type = AugPredType.PRED_EQ;
      PerfQueryParser.ExprEqContext newCtx = (PerfQueryParser.ExprEqContext)ctx;
      this.childExprs = makeExprChildren(new AugExpr(newCtx.expr(0)), new AugExpr(newCtx.expr(1)));
    } else if(ctx instanceof PerfQueryParser.ExprGtContext) {
      this.type = AugPredType.PRED_GT;
      PerfQueryParser.ExprGtContext newCtx = (PerfQueryParser.ExprGtContext)ctx;
      this.childExprs = makeExprChildren(new AugExpr(newCtx.expr(0)), new AugExpr(newCtx.expr(1)));
    } else if(ctx instanceof PerfQueryParser.ExprLtContext) {
      this.type = AugPredType.PRED_LT;
      PerfQueryParser.ExprLtContext newCtx = (PerfQueryParser.ExprLtContext)ctx;
      this.childExprs = makeExprChildren(new AugExpr(newCtx.expr(0)), new AugExpr(newCtx.expr(1)));
    } else if(ctx instanceof PerfQueryParser.ExprNeContext) {
      this.type = AugPredType.PRED_NE;
      PerfQueryParser.ExprNeContext newCtx = (PerfQueryParser.ExprNeContext)ctx;
      this.childExprs = makeExprChildren(new AugExpr(newCtx.expr(0)), new AugExpr(newCtx.expr(1)));
    } else if(ctx instanceof PerfQueryParser.PredAndContext) {
      this.type = AugPredType.PRED_AND;
      PerfQueryParser.PredAndContext newCtx = (PerfQueryParser.PredAndContext)ctx;
      this.childPreds = makePredChildren(new AugPred(newCtx.predicate(0)), new AugPred(newCtx.predicate(1)));
    } else if(ctx instanceof PerfQueryParser.PredOrContext) {
      this.type = AugPredType.PRED_OR;
      PerfQueryParser.PredOrContext newCtx = (PerfQueryParser.PredOrContext)ctx;
      this.childPreds = makePredChildren(new AugPred(newCtx.predicate(0)), new AugPred(newCtx.predicate(1)));
    } else if(ctx instanceof PerfQueryParser.PredNotContext) {
      this.type = AugPredType.PRED_NOT;
      PerfQueryParser.PredNotContext newCtx = (PerfQueryParser.PredNotContext)ctx;
      this.childPreds = new ArrayList<>(Arrays.asList(new AugPred(newCtx.predicate())));
    } else if(ctx instanceof PerfQueryParser.PredParenContext) {
      /// TODO: Ideally, this could use something like a factory method. But using something like a
      /// copy constructor for now.
      PerfQueryParser.PredParenContext newCtx = (PerfQueryParser.PredParenContext)ctx;
      copy(new AugPred(newCtx.predicate()));
    } else {
      assert(false); // Logic error. Expecting a different kind of predicate?
    }
  }

  /// Constructors to get AugPreds from existing AugPreds, enabling trees of AugPreds.
  public <T> AugPred(AugPredType type, List<T> children) {
    if(type == AugPredType.PRED_AND || type == AugPredType.PRED_OR
       || type == AugPredType.PRED_NOT) {
      this.childPreds = new ArrayList<AugPred>();
      for (T child: children) {
        this.childPreds.add((AugPred)child);
      }
    } else if(type == AugPredType.PRED_EQ || type == AugPredType.PRED_NE
              || type == AugPredType.PRED_GT || type == AugPredType.PRED_LT) {
      this.childExprs = new ArrayList<AugExpr>();
      for (T child: children) {
        this.childExprs.add((AugExpr)child);
      }
    } else {
      assert(false); // Logic error. Not expecting other types here.
    }
    this.type = type;
  }

  /// Constructor to get AugPred from an identifier
  public AugPred(String preAssignedId) {
    this.type = AugPredType.PRED_ID;
    this.predId = preAssignedId;
  }

  /// Something like a copy constructor
  private void copy(AugPred copySrc) {
    this.type = copySrc.type;
    this.childPreds = copySrc.childPreds;
    this.childExprs = copySrc.childExprs;
    this.predId = copySrc.predId;
  }

  public AugPred(boolean isTrue) {
    this.type = isTrue ? AugPredType.PRED_TRUE : AugPredType.PRED_FALSE;
  }

  /// Structural checks to simplify predicate construction
  public boolean isIdenticallyTrue() {
    return this.type == AugPredType.PRED_TRUE;
  }

  public boolean isIdenticallyFalse() {
    return this.type == AugPredType.PRED_FALSE;
  }

  /// Predicate combinators on existing AugPreds, resulting in new AugPreds.
  public AugPred and(AugPred other) {
    if (this.isIdenticallyTrue()) {
      return other;
    } else if (other.isIdenticallyTrue()) {
      return this;
    } else if (this.isIdenticallyFalse() || other.isIdenticallyFalse()) {
      return new AugPred(false);
    } else {
      return new AugPred(AugPredType.PRED_AND, makePredChildren(this, other));
    }
  }

  public AugPred or(AugPred other) {
    if (this.isIdenticallyFalse()) {
      return other;
    } else if (other.isIdenticallyFalse()) {
      return this;
    } else if (this.isIdenticallyTrue() || other.isIdenticallyTrue()) {
      return new AugPred(true);
    } else {
      return new AugPred(AugPredType.PRED_OR, makePredChildren(this, other));
    }
  }

  public AugPred not() {
    if (this.isIdenticallyTrue()) {
      return new AugPred(false);
    } else if (this.isIdenticallyFalse()) {
      return new AugPred(true);
    } else {
      return new AugPred(AugPredType.PRED_NOT, new ArrayList<>(Arrays.asList(this)));
    }
  }

  /// Helper for constructing lists of child predicates from two inputs
  private List<AugPred> makePredChildren(AugPred childLeft, AugPred childRight) {
    List<AugPred> childList = new ArrayList<AugPred>();
    childList.add(childLeft);
    childList.add(childRight);
    return childList;
  }

  /// Helper for constructing lists of child expressions from two inputs
  private List<AugExpr> makeExprChildren(AugExpr childLeft, AugExpr childRight) {
    List<AugExpr> childList = new ArrayList<AugExpr>();
    childList.add(childLeft);
    childList.add(childRight);
    return childList;
  }

  /// Helper to extract expression for expression child i
  private String getExprStr(int i) {
    return childExprs.get(i).print();
  }

  /// Helper to extract expression for predicate child i
  private String getPredStr(int i) {
    return childPreds.get(i).print();
  }

  public HashSet<String> getUsedVars() {
    HashSet<String> usedVars;
    switch(type) {
      case PRED_TRUE:
      case PRED_FALSE:
        return new HashSet<>();
      case PRED_ID:
        return new HashSet<>(Arrays.asList(predId));
      case PRED_EQ:
      case PRED_NE:
      case PRED_GT:
      case PRED_LT:
        usedVars = childExprs.get(0).getUsedVars();
        usedVars.addAll(childExprs.get(1).getUsedVars());
        return usedVars;
      case PRED_AND:
      case PRED_OR:
        usedVars = childPreds.get(0).getUsedVars();
        usedVars.addAll(childPreds.get(1).getUsedVars());
        return usedVars;
      case PRED_NOT:
        return childPreds.get(0).getUsedVars();
      default:
        assert(false); // Logic error. Expecting a new predicate type?
        return null;
    }
  }

  /// Printing for inspection on console
  public String print() {
    if(type == AugPredType.PRED_TRUE) {
      return "true";
    } else if(type == AugPredType.PRED_FALSE) {
      return "false";
    } else if(type == AugPredType.PRED_ID) {
      return predId;      
    } else if(type == AugPredType.PRED_EQ) {
      return "(" + getExprStr(0) + ") == (" + getExprStr(1) + ")";
    } else if(type == AugPredType.PRED_NE) {
      return "(" + getExprStr(0) + ") != (" + getExprStr(1) + ")";
    } else if(type == AugPredType.PRED_GT) {
      return "(" + getExprStr(0) + ") > (" + getExprStr(1) + ")";
    } else if(type == AugPredType.PRED_LT) {
      return "(" + getExprStr(0) + ") < (" + getExprStr(1) + ")";
    } else if(type == AugPredType.PRED_AND) {
      return "(" + getPredStr(0) + ") && (" + getPredStr(1) + ")";
    } else if(type == AugPredType.PRED_OR) {
      return "(" + getPredStr(0) + ") || (" + getPredStr(1) + ")";
    } else if(type == AugPredType.PRED_NOT) {
      return "! (" + getPredStr(0) + ")";
    } else {
      assert (false); // Logic error. Must be one of predetermined pred types
      return null;
    }
  }

  @Override public String toString() {
    return this.print();
  }

  /// Helper to extract P4 output for expression child i
  private String getExprP4(int i, HashMap<String, AggFunVarType> symTab) {
    return childExprs.get(i).getP4(symTab);
  }

  private String getExprDomino(int i, HashMap<String, AggFunVarType> symTab) {
    return childExprs.get(i).getDomino(symTab);
  }

  /// Helper to extract P4 output from predicate child i
  private String getPredP4(int i, HashMap<String, AggFunVarType> symTab) {
    return childPreds.get(i).getP4(symTab);
  }

  private String getPredDomino(int i, HashMap<String, AggFunVarType> symTab) {
    return childPreds.get(i).getDomino(symTab);
  }

  /// Print P4 code
  public String getP4(HashMap<String, AggFunVarType> symTab) {
    if(type == AugPredType.PRED_TRUE) {
      return P4Printer.P4_TRUE;
    } else if(type == AugPredType.PRED_FALSE) {
      return P4Printer.P4_FALSE;
    } else if(type == AugPredType.PRED_ID) {
      if (! symTab.containsKey(predId)) {
        System.out.println("Missing symbol table entry for " + predId);
      }
      assert (symTab.containsKey(predId)); // ensure id exists in symbol table!
      return P4Printer.p4Ident(predId, symTab.get(predId));
    } else if(type == AugPredType.PRED_EQ) {
      return "(" + getExprP4(0, symTab) + ") == (" + getExprP4(1, symTab) + ")";
    } else if(type == AugPredType.PRED_NE) {
      return "(" + getExprP4(0, symTab) + ") != (" + getExprP4(1, symTab) + ")";
    } else if(type == AugPredType.PRED_GT) {
      return "(" + getExprP4(0, symTab) + ") > (" + getExprP4(1, symTab) + ")";
    } else if(type == AugPredType.PRED_LT) {
      return "(" + getExprP4(0, symTab) + ") < (" + getExprP4(1, symTab) + ")";
    } else if(type == AugPredType.PRED_AND) {
      return "(" + getPredP4(0, symTab) + ") && (" + getPredP4(1, symTab) + ")";
    } else if(type == AugPredType.PRED_OR) {
      return "(" + getPredP4(0, symTab) + ") || (" + getPredP4(1, symTab) + ")";
    } else if(type == AugPredType.PRED_NOT) {
      return "! (" + getPredP4(0, symTab) + ")";
    } else {
      assert (false);
      return null;
    }
  }

  /// Print domino code
  public String getDomino(HashMap<String, AggFunVarType> symTab) {
    if(type == AugPredType.PRED_TRUE) {
      return DominoPrinter.DOMINO_TRUE;
    } else if(type == AugPredType.PRED_FALSE) {
      return DominoPrinter.DOMINO_FALSE;
    } else if(type == AugPredType.PRED_ID) {
      if (! symTab.containsKey(predId)) {
        System.out.println("Missing symbol table entry for " + predId);
      }
      assert (symTab.containsKey(predId)); // ensure id exists in symbol table!
      return DominoPrinter.dominoIdent(predId, symTab.get(predId));
    } else if(type == AugPredType.PRED_EQ) {
      return "(" + getExprDomino(0, symTab) + ") == (" + getExprDomino(1, symTab) + ")";
    } else if(type == AugPredType.PRED_NE) {
      return "(" + getExprDomino(0, symTab) + ") != (" + getExprDomino(1, symTab) + ")";
    } else if(type == AugPredType.PRED_GT) {
      return "(" + getExprDomino(0, symTab) + ") > (" + getExprDomino(1, symTab) + ")";
    } else if(type == AugPredType.PRED_LT) {
      return "(" + getExprDomino(0, symTab) + ") < (" + getExprDomino(1, symTab) + ")";
    } else if(type == AugPredType.PRED_AND) {
      return "(" + getPredDomino(0, symTab) + ") && (" + getPredDomino(1, symTab) + ")";
    } else if(type == AugPredType.PRED_OR) {
      return "(" + getPredDomino(0, symTab) + ") || (" + getPredDomino(1, symTab) + ")";
    } else if(type == AugPredType.PRED_NOT) {
      return "! (" + getPredDomino(0, symTab) + ")";
    } else {
      assert (false);
      return null;
    }
  }

  /// Get all internally used expressions.
  public ArrayList<AugExpr> getUsedExprs() {
    ArrayList<AugExpr> exprs = new ArrayList<>();
    if (type == AugPredType.PRED_TRUE || type == AugPredType.PRED_FALSE || type == AugPredType.PRED_ID) {
      return exprs;
    } else if (type == AugPredType.PRED_EQ || type == AugPredType.PRED_NE ||
               type == AugPredType.PRED_GT || type == AugPredType.PRED_LT) {
      return new ArrayList<AugExpr>(this.childExprs);
    } else if (type == AugPredType.PRED_AND || type == AugPredType.PRED_OR) {
      exprs.addAll(this.childPreds.get(0).getUsedExprs());
      exprs.addAll(this.childPreds.get(1).getUsedExprs());
      return exprs;
    } else if (type == AugPredType.PRED_NOT) {
      return this.childPreds.get(0).getUsedExprs();
    } else {
      assert (false); // Expecting one of the above predicate types.
      return null;
    }
  }
}
