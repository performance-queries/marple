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
public class GlobalAnalyzer extends perf_queryBaseVisitor<LocatedExprTree> {
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
    private LocatedExprTree RecurseDeps(String stream) {
	/// Recursively get OpLocation information for the operand streams.
	/// But first, ensure that the input stream has been seen before. It's an assert instead of
	/// exception because the previous pass should have caught the error.
	LocatedExprTree let_input;
	if (! stream.equals("T")) {
	    ParserRuleContext subquery = sym_tree_.get(stream);
	    assert(subquery != null);
	    let_input = visit(subquery);
	} else {
	    let_input = new LocatedExprTree(OperationType.PKTLOG, new OpLocation());
	}
	return let_input;
    }

    private ArrayList<LocatedExprTree> singletonList(LocatedExprTree t) {
	ArrayList<LocatedExprTree> children = new ArrayList<LocatedExprTree>();
	children.add(t);
	return children;
    }

    /// Test methods to check out some basic functionalities
    @Override public LocatedExprTree visitFilter(perf_queryParser.FilterContext ctx) {
	HashSet<Integer> sw_set = new SwitchPredicateExtractor(all_switches_).visit(ctx.pred());
	LocatedExprTree let_input = RecurseDeps(ctx.stream().getText());
	OpLocation opl_input = let_input.opl();
	/// Merge values from recursive call and current
	sw_set.retainAll(opl_input.getSwitchSet());
	OpLocation opl_output;
	if (opl_input.getStreamType() == StreamType.SINGLE_SWITCH_STREAM) {
	    opl_output = new OpLocation(sw_set, StreamType.SINGLE_SWITCH_STREAM);
	} else if (sw_set.size() > 1) {
	    opl_output = new OpLocation(sw_set, StreamType.MULTI_SWITCH_STREAM);
	} else {
	    opl_output = new OpLocation(sw_set, StreamType.SINGLE_SWITCH_STREAM);
	}
	return new LocatedExprTree(OperationType.FILTER, opl_output, singletonList(let_input));
    }

    @Override public LocatedExprTree visitProject(perf_queryParser.ProjectContext ctx) {
	LocatedExprTree let_input = RecurseDeps(ctx.stream().getText());
	OpLocation opl_output = let_input.opl();
	return new LocatedExprTree(OperationType.PROJECT, opl_output, singletonList(let_input));
    }

    private LocatedExprTree foldHelper(String stream_name,
				       perf_queryParser.Column_listContext ctx,
				       String queryText,
				       OperationType opcode) {
	LocatedExprTree let_input = RecurseDeps(stream_name);
	OpLocation opl_input = let_input.opl();
	Boolean per_switch_stream = new ColumnChecker(Fields.switch_hdr).visit(ctx);
	Boolean per_packet_stream = new ColumnChecker(Fields.packet_uid).visit(ctx);
	OpLocation opl_output;
	if (per_switch_stream) {
	    opl_output = new OpLocation(opl_input.getSwitchSet(), StreamType.SINGLE_SWITCH_STREAM);
	} else if (per_packet_stream) {
	    opl_output = new OpLocation(opl_input.getSwitchSet(), opl_input.getStreamType());
	} else {
	    throw new RuntimeException("Cannot perform a fold over multiple switches and multiple"
				       + " packets in query\n" + queryText);
	}
	return new LocatedExprTree(opcode, opl_output, singletonList(let_input));
    }

    @Override public LocatedExprTree visitRfold(perf_queryParser.RfoldContext ctx) {
	return foldHelper(ctx.stream().getText(), ctx.column_list(), ctx.getText(), OperationType.RFOLD);
    }

    @Override public LocatedExprTree visitSfold(perf_queryParser.SfoldContext ctx) {
	return foldHelper(ctx.stream().getText(), ctx.column_list(), ctx.getText(), OperationType.SFOLD);
    }

    @Override public LocatedExprTree visitJoin(perf_queryParser.JoinContext ctx) {
	LocatedExprTree let_first  = RecurseDeps(ctx.stream(0).getText());
	LocatedExprTree let_second = RecurseDeps(ctx.stream(1).getText());
	OpLocation opl_first  = let_first.opl();
	OpLocation opl_second = let_second.opl();
	HashSet<Integer> result_set = new HashSet<Integer>(opl_first.getSwitchSet());
	result_set.retainAll(opl_second.getSwitchSet());
	StreamType result_type = ((opl_first.getStreamType() == opl_second.getStreamType()) ?
				  opl_first.getStreamType() : StreamType.SINGLE_SWITCH_STREAM);
	ArrayList<LocatedExprTree> children = new ArrayList<LocatedExprTree>();
	children.add(let_first);
	children.add(let_second);
	return new LocatedExprTree(OperationType.JOIN,
				   new OpLocation(result_set, result_type),
				   children);
    }

    @Override public LocatedExprTree visitProg(perf_queryParser.ProgContext ctx) {
	ParserRuleContext subquery = sym_tree_.get(last_assigned_id_);
	assert (subquery != null);
	return visit(subquery);
    }
}
