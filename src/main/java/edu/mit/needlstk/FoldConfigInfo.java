package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.List;
import java.util.ArrayList;

public class FoldConfigInfo implements PipeConfigInfo {
  private String fnName;
  private List<String> keyFields;
  private List<String> stateArgs;
  private List<String> fieldArgs;
  private List<ThreeOpStmt> code;
  
  public FoldConfigInfo(PerfQueryParser.ColumnListContext colList,
                        String aggFunc,
                        ThreeOpCode code,
                        List<String> stateArgs,
                        List<String> fieldArgs) {
    this.keyFields = ColumnExtractor.getColumns(colList);
    this.stateArgs = stateArgs;
    this.fieldArgs = fieldArgs;
    this.fnName = aggFunc;
    this.code = code.getStmts();
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

  public void addValidStmt(String queryId, String operandQueryId) {
    /// TODO: dummy validity statement for testing purposes
    ThreeOpStmt validStmt = new ThreeOpStmt(queryId, new AugPred(true));
    code.add(validStmt);
  }
}
