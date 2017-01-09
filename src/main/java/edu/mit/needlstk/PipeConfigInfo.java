package edu.mit.needlstk;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

public abstract class PipeConfigInfo {
  protected ThreeOpCode code;
  protected HashMap<String, AggFunVarType> symTab;
  protected HashSet<String> setFields;
  protected HashSet<String> usedFields;
  /// Preamble and postamble for function-level variables to packet meta variables
  protected ThreeOpCode preamble;
  protected ThreeOpCode postamble;

  public static String tmpTransformQueryId(String queryId) {
    return "_tmp_" + queryId + "_valid";
  }

  public static String fieldTransformQueryId(String queryId) {
    return "_" + queryId + "_valid";
  }

  protected void addTmpOfField(String tmp, boolean readOffField) {
    String tmpId = tmpTransformQueryId(tmp);
    String fieldId = fieldTransformQueryId(tmp);
    if (! symTab.containsKey(tmpId)) { // check that we didn't already add this field.
      // Add new declaration for temporary bool variables of packet meta fields
      this.preamble.addDecl(new ThreeOpDecl(P4Printer.BOOL_WIDTH, P4Printer.BOOL_TYPE, tmpId));
      // if the tmp reads off a field, then add a preamble stmt:
      // tmpId = (fieldId == 1w1)
      if (readOffField) {
        this.preamble.appendStmt(new ThreeOpStmt(tmpId, new AugPred(
            AugPred.AugPredType.PRED_EQ,
            new ArrayList<AugExpr>(Arrays.asList(
                new AugExpr(fieldId),
                new AugExpr(1, 1))))));
      }
      // add a postamble stmt to write back the tmp into a packet field:
      // field = tmpId ? 1w1: 1w0
      if (! readOffField) {
        this.postamble.appendStmt(new ThreeOpStmt(fieldId,
                                                  tmpId,
                                                  new AugExpr(1, 1),
                                                  new AugExpr(0, 1)));
      }
      symTab.put(tmpId, AggFunVarType.FN_VAR);
      symTab.put(fieldId, AggFunVarType.FIELD);
    }
  }

  public String getP4() {
    String res = "// Preamble\n";
    res += this.preamble.getP4(symTab);
    res += "// Function body\n";
    res += code.getP4(symTab);
    res += "// Postamble\n";
    res += this.postamble.getP4(symTab);
    return res;
  }

  public String getDomino() {
    return code.getDomino(symTab);
  }

  public String print() {
    String res = "// Preamble\n";
    res += this.preamble.print();
    res += "// Function body\n";
    res += code.print();
    res += "// Postamble\n";
    res += this.postamble.print();
    return res;
  }

  public HashMap<String, AggFunVarType> getSymTab() {
    return symTab;
  }

  public HashSet<String> getSetFields() {
    return setFields;
  }

  public HashSet<String> getUsedFields() {
    return usedFields;
  }

  public HashSet<String> getFieldsOfType(AggFunVarType typ) {
    HashSet<String> fields = new HashSet<>();
    for (Map.Entry<String, AggFunVarType> entry: symTab.entrySet()) {
      if (entry.getValue() == typ) {
        fields.add(entry.getKey());
      }
    }
    return fields;
  }

  public HashSet<String> getPacketFields() {
    return getFieldsOfType(AggFunVarType.FIELD);
  }

  public HashSet<String> getRegisters() {
    return getFieldsOfType(AggFunVarType.STATE);
  }

  public HashSet<String> getNonRegisters() {
    HashSet<String> nonRegisters = new HashSet<>();
    nonRegisters.addAll(getFieldsOfType(AggFunVarType.FIELD));
    nonRegisters.addAll(getFieldsOfType(AggFunVarType.PRED_VAR));
    nonRegisters.addAll(getFieldsOfType(AggFunVarType.FN_VAR));
    return nonRegisters;
  }

  public HashSet<String> getAllFields() {
    return new HashSet<String>(symTab.keySet());
  }

  public abstract void addValidStmt(String q, String oq, boolean opT);

  protected void initPrePostAmble() {
    this.preamble = new ThreeOpCode();
    this.postamble = new ThreeOpCode();
  }
}
