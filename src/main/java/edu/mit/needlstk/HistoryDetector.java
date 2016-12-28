package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.Map;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.stream.Collectors;

public class HistoryDetector extends PerfQueryBaseVisitor<Void> {
  /// Maximum length of packet history permitted
  private Integer MAX_PKT_HISTORY = 100;
  /// Keep track of "outer" predicate for the current context
  private AugPred outerPred;
  private Integer outerPredId;
  private Integer maxOuterPredId;
  private Integer outerPredHist;
  /// Track current aggregate function
  private String currAggFun;
  /// Predicate tree representing tree of contexts
  private PredTree predTree;
  /// List of field and state parameters for aggregation functions
  private HashMap<String, List<String>> fields;
  private HashMap<String, List<String>> states;

  /// Fixed-point computation variables
  /// Iteration count
  private Integer iterCount;
  private HashMap<String, Integer> iterCountsMap;
  /// Tracking history information
  private HashMap<String, HashMap<String, Integer>> currIterHist;
  private HashMap<String, HashMap<String, Integer>> prevIterHist;

  public HistoryDetector(HashMap<String, List<String>> statesMap,
                         HashMap<String, List<String>> fieldsMap) {
    this.fields = fieldsMap;
    this.states = statesMap;
    this.maxOuterPredId = 0;
    this.currIterHist = new HashMap<String, HashMap<String, Integer>>();
    this.prevIterHist = new HashMap<String, HashMap<String, Integer>>();
    this.iterCountsMap = new HashMap<String, Integer>();
  }

  /// Get the history count for a given identifier from the current or previous iteration's history.
  private Integer getHist(String ident) {
    if (currIterHist.get(currAggFun).containsKey(ident)) {
      return currIterHist.get(currAggFun).get(ident);
    } else if (prevIterHist.get(currAggFun).containsKey(ident)) {
      return Math.min(prevIterHist.get(currAggFun).get(ident) + 1, MAX_PKT_HISTORY);
    } else if (fields.get(currAggFun).contains(ident)) {
      return 0;
    } else if (states.get(currAggFun).contains(ident)) {
      return MAX_PKT_HISTORY;
    } else {
      /// All used variables either already in one of the histories, or are states or fields unless
      /// there are use-before-define errors, which should already have been caught.
      assert(false); // Logic error. Forgot to insert variable into history?
      return -1;
    }
  }

  private Integer compareHist(Integer a, Integer b) {
    // TODO: Not a simple max or history count! organize by predicates.
    return Math.max(a, b);
  }

  private Integer getHistFromList(HashSet<String> usedVars) {
    // TODO: Not a simple max or history count! organize by predicates.
    Integer histBound = usedVars.stream().
        map(var -> getHist(var)).
        reduce(0, (maxhist, hist) -> compareHist(maxhist, hist));
    return histBound;
  }

  /// ANTLR visitor for primitive statements
  @Override public Void visitPrimitive(PerfQueryParser.PrimitiveContext ctx) {
    if (ctx.ID() != null) {
      // get maximum historical dependence among used vars in the current assignment
      Integer exprHist = getHistFromList(new AugExpr(ctx.expr()).getUsedVars());
      Integer assignHist = compareHist(exprHist, this.outerPredHist);
      // insert history entry for defined variable
      currIterHist.get(currAggFun).put(ctx.ID().getText(), assignHist);
    } // else do nothing (EMIT and ;)
    return null;
  }

  private void initNewOuterPred(AugPred pred) {
    this.outerPred = pred;
    this.maxOuterPredId++;
    this.outerPredId = this.maxOuterPredId;
    this.outerPredHist = getHistFromList(pred.getUsedVars());
  }

  private <T extends ParserRuleContext> void handleIfOrElse(List<T> ctxList, AugPred currPred,
                                                            AugPred oldOuterPred) {
    // Initialize a new "outer" predicate for the new context
    initNewOuterPred(oldOuterPred.and(currPred));
    // Visit new contexts
    for (T ctx : ctxList) {
      visit(ctx);
    }
  }

  /// ANTLR visitor for if construct
  @Override public Void visitIfConstruct(PerfQueryParser.IfConstructContext ctx) {
    // Save old "outer predicate" state
    AugPred oldOuterPred = this.outerPred;
    Integer oldOuterPredId = this.outerPredId;
    Integer oldOuterPredHist = this.outerPredHist;
    // Handle if/then/else stuff; very similar in both cases. See handleIfOrElse
    AugPred currPred = new AugPred(ctx.predicate());
    handleIfOrElse(ctx.ifPrimitive(), currPred, oldOuterPred);
    handleIfOrElse(ctx.elsePrimitive(), currPred.not(), oldOuterPred);
    // Restore outer predicate context
    this.outerPred = oldOuterPred;
    this.outerPredId = oldOuterPredId;
    this.outerPredHist = oldOuterPredHist;
    return null;
  }

  /// Condition to detect whether we've reached a fixed point
  private boolean reachedFixedPoint() {
    return prevIterHist.get(currAggFun).equals(currIterHist.get(currAggFun));
  }

  /// ANTLR visitor for main aggregation function
  @Override public Void visitAggFun(PerfQueryParser.AggFunContext ctx) {
    /// Initialize outer predicate and per-function history metadata
    initNewOuterPred(new AugPred(true));
    this.currAggFun = ctx.aggFunc().getText();
    this.currIterHist.put(currAggFun, new HashMap<String, Integer>());
    this.prevIterHist.put(currAggFun, new HashMap<String, Integer>());
    this.iterCount = 0;
    boolean needMoreIterations = true;
    while (needMoreIterations) {
      // Clear current history for a fresh start and generate history
      this.iterCount += 1;
      this.currIterHist.put(currAggFun, new HashMap<String, Integer>());
      for (PerfQueryParser.StmtContext stmt: ctx.stmt()) {
        visit(stmt);
      }
      // Replace results of previous iteration by current iteration
      needMoreIterations = (! reachedFixedPoint()) && this.iterCount < MAX_PKT_HISTORY;
      this.prevIterHist.get(currAggFun).clear();
      this.prevIterHist.put(currAggFun, this.currIterHist.get(currAggFun));
    }
    this.iterCountsMap.put(currAggFun, iterCount);
    return null;
  }

  /// Helper to print history status
  public String reportHistory() {
    String res = "History bounds after fixed-point iterations:\n";
    res += this.currIterHist.toString();
    res += "\n";
    res += "Number of iterations before fixed point/termination:\n";
    res += this.iterCountsMap.toString();
    res += "\n";
    return res;
  }
}
