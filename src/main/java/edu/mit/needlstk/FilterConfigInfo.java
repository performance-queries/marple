package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;

/// Generate code for filters in a pipeline stage
public class FilterConfigInfo extends PipeConfigInfo {
  private AugPred pred;

  public FilterConfigInfo(PerfQueryParser.PredicateContext ctx) {
    this.pred = new AugPred(ctx);
    this.usedFields = this.pred.getUsedVars();
    String predVar = "0_pred_test"; /// Name local to stage; no need to generate unique names.
    ArrayList<ThreeOpStmt> stmts = new ArrayList<>(Arrays.asList(new ThreeOpStmt(predVar, pred)));
    ArrayList<ThreeOpDecl> decls = new ArrayList<>(Arrays.asList(new ThreeOpDecl(ThreeOpCode.BOOL_WIDTH, predVar)));
    this.code = new ThreeOpCode(decls, stmts);
    this.symTab = new HashMap<String, AggFunVarType>();
    for (String inputField: usedFields) {
      symTab.put(inputField, AggFunVarType.FIELD);
    }
    this.setFields = new HashSet<>();
  }

  public void addValidStmt(String queryId, String operandQueryId, boolean isOperandPktLog) {
    AugPred operandValid;
    if (! isOperandPktLog) {
      operandValid = new AugPred(operandQueryId);
    } else {
      operandValid = new AugPred(true);
    }
    ThreeOpStmt validStmt = new ThreeOpStmt(queryId, operandValid.and(pred));
    this.code = new ThreeOpCode(new ArrayList<ThreeOpDecl>(),
                                Arrays.asList(validStmt));
    this.symTab.put(queryId, AggFunVarType.FIELD);
  }
}
