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

    /// A reference to the symbol to query parse tree from previous passes.
    private HashMap<String, ParserRuleContext> sym_tree_;

    /// A reference to the last assigned ID in the query program.
    private String last_assigned_id_;

    /// Constructor
    public GlobalAnalyzer(HashSet<Integer> all_switches,
			  HashMap<String, ParserRuleContext> sym_tree,
			  String last_assigned_id) {
	all_switches_ = all_switches;
	sym_tree_ = sym_tree;
	last_assigned_id_ = last_assigned_id;
    }

    /// Return OpLocation from underlying operators, which are inputs to the current operator.
    private OpLocation RecurseDeps(String stream) {
	/// Recursively get OpLocation information for the operand streams.
	/// But first, ensure that the input stream has been seen before. It's an assert instead of
	/// exception because the previous pass should have caught the error.
	OpLocation opl_input;
	if (! stream.equals("T")) {
	    ParserRuleContext subquery = sym_tree_.get(stream);
	    assert(subquery != null);
	    opl_input = visit(subquery);
	} else {
	    opl_input = new OpLocation();
	}
	return opl_input;
    }

    /// Test methods to check out some basic functionalities
    @Override public OpLocation visitFilter(perf_queryParser.FilterContext ctx) {
	HashSet<Integer> sw_set = new SwitchPredicateExtractor(all_switches_).visit(ctx.pred());
	OpLocation opl_input = RecurseDeps(ctx.stream().getText());
	/// Merge values from recursive call and current
	sw_set.retainAll(opl_input.getSwitchSet());
	if (opl_input.getStreamType() == StreamType.SINGLE_SWITCH_STREAM) {
	    return new OpLocation(sw_set, StreamType.SINGLE_SWITCH_STREAM);
	} else if (sw_set.size() > 1) {
	    return new OpLocation(sw_set, StreamType.MULTI_SWITCH_STREAM);
	} else {
	    return new OpLocation(sw_set, StreamType.SINGLE_SWITCH_STREAM);
	}
    }

    @Override public OpLocation visitProject(perf_queryParser.ProjectContext ctx) {
	return RecurseDeps(ctx.stream().getText());
    }

    private OpLocation foldHelper(perf_queryParser.Column_listContext ctx,
				  OpLocation opl_input,
				  String queryText) {
	Boolean per_switch_stream = new ColumnChecker(Fields.switch_hdr).visit(ctx);
	Boolean per_packet_stream = new ColumnChecker(Fields.packet_uid).visit(ctx);
	if (per_switch_stream) {
	    return new OpLocation(opl_input.getSwitchSet(), StreamType.SINGLE_SWITCH_STREAM);
	} else if (per_packet_stream) {
	    return new OpLocation(opl_input.getSwitchSet(), opl_input.getStreamType());
	} else {
	    throw new RuntimeException("Cannot perform a fold over multiple switches and multiple"
				       + " packets in query\n" + queryText);
	}
    }

    @Override public OpLocation visitRfold(perf_queryParser.RfoldContext ctx) {
	OpLocation opl_input = RecurseDeps(ctx.stream().getText());
	return foldHelper(ctx.column_list(), opl_input, ctx.getText());
    }

    @Override public OpLocation visitSfold(perf_queryParser.SfoldContext ctx) {
	OpLocation opl_input = RecurseDeps(ctx.stream().getText());
	return foldHelper(ctx.column_list(), opl_input, ctx.getText());
    }

    @Override public OpLocation visitJoin(perf_queryParser.JoinContext ctx) {
	return new OpLocation();
    }

    @Override public OpLocation visitProg(perf_queryParser.ProgContext ctx) {
	ParserRuleContext subquery = sym_tree_.get(last_assigned_id_);
	assert (subquery != null);
	return visit(subquery);
    }
}
