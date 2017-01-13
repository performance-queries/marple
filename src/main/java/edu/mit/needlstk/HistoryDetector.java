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
  public static Integer MAX_PKT_HISTORY = 100;
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
  private HashMap<ParserRuleContext, Integer> ctxToPredIdTrueMap;
  private HashMap<ParserRuleContext, Integer> ctxToPredIdFalseMap;

  /// Fixed-point computation variables
  /// Iteration count
  private Integer iterCount;
  private HashMap<String, Integer> iterCountsMap;
  /// Tracking history information
  private HashMap<String, HashMap<String, PredHist>> currIterHist;
  private HashMap<String, HashMap<String, PredHist>> prevIterHist;
  /// Tracking AST information
  private HashMap<String, HashMap<String, PredAST>> ast;

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
    this.ctxToPredIdTrueMap = new HashMap<ParserRuleContext, Integer>();
    this.ctxToPredIdFalseMap = new HashMap<ParserRuleContext, Integer>();
  }

  /// Get predicated history for a given identifier from the current or previous iteration's
  /// history. This returns a predicate history that is complete with respect to the current outer
  /// predicate.
  private PredHist getHist(String ident, Integer givenOuterPredId) {
    /// Get a default history value in case the current outer predicate isn't completely covered by
    /// the current history.
    Integer defaultHist;
    Integer noHist = -1;
    if (prevIterHist.get(currAggFun).containsKey(ident)) {
      defaultHist = Math.min((Integer)prevIterHist.get(currAggFun).get(ident).getSingletonHist()
                             + 1,
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
    PredHist result;
    if (currIterHist.get(currAggFun).containsKey(ident)) {
      result = new PredHist(currIterHist.get(currAggFun).get(ident).getRelevantPredHist(
          givenOuterPredId,
          this.predTree.get(this.currAggFun),
          defaultHist,
          noHist));
    } else {
      result = new PredHist(givenOuterPredId, defaultHist);
    }
    return result;
  }

  private PredHist getHistFromList(HashSet<String> usedVars, Integer givenOuterPredId) {
    return usedVars.stream().
        map(var -> getHist(var, givenOuterPredId)).
        reduce(new PredHist(givenOuterPredId, 0),
               (maxhist, hist) -> PredHist.getMaxHist(maxhist, hist,
                                                      this.predTree.get(currAggFun)));
  }

  /// ANTLR visitor for primitive statements
  @Override public Void visitPrimitive(PerfQueryParser.PrimitiveContext ctx) {
    if (ctx.ID() != null) {
      // get maximum historical dependence among used vars in the current assignment
      PredTree pt = this.predTree.get(this.currAggFun);
      PredHist exprHist = getHistFromList(new AugExpr(ctx.expr()).getUsedVars(), this.outerPredId);
      PredHist relevantOuterHist = new PredHist(this.outerPredHist.getRelevantPredHist(
          this.outerPredId, pt, -1, -1));
      PredHist assignHist = PredHist.getMaxHist(exprHist, relevantOuterHist, pt);
      // insert history entry for defined variable
      String ident = ctx.ID().getText();
      if (currIterHist.get(currAggFun).containsKey(ident)) {
        currIterHist.get(currAggFun).get(ident).setHist(
            assignHist, this.predTree.get(this.currAggFun));
      } else {
        currIterHist.get(currAggFun).put(ident, assignHist);
      }
    } // else do nothing (EMIT and ;)
    return null;
  }

  /// Initialize new outer predicate for a given context
  private <T extends ParserRuleContext> void initNewOuterPred(T ctx,
                                                              AugPred outerPred,
                                                              AugPred currPred,
                                                              boolean rooted,
                                                              Integer givenOuterPredId) {
    assert (ctx != null);
    assert (! this.ctxToPredIdTrueMap.containsKey(ctx));
    assert (! this.ctxToPredIdFalseMap.containsKey(ctx));
    /// Initialize two new predicates for this context, corresponding to true and false.
    this.maxOuterPredId++;
    Integer outerPredTrue = this.maxOuterPredId;
    this.predIdToPredMap.put(outerPredTrue, outerPred.and(currPred));
    /// Adjust predicate tree
    PredTree pt = this.predTree.get(this.currAggFun);
    pt.addNewPred(outerPredTrue);
    /// Set up context to predicate mappings
    this.ctxToPredIdTrueMap.put(ctx, outerPredTrue);
    /// Repeat the exact same things for the false branch, if this predicate isn't the root.
    if (! rooted) {
      this.maxOuterPredId++;
      Integer outerPredFalse = this.maxOuterPredId;
      this.predIdToPredMap.put(outerPredFalse, outerPred.and(currPred.not()));
      pt.addNewPred(outerPredFalse);
      this.ctxToPredIdFalseMap.put(ctx, outerPredFalse);
      /// Additionally, add new predicates as children to outer predicate if not the root.
      pt.addChildToParent(outerPredTrue, givenOuterPredId);
      pt.addChildToParent(outerPredFalse, givenOuterPredId);
    }
  }

  /// Used by handleIfOrElse to restore a specific outer predicate context.
  private <T extends ParserRuleContext> void restoreFromStoredContext(T ctx,
                                                                      boolean trueBranch,
                                                                      Integer givenOuterPredId) {
    assert (ctx != null && this.ctxToPredIdTrueMap.containsKey(ctx));
    HashMap<ParserRuleContext, Integer> ctxToPredMap = trueBranch ?
        this.ctxToPredIdTrueMap : this.ctxToPredIdFalseMap;
    this.outerPredId = ctxToPredMap.get(ctx);
    this.outerPred = this.predIdToPredMap.get(this.outerPredId);
    /// Evaluate history for current outer predicate in the context of given outer predicate.
    this.outerPredHist = getHistFromList(this.outerPred.getUsedVars(), givenOuterPredId);
  }

  private <T extends ParserRuleContext> void handleIfOrElse(T ifCtx,
                                                            T elseCtx,
                                                            Integer oldOuterPredId) {
    // Visit If and Else blocks after setting up appropriate predicate contexts
    restoreFromStoredContext(ifCtx, true, oldOuterPredId);
    visit(ifCtx);
    if (elseCtx != null) {
      restoreFromStoredContext(ifCtx, false, oldOuterPredId);
      visit(elseCtx);
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
    // Initialize new predicates corresponding to if and else branches. Do this only for the first
    // iteration of the fixed point loop.
    AugPred currPred = new AugPred(ctx.predicate());
    if (this.iterCount == 1) {
      initNewOuterPred(ctx.ifCodeBlock(), oldOuterPred, currPred, false, oldOuterPredId);
    }
    // Visit the if and else branches. see handleIfOrElse.
    handleIfOrElse(ctx.ifCodeBlock(), ctx.elseCodeBlock(), oldOuterPredId);
    restorePredContext(oldOuterPred, oldOuterPredId, oldOuterPredHist);
    return null;
  }

  /// Condition to detect whether we've reached a fixed point
  private boolean reachedFixedPoint() {
    HashMap<String, PredHist> squashedCurrHist = squashIterHist(
        this.currIterHist.get(this.currAggFun),
        this.predTree.get(this.currAggFun),
        this.states.get(this.currAggFun));
    HashMap<String, PredHist> prevHist = this.prevIterHist.get(this.currAggFun);
    /// Compare histories for each identifier and return true if none of the histories changed.
    if (! prevHist.keySet().equals(squashedCurrHist.keySet())) {
      return false;
    }
    for (String ident: squashedCurrHist.keySet()) {
      if (! prevHist.get(ident).structuralEquals(squashedCurrHist.get(ident))) {
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
    this.predTree.put(currAggFun, new PredTree(this.truePredId));
    initNewOuterPred(ctx, new AugPred(true), new AugPred(true), true, this.truePredId);
    restoreFromStoredContext(ctx, true, this.truePredId);
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
      // this.prevIterHist.put(currAggFun, this.currIterHist.get(currAggFun));
      // summarize previous iteration's history
      HashMap<String, PredHist> squashedIterHist = squashIterHist(
          this.currIterHist.get(currAggFun),
          this.predTree.get(currAggFun),
          this.states.get(currAggFun));
      this.prevIterHist.put(currAggFun, squashedIterHist);
    }
    this.iterCountsMap.put(currAggFun, iterCount);
    return null;
  }

  /// Summarize an iteration's history for each variable.
  private HashMap<String, PredHist> squashIterHist(HashMap<String, PredHist> iterHist,
                                                   PredTree predTree,
                                                   List<String> states) {
    HashMap<String, PredHist> newIterHist = new HashMap<String, PredHist>();
    HashSet<String> unseenStates = new HashSet<>(states);
    for (Map.Entry<String, PredHist> entry: iterHist.entrySet()) {
      String var = entry.getKey();
      PredHist varHist = entry.getValue();
      /// ensure histories are "complete" with respect to entire predicate space
      PredHist adjustedHist;
      if (states.contains(var)) {
        adjustedHist = new PredHist(this.truePredId, MAX_PKT_HISTORY);
        adjustedHist.setHist(varHist, predTree);
        unseenStates.remove(var);
      } else {
        adjustedHist = varHist;
      }
      newIterHist.put(var, adjustedHist.squash(this.truePredId));
    }
    for (String unseen: unseenStates) {
      newIterHist.put(unseen, new PredHist(this.truePredId, -1)); // state never assigned: set history -1
    }
    return newIterHist;
  }

  /// Helper to print history status
  public String reportHistory() {
    String res = "History bounds after fixed-point iterations:\n";
    res += this.prevIterHist.toString();
    res += "\n";
    res += "Number of iterations before fixed point/termination:\n";
    res += this.iterCountsMap.toString();
    res += "\n";
    // res += "Predicate tree:\n";
    // res += this.predTree.toString();
    // res += "\n";
    // res += "Predicate ID to predicate mapping:\n";
    // for (Map.Entry<Integer, AugPred> entry: this.predIdToPredMap.entrySet()) {
    //   res += String.valueOf(entry.getKey()) + ": " + entry.getValue().print() + "\n";
    // }
    return res;
  }

  public HashMap<String, HashMap<String, Integer>> getConvergedHistory() {
    HashMap<String, HashMap<String, Integer>> converged = new HashMap<>();
    for (String aggFun: this.prevIterHist.keySet()) {
      converged.put(aggFun, new HashMap<String, Integer>());
      for (String var: this.prevIterHist.get(aggFun).keySet()) {
        converged.get(aggFun).put(var, this.prevIterHist.get(aggFun).get(var).getSingletonHist());
      }
    }
    return converged;
  }
}
