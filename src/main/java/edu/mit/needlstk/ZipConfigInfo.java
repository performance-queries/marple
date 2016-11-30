package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.List;
import java.util.ArrayList;

public class ZipConfigInfo implements PipeConfigInfo {
  private List<ThreeOpStmt> code;
  private AugPred validPred;

  public ZipConfigInfo() {
    validPred = new AugPred(true);
    code = new ArrayList<>();
  }

  public String getP4() {
    String res = "";
    for (ThreeOpStmt stmt: code) {
      res += stmt.print();
      res += "\n";
    }
    return res;
  }

  public List<ThreeOpStmt> getCode() {
    return code;
  }

  public ThreeOpStmt addValidStmt(String queryId, String operandQueryId) {
    /// Each operand coming in must be valid for the zip to be valid.
    validPred = validPred.and(new AugPred(operandQueryId));
    ThreeOpStmt validStmt = new ThreeOpStmt(queryId, validPred);
    this.code = new ArrayList<>(Arrays.asList(validStmt));
    return validStmt;
  }
}
