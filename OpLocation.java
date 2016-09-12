import java.util.HashSet;

/// A class to annotate operators with (i) the switches where they look at their streaming inputs;
/// and (ii) conceptually the type of stream inputs they are operating on.
public class OpLocation {
    public HashSet<Integer> switch_set_;
    public StreamType stream_type_;

    public OpLocation(HashSet<Integer> switch_set, StreamType stream_type) {
	switch_set_ = switch_set;
	stream_type_ = stream_type;
    }

    public OpLocation() {
	switch_set_ = new SwitchSet().getSwitches();
	stream_type_ = StreamType.MULTI_SWITCH_STREAM;
    }

    public HashSet<Integer> getSwitchSet() {
	return switch_set_;
    }

    public StreamType getStreamType() {
	return stream_type_;
    }

    public String toString() {
	return "Switch set:" + switch_set_.toString() + "\nStream type:" + stream_type_.toString();
    }
}
