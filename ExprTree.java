import java.util.ArrayList;
class ExprTree {
  private static int uid_count = 0;
  private OperationType opcode;
  private ArrayList<ExprTree> operands;
  private int uid;
  public ExprTree(OperationType t_opcode) {
    opcode = t_opcode;
    operands = new ArrayList<ExprTree>();
    uid = uid_count;
    uid_count++;
  }

  public ExprTree(OperationType t_opcode, ArrayList<ExprTree> t_operands) {
    opcode = t_opcode;
    operands = t_operands;
    uid = uid_count;
    uid_count++;
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

  public String dot_output() {
    return "digraph tree {" + dot_edges() + "}";
  }

  public String dot_edges() {
    String ret = "";
    if (operands.size() != 0) {
      for (int i = 0; i < operands.size(); i++) {
        ret += opcode + Integer.toString(this.uid) +
               " -> " +
               operands.get(i).opcode + Integer.toString(operands.get(i).uid)
               + ";" +
               operands.get(i).dot_edges();
      }
    }
    return ret;
  }
}
