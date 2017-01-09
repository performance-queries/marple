package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.List;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;

public class FoldConfigInfo extends PipeConfigInfo {
  private String fnName;
  private List<String> keyFields;
  private List<String> stateArgs;
  private List<String> fieldArgs;
  private String outerPredId;
  
  public FoldConfigInfo(PerfQueryParser.ColumnListContext colList,
                        String aggFunc,
                        ThreeOpCode code,
                        HashMap<String, AggFunVarType> symTab,
                        List<String> stateArgs,
                        List<String> fieldArgs) {
    this.keyFields = ColumnExtractor.getColumns(colList);
    this.stateArgs = stateArgs;
    this.fieldArgs = fieldArgs;
    this.fnName = aggFunc;
    this.code = code;
    this.symTab = symTab;
    this.outerPredId = code.peekIdFirstDecl();
    this.setFields = new HashSet<String>(stateArgs);
    this.setFields.addAll(this.keyFields);
    this.usedFields = new HashSet<String>(fieldArgs);
    this.usedFields.addAll(this.keyFields);
    initPrePostAmble();
  }

  public void addValidStmt(String queryId, String operandQueryId, boolean isOperandPktLog) {
    ArrayList<ThreeOpStmt> newStmts = new ArrayList<ThreeOpStmt>();
    AugPred operandPred = isOperandPktLog ? (new AugPred(true)) : (new AugPred(tmpTransformQueryId(operandQueryId)));
    newStmts.add(new ThreeOpStmt(tmpTransformQueryId(queryId), new AugPred(false)));
    addTmpOfField(queryId, false);
    if (! isOperandPktLog) {
      addTmpOfField(operandQueryId, true);
    }
    for (ThreeOpStmt line: code.getStmts()) {
      /// Replace the "outermost predicate" of the function to operandPred.
      if (line.isPredAssign()) {
        if (line.getDefinedVar().equals(this.outerPredId)) {
          /// If this line assigns the outermost predicate, we should modify it.
          newStmts.add(new ThreeOpStmt(this.outerPredId, operandPred));
        } else {
          /// Add the line unmodified otherwise.
          newStmts.add(line);
        }
      } else if (line.isEmit()) {
        // The result from this stage is valid if two conditions are met.
        // (1) The predicate corresponding to the emit is true;
        // (2) the operand is valid.
        AugPred thisBranch = new AugPred(line.getEmitPred());
        newStmts.add(new ThreeOpStmt(tmpTransformQueryId(queryId),
                                     thisBranch.or(new AugPred(tmpTransformQueryId(queryId)))));
        newStmts.add(line);
      } else {
        /// Add the line unmodified otherwise.
        newStmts.add(line);
      }
    }
    code = new ThreeOpCode(code.getDecls(), newStmts);
  }

  public List<String> getKeyFields() {
    return keyFields;
  }

  public List<String> getStateArgs() {
    return stateArgs;
  }
}
