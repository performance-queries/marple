package edu.mit.needlstk;

public class PredTree {
  public AugPred pred;
  public PredTree trueChild;
  public PredTree falseChild;

  public PredTree() {
    pred = new AugPred(true);
    trueChild = null;
    falseChild = null;
  }

  public PredTree(AugPred pred, PredTree trueChild, PredTree falseChild) {
    this.pred = pred;
    this.trueChild = trueChild;
    this.falseChild = falseChild;
  }

  public PredTree(AugPred pred) {
    this.pred = pred;
    this.trueChild = null;
    this.falseChild = null;
  }
}
