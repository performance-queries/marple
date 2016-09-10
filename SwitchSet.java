import java.util.HashSet;

/// Create a set representing the set of switch identifiers in the network.
public class SwitchSet {
    private HashSet<Integer> all_switches_ = new HashSet<Integer>();

    public SwitchSet(int net_size) {
	for (int i=0; i < net_size; i++) {
	    all_switches_.add(i);
	}
    }

    public HashSet<Integer> get_switches() {
	return all_switches_;
    }
}
