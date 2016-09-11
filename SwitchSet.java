import java.util.HashSet;

/// Create a set representing the set of switch identifiers in the network.
public class SwitchSet {
    /// The set of all switch identifiers in the network.
    private HashSet<Integer> all_switches_ = new HashSet<Integer>();

    /// An integer denoting the number of switches.
    private Integer switch_count_ = 20;

    /// A temporary construction for the set of network switch identifiers.
    public SwitchSet() {
	for (int i=0; i < switch_count_; i++) {
	    all_switches_.add(i);
	}
    }

    public HashSet<Integer> getSwitches() {
	return all_switches_;
    }
}
