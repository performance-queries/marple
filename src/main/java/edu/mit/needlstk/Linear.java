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
import java.util.Set;
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
  /// Symbol table
  private HashMap<String, HashMap<String, AggFunVarType>> symTab;
  
  public Linear(HashMap<String, ThreeOpCode> aggFunCode,
                HashMap<String, HashMap<String, Integer>> histMap,
                HashMap<String, List<String>> stateVars,
                HashMap<String, HashMap<String, AggFunVarType>> symTab) {
    this.tocs = aggFunCode;
    this.hists = histMap;
    this.states = stateVars;
    this.symTab = symTab;
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

  /// For each aggregation function and state, detect and adjust linear-in-state updates.
  public void extractLinearUpdates() {
    /// Basic sanity checks
    Set<String> aggFuns = this.tocs.keySet();
    assert (aggFuns.equals(this.hists.keySet()));
    assert (aggFuns.equals(this.states.keySet()));
    assert (aggFuns.equals(this.fields.keySet()));
    assert (aggFuns.equals(this.symTab.keySet()));
    /// For each function and state, detect and adjust linear-in-state updates.
    for (String fun: aggFuns) {
      List<String> stateList = this.states.get(fun);
      ArrayList<String> infStates = getInfHistoryStates(stateList, this.hists.get(fun));
      ThreeOpCode currToc = this.tocs.get(fun);
      ThreeOpCode newToc;
      for (String state: infStates) {
        boolean lis = detectLinearInState(currToc, state, this.hists.get(fun));
        if (lis) {
          newToc = adjustLinearInState(
              currToc, state, this.hists.get(fun), this.symTab.get(fun));
          currToc = newToc;
        }
      }
      this.tocs.put(fun, currToc);
    }
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

  /// Make adjustments to aggregation function code to isolate linear in state update into one
  /// statement for all cases
  public ThreeOpCode adjustLinearInState(ThreeOpCode toc,
                                         String state,
                                         HashMap<String, Integer> hist,
                                         HashMap<String, AggFunVarType> syms) {
    ThreeOpCode newToc = new ThreeOpCode(toc.decls, new ArrayList<ThreeOpStmt>());
    /// Add declarations for A and B coefficients
    String aId = aCoeff(state);
    String bId = bCoeff(state);
    newToc.addDecl(new ThreeOpDecl(P4Printer.INT_WIDTH, P4Printer.INT_TYPE, aId));
    newToc.addDecl(new ThreeOpDecl(P4Printer.INT_WIDTH, P4Printer.INT_TYPE, bId));
    syms.put(aId, AggFunVarType.FN_VAR);
    syms.put(bId, AggFunVarType.FN_VAR);
    /// Add default definitions for A and B.
    newToc.appendStmt(new ThreeOpStmt(aId, new AugExpr(1)));
    newToc.appendStmt(new ThreeOpStmt(bId, new AugExpr(0)));
    /// Check line-by-line to adjust assignments to the state.
    for (ThreeOpStmt stmt: toc.stmts) {
      if (state.equals(stmt.getDefinedVar())) {
        /// State assignment: only modify 'a' and 'b' now; not the state itself.
        assert (stmt.isTernary() || stmt.isExprAssign());
        if (stmt.isTernary()) {
          newToc.appendStmts(adjustTernary(stmt, state, aId, bId));
        } else if (stmt.isExprAssign()) {
          newToc.appendStmts(adjustExprAssign(stmt, state, aId, bId));
        } else {
          assert (false); // state assignment statements have to be one of two types!
        }
      } else { // statement not involving the state at all
        newToc.appendStmt(stmt);
      }
    }
    /// Add a final statement adjusting the state through a linear-in-state update using the 'a' and
    /// 'b' coefficients:
    /// state = (aId * state) + bId
    newToc.appendStmt(new ThreeOpStmt(state, new AugExpr(
        new AugExpr(new AugExpr(aId), new AugExpr(state), "*"),
        new AugExpr(bId), "+")));
    return newToc;
  }

  /// Helpers to get linear-in-state updates to all happen in the end.
  private ArrayList<ThreeOpStmt> adjustTernary(ThreeOpStmt stmt, String state, String aId, String bId) {
    assert (stmt.isTernary());
    String predVar = stmt.getPredVarOfTernary();
    ArrayList<AugExpr> exprs = stmt.getUsedExprs();
    assert (exprs.size() == 2); // exprIf and exprElse
    AugExpr exprIf = exprs.get(0);
    AugExpr exprElse = exprs.get(1);
    /// Get affine coefficients for the expressions
    ArrayList<AugExpr> ifAffines   = exprIf.getAffineCoefficients(state);
    ArrayList<AugExpr> elseAffines = exprElse.getAffineCoefficients(state);
    /// Set up 'a' and 'b' assignment statements
    ArrayList<ThreeOpStmt> stmts = new ArrayList<>();
    /// TODO: not dealing with repeated assignments to state
    stmts.add(new ThreeOpStmt(aId, predVar, ifAffines.get(0), elseAffines.get(0)));
    stmts.add(new ThreeOpStmt(bId, predVar, ifAffines.get(1), elseAffines.get(1)));
    return stmts;
  }

  private ArrayList<ThreeOpStmt> adjustExprAssign(ThreeOpStmt stmt,
                                                  String state,
                                                  String aId,
                                                  String bId) {
    assert (stmt.isExprAssign());
    ArrayList<AugExpr> exprs = stmt.getUsedExprs();
    assert (exprs.size() == 1);
    AugExpr expr = exprs.get(0);
    ArrayList<AugExpr> affines = expr.getAffineCoefficients(state);
    /// Set up 'a' and 'b' assignment statements
    ArrayList<ThreeOpStmt> stmts = new ArrayList<>();
    /// TODO: not dealing with repeated assignments to state
    stmts.add(new ThreeOpStmt(aId, affines.get(0)));
    stmts.add(new ThreeOpStmt(bId, affines.get(1)));
    return stmts;
  }

  private String aCoeff(String var) {
    return "_" + var + "_a";
  }

  private String bCoeff(String var) {
    return "_" + var + "_b";
  }

  /// Helpers to extract new code and new symbol tables.
  public HashMap<String, ThreeOpCode> getAggFunCode() {
    return this.tocs;
  }

  public HashMap<String, HashMap<String, AggFunVarType>> getGlobalSymTab() {
    return this.symTab;
  }
}
