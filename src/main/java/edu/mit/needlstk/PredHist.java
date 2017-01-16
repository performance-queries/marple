package edu.mit.needlstk;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

public class PredHist extends PredState<Integer> {
  public PredHist() {
    super();
  }

  /// Constructor from super type
  public PredHist(PredState<Integer> p) {
    this.hists = p.getHists();
  }

  /// Constructor for important case of a single history value
  public PredHist(Integer truePred, Integer defaultVal) {
    super(truePred, defaultVal);
  }

  /// merge into one history value, which is the maximum across all the predicates in the history
  public PredHist squash(Integer truePredId) {
    Integer squashedHist = this.hists.values().stream().
        reduce(0, (max, hist) -> Math.max(max, hist));
    return new PredHist(truePredId, squashedHist);
  }

  /// Static function that finds a predicate-wise maximum of two histories
  public static PredHist getMaxHist(PredHist p1, PredHist p2, PredTree predTree) {
    return new PredHist(compareHist((PredState<Integer>)p1,
                                    (PredState<Integer>)p2,
                                    predTree,
                                    (a,b) -> Math.max(a,b)));
  }
}
