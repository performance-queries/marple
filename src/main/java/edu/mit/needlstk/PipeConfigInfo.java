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

  public abstract void addValidStmt(String q, String oq, boolean opT);
}
