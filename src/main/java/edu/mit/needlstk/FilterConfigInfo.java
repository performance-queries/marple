package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/// Generate code for filters in a pipeline stage
public class FilterConfigInfo implements PipeConfigInfo {
  private AugPred pred;
  private List<ThreeOpStmt> code;
  private Integer incr; // It doesn't matter what the value of this is.

  public FilterConfigInfo(PerfQueryParser.PredicateContext ctx) {
    this.pred = new AugPred(ctx);
    String predVar = "0_pred_test"; /// Name local to stage; no need to generate unique names.
    this.code = new ArrayList<>(Arrays.asList(new ThreeOpStmt(predVar, pred)));
  }

  public String getP4() {
    return code.get(0).print() + "\n";
  }

  public List<ThreeOpStmt> getCode() {
    return code;
  }

  public void addValidStmt(String queryId, String operandQueryId) {
    AugPred operandValid = new AugPred(operandQueryId);
    ThreeOpStmt validStmt = new ThreeOpStmt(queryId, operandValid.and(pred));
    this.code = new ArrayList<>(Arrays.asList(validStmt));
  }
}
