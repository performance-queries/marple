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

    /// TODO: The values in this default constructor don't make much sense. It should be removed
    /// once all visitors that must output OpLocation instantiate it using the argumented
    /// constructor.
    public OpLocation() {
	switch_set_ = new HashSet<Integer>();
	stream_type_ = StreamType.MULTI_SWITCH_STREAM;
    }

    public HashSet<Integer> get_switch_set() {
	return switch_set_;
    }

    public StreamType get_stream_type() {
	return stream_type_;
    }
}
