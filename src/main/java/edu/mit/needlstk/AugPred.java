package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

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
    PRED_TRUE,
    PRED_FALSE,
    PRED_EQ,
    PRED_NE,
    PRED_GT,
    PRED_LT,
    PRED_AND,
    PRED_OR,
    PRED_NOT
  };

  /// Type enum identifying the structure of the tree
  public AugPredType type;

  /// If this is a compound predicate (predAnd, etc.), this contains the child predicates. According
  /// to the grammar, it's enough to have at most two children, so this declaration allows for more
  /// general predicate trees than the grammar.
  public List<AugPred> childPreds;

  /// If this is a simple predicate (exprEq, etc.), this contains the child expressions.
  public List<AugExpr> childExprs;

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
      PerfQueryParser.PredAndContext newCtx = (PerfQueryParser.PredAndContext)ctx;
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

  /// Something like a copy constructor
  private void copy(AugPred copySrc) {
    this.type = copySrc.type;
    this.childPreds = copySrc.childPreds;
    this.childExprs = copySrc.childExprs;
  }

  public AugPred(boolean isTrue) {
    this.type = isTrue ? AugPredType.PRED_TRUE : AugPredType.PRED_FALSE;
  }

  /// Predicate combinators on existing AugPreds, resulting in new AugPreds.
  public AugPred and(AugPred other) {
    return new AugPred(AugPredType.PRED_AND, makePredChildren(this, other));
  }

  public AugPred or(AugPred other) {
    return new AugPred(AugPredType.PRED_OR, makePredChildren(this, other));
  }

  public AugPred not() {
    return new AugPred(AugPredType.PRED_NOT, new ArrayList<>(Arrays.asList(this)));
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

  /// Printing for inspection on console
  public String print() {
    if(type == AugPredType.PRED_TRUE) {
      return "true";
    } else if(type == AugPredType.PRED_FALSE) {
      return "false";
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
}
