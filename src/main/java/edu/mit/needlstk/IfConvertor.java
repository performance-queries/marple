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
  /// HashMap that maintains an "outer" predicate for each statement
  private Map<ParserRuleContext, String> outerPredIdMap;
  private Map<ParserRuleContext, AugPred> outerPredTreeMap;
  /// Default integer bitwidth used for declarations in emitted code.
  private Integer INT_WIDTH = 32;
  /// Map aggregation function names to threeopcode function body
  private HashMap<String, ThreeOpCode> aggFunCode;

  /// Constructor
  public IfConvertor() {
    this.outerPredIdMap = new IdentityHashMap<ParserRuleContext, String>();
    this.outerPredTreeMap = new IdentityHashMap<ParserRuleContext, AugPred>();
    this.aggFunCode = new HashMap<String, ThreeOpCode>();
  }

  /// Get unique integers across all instances of this class. NOT thread-safe.
  private String getUid() {
    incr += 1;
    return incr.toString();
  }

  /// ANTLR visitor for primitives
  public ThreeOpCode visitPrimitive(PerfQueryParser.PrimitiveContext ctx) {
    assert(outerPredIdMap.containsKey(ctx));
    // Check whether this is an assignment. Only some primitives are assignments!
    if (ctx.ID() != null) {
      ThreeOpStmt stmt = new ThreeOpStmt(ctx.ID().getText(),
                                         outerPredIdMap.get(ctx),
                                         new AugExpr(ctx.expr()),
                                         new AugExpr(ctx.ID().getText()));
      return new ThreeOpCode(new ArrayList<ThreeOpDecl>(),
                             new ArrayList<ThreeOpStmt>(Arrays.asList(stmt)));
    } else {
      return new ThreeOpCode();
    }
  }

  /// ANTLR visitor for ifConstruct
  public ThreeOpCode visitIfConstruct(PerfQueryParser.IfConstructContext ctx) {
    assert(outerPredIdMap.containsKey(ctx));
    assert(outerPredTreeMap.containsKey(ctx));
    AugPred outerPred = outerPredTreeMap.get(ctx);
    AugPred currPred = new AugPred(ctx.predicate());
    ThreeOpCode code = handleIfOrElse(ctx.ifPrimitive(), currPred, outerPred, true);
    if(ctx.elsePrimitive().size() > 0) {
      code = code.orderedMerge(handleIfOrElse(ctx.elsePrimitive(), currPred, outerPred, false));
    }
    return code;
  }

  /// ANTLR visitor for aggFun
  public ThreeOpCode visitAggFun(PerfQueryParser.AggFunContext ctx) {
    // Set up outer predicate "true"
    AugPred truePred = new AugPred(true);
    ThreeOpCode toc = setupPred(truePred);
    // Merge code from internal statements
    for (PerfQueryParser.StmtContext stmt: ctx.stmt()) {
      outerPredIdMap.put(stmt, toc.peekIdFirstDecl());
      outerPredTreeMap.put(stmt, truePred);
      toc = toc.orderedMerge(visit(stmt));
    }
    /// Add generated code for aggregation function
    aggFunCode.put(ctx.aggFunc().getText(), toc);
    return toc;
  }

  /// ANTLR visitor for stmt
  public ThreeOpCode visitStmt(PerfQueryParser.StmtContext ctx) {
    assert(outerPredIdMap.containsKey(ctx));
    assert(outerPredTreeMap.containsKey(ctx));
    if(ctx.primitive() != null) {
      PerfQueryParser.PrimitiveContext pctx = ctx.primitive();
      outerPredIdMap.put(pctx, outerPredIdMap.get(ctx));
      outerPredTreeMap.put(pctx, outerPredTreeMap.get(ctx));
      return visit(pctx);
    } else if(ctx.ifConstruct() != null) {
      PerfQueryParser.IfConstructContext ictx = ctx.ifConstruct();
      outerPredIdMap.put(ictx, outerPredIdMap.get(ctx));
      outerPredTreeMap.put(ictx, outerPredTreeMap.get(ctx));
      return visit(ictx);
    } else {
      assert (false); // Logic error. Expecint a different kind of statement?
      return null;
    }
  }

  public <T extends ParserRuleContext> ThreeOpCode processInternalPrimitive(
      T ctx, PerfQueryParser.PrimitiveContext pctx) {
    assert(outerPredIdMap.containsKey(ctx));
    assert(outerPredTreeMap.containsKey(ctx));
    outerPredIdMap.put(pctx, outerPredIdMap.get(ctx));
    outerPredTreeMap.put(pctx, outerPredTreeMap.get(ctx));
    return visit(pctx);
  }

  public ThreeOpCode visitIfPrimitive(PerfQueryParser.IfPrimitiveContext ctx) {
    return processInternalPrimitive(ctx, ctx.primitive());
  }

  public ThreeOpCode visitElsePrimitive(PerfQueryParser.ElsePrimitiveContext ctx) {
    return processInternalPrimitive(ctx, ctx.primitive());
  }

  /// Return a three op code initializing a new variable to the supplied predicate
  private ThreeOpCode setupPred(AugPred pred) {
    /// step 1. Get a new predicate corresponding to this case.
    /// 1.1 Declare a new predicate variable
    String predVarId = getUid() + "_pred"; // ensure variables can't appear in user program
    ThreeOpDecl predVarDecl = new ThreeOpDecl(INT_WIDTH, predVarId);
    /// 1.2 Assign pred to predVarId
    ThreeOpStmt predVarStmt = new ThreeOpStmt(predVarId, pred);
    return new ThreeOpCode(
        new ArrayList<>(Arrays.asList(predVarDecl)),
        new ArrayList<>(Arrays.asList(predVarStmt)));
  }
  
  /// Helper to handle one part of an IF ... ELSE statement: either the IF or ELSE. The logic is
  /// very similar for the two, so they can be handled through the same function.
  private <T extends ParserRuleContext> ThreeOpCode handleIfOrElse(List<T> ctxList,
                                                                   AugPred currPred,
                                                                   AugPred outerPred,
                                                                   boolean ifPred) {
    AugPred clausePred = outerPred.and(ifPred ? currPred : currPred.not());
    ThreeOpCode toc = setupPred(clausePred);
    /// Now merge three operand codes from each internal statement    
    for (T ctx : ctxList) {
      outerPredIdMap.put(ctx, toc.peekIdFirstDecl());
      outerPredTreeMap.put(ctx, clausePred);
      toc = toc.orderedMerge(visit(ctx));
    }
    return toc;
  }

  public HashMap<String, ThreeOpCode> getAggFunCode() {
    return this.aggFunCode;
  }
}
