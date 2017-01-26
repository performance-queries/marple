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
                HashMap<String, List<String>> fieldVars,
                HashMap<String, HashMap<String, AggFunVarType>> symTab) {
    this.tocs = aggFunCode;
    this.hists = histMap;
    this.states = stateVars;
    this.fields = fieldVars;
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
  public HashMap<String, Integer> getPredHist(ThreeOpCode toc,
                                              HashMap<String, Integer> hist,
                                              List<String> fieldList) {
    HashMap<String, Integer> predHist = new HashMap<>();
    HashMap<String, Integer> newHist = new HashMap<String, Integer>(hist);
    for (ThreeOpStmt stmt: toc.stmts) {
      if (stmt.isPredAssign()) {
        Integer maxHist = stmt.getUsedVars().stream().
            filter(var -> ! (fieldList.contains(var))).
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
    /// For each function and state, detect whether all infinite history states are
    /// linear-in-state. if they are, transform to use the multiply accumulate atom. Otherwise,
    /// leave the function unchanged.
    /// TODO (NG): there is some code repetition in this function. Clean up later.
    boolean infStatesPresent = false;
    boolean allInfStatesLis  = true;
    for (String fun: aggFuns) {
      List<String> stateList = this.states.get(fun);
      ArrayList<String> infStates = getInfHistoryStates(stateList, this.hists.get(fun));
      ThreeOpCode currToc = this.tocs.get(fun);
      HashMap<String, Integer> newHist = getPredHist(currToc, this.hists.get(fun),
  this.fields.get(fun));
      for (String state: infStates) {
        infStatesPresent = true;
        boolean lis = detectLinearInState(currToc, state, newHist);
        if (! lis) allInfStatesLis = false;
      }
    }
    /// Only transform if infinite history states are present AND they're all LIS.
    if (infStatesPresent && ! allInfStatesLis) {
      System.out.println("Skipping the linear-in-state transformation pass.");
      return;
    }
    
    /// For each function and state, detect and adjust linear-in-state updates.
    for (String fun: aggFuns) {
      List<String> stateList = this.states.get(fun);
      ArrayList<String> infStates = getInfHistoryStates(stateList, this.hists.get(fun));
      ThreeOpCode currToc = this.tocs.get(fun);
      HashMap<String, Integer> newHist = getPredHist(currToc, this.hists.get(fun), this.fields.get(fun));
      ThreeOpCode newToc;
      for (String state: infStates) {
        boolean lis = detectLinearInState(currToc, state, newHist);
        if (lis) {
          newToc = adjustLinearInState(
              currToc, state, newHist, this.symTab.get(fun));
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
        if (stmt.isTernary()) {
          assert (hist.containsKey(stmt.getPredVarOfTernary()));
          if (hist.get(stmt.getPredVarOfTernary()) == HistoryDetector.MAX_PKT_HISTORY) return false;
        }
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

  /// Updated expressions from repeated linear updates of state
  private AugExpr updateA(String aId, AugExpr newExprA) {
    return new AugExpr(new AugExpr(aId), newExprA, "*");
  }

  private AugExpr updateB(String bId, AugExpr newExprA, AugExpr newExprB) {
    return new AugExpr(newExprB,
                       new AugExpr(newExprA, new AugExpr(bId), "*"),
                       "+");
  }

  /// Return adjusting statements when state is updated using the ternary statement
  private ArrayList<ThreeOpStmt> adjustTernary(ThreeOpStmt stmt, String state, String aId, String bId) {
    assert (stmt.isTernary());
    String predVar = stmt.getPredVarOfTernary();
    ArrayList<AugExpr> exprs = stmt.getUsedExprs();
    assert (exprs.size() == 2); // exprIf and exprElse
    AugExpr exprIf = exprs.get(0);
    /// Get affine coefficients for the expressions. There is an implicit assumption that we leave
    /// the values of the coefficients same in the else case. This is a result of our if-conversion
    /// algorithm.
    ArrayList<AugExpr> ifAffines  = exprIf.getAffineCoefficients(state);
    AugExpr newExprA = ifAffines.get(0);
    AugExpr newExprB = ifAffines.get(1);
    /// Set up 'a' and 'b' assignment statements
    ArrayList<ThreeOpStmt> stmts = new ArrayList<>();
    stmts.add(new ThreeOpStmt(aId, predVar, updateA(aId, newExprA), new AugExpr(aId)));
    stmts.add(new ThreeOpStmt(bId, predVar, updateB(bId, newExprA, newExprB), new AugExpr(bId)));
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
    /// Get affine coefficients for the expressions
    ArrayList<AugExpr> affines = expr.getAffineCoefficients(state);
    AugExpr newExprA = affines.get(0);
    AugExpr newExprB = affines.get(1);
    /// Set up 'a' and 'b' assignment statements
    ArrayList<ThreeOpStmt> stmts = new ArrayList<>();
    stmts.add(new ThreeOpStmt(aId, updateA(aId, newExprA)));
    stmts.add(new ThreeOpStmt(bId, updateB(bId, newExprA, newExprB)));
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
