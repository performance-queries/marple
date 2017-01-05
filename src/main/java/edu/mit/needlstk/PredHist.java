package edu.mit.needlstk;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

public class PredHist {
  /// HashMap tracking histories per predicate condition (predId -> hist)
  private HashMap<Integer, Integer> hists;

  public PredHist() {
    this.hists = new HashMap<Integer, Integer>();
  }

  /// Constructor for important case of a single history value
  public PredHist(Integer truePred, Integer defaultVal) {
    this.hists = new HashMap<Integer, Integer>();
    this.hists.put(truePred, defaultVal);
  }

  /// Construct directly through a hashmap
  public PredHist(HashMap<Integer, Integer> hists) {
    this.hists = hists;
  }

  public HashMap<Integer, Integer> getHists() {
    return this.hists;
  }

  /// merge into one history value, which is the maximum across all the predicates in the history
  public PredHist squash(Integer truePredId) {
    Integer squashedHist = this.hists.values().stream().
        reduce(0, (max, hist) -> Math.max(max, hist));
    return new PredHist(truePredId, squashedHist);
  }

  public boolean structuralEquals(PredHist other) {
    return other.getHists().equals(this.hists);
  }

  /// Given an outer predicate id, return only the relevant predicated history. If there are gaps in
  /// the existing predicated history with respect to the outer predicate, they are filled with the
  /// default history value provided, so that the following invariants hold:
  /// (1) union of predicates in the returned PredHist = predicate corresponding to outerPredId,
  /// (2) predicates in the returned PredHist are mutually disjoint.
  /// i.e., a set of "basis predicates" within outerPred.
  public PredHist getRelevantPredHist(Integer outerPredId, PredTree predTree, Integer defaultHist) {
    assert (predTree.contains(outerPredId));
    HashMap<Integer, Integer> relevantHist = new HashMap<>();
    HashSet<Integer> coveringPreds = new HashSet<>(Arrays.asList(outerPredId));
    for (Map.Entry<Integer, Integer> entry: this.hists.entrySet()) {
      Integer predId = entry.getKey();
      Integer hist = entry.getValue();
      Integer intersection = predTree.intersect(predId, outerPredId);
      if (intersection != -1) { // there is an intersection
        relevantHist.put(intersection, hist);
        predTree.adjustCoverSet(coveringPreds, intersection);
      }
    }
    /// Insert default history value for each of the other "covering" predicates.
    if (coveringPreds.size() > 0) {
      assert (defaultHist != -1); // Logic error: if no default history, then the entire space of
      /// outerPredId should be covered!
      for (Integer covering: coveringPreds) {
        relevantHist.put(covering, defaultHist);
      }
    }
    return new PredHist(relevantHist);
  }

  /// Helper to get the single history value stored in a history
  public Integer getSingletonHist() {
    assert (this.hists.size() == 1);
    return new ArrayList<Integer>(this.hists.values()).get(0);
  }

  /// Static function that finds a predicate-wise maximum of two histories
  public static PredHist getMaxHist(PredHist p1, PredHist p2, PredTree predTree) {
    HashMap<Integer, Integer> h1 = p1.getHists();
    HashMap<Integer, Integer> h2 = p2.getHists();
    HashMap<Integer, Integer> newH = new HashMap<>();
    for (Integer pred1: h1.keySet()) {
      for (Integer pred2: h2.keySet()) {
        Integer intersection = predTree.intersect(pred1, pred2);
        if (intersection != -1) {
          newH.put(intersection, Math.max(h1.get(pred1), h2.get(pred2)));
        }
      }
    } // end for each combination of predicates in histories p1 and p2
    return new PredHist(newH);
  }

  /// Static function that merges a new predicate history into this history.
  public void setHist(PredHist p, PredTree predTree) {
    HashMap<Integer, Integer> newHist = p.getHists();
    for (Integer newPred: newHist.keySet()) {
      HashSet<Integer> coverNew = new HashSet<>(Arrays.asList(newPred));
      for (Integer oldPred: this.hists.keySet()) {
        /// Various cases of intersection between the predicates
        if (oldPred == newPred) {
          this.hists.put(oldPred, newHist.get(oldPred));
        } else if (predTree.isAncestor(newPred, oldPred)) {
          this.hists.put(oldPred, newHist.get(newPred));
          predTree.adjustCoverSet(coverNew, oldPred);
        } else if (predTree.isAncestor(oldPred, newPred)) {
          this.hists.put(newPred, newHist.get(newPred));
          /// Break up old predicate into new disjoint pieces
          Integer oldHist = this.hists.get(oldPred);
          this.hists.remove(oldPred);
          HashSet<Integer> coverOld = new HashSet<>(Arrays.asList(oldPred));
          predTree.adjustCoverSet(coverOld, newPred);
          for (Integer coverPred: coverOld) {
            this.hists.put(coverPred, oldHist); // concurrent modification exception?
          }
          break;
        } else if (predTree.intersect(oldPred, newPred) == -1) {
          /// Do nothing.
        } else {
          assert (false); // Logic error. Predicate pairs should be in one of 4 categories
        }
      }
      /// Add whatever remains in coverNew to the history
      for (Integer coverPred: coverNew) {
        this.hists.put(coverPred, newHist.get(newPred));
      }
    }
  }

  @Override public String toString() {
    return this.hists.toString();
  }
}
