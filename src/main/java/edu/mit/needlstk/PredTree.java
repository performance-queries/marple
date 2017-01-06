package edu.mit.needlstk;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Arrays;

public class PredTree {
  /// node --> list of children nodes.
  public HashMap<Integer, ArrayList<Integer>> childMap;
  /// node --> parent node. If parent is -1, this node is the root.
  public HashMap<Integer, Integer> parentMap;
  /// Root of the predicate tree
  private Integer rootId;
  /// Ancestry information, book-kept and checked recursively. Enables quick intersection
  /// checks. These are *strict* ancestors, meaning that a node is not an ancestor of itself.
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
      ancestors.get(childId).add(parentId);
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
    } else if (predA == predB) {
      return predA;
    } else { // no intersection
      return -1;
    }
  }

  /// Check if a node is contained in the predicate tree.
  public boolean contains(Integer predId) {
    return (this.childMap.containsKey(predId) &&
            this.parentMap.containsKey(predId) &&
            this.ancestors.containsKey(predId));
  }

  /// Get a set of nodes "covering" the subtree rooted at the ancestor, once predicate pred is
  /// removed.
  private HashSet<Integer> getFrontierNodes(Integer ancestor, Integer node) {
    assert (isAncestor(ancestor, node) || ancestor == node);
    HashSet<Integer> frontier = new HashSet<>();
    Integer currNode = node;
    while (currNode != ancestor) {
      ArrayList<Integer> siblings = this.childMap.get(this.parentMap.get(currNode));
      frontier.addAll(siblings);
      frontier.remove(currNode);
      currNode = this.parentMap.get(currNode);
    }
    return frontier;
  }

  /// Given a set of nodes, determine the least ancestor (defined by the tree hierarchy, with root
  /// being the highest ancestor) of a given node in the set, if any, and -1 otherwise. An element
  /// is defined to be an ancestor of itself for this function.
  private Integer getLeastAncestorInSet(HashSet<Integer> nodeSet, Integer node) {
    while (node != -1) {
      if (nodeSet.contains(node)) return node;
      node = this.parentMap.get(node);
    }
    return -1;
  }

  /// Help the maintenance of a "cover set" under a given predicate. This is used by PredHist to
  /// maintain coverage of an entire predicate in the process of breaking predicates up.
  public void adjustCoverSet(HashSet<Integer> coverSet, Integer pred) {
    if (! this.childMap.containsKey(pred)) {
      throw new RuntimeException("Predicate " + pred + " not found in tree ("
                                 + this.toString() + "); cannot adjust cover set.");
    }
    Integer leastAncestor = this.getLeastAncestorInSet(coverSet, pred);
    if (leastAncestor == -1) {
      throw new RuntimeException("There should be at least one ancestor of the predicate in the"
                                 + " cover set.");
    }
    coverSet.addAll(this.getFrontierNodes(leastAncestor, pred));
    coverSet.remove(leastAncestor);
  }

  @Override public String toString() {
    return this.childMap.toString();
  }

  public Integer getNodes() {
    return this.childMap.size();
  }
}
