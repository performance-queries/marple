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
  private PredHist outerPredHist;
  /// Running counter containing maximum predicate ID so far
  private Integer maxOuterPredId;
  /// "True" predicate corresponding to current aggregation function
  private Integer truePredId;
  /// Track current aggregate function
  private String currAggFun;
  /// Predicate tree representing tree of contexts
  private HashMap<String, PredTree> predTree;
  /// Track relationship of predicate IDs to predicates
  private HashMap<Integer, AugPred> predIdToPredMap;
  /// List of field and state parameters for aggregation functions
  private HashMap<String, List<String>> fields;
  private HashMap<String, List<String>> states;
  /// Mapping from a context in the grammar to an outer predicate ID
  private HashMap<ParserRuleContext, Integer> ctxToPredIdMap;

  /// Fixed-point computation variables
  /// Iteration count
  private Integer iterCount;
  private HashMap<String, Integer> iterCountsMap;
  /// Tracking history information
  private HashMap<String, HashMap<String, PredHist>> currIterHist;
  private HashMap<String, HashMap<String, PredHist>> prevIterHist;

  public HistoryDetector(HashMap<String, List<String>> statesMap,
                         HashMap<String, List<String>> fieldsMap) {
    this.fields = fieldsMap;
    this.states = statesMap;
    this.maxOuterPredId = 0;
    this.currIterHist = new HashMap<String, HashMap<String, PredHist>>();
    this.prevIterHist = new HashMap<String, HashMap<String, PredHist>>();
    this.iterCountsMap = new HashMap<String, Integer>();
    this.predTree = new HashMap<String, PredTree>();
    this.predIdToPredMap = new HashMap<Integer, AugPred>();
    this.ctxToPredIdMap = new HashMap<ParserRuleContext, Integer>();
  }

  /// Get predicated history for a given identifier from the current or previous iteration's
  /// history. This returns a predicate history that is complete with respect to the current outer
  /// predicate.
  private PredHist getHist(String ident) {
    /// Get a default history value in case the current outer predicate isn't completely covered by
    /// the current history.
    Integer defaultHist;
    if (prevIterHist.get(currAggFun).containsKey(ident)) {
      defaultHist = Math.min(prevIterHist.get(currAggFun).get(ident).getSingletonHist() + 1,
                             MAX_PKT_HISTORY);
    } else if (fields.get(currAggFun).contains(ident)) {
      defaultHist = 0;
    } else if (states.get(currAggFun).contains(ident)) {
      defaultHist = MAX_PKT_HISTORY;
    } else {
      assert (currIterHist.get(currAggFun).containsKey(ident)); // One of the 4 cases MUST be
      // true. It can't be the case that there is no default history from cases above AND no history
      // entry in the current iteration. It means there are uses before definition for some
      // variables internal to the function (i.e. not fields or states), which errors should have
      // already been caught.
      defaultHist = -1;
    }
    /// Check for current history for this variable, filling in gaps with the default value where
    /// necessary.
    if (currIterHist.get(currAggFun).containsKey(ident)) {
      return currIterHist.get(currAggFun).get(ident).
          getRelevantPredHist(this.outerPredId, this.predTree.get(this.currAggFun), defaultHist);
    } else {
      return new PredHist(this.outerPredId, defaultHist);
    }
  }

  private PredHist getHistFromList(HashSet<String> usedVars) {
    return usedVars.stream().
        map(var -> getHist(var)).
        reduce(new PredHist(this.truePredId, 0),
               (maxhist, hist) -> PredHist.getMaxHist(maxhist, hist,
                                                      this.predTree.get(currAggFun)));
  }

  /// ANTLR visitor for primitive statements
  @Override public Void visitPrimitive(PerfQueryParser.PrimitiveContext ctx) {
    if (ctx.ID() != null) {
      // get maximum historical dependence among used vars in the current assignment
      PredHist exprHist = getHistFromList(new AugExpr(ctx.expr()).getUsedVars());
      PredHist assignHist = PredHist.getMaxHist(exprHist, this.outerPredHist, this.predTree.get(this.currAggFun));
      // insert history entry for defined variable
      if (currIterHist.get(currAggFun).containsKey(ctx.ID().getText())) {
        currIterHist.get(currAggFun).get(ctx.ID().getText()).setHist(assignHist,
    this.predTree.get(this.currAggFun));
      } else {
        currIterHist.get(currAggFun).put(ctx.ID().getText(), assignHist);
      }
    } // else do nothing (EMIT and ;)
    return null;
  }

  /// Initialize new outer predicate for a given context
  private <T extends ParserRuleContext> void initNewOuterPred(T ctx,
                                                              AugPred pred,
                                                              boolean addParent) {
    if (! this.ctxToPredIdMap.containsKey(ctx)) {
      /// Initialize a new outer predicate for this context
      Integer oldOuterPredId = this.outerPredId;
      this.outerPred = pred;
      this.maxOuterPredId++;
      this.outerPredId = this.maxOuterPredId;
      this.predIdToPredMap.put(this.outerPredId, this.outerPred);
      predTree.get(this.currAggFun).addNewPred(this.maxOuterPredId);
      if (addParent) {
        predTree.get(this.currAggFun).addChildToParent(this.outerPredId, oldOuterPredId);
      }
      this.ctxToPredIdMap.put(ctx, this.outerPredId);
    } else {
      /// Restore information from the stored predicate ID for this context
      this.outerPredId = this.ctxToPredIdMap.get(ctx);
      this.outerPred = this.predIdToPredMap.get(this.outerPredId);
    }
    /// Evaluate history for the current outer predicate
    this.outerPredHist = getHistFromList(this.outerPred.getUsedVars());
  }

  private <T extends ParserRuleContext> void handleIfOrElse(T ctx,
                                                            AugPred currPred,
                                                            AugPred oldOuterPred) {
    // Initialize a new "outer" predicate for the new context
    initNewOuterPred(ctx, oldOuterPred.and(currPred), true);
    // Visit new contexts
    if (ctx != null) {
      visit(ctx);
    }
  }

  private void restorePredContext(AugPred oldOuterPred,
                                  Integer oldOuterPredId,
                                  PredHist oldOuterPredHist) {
    this.outerPred = oldOuterPred;
    this.outerPredId = oldOuterPredId;
    this.outerPredHist = oldOuterPredHist;
  }

  /// ANTLR visitor for if construct
  @Override public Void visitIfConstruct(PerfQueryParser.IfConstructContext ctx) {
    // Save old "outer predicate" state
    AugPred oldOuterPred = this.outerPred;
    Integer oldOuterPredId = this.outerPredId;
    PredHist oldOuterPredHist = this.outerPredHist;
    // Handle if/then/else stuff; very similar in both cases. See handleIfOrElse
    AugPred currPred = new AugPred(ctx.predicate());
    handleIfOrElse(ctx.ifCodeBlock(), currPred, oldOuterPred);
    restorePredContext(oldOuterPred, oldOuterPredId, oldOuterPredHist);
    handleIfOrElse(ctx.elseCodeBlock(), currPred.not(), oldOuterPred);
    restorePredContext(oldOuterPred, oldOuterPredId, oldOuterPredHist);
    return null;
  }

  /// Condition to detect whether we've reached a fixed point
  private boolean reachedFixedPoint() {
    HashMap<String, PredHist> currHist = this.currIterHist.get(this.currAggFun);
    HashMap<String, PredHist> prevHist = this.prevIterHist.get(this.currAggFun);
    for (Map.Entry<String, PredHist> entry: currHist.entrySet()) {
      if (! prevHist.containsKey(entry.getKey())) {
        return false;
      } else if (prevHist.get(entry.getKey()).structuralEquals(entry.getValue().squash(this.truePredId))) {
        return false;
      }
    }
    return true;
  }

  /// ANTLR visitor for main aggregation function
  @Override public Void visitAggFun(PerfQueryParser.AggFunContext ctx) {
    /// Initialize outer predicate and per-function history metadata
    this.currAggFun = ctx.aggFunc().getText();
    this.truePredId = this.maxOuterPredId + 1;
    this.predTree.put(currAggFun, new PredTree(truePredId));
    initNewOuterPred(ctx, new AugPred(true), false);
    this.currIterHist.put(currAggFun, new HashMap<String, PredHist>());
    this.prevIterHist.put(currAggFun, new HashMap<String, PredHist>());
    this.iterCount = 0;
    boolean needMoreIterations = true;
    while (needMoreIterations) {
      // Clear current history for a fresh start and generate history
      this.iterCount += 1;
      this.currIterHist.put(currAggFun, new HashMap<String, PredHist>());
      visit(ctx.codeBlock());
      // Replace results of previous iteration by current iteration
      needMoreIterations = (! reachedFixedPoint()) && this.iterCount < MAX_PKT_HISTORY;
      this.prevIterHist.get(currAggFun).clear();
      this.prevIterHist.put(currAggFun, this.currIterHist.get(currAggFun));
      this.squashPrevIterHist(); // summarize previous iteration's history
    }
    this.iterCountsMap.put(currAggFun, iterCount);
    return null;
  }

  /// Summarize the previous iteration's history for each variable.
  private void squashPrevIterHist() {
    for (Map.Entry<String, PredHist> entry: this.prevIterHist.get(this.currAggFun).entrySet()) {
      String var = entry.getKey();
      PredHist varHist = entry.getValue();
      this.prevIterHist.get(this.currAggFun).put(var, varHist.squash(this.truePredId));
    }
  }

  /// Helper to print history status
  public String reportHistory() {
    String res = "History bounds after fixed-point iterations:\n";
    res += this.currIterHist.toString();
    res += "\n";
    res += "Number of iterations before fixed point/termination:\n";
    res += this.iterCountsMap.toString();
    res += "\n";
    res += "Predicate tree:\n";
    res += this.predTree.toString();
    res += "\n";
    res += "Predicate ID to predicate mapping:\n";
    res += this.predIdToPredMap.toString();
    res += "\n";
    return res;
  }
}
