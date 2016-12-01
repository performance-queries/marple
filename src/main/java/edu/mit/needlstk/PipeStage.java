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
    res += "\n";
    res += configInfo.getP4();
    res += "\n";
    return res;
  }

  public PipeConfigInfo getConfigInfo() {
    return configInfo;
  }
}
