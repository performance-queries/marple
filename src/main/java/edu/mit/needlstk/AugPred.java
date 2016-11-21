package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;

/// A class for augmentable predicates, which predicates are mostly constructed from predicates
/// within aggregation function bodies. The class structure mirrors the structure of predicates in
/// the language grammar.

/// This code is unfortunate boilerplate resulting from an implicit design choice of ANTLR in
/// considering all ASTs as parse trees, i.e., ones generated from parsing. It appears they don't
/// consider usecases where ASTs are constructed and augmented in the code itself. There are other
/// custom mechanisms, like context-independent rewriting (using the => within the .g4 grammar
/// file), but this seems to tie the grammar to specific manipulations in the compiler application.
public class AugPred extends PerfQueryBaseVisitor<> {
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
  }

  /// Type enum denoting the structure of the tree
  public AugPredType type;

  /// If this is a compound predicate (predAnd, etc.), this contains the child predicates. According
  /// to the grammar, it's enough to have at most two children, so this declaration allows for more
  /// general predicate trees than the grammar.
  public List<AugPred> childPreds;

  /// If this is a simple predicate (exprEq, etc.), this contains the child expressions.
  public List<AugExpr> childExprs;

  /// Default constructor with an input PredicateContext ctx
  public AugPred(PerfQueryParser.PredicateContext ctx) {
    if(PerfQueryParser.TruePredContext.isInstance(ctx)) {
      this.type = PRED_TRUE;
    } else if(PerfQueryParser.FalsePredContext.isInstance(ctx)) {
      this.type = PRED_FALSE;
    } else if(PerfQueryParser.ExprEqContext.isInstance(ctx)) {
      this.type = PRED_EQ;
      this.childExprs = MakeExprChildren(AugExpr(ctx.expr(0)), AugExpr(ctx.expr(1)));
    } else if(PerfQueryParser.ExprGtContext.isInstance(ctx)) {
      this.type = PRED_GT;
      this.childExprs = MakeExprChildren(AugExpr(ctx.expr(0)), AugExpr(ctx.expr(1)));
    } else if(PerfQueryParser.ExprLtContext.isInstance(ctx)) {
      this.type = PRED_LT;
      this.childExprs = MakeExprChildren(AugExpr(ctx.expr(0)), AugExpr(ctx.expr(1)));
    } else if(PerfQueryParser.ExprNeContext.isInstance(ctx)) {
      this.type = PRED_NE;
      this.childExprs = MakeExprChildren(AugExpr(ctx.expr(0)), AugExpr(ctx.expr(1)));
    } else if(PerfQueryParser.PredAndContext.isInstance(ctx)) {
      this.type = PRED_AND;
      this.childPreds = MakePredChildren(AugPred(ctx.predicate(0)), AugPred(ctx.predicate(1)));
    } else if(PerfQueryParser.PredOrContext.isInstance(ctx)) {
      this.type = PRED_OR;
      this.childPreds = MakePredChildren(AugPred(ctx.predicate(0)), AugPred(ctx.predicate(1)));
    } else if(PerfQueryParser.PredNotContext.isInstance(ctx)) {
      this.type = PRED_NOT;
      this.childPreds = new ArrayList<>(Arrays.asList(AugPred(ctx.predicate())));
    } else {
      assert(false); // Logic error. Expecting a different kind of predicate?
    }
  }

  /// Constructors to get AugPreds from existing AugPreds, enabling trees of AugPreds.
  public AugPred(AugPredType type, List<AugPred> childPreds) {
    assert(type == AugPredType.PRED_AND || type == AugPredType.PRED_OR
           || type == AugPredType.PRED_NOT);
    this.type = type;
    this.childPreds = childPreds;
  }

  public AugPred(AugPredType type, List<AugExpr> childExprs) {
    assert (type == AugPredType.PRED_EQ || type == AugPredType.PRED_NE
            || type == AugPredType.PRED_GT || type == AugPredType.PRED_LT);
    this.type = type;
    this.childExprs = childExprs;
  }

  public AugPred(boolean isTrue) {
    this.type = isTrue ? AugPredType.PRED_TRUE : AugPredType.PRED_FALSE;
  }

  /// Predicate combinators on existing AugPreds, resulting in new AugPreds.
  public AugPred and(AugPred other) {
    return AugPred(AugPredType.PRED_AND, MakePredChildren(this, other));
  }

  public AugPred or(AugPred other) {
    return AugPred(AugPredType.PRED_OR, MakePredChildren(this, other));
  }

  public AugPred not() {
    return AugPred(AugPredType.PRED_NOT, new ArrayList<>(Arrays.asList(this)));
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
}
