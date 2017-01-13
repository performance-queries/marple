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

  public static PredAST getSubstAST(PredAST p1, PredAST p2, PredTree predTree,
                                    String definedIdent,
                                    AugExprVer.VarWithVersion used) {
    String usedIdent = used.var;
    Integer usedVersion = used.version;
    return new PredAST(compareHist((PredState<AugExprVer>)p1,
                                   (PredState<AugExprVer>)p2,
                                   predTree,
                                   (a, b) -> AugExprVer.subst(
                                       a, b, usedIdent, usedVersion)));
  }
}
