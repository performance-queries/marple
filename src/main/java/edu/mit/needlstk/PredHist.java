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

  /// Interface for lambda of (Integer, Integer) -> Integer
  public interface TwoArgIntOperator {
    public Integer op(Integer a, Integer b);
  }

  public static PredHist compareHist(PredHist p1,
                                     PredHist p2,
                                     PredTree predTree,
                                     TwoArgIntOperator taop) {
    HashMap<Integer, Integer> oldHist = new HashMap<>(p1.getHists()); // copy that gets modified.
    HashMap<Integer, Integer> newHist = p2.getHists();
    for (Integer newPred: newHist.keySet()) {
      HashSet<Integer> coverNew = new HashSet<>(Arrays.asList(newPred));
      for (Integer oldPred: oldHist.keySet()) {
        /// Various cases of intersection between the predicates
        Integer newHistVal = taop.op(oldHist.get(oldPred), newHist.get(newPred));
        if (oldPred == newPred) {
          oldHist.put(oldPred, newHistVal);
          coverNew.clear();
        } else if (predTree.isAncestor(newPred, oldPred)) {
          oldHist.put(oldPred, newHistVal);
          predTree.adjustCoverSet(coverNew, oldPred);
        } else if (predTree.isAncestor(oldPred, newPred)) {
          oldHist.put(newPred, newHistVal);
          /// Break up old predicate into new disjoint pieces
          HashSet<Integer> coverOld = new HashSet<>(Arrays.asList(oldPred));
          predTree.adjustCoverSet(coverOld, newPred);
          for (Integer coverPred: coverOld) {
            oldHist.put(coverPred, oldHist.get(oldPred));
          }
          oldHist.remove(oldPred);
          /// NewPred is completely covered; clear the cover set and break.
          coverNew.clear();
          break;
        } else if (predTree.intersect(oldPred, newPred) == -1) {
          /// Do nothing.
        } else {
          assert (false); // Logic error. Predicate pairs should be in one of 4 categories
        }
      }
      /// Add whatever remains in coverNew to the history
      for (Integer coverPred: coverNew) {
        oldHist.put(coverPred, newHist.get(newPred));
      }
    }
    return new PredHist(oldHist);
  }

  /// Incorporate a provided predicated history p into the current history.
  public void setHist(PredHist p, PredTree predTree) {
    PredHist newHist = compareHist(this, p, predTree, (a,b) -> b);
    this.hists = newHist.getHists();
  }

  /// Static function that finds a predicate-wise maximum of two histories
  public static PredHist getMaxHist(PredHist p1, PredHist p2, PredTree predTree) {
    return compareHist(p1, p2, predTree, (a,b) -> Math.max(a,b));
  }

  @Override public String toString() {
    return this.hists.toString();
  }
}
