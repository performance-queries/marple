package edu.mit.needlstk;
import java.util.List;
import java.util.HashSet;
import java.util.HashMap;
import java.util.Map;

public abstract class PipeConfigInfo {
  protected ThreeOpCode code;
  protected HashMap<String, AggFunVarType> symTab;
  protected HashSet<String> setFields;
  protected HashSet<String> usedFields;

  public String getP4() {
    return code.getP4(symTab);
  }

  public String getDomino() {
    return code.getDomino(symTab);
  }

  public String print() {
    return code.print();
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
}
