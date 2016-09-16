package edu.mit.needlstk;
import java.util.HashSet;

/// Create a set representing the set of switch identifiers in the network.
public class SwitchSet {
  /// The set of all switch identifiers in the network.
  private HashSet<Integer> allSwitches = new HashSet<Integer>();

  /// An integer denoting the number of switches.
  private Integer switchCount = 20;

  /// A temporary construction for the set of network switch identifiers.
  public SwitchSet() {
    for (int i=0; i < switchCount; i++) {
      allSwitches.add(i);
    }
  }

  public HashSet<Integer> getSwitches() {
    return allSwitches;
  }
}
