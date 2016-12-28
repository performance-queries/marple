package edu.mit.needlstk;
import java.util.ArrayList;
import java.util.HashMap;

public class PredTree {
  /// node --> list of children nodes.
  public HashMap<Integer, ArrayList<Integer>> childMap;
  /// node --> parent node. If parent is -1, this node is the root.
  public HashMap<Integer, Integer> parentMap;
  private Integer rootId;

  public PredTree(Integer rootId) {
    this.childMap = new HashMap<Integer, ArrayList<Integer>>();
    this.parentMap = new HashMap<Integer, Integer>();
    this.rootId = rootId;
  }

  public void addNewPred(Integer predId) {
    assert(! childMap.containsKey(predId));
    childMap.put(predId, new ArrayList<Integer>());
    parentMap.put(predId, -1); // Initialize as root by default
  }

  public boolean addChildToParent(Integer childId, Integer parentId) {
    assert(childMap.containsKey(childId));
    assert(childMap.containsKey(parentId));
    if (! childMap.get(parentId).contains(childId)) {
      childMap.get(parentId).add(childId);
      parentMap.put(childId, parentId);
      return true;
    } else {
      return false;
    }
  }

  @Override public String toString() {
    return this.childMap.toString();
  }
}
