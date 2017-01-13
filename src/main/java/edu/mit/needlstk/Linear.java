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

public class Linear {
  /// ThreeOpCode for aggregation functions
  private HashMap<String, ThreeOpCode> tocs;
  /// Converged history for aggregation function variables
  private HashMap<String, HashMap<String, Integer>> hists;
  /// State variable inputs for aggregation functions
  private HashMap<String, List<String>> states;
  /// Field variable inputs for aggregation functions
  private HashMap<String, List<String>> fields;
  
  public Linear(HashMap<String, ThreeOpCode> aggFunCode,
                HashMap<String, HashMap<String, Integer>> histMap,
                HashMap<String, List<String>> stateVars,
                HashMap<String, List<String>> fieldVars) {
    this.tocs = aggFunCode;
    this.hists = histMap;
    this.states = stateVars;
    this.fields = fieldVars;
  }

  /// Given a list of states and a converged history, return whether 
  public ArrayList<String> getInfHistoryStates(List<String> states, HashMap<String, Integer> hist) {
    return new ArrayList<String>(
        states.stream().
        filter(var -> (hist.get(var) == HistoryDetector.MAX_PKT_HISTORY)).
        collect(Collectors.toList()));
  }

  /// Assign histories to predicates by passing over the history and the code.
  public HashMap<String, Integer> getPredHist(ThreeOpCode toc, HashMap<String, Integer> hist) {
    HashMap<String, Integer> predHist = new HashMap<>();
    HashMap<String, Integer> newHist = new HashMap<String, Integer>(hist);
    for (ThreeOpStmt stmt: toc.stmts) {
      if (stmt.isPredAssign()) {
        Integer maxHist = stmt.getUsedVars().stream().
            map(var -> newHist.get(var)).
            reduce(0, (a,b) -> Math.max(a, b));
        assert (! newHist.containsKey(stmt.getDefinedVar()));
        newHist.put(stmt.getDefinedVar(), maxHist);
      }
    }
    return newHist;
  }

  /// Detect two conditions to scale key-value-type updates for a state of infinite packet history:
  /// (1) That the state is only ever read for the purpose of updating itself.
  /// (2) Every update to the state is of the linear-in-state form, namely:
  ///     if (pred) state <- A * state + B where
  ///     pred, A and B are all functions of bounded packet history.
  /// Note: Vector state updates which are linear-in-state aren't considered yet.
  public boolean detectLinearInState(ThreeOpCode toc,
                                     String state,
                                     HashMap<String, Integer> hist) {
    for (ThreeOpStmt stmt: toc.stmts) {
      /// check if state reads are only to update the same state
      if (stmt.getUsedVars().contains(state) && ! stmt.getDefinedVar().equals(state)) return false;
      /// check if state writes have the linear-in-state form
      if (state.equals(stmt.getDefinedVar())) {
        ArrayList<AugExpr> exprs = stmt.getUsedExprs();
        if (exprs.size() == 0) return false;
        /// check if the enclosing predicate only involves bounded packet history
        if (stmt.isTernary() &&
            hist.get(stmt.getPredVarOfTernary()) == HistoryDetector.MAX_PKT_HISTORY)
          return false;
        /// each used expression should be affine.
        for (AugExpr expr: exprs) {
          if (! expr.isAffine(state)) return false;
          ArrayList<AugExpr> affineCoeffs = expr.getAffineCoefficients(state);
          /// each affine coefficient should only involve bounded packet history
          for (AugExpr coeff: affineCoeffs) {
            if (getInfHistoryStates(new ArrayList<>(coeff.getUsedVars()), hist).size() > 0)
              return false;
          }
        }
      }
    }
    return true;
  }
}
