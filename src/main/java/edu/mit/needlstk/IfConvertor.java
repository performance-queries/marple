package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.Map;
import java.util.IdentityHashMap;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/// Convert aggregation function into three-operand code for processing both by the P4 as well as
/// domino compiler.
public class IfConvertor extends PerfQueryBaseVisitor<ThreeOpCode> {
  /// Unique integer instance
  private Integer incr = 0;
  /// Maintain "outer" predicate context
  private AugPred outerPred;
  private String outerPredId;
  /// Map aggregation function names to threeopcode function body
  private HashMap<String, ThreeOpCode> aggFunCode;
  /// Global symbol table of variables and state for each aggregation function
  private HashMap<String, HashMap<String, AggFunVarType>> symTab;
  /// Current indicator of the aggregation function being processed
  private String currAggFun;

  /// Constructor
  public IfConvertor(HashMap<String, HashMap<String, AggFunVarType>> symTab) {
    this.aggFunCode = new HashMap<String, ThreeOpCode>();
    this.symTab = symTab;
  }

  /// Get unique integers across all instances of this class. NOT thread-safe.
  private String getUid() {
    incr += 1;
    return incr.toString();
  }

  private ThreeOpStmt makePrimitiveAssignmentStmt(PerfQueryParser.PrimitiveContext ctx) {
    /// Simplify statement construction by checking for identically true or false statmeents
    String ident = ctx.ID().getText();
    if (this.outerPred.isIdenticallyTrue()) {
      return new ThreeOpStmt(ident, new AugExpr(ctx.expr()));
    } else if (this.outerPred.isIdenticallyFalse()) {
      return new ThreeOpStmt(ident, new AugExpr(ident));
    } else {
      return new ThreeOpStmt(ident,
                             this.outerPredId,
                             new AugExpr(ctx.expr()),
                             new AugExpr(ident));
    }
  }

  /// ANTLR visitor for code block that aggregates code generated within individual productions
  @Override public ThreeOpCode visitCodeBlock(PerfQueryParser.CodeBlockContext ctx) {
    ThreeOpCode toc = new ThreeOpCode();
    for (ParseTree child: ctx.children) {
      toc = toc.orderedMerge(visit((ParserRuleContext)child));
    }
    return toc;
  }

  /// ANTLR visitor for primitives
  public ThreeOpCode visitPrimitive(PerfQueryParser.PrimitiveContext ctx) {
    if (ctx.ID() != null) {
      // Check whether this is an assignment. Only some primitives are assignments!
      ThreeOpStmt stmt = makePrimitiveAssignmentStmt(ctx);
      return new ThreeOpCode(new ArrayList<ThreeOpDecl>(),
                             new ArrayList<ThreeOpStmt>(Arrays.asList(stmt)));
    } else if (ctx.EMIT() != null) {
      // TODO: Emit statements are special kinds of `ThreeOpStmt`s.
      ThreeOpStmt stmt = new ThreeOpStmt(outerPredId);
      return new ThreeOpCode(new ArrayList<ThreeOpDecl>(),
                             new ArrayList<ThreeOpStmt>(Arrays.asList(stmt)));
    } else {
      return new ThreeOpCode();
    }
  }

  private void restorePredContext(AugPred oldOuterPred, String oldOuterPredId) {
    this.outerPred = oldOuterPred;
    this.outerPredId = oldOuterPredId;
  }

  /// ANTLR visitor for ifConstruct
  public ThreeOpCode visitIfConstruct(PerfQueryParser.IfConstructContext ctx) {
    /// Set up a predicate corresponding to the branch condition
    AugPred currPred = new AugPred(ctx.predicate());
    ThreeOpCode code = setupPred(currPred);
    String currPredId = code.peekIdFirstDecl();
    /// Save context for outer predicate
    AugPred oldOuterPred = this.outerPred;
    String oldOuterPredId = this.outerPredId;
    code = code.orderedMerge(handleIfOrElse(ctx.ifCodeBlock(),
                                            new AugPred(currPredId),
                                            new AugPred(outerPredId),
                                            true));
    restorePredContext(oldOuterPred, oldOuterPredId);
    if(ctx.elseCodeBlock() != null) {
      code = code.orderedMerge(handleIfOrElse(ctx.elseCodeBlock(),
                                              new AugPred(currPredId),
                                              new AugPred(outerPredId),
                                              false));
      restorePredContext(oldOuterPred, oldOuterPredId);
    }
    return code;
  }

  private void addFnVarDecl(HashMap<String, AggFunVarType> localSymTab, ThreeOpCode toc) {
    for (Map.Entry<String, AggFunVarType> entry: localSymTab.entrySet()) {
      if (entry.getValue() == AggFunVarType.FN_VAR) {
        toc.addDecl(new ThreeOpDecl(P4Printer.INT_WIDTH, P4Printer.INT_TYPE, entry.getKey()));
      }
    }
  }

  /// ANTLR visitor for aggFun
  public ThreeOpCode visitAggFun(PerfQueryParser.AggFunContext ctx) {
    // Indicate the current aggregation function
    currAggFun = ctx.aggFunc().getText();
    // Set up outer predicate "true" context
    AugPred truePred = new AugPred(true);
    ThreeOpCode toc = setupPred(truePred);
    this.outerPred = truePred;
    this.outerPredId = toc.peekIdFirstDecl();
    // Add declarations for function variables
    addFnVarDecl(this.symTab.get(currAggFun), toc);
    // Merge code from internal statements
    toc = toc.orderedMerge(visit(ctx.codeBlock()));
    /// Add generated code for aggregation function
    aggFunCode.put(ctx.aggFunc().getText(), toc);
    return toc;
  }

  /// Return a three op code initializing a new variable to the supplied predicate
  private ThreeOpCode setupPred(AugPred pred) {
    /// step 1. Get a new predicate corresponding to this case.
    /// 1.1 Declare a new predicate variable
    String predVarId = "_pred_" + getUid(); // ensure variables can't appear in user program
    ThreeOpDecl predVarDecl = new ThreeOpDecl(P4Printer.BOOL_WIDTH, P4Printer.BOOL_TYPE, predVarId);
    /// 1.2 Assign pred to predVarId
    ThreeOpStmt predVarStmt = new ThreeOpStmt(predVarId, pred);
    /// 1.3 Add new predicate to symbol table
    this.symTab.get(currAggFun).put(predVarId, AggFunVarType.PRED_VAR);
    /// Return code with the new predicate assignment statement
    return new ThreeOpCode(
        new ArrayList<>(Arrays.asList(predVarDecl)),
        new ArrayList<>(Arrays.asList(predVarStmt)));
  }
  
  /// Helper to handle one part of an IF ... ELSE statement: either the IF or ELSE. The logic is
  /// very similar for the two, so they can be handled through the same function.
  private <T extends ParserRuleContext> ThreeOpCode handleIfOrElse(T ctx,
                                                                   AugPred currPred,
                                                                   AugPred outerPred,
                                                                   boolean ifPred) {
    /// Set up new outer predicate context
    this.outerPred = outerPred.and(ifPred ? currPred : currPred.not());
    ThreeOpCode toc = setupPred(this.outerPred);
    this.outerPredId = toc.peekIdFirstDecl();
    /// Now merge three operand codes from each internal statement
    toc = toc.orderedMerge(visit(ctx));
    return toc;
  }

  public HashMap<String, ThreeOpCode> getAggFunCode() {
    return this.aggFunCode;
  }
}
