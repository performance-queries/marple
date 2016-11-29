package edu.mit.needlstk;

public class PipeStage {
  private OperationType op;
  private PipeConfigInfo configInfo;

  public PipeStage(OperationType op, PipeConfigInfo configInfo) {
    this.op = op;
    this.configInfo = configInfo;
  }

  @Override public String toString() {
    String res = this.op.toString();
    res += " ";
    res += configInfo.getP4();
    return res;
  }
}
