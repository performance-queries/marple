package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.RuntimeException;

/// Determine if a predicate in a FILTER operation is restricting the stream to
/// specific switches, and return the set of switches to which the stream is
/// restricted.
public class SwitchPredicateExtractor extends PerfQueryBaseVisitor<HashSet<Integer>> {
  /// Visitor class instances for various recursive property evaluations
  /// Value expression detection
  private ValueExprDetector valueDetector;
  
  /// Value expression evaluation
  private ValueExprEvaluator valueEvaluator;
  
  /// The set of all network switches
  private HashSet<Integer> allSwitches;
  
  /// Constructor
  public SwitchPredicateExtractor(HashSet<Integer> allSwitches) {
    valueDetector = new ValueExprDetector();
    valueEvaluator = new ValueExprEvaluator();
    this.allSwitches = allSwitches;
  }
  
  /// Return True when the expression is just the identifier corresponding to "switch".
  private Boolean isSwitchExpr(String text) {
    return new Boolean(text.equals(Fields.switchHdr));
  }
  
  /// Return an (optional) integer from evaluating the two sides of a predicate, whenever one of
  /// them is a "switch" and the other is a "value expression."
  private Optional<Integer> getSwValue(PerfQueryParser.ExprContext expr1,
      				 PerfQueryParser.ExprContext expr2) {
    Boolean rightSingleSwitch = (isSwitchExpr(expr1.getText()) &&
    			       valueDetector.visit(expr2));
    Boolean leftSingleSwitch  = (isSwitchExpr(expr2.getText()) &&
    			       valueDetector.visit(expr1));
    if (rightSingleSwitch) {
      return Optional.of(valueEvaluator.visit(expr2));
    } else if (leftSingleSwitch) {
      return Optional.of(valueEvaluator.visit(expr1));
    } else {
      return Optional.empty();
    }
  }
  
  /// Look at a predicate e1 == e2, and determine if it's of the form sw == K, where K is a
  /// constant. To do this, we determine whether either of e1 or e2 is a "value" expression, and
  /// whether the other is the switch field identifier. Note that such checks are necessarily
  /// "incomplete", as they miss more complicated ways of restricting packets to switches,
  /// e.g., (1+(2*switch/2)-1) == 4, or
  /// x = 4; switch = x*2, and so on.
  @Override public HashSet<Integer> visitExprEq (PerfQueryParser.ExprEqContext ctx) {
    Optional<Integer> switchEq = getSwValue(ctx.expr(0), ctx.expr(1));
    HashSet<Integer> result = new HashSet<Integer>();
    if (switchEq.isPresent()) {
      result.add(switchEq.get());
    } else {
      result = allSwitches;
    }
    return result;
  }
  
  /// Currently, you can't order-compare switch identifiers to one another.
  @Override public HashSet<Integer> visitExprGt (PerfQueryParser.ExprGtContext ctx) {
    return allSwitches;
  }
  
  /// Currently, you can't order-compare switch identifiers to one another.
  @Override public HashSet<Integer> visitExprLt (PerfQueryParser.ExprLtContext ctx) {
    return allSwitches;
  }
  
  /// Detect expressions of the form sw != K, where K is a constant.
  @Override public HashSet<Integer> visitExprNe (PerfQueryParser.ExprNeContext ctx) {
    Optional<Integer> switchEq = getSwValue(ctx.expr(0), ctx.expr(1));
    HashSet<Integer> result = allSwitches;
    if (switchEq.isPresent()) {
      result.remove(switchEq.get());
    } else {
      result = allSwitches;
    }
    return result;
  }
  
  /// When two predicates intersect, the final set of switches is the intersection of the switches
  /// which each predicate restricts the stream to.
  @Override public HashSet<Integer> visitPredAnd (PerfQueryParser.PredAndContext ctx) {
    HashSet<Integer> inter = new HashSet<Integer>(visit(ctx.predicate(0)));
    inter.retainAll(visit(ctx.predicate(1)));
    return inter;
  }
  
  /// When two predicates union, the final set of switches is the union of the switches
  /// which each predicate restricts the stream to.
  @Override public HashSet<Integer> visitPredOr (PerfQueryParser.PredOrContext ctx) {
    HashSet<Integer> union = new HashSet<Integer>(visit(ctx.predicate(0)));
    union.addAll(visit(ctx.predicate(1)));
    return union;
  }
  
  /// When a predicte is negated, the final set of switches is the complement of the switches
  /// which the predicate restricts the stream to.
  @Override public HashSet<Integer> visitPredNot (PerfQueryParser.PredNotContext ctx) {
    HashSet<Integer> diff = new HashSet<Integer>(allSwitches);
    diff.removeAll(visit(ctx.predicate()));
    return diff;
  }
  
  /// Return the switch set of the inner predicate within the parentheses.
  @Override public HashSet<Integer> visitPredParen (PerfQueryParser.PredParenContext ctx) {
    return visit(ctx.predicate());
  }
  
  /// When a predicate is True, it is matched on all network switches.
  @Override public HashSet<Integer> visitTruePred (PerfQueryParser.TruePredContext ctx) {
    return allSwitches;
  }
  
  /// When a predicate is False, it is matched on no network switches.
  @Override public HashSet<Integer> visitFalsePred (PerfQueryParser.FalsePredContext ctx) {
    return new HashSet<Integer>();
  }
}
