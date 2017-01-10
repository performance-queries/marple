package edu.mit.needlstk;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

public class PredAST extends PredState<AugExprVer> {
  public PredAST() {
    super();
  }

  /// Constructor from super type
  public PredAST(PredState<AugExprVer> p) {
    this.hists = p.getHists();
  }
}
