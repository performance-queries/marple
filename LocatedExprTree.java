import java.util.ArrayList;

class LocatedExprTree extends ExprTree {
  private OpLocation opl_;
  private ArrayList<LocatedExprTree> loc_operands;

  public LocatedExprTree(OperationType t_opcode, OpLocation opl,
      		   ArrayList<LocatedExprTree> t_operands) {
    super(t_opcode);
    opl_ = opl;
    loc_operands = t_operands;
  }

  public LocatedExprTree(OperationType t_opcode, OpLocation opl) {
      super(t_opcode);
    opl_ = opl;
    loc_operands = new ArrayList<LocatedExprTree>();
  }

  public OpLocation opl() {
    return opl_;
  }

  @Override public String toString() {
    if (loc_operands.size() == 0) return opcode.toString();

    String ret = opcode + "(";
    for (int i = 0; i < loc_operands.size(); i++) {
      ret += loc_operands.get(i).toString() + ",";
    }
    ret = ret.substring(0, ret.length() - 1) + ")";
    return ret;
  }

  public String node_label() {
    return opcode + Integer.toString(uid) + " ";
  }

  public String edge_label() {
    return opl_.toConciseString();
  }

  @Override public String dot_edges() {
    String ret = "";
    if (loc_operands.size() != 0) {
      for (int i = 0; i < loc_operands.size(); i++) {
      ret += node_label() + " -> " + loc_operands.get(i).node_label()
          + " [label=\"" + loc_operands.get(i).edge_label() + "\"];\n"
          + loc_operands.get(i).dot_edges();
      }
    }
    return ret;
  }
}
