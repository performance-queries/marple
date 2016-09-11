import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.lang.RuntimeException;

/// Determine which queries run where, and (conceptually) which set of packets
/// each instance of the query processes. This results in an annotated query
/// expression tree.
public class GlobalAnalyzer extends perf_queryBaseVisitor<OpLocation> {
    /// A reference to the set of switches globally in the network
    private HashSet<Integer> all_switches_;

    /// Internal "value expression" detection instance
    private SwitchPredicateExtractor switch_extractor_;

    /// Constructor
    public GlobalAnalyzer(HashSet<Integer> all_switches) {
	all_switches_ = all_switches;
	switch_extractor_ = new SwitchPredicateExtractor(all_switches_);
    }

    /// Test methods to check out some basic functionalities
    @Override public OpLocation visitFilter(perf_queryParser.FilterContext ctx) {
	HashSet<Integer> sw_set = switch_extractor_.visit(ctx.pred());
	OpLocation filter_opl = new OpLocation(predicated_sw, StreamType.SINGLE_SWITCH_STREAM);
	System.out.println("This filter restricts the stream to the following switches:");
	System.out.println(sw_set.toString());
	/* TODO: Recursive call to input stream/relation's OpLocation, and merging of the two
	OpLocations. */
	return filter_opl;
    }
}
