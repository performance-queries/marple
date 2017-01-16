package edu.mit.needlstk;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.HashSet;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;

/// Generic class for predicated state.
public class PredState<T> {
  /// HashMap tracking state per predicate condition (predId -> state). We use them mostly to track
  /// history-related information, so we call this mapping hists.
  protected HashMap<Integer, T> hists;

  public PredState() {
    this.hists = new HashMap<Integer, T>();
  }

  /// Constructor for important case of a single history value
  public PredState(Integer truePred, T defaultVal) {
    this.hists = new HashMap<>();
    this.hists.put(truePred, defaultVal);
  }

  /// Construct directly through a hashmap
  public PredState(HashMap<Integer, T> hists) {
    this.hists = hists;
  }

  public HashMap<Integer, T> getHists() {
    return this.hists;
  }

  public boolean structuralEquals(PredState<T> other) {
    return other.getHists().equals(this.hists);
  }

  /// Given an outer predicate id, return only the relevant predicated history. If there are gaps in
  /// the existing predicated history with respect to the outer predicate, they are filled with the
  /// default history value provided, so that the following invariants hold:
  /// (1) union of predicates in the returned PredHist = predicate corresponding to outerPredId,
  /// (2) predicates in the returned PredHist are mutually disjoint.
  /// i.e., a set of "basis predicates" within outerPred.
  public PredState<T> getRelevantPredHist(Integer outerPredId,
                                          PredTree predTree,
                                          T defaultHist,
                                          T defaultNoHistoryExists) {
    assert (predTree.contains(outerPredId));
    HashMap<Integer, T> relevantHist = new HashMap<>();
    HashSet<Integer> coveringPreds = new HashSet<>(Arrays.asList(outerPredId));
    for (Map.Entry<Integer, T> entry: this.hists.entrySet()) {
      Integer predId = entry.getKey();
      T hist = entry.getValue();
      Integer intersection = predTree.intersect(predId, outerPredId);
      if (intersection != -1) { // there is an intersection
        relevantHist.put(intersection, hist);
        predTree.adjustCoverSet(coveringPreds, intersection);
      }
    }
    /// Insert default history value for each of the other "covering" predicates.
    if (coveringPreds.size() > 0) {
      assert (! defaultHist.equals(defaultNoHistoryExists)); // Logic error: if no default history,
      /// then the entire space of outerPredId should be covered!
      for (Integer covering: coveringPreds) {
        relevantHist.put(covering, defaultHist);
      }
    }
    return new PredState<T>(relevantHist);
  }

  /// Helper to get the single history value stored in a history
  public T getSingletonHist() {
    assert (this.hists.size() == 1);
    return new ArrayList<T>(this.hists.values()).get(0);
  }

  /// Interface for lambda of (T, T) -> T
  public interface TwoArgOperator<U> {
    public U op(U a, U b);
  }

  public static <U> PredState<U> compareHist(PredState<U> p1,
                                             PredState<U>  p2,
                                             PredTree predTree,
                                             TwoArgOperator<U> taop) {
    HashMap<Integer, U> oldHist = new HashMap<>(p1.getHists()); // copy that gets modified.
    HashMap<Integer, U> newHist = p2.getHists();
    for (Integer newPred: newHist.keySet()) {
      HashSet<Integer> coverNew = new HashSet<>(Arrays.asList(newPred));
      for (Integer oldPred: oldHist.keySet()) {
        /// Various cases of intersection between the predicates
        U newHistVal = taop.op(oldHist.get(oldPred), newHist.get(newPred));
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
    return new PredState<U>(oldHist);
  }

  /// Incorporate a provided predicated history p into the current history.
  public void setHist(PredState<T> p, PredTree predTree) {
    PredState<T> newHist = compareHist(this, p, predTree, (a,b) -> b);
    this.hists = newHist.getHists();
  }

  @Override public String toString() {
    return this.hists.toString();
  }
}
