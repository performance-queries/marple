package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.RuntimeException;

/// First, generate dependencies for each stream or relational query
/// , i.e., for each query print out the streams written to and read from
/// Second, use this dependency table to build an expression tree
public class ExprTreeCreator extends PerfQueryBaseListener {
  /// The token type for identifiers. This is required to check if a given token
  /// is an identifier or not.
  private int idTtype;

  /// A reference to the symbol table created by the SymbolTableCreator pass
  private HashMap<String, IdentifierType> symbolTable;

  /// The dependency table mapping from identifiers to operations, populated
  /// by this pass.
  private HashMap<String, Operation> depTable = new HashMap<String, Operation>();

  /// A map from a stream/relation symbol to its context in the overall parse tree.
  private HashMap<String, ParserRuleContext> symTree = new HashMap<String, ParserRuleContext>();

  /// The last identifier assigned so far. Used to build an expression tree.
  private String lastAssignedId = "";

  /// Constructor
  public ExprTreeCreator(int identifierTtype, HashMap<String, IdentifierType> symbolTable) {
    idTtype = identifierTtype;
    this.symbolTable = symbolTable;
  }

  @Override public void exitStreamStmt(PerfQueryParser.StreamStmtContext ctx) {
    PerfQueryParser.StreamContext stream = ctx.stream();

    PerfQueryParser.StreamQueryContext query = ctx.streamQuery();

    Operation operation = getOperation(query);
    for (int i = 0; i < operation.operands.size(); i++) {
      assert(symbolTable.get(operation.operands.get(i)) == IdentifierType.STREAM);
    }
    depTable.put(stream.getText(), operation);
    lastAssignedId = stream.getText();
    symTree.put(stream.getText(), (ParserRuleContext)query);
  }

  @Override public void exitProg(PerfQueryParser.ProgContext ctx) {
    System.out.println("depTable: " + depTable);
    System.out.println("exprTree : " + buildExprTree(lastAssignedId));
    //System.err.println(buildExprTree(lastAssignedId).dotOutput());
  }

  /// Get operands and operator that are required for the given query
  private Operation getOperation(PerfQueryParser.StreamQueryContext query) {
    assert(query.getChildCount() == 1);
    ParseTree op = query.getChild(0);
    if (op instanceof PerfQueryParser.FilterContext) {
      PerfQueryParser.FilterContext filter = (PerfQueryParser.FilterContext)op;
      return new Operation(OperationType.FILTER, Utility.getAllTokens(filter.stream(), idTtype));
    } else if (op instanceof PerfQueryParser.GroupbyContext) {
      PerfQueryParser.GroupbyContext groupby = (PerfQueryParser.GroupbyContext)op;
      return new Operation(OperationType.GROUPBY, Utility.getAllTokens(groupby.stream(), idTtype));
    } else if (op instanceof PerfQueryParser.MapContext) {
      PerfQueryParser.MapContext map = (PerfQueryParser.MapContext)op;
      return new Operation(OperationType.PROJECT, Utility.getAllTokens(map.stream(), idTtype));
    } else if (op instanceof PerfQueryParser.ZipContext) {
      PerfQueryParser.ZipContext zip = (PerfQueryParser.ZipContext)op;
      ArrayList<PerfQueryParser.StreamContext> streamList = new ArrayList<>(zip.stream());
      ArrayList<String> ret = Utility.getAllTokens(streamList.get(0), idTtype);
      ret.addAll(Utility.getAllTokens(streamList.get(1), idTtype));
      return new Operation(OperationType.JOIN, ret);
    } else {
      assert(false);
      return new Operation();
    }
  }

  private ExprTree buildExprTree(String idName) {
   if (idName.equals("T")) {
     return new ExprTree(OperationType.PKTLOG);
   } else {
     // Get operands using depTable
     Operation operation = depTable.get(idName);
     if (operation == null) {
       throw new RuntimeException(idName + " doesn't exist in dependency table. It was likely not defined before use.");
     }

     // Recursively buildExprTree for each operand
     ArrayList<ExprTree> children = new ArrayList<ExprTree>();
     assert(operation.operands.size() >= 1);
     for (int i = 0; i < operation.operands.size(); i++) {
       children.add(buildExprTree(operation.operands.get(i)));
     }
     return new ExprTree(operation.opcode, children);
   }
  }

  public HashMap<String, ParserRuleContext> getSymTree() {
    return symTree;
  }

  public String getLastAssignedId() {
    return lastAssignedId;
  }

  public HashMap<String, Operation> getDepTable() {
    return depTable;
  }
}
