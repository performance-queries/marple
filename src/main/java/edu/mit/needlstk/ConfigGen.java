package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

public class ConfigGen extends PerfQueryBaseVisitor<PipeStage> {
  private HashMap<String, PipeStage> queryToPipe;
  private HashMap<String, ThreeOpCode> aggFunCode;
  private HashMap<String, HashMap<String, AggFunVarType>> aggFunSymTab;
  private HashMap<String, List<String>> stateVars;
  private HashMap<String, List<String>> fieldVars;

  public ConfigGen(HashMap<String, ThreeOpCode> aggFunCode,
                   HashMap<String, HashMap<String, AggFunVarType>> aggFunSymTab,
                   HashMap<String, List<String>> stateVars,
                   HashMap<String, List<String>> fieldVars) {
    this.queryToPipe = new HashMap<>();
    this.aggFunCode = aggFunCode;
    this.aggFunSymTab = aggFunSymTab;
    this.stateVars = stateVars;
    this.fieldVars = fieldVars;
  }

  @Override public PipeStage visitStreamStmt(PerfQueryParser.StreamStmtContext ctx) {
    PipeStage result = visit(ctx.streamQuery());
    String query = ctx.stream().getText();
    result.setPipeName(query);
    queryToPipe.put(query, result);
    return result;
  }
  
  @Override public PipeStage visitFilter(PerfQueryParser.FilterContext ctx) {
    FilterConfigInfo fci = new FilterConfigInfo(ctx.predicate());
    return new PipeStage(OperationType.FILTER, fci);
  }

  @Override public PipeStage visitMap(PerfQueryParser.MapContext ctx) {
    MapConfigInfo mci = new MapConfigInfo(ctx.columnList(), ctx.exprList());
    return new PipeStage(OperationType.PROJECT, mci);
  }

  @Override public PipeStage visitGroupby(PerfQueryParser.GroupbyContext ctx) {
    String aggFun = ctx.aggFunc().getText();
    FoldConfigInfo fci = new FoldConfigInfo(ctx.columnList(),
                                            aggFun,
                                            aggFunCode.get(aggFun),
                                            aggFunSymTab.get(aggFun),
                                            stateVars.get(aggFun),
                                            fieldVars.get(aggFun));
    return new PipeStage(OperationType.GROUPBY, fci);
  }

  @Override public PipeStage visitZip(PerfQueryParser.ZipContext ctx) {
    ZipConfigInfo zci = new ZipConfigInfo();
    return new PipeStage(OperationType.JOIN, zci);
  }

  /// Override toString for helpful printing.
  @Override public String toString() {
    return queryToPipe.toString();
  }

  public HashMap<String, PipeStage> getQueryToPipe() {
    return queryToPipe;
  }
}
