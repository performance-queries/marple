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
    validPred = new AugPred(true);
    code = new ThreeOpCode();
    symTab = new HashMap<String, AggFunVarType>();
    setFields = new HashSet<String>();
    usedFields = new HashSet<String>();
  }

  public void addValidStmt(String queryId, String operandQueryId, boolean isOperandPktLog) {
    /// Each operand coming in must be valid for the zip to be valid.
    AugPred operandValid;
    if (! isOperandPktLog) {
      operandValid = new AugPred(operandQueryId);
    } else {
      operandValid = new AugPred(true);
    }
    validPred = validPred.and(operandValid);
    ThreeOpStmt validStmt = new ThreeOpStmt(queryId, validPred);
    /// Update symbol table
    symTab.put(queryId, AggFunVarType.FIELD);
    for (String inputField: validPred.getUsedVars()) {
      symTab.put(inputField, AggFunVarType.FIELD);
    }
    this.code = new ThreeOpCode(new ArrayList<ThreeOpDecl>(),
                                Arrays.asList(validStmt));
  }
}
