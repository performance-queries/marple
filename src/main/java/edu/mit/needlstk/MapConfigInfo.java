package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.List;
import java.util.ArrayList;

public class MapConfigInfo implements PipeConfigInfo {
  private List<ThreeOpStmt> code;
  private Integer numExprs;

  public MapConfigInfo(PerfQueryParser.ColumnListContext colList,
                       PerfQueryParser.ExprListContext exprList) {
    code = new ArrayList<>();
    List<String> cols = ColumnExtractor.getColumns(colList);
    List<PerfQueryParser.ExprContext> exprs = ExprExtractor.getExprs(exprList);
    if (cols.size() != exprs.size()) {
      throw new RuntimeException("List sizes of columns and expressions should match"
                                 + " in map query!");
    }
    for (int i=0; i<cols.size(); i++) {
      ThreeOpStmt stmt = new ThreeOpStmt(cols.get(i), new AugExpr(exprs.get(i)));
      code.add(stmt);
    }
    numExprs = exprs.size();
  }

  public String getP4() {
    String res = "";
    for (ThreeOpStmt frag: code) {
      res += frag.print();
      res += "\n";
    }
    return res;
  }

  public List<ThreeOpStmt> getCode() {
    return code;
  }

  public void addValidStmt(String queryId, String operandQueryId) {
    /// Set validity of the map result to the validity of the operand.
    /// TODO: this creates dead code if the operand is invalid,
    /// since we wouldn't need to evaluate any of the map expressions.
    AugPred operandValid = new AugPred(operandQueryId);
    ThreeOpStmt validStmt = new ThreeOpStmt(queryId, operandValid);
    code.add(validStmt);
  }
}
