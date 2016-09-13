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
public class GlobalAnalyzer extends PerfQueryBaseVisitor<LocatedExprTree> {
  /// A reference to the set of switches globally in the network
  private HashSet<Integer> allSwitches;
 
  /// A reference to the symbol to query parse tree from previous passes.
  private HashMap<String, ParserRuleContext> symTree;
  
  /// A reference to the last assigned ID in the query program.
  private String lastAssignedId;

  /// A reference to associativity information for aggregation functions
  private HashMap<String, Boolean> aggFunAssocMap;

  /// State variable denoting we are at the top level of the parse
  private boolean isTopLevel = true;
  private void unsetTopLevel() { isTopLevel = false; }
  
  /// Constructor
  public GlobalAnalyzer(HashSet<Integer> allSwitches,
                        HashMap<String, ParserRuleContext> symTree,
                        String lastAssignedId,
                        HashMap<String, Boolean> aggFunAssocMap) {
    this.allSwitches = allSwitches;
    this.symTree = symTree;
    this.lastAssignedId = lastAssignedId;
    this.aggFunAssocMap = aggFunAssocMap;
  }
  
  /// Return OpLocation from underlying operators, which are inputs to the current operator.
  private LocatedExprTree RecurseDeps(String stream) {
    /// Recursively get OpLocation information for the operand streams.
    /// But first, ensure that the input stream has been seen before. It's an assert instead of
    /// exception because the previous pass should have caught the error.
    LocatedExprTree letInput;
    if (! stream.equals("T")) {
      ParserRuleContext subquery = symTree.get(stream);
      assert(subquery != null);
      letInput = visit(subquery);
    } else {
      letInput = new LocatedExprTree(OperationType.PKTLOG, new OpLocation());
    }
    return letInput;
  }
  
  private ArrayList<LocatedExprTree> singletonList(LocatedExprTree t) {
    ArrayList<LocatedExprTree> children = new ArrayList<LocatedExprTree>();
    children.add(t);
    return children;
  }
  
  /// Test methods to check out some basic functionalities
  @Override public LocatedExprTree visitFilter(PerfQueryParser.FilterContext ctx) {
    unsetTopLevel();
    HashSet<Integer> swSet = new SwitchPredicateExtractor(allSwitches).visit(ctx.predicate());
    LocatedExprTree letInput = RecurseDeps(ctx.stream().getText());
    OpLocation oplInput = letInput.opl();
    /// Merge values from recursive call and current
    swSet.retainAll(oplInput.getSwitchSet());
    OpLocation oplOutput;
    if (oplInput.getStreamType() == StreamType.SINGLE_SWITCH_STREAM) {
      oplOutput = new OpLocation(swSet, StreamType.SINGLE_SWITCH_STREAM);
    } else if (swSet.size() > 1) {
      oplOutput = new OpLocation(swSet, StreamType.MULTI_SWITCH_STREAM);
    } else {
      oplOutput = new OpLocation(swSet, StreamType.SINGLE_SWITCH_STREAM);
    }
    return new LocatedExprTree(OperationType.FILTER, oplOutput, singletonList(letInput));
  }

  @Override public LocatedExprTree visitMap(PerfQueryParser.MapContext ctx) {
    unsetTopLevel();
    LocatedExprTree letInput = RecurseDeps(ctx.stream().getText());
    OpLocation oplOutput = letInput.opl();
    return new LocatedExprTree(OperationType.PROJECT, oplOutput, singletonList(letInput));
  }
  
  private LocatedExprTree foldHelper(String streamName,
                                     PerfQueryParser.ColumnListContext ctx,
                                     String queryText,
                                     OperationType opcode,
                                     String aggFunc) {
    boolean isGroupTopLevel = isTopLevel;
    unsetTopLevel();
    LocatedExprTree letInput = RecurseDeps(streamName);
    OpLocation oplInput = letInput.opl();
    Boolean perSwitchStream = new ColumnChecker(Fields.switchHdr).visit(ctx);
    Boolean perPacketStream = new ColumnChecker(Fields.packetUid).visit(ctx);
    OpLocation oplOutput;
    // Ensure that metadata for the aggregation function has been recorded earlier.
    // This would have been caught in the symbol table generation pass, so asserting away.
    assert (aggFunAssocMap.get(aggFunc) != null);
    if (perSwitchStream || oplInput.getStreamType() == StreamType.SINGLE_SWITCH_STREAM) {
      oplOutput = new OpLocation(oplInput.getSwitchSet(), StreamType.SINGLE_SWITCH_STREAM);
    } else if (perPacketStream || (aggFunAssocMap.get(aggFunc) && isGroupTopLevel)) {
      oplOutput = new OpLocation(oplInput.getSwitchSet(), oplInput.getStreamType());
    } else {
        throw new RuntimeException("Cannot perform a fold over multiple switches and multiple"
    			       + " packets in query\n" + queryText);
    }
    return new LocatedExprTree(opcode, oplOutput, singletonList(letInput));
  }
  
  @Override public LocatedExprTree visitGroupby(PerfQueryParser.GroupbyContext ctx) {
    return foldHelper(ctx.stream().getText(), ctx.columnList(), ctx.getText(),
                      OperationType.GROUPBY, ctx.aggFunc().getText());
  }
  
  @Override public LocatedExprTree visitZip(PerfQueryParser.ZipContext ctx) {
    unsetTopLevel();
    LocatedExprTree letFirst  = RecurseDeps(ctx.stream(0).getText());
    LocatedExprTree letSecond = RecurseDeps(ctx.stream(1).getText());
    OpLocation oplFirst  = letFirst.opl();
    OpLocation oplSecond = letSecond.opl();
    HashSet<Integer> resultSet = new HashSet<Integer>(oplFirst.getSwitchSet());
    resultSet.retainAll(oplSecond.getSwitchSet());
    StreamType resultType = ((oplFirst.getStreamType() == oplSecond.getStreamType()) ?
    			  oplFirst.getStreamType() : StreamType.SINGLE_SWITCH_STREAM);
    ArrayList<LocatedExprTree> children = new ArrayList<LocatedExprTree>();
    children.add(letFirst);
    children.add(letSecond);
    return new LocatedExprTree(OperationType.JOIN,
    			   new OpLocation(resultSet, resultType),
    			   children);
  }
  
  @Override public LocatedExprTree visitProg(PerfQueryParser.ProgContext ctx) {
    ParserRuleContext subquery = symTree.get(lastAssignedId);
    assert (subquery != null);
    return visit(subquery);
  }
}
