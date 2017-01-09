package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

public class MapConfigInfo extends PipeConfigInfo {
  private Integer numExprs;

  public MapConfigInfo(PerfQueryParser.ColumnListContext colList,
                       PerfQueryParser.ExprListContext exprList) {
    ArrayList<ThreeOpStmt> stmts = new ArrayList<>();
    usedFields = new HashSet<String>();
    symTab = new HashMap<String, AggFunVarType>();
    List<String> cols = ColumnExtractor.getColumns(colList);
    List<PerfQueryParser.ExprContext> exprs = ExprExtractor.getExprs(exprList);
    if (cols.size() != exprs.size()) {
      throw new RuntimeException("List sizes of columns and expressions should match"
                                 + " in map query!");
    }
    /// For each expression assignment:
    /// (1) generate ThreeOpCode
    /// (2) get the set of used variables
    /// (3) update the stage-local symbol table
    for (int i=0; i<cols.size(); i++) {
      String col = cols.get(i);
      AugExpr expr = new AugExpr(exprs.get(i));
      HashSet<String> usedVars = expr.getUsedVars();
      usedFields.addAll(usedVars);
      /// generate ThreeOpCode
      ThreeOpStmt stmt = new ThreeOpStmt(col, expr);
      stmts.add(stmt);
      /// Update the symbol table
      symTab.put(col, AggFunVarType.FIELD);
      for (String inputField: usedVars) {
        symTab.put(inputField, AggFunVarType.FIELD);
      }
    }
    this.setFields = new HashSet<String>(cols);
    this.code = new ThreeOpCode(new ArrayList<ThreeOpDecl>(), stmts);
    numExprs = exprs.size();
    initPrePostAmble();
  }

  public void addValidStmt(String queryId, String operandQueryId, boolean isOperandPktLog) {
    /// Set validity of the map result to the validity of the operand.
    /// TODO: this creates dead code if the operand is invalid,
    /// since we wouldn't need to evaluate any of the map expressions.
    AugPred operandValid;
    if (! isOperandPktLog) {
      operandValid = new AugPred(tmpTransformQueryId(operandQueryId));
    } else {
      operandValid = new AugPred(true);
    }
    ThreeOpStmt validStmt = new ThreeOpStmt(tmpTransformQueryId(queryId), operandValid);
    code.appendStmt(validStmt);
    addTmpOfField(queryId, false);
    if (! isOperandPktLog) {
      addTmpOfField(operandQueryId, true);
    }
  }
}
