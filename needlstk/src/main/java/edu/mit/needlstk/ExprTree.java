package edu.mit.needlstk;
import java.util.ArrayList;
class ExprTree {
  protected static int uidCount = 0;
  protected OperationType opcode;
  private ArrayList<ExprTree> operands;
  protected int uid;
  public ExprTree(OperationType tOpcode) {
    opcode = tOpcode;
    operands = new ArrayList<ExprTree>();
    uid = uidCount;
    uidCount++;
  }

  public ExprTree(OperationType tOpcode, ArrayList<ExprTree> tOperands) {
    opcode = tOpcode;
    operands = tOperands;
    uid = uidCount;
    uidCount++;
  }

  public String toString() {
    if (operands.size() == 0) return opcode.toString();
 
    String ret = opcode + "(";
    assert(operands.size() >= 1);
    for (int i = 0; i < operands.size(); i++) {
      ret += (operands.get(i).toString()) + ",";
    }
    ret = ret.substring(0, ret.length() - 1) + ")";
    return ret;
  }

  public String dotOutput() {
    return "digraph tree {" + dotEdges() + "}";
  }

  public String dotEdges() {
    String ret = "";
    if (operands.size() != 0) {
      for (int i = 0; i < operands.size(); i++) {
        ret += opcode + Integer.toString(this.uid) +
               " -> " +
               operands.get(i).opcode + Integer.toString(operands.get(i).uid)
               + ";" +
               operands.get(i).dotEdges();
      }
    }
    return ret;
  }
}
