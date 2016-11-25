package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.TerminalNode;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Arrays;

public class AggFunParamExtractor extends PerfQueryBaseVisitor<List<String>> {
  private HashMap<String, List<String>> stateVars;
  private HashMap<String, List<String>> fieldVars;
  
  /// Default constructor
  public AggFunParamExtractor() {
    stateVars = new HashMap<>();
    fieldVars = new HashMap<>();
  }

  public List<String> visitAggFun(PerfQueryParser.AggFunContext ctx) {
    List<String> states  = visit(ctx.stateList());
    List<String> fields = visit(ctx.columnList());
    String aggFun = ctx.aggFunc().getText();
    stateVars.put(aggFun, states);
    fieldVars.put(aggFun, fields);
    return new ArrayList<String>();
  }

  public List<String> visitStateList(PerfQueryParser.StateListContext ctx) {
    List<String> argsList = new ArrayList<>();
    if(ctx.state() != null) {
      argsList.add(ctx.state().getText());
      for (PerfQueryParser.StateWithCommaContext sctx: ctx.stateWithComma()) {
        argsList.add(sctx.state().getText());
      }
    }
    return argsList;
  }

  public List<String> visitOneColsList(PerfQueryParser.OneColsListContext ctx) {
    return new ArrayList<>(Arrays.asList(ctx.column().getText()));
  }

  public List<String> visitNoColsList(PerfQueryParser.NoColsListContext ctx) {
    return new ArrayList<>();
  }

  public List<String> visitMulColsList(PerfQueryParser.MulColsListContext ctx) {
    List<String> argsList = new ArrayList<>();
    argsList.add(ctx.column().getText());    
    for (PerfQueryParser.ColumnWithCommaContext cctx: ctx.columnWithComma()) {
      argsList.add(cctx.column().getText());
    }
    return argsList;
  }

  public HashMap<String, List<String>> getStateVars() {
    return stateVars;
  }

  public HashMap<String, List<String>> getFieldVars() {
    return fieldVars;
  }
}
