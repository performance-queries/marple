import java.util.ArrayList;
public class Operation {
  public OperationType opcode = OperationType.UNDEFINED;
  public ArrayList<String> operands = new ArrayList<String>();
  public Operation(OperationType t_opcode, ArrayList<String> t_operands) {
    opcode = t_opcode;
    operands = t_operands;
  }
  public Operation() {}
  @Override public String toString() {
   return "{ Opcode: " + opcode + " operands: " + operands + "}";
  }
}
