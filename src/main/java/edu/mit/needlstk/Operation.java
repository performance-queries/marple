package edu.mit.needlstk;
import java.util.ArrayList;
public class Operation {
  public OperationType opcode = OperationType.UNDEFINED;
  public ArrayList<String> operands = new ArrayList<String>();
  public Operation(OperationType tOpcode, ArrayList<String> tOperands) {
    opcode = tOpcode;
    operands = tOperands;
  }
  public Operation() {}
  @Override public String toString() {
   return "{ Opcode: " + opcode + " operands: " + operands + "}";
  }
}
