package edu.mit.needlstk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class PredTree {
  /// node --> list of children nodes.
  public HashMap<Integer, ArrayList<Integer>> childMap;
  /// node --> parent node. If parent is -1, this node is the root.
  public HashMap<Integer, Integer> parentMap;
  /// Root of the predicate tree
  private Integer rootId;
  /// Ancestry information, book-kept and checked recursively. Enables quick intersection checks.
  private HashMap<Integer, HashSet<Integer>> ancestors;

  public PredTree(Integer rootId) {
    this.childMap = new HashMap<Integer, ArrayList<Integer>>();
    this.parentMap = new HashMap<Integer, Integer>();
    this.ancestors = new HashMap<Integer, HashSet<Integer>>();
    this.rootId = rootId;
  }

  public void addNewPred(Integer predId) {
    assert(! childMap.containsKey(predId));
    childMap.put(predId, new ArrayList<Integer>());
    parentMap.put(predId, -1); // Initialize as root by default
    ancestors.put(predId, new HashSet<Integer>());
  }

  public boolean addChildToParent(Integer childId, Integer parentId) {
    assert(childMap.containsKey(childId));
    assert(childMap.containsKey(parentId));
    if (! childMap.get(parentId).contains(childId)) {
      childMap.get(parentId).add(childId);
      parentMap.put(childId, parentId);
      ancestors.get(childId).addAll(ancestors.get(parentId));
      return true;
    } else {
      return false;
    }
  }

  /// Track whether the first argument is an ancestor of the second in the predicate tree.
  public boolean isAncestor(Integer possibleAncestorId, Integer childId) {
    return ancestors.get(childId).contains(possibleAncestorId);
  }

  /// Intersect two predicates in the the predicate tree using ancestry information
  /// Returns the ID of the intersecting predicate if the two predicates intersect, else -1.
  /// Because the predicates divide up the space of possibilities in the form of a tree, there are
  /// only those two possibilities.
  public Integer intersect(Integer predA, Integer predB) {
    if (isAncestor(predA, predB)) {
      return predB;
    } else if (isAncestor(predB, predA)) {
      return predA;
    } else { // no intersection
      return -1;
    }
  }

  @Override public String toString() {
    return this.childMap.toString();
  }
}
