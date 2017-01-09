package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HashMap;

public class ZipConfigInfo extends PipeConfigInfo {
  private AugPred validPred;

  public ZipConfigInfo() {
    validPred = null;
    code = new ThreeOpCode();
    symTab = new HashMap<String, AggFunVarType>();
    setFields = new HashSet<String>();
    usedFields = new HashSet<String>();
    initPrePostAmble();
  }

  public void addValidStmt(String queryId, String operandQueryId, boolean isOperandPktLog) {
    /// Each operand coming in must be valid for the zip to be valid.
    AugPred operandValid;
    if (! isOperandPktLog) {
      operandValid = new AugPred(tmpTransformQueryId(operandQueryId));
    } else {
      operandValid = new AugPred(true);
    }
    if (validPred != null) {
      validPred = validPred.and(operandValid);
    } else {
      validPred = operandValid;
    }
    ThreeOpStmt validStmt = new ThreeOpStmt(tmpTransformQueryId(queryId), validPred);
    /// Update symbol table
    addTmpOfField(queryId, false);
    if (! isOperandPktLog) {
      addTmpOfField(operandQueryId, true);
    }
    this.code = new ThreeOpCode(new ArrayList<ThreeOpDecl>(),
                                Arrays.asList(validStmt));
  }
}
