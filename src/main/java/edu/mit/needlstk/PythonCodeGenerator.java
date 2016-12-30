package edu.mit.needlstk;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ErrorNode;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.misc.Interval;
import java.util.stream.Collectors;
import java.util.List;
import java.util.TreeSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.lang.RuntimeException;

public class PythonCodeGenerator extends PerfQueryBaseListener {
  /// Intrinsic fields understood by the compiler
  private final TreeSet<String> intrinsicFields = new TreeSet<String>(Arrays.asList("qid", "qin", "tin", "tout", "pktPath", "switchId", "uid"));

  /// Reference to parser to get underlying token stream.
  /// Required to preserve spaces and retrieve them when unparsing productions
  private PerfQueryParser parser;

  /// Names of all key-value stores used for groupbys
  private TreeSet<String> kvStores = new TreeSet<String>();

  /// All fields in tuples,
  /// start with intrinsic fields alone
  private TreeSet<String> tupleFieldSet = new TreeSet<String>(intrinsicFields);

  /// All fields in state
  private TreeSet<String> stateFieldSet = new TreeSet<String>();

  /// Build up function calls string
  private String functionCalls = "";

  /// Build up function definitions string
  private String functionDefs = "";

  /// Last assigned stream
  private String lastAssigned = "";

  public PythonCodeGenerator(PerfQueryParser tParser, HashMap<String, IdentifierType> tSymTable) {
    parser = tParser;

    tupleFieldSet = tSymTable.entrySet()
                       .stream()
                       .filter(kv -> (kv.getValue() == IdentifierType.COLUMN) ||
                              (kv.getValue() == IdentifierType.STATE_OR_COLUMN))
                       .map(kv -> kv.getKey())
                       .collect(Collectors.toCollection(TreeSet::new));
    tupleFieldSet.add("valid");

    stateFieldSet = tSymTable.entrySet()
                       .stream()
                       .filter(kv -> (kv.getValue() == IdentifierType.STATE) ||
                              (kv.getValue() == IdentifierType.STATE_OR_COLUMN))
                       .map(kv -> kv.getKey())
                       .collect(Collectors.toCollection(TreeSet::new));
  }

  /// Use this to print declarations at the beginning of Python code
  /// For tuples and state
  @Override public void enterProg(PerfQueryParser.ProgContext ctx) {
    System.err.println(generateStateClass());
    System.err.println(generateTupleClass());
    System.err.println("def fuse(x, y):");
    System.err.println("  if (x is None) : return y;");
    System.err.println("  if (y is None) : return x;");
    System.err.println("  raise Exception (\"Can't fuse, both x and y are not None\")");
    System.err.println("def randomTuple():");
    System.err.println("  ret = Tuple();");
    for (String key : intrinsicFields) {
      System.err.println("  " + "ret." + key + " = random.randint(1, 65536);");
    }
    System.err.println("  return ret;");
  }

  /// Use this to print the packet loop that tests the given sql program
  /// at the end of the Python code
  @Override public void exitProg(PerfQueryParser.ProgContext ctx) {
    System.err.println(functionDefs);
    System.err.println("# main loop of function calls");
    for (String kv : kvStores) {
      System.err.println(kv+ " = dict();");
    }
    System.err.println("for i in range(1000):");
    System.err.println("  T = randomTuple(); # generates a random tuple");
    System.err.println("  print(\"input:\", T);");
    System.err.println("  T.printTuple();");
    System.err.println(functionCalls);
    System.err.println("  print(\"output:\"," + lastAssigned + ");");
    System.err.println("  " + lastAssigned + ".printTuple();");
  }

  /// Turn aggregation function into a Python function definitoon
  @Override public void exitAggFun(PerfQueryParser.AggFunContext ctx) {
    System.err.println("def " + ctx.aggFunc().getText() + " (state, tupleVar):\n");
    TreeSet<String> states = new TreeSet<>(Utility.getAllTokens(ctx.stateList(), PerfQueryParser.ID));
    TreeSet<String> cols = new TreeSet<>(Utility.getAllTokens(ctx.columnList(), PerfQueryParser.ID));
    System.err.println(generateStatePreamble(states));
    System.err.println(generateTuplePreamble(cols));
    System.err.println(processCodeBlock(ctx.codeBlock(), states, 0) + "\n");
    System.err.println(generateStatePostamble(states));
    System.err.println("  return tupleVar;");
  }

  private String nSpaces(Integer n) {
    return String.format("%1$" + n + "s", "");
  }

  private String processCodeBlock(PerfQueryParser.CodeBlockContext codeBlock,
                                  TreeSet<String> states,
                                  Integer indent) {
    String ret = "";
    for (int i = 0; i < codeBlock.getChildCount(); i++) {
      //assert(codeBlock.get(i).getChildCount() == 1);
      //assert(codeBlock.get(i).getChild(0) instanceof ParserRuleContext);
      //ParserRuleContext singleStmt = (ParserRuleContext)codeBlock.get(i).getChild(0);
      ParserRuleContext singleStmt = (ParserRuleContext)codeBlock.getChild(i);
      assert(singleStmt instanceof PerfQueryParser.PrimitiveContext ||
             singleStmt instanceof PerfQueryParser.IfConstructContext);
      if (singleStmt instanceof PerfQueryParser.PrimitiveContext) {
        ret += nSpaces(indent+2) + processPrimitive(singleStmt, states) + "\n";
      } else if (singleStmt instanceof PerfQueryParser.IfConstructContext) {
        PerfQueryParser.IfConstructContext ifStmt = (PerfQueryParser.IfConstructContext)singleStmt;
        ret += nSpaces(indent+2) + "if " + textWithSpaces(ifStmt.predicate()) + " : \n";
        assert (ifStmt.ifCodeBlock().getChildCount() == 1);
        ret += processCodeBlock((PerfQueryParser.CodeBlockContext)ifStmt.ifCodeBlock().getChild(0),
                                states, indent+2);
        // for (int j = 0; j < ifStmt.ifPrimitive().size(); j++) {
        //   ret += "    " + processPrimitive(ifStmt.ifPrimitive().get(j), states) + "\n";
        // }
        // Optional else
        // if (ifStmt.ELSE() != null) {
        if (ifStmt.ELSE() != null) {
          ret += nSpaces(indent+2) + "else : \n";
          assert (ifStmt.elseCodeBlock().getChildCount() == 1);
          ret += processCodeBlock(
              (PerfQueryParser.CodeBlockContext)ifStmt.elseCodeBlock().getChild(0),
              states, indent+2);
          // for (int j = 0; j < ifStmt.elsePrimitive().size(); j++) {
          //   ret += "    " + processPrimitive(ifStmt.elsePrimitive().get(j), states) + "\n";
          // }
        }
      }
    }
    return ret;
  }

  private String processPrimitive(ParserRuleContext singleStmt, TreeSet<String> states) {
    if (textWithSpaces(singleStmt).contains("emit()")) return emitStub(states);
    else if (textWithSpaces(singleStmt).contains(";")) return "";
    else return textWithSpaces(singleStmt);
  }

  /// Turn selects into a Python function definition
  private String filterDef(PerfQueryParser.StreamQueryContext query, PerfQueryParser.StreamContext stream) {
    ParserRuleContext predicate = query.filter().predicate();
    return (spgQuerySignature(stream) +
            generateTuplePreamble(tupleFieldSet) + "\n" +
            "  valid = " + textWithSpaces(predicate) + "\n\n" +
            generateTuplePostamble(tupleFieldSet) + "\n");
  }

  /// Turn SQL projections into Python function definitions
  private String projectDef(PerfQueryParser.StreamQueryContext query, PerfQueryParser.StreamContext stream) {
    ParserRuleContext exprList = query.map().exprList();
    ParserRuleContext colList  = query.map().columnList();
    return (spgQuerySignature(stream) +
            generateTuplePreamble(tupleFieldSet) + "\n" +
            "  " + textWithSpaces(colList) + " = " + textWithSpaces(exprList) + ";\n\n" +
            generateTuplePostamble(tupleFieldSet)+ "\n");
  }

  /// Turn SQL joins into Python function definitions
  private String joinDef(PerfQueryParser.StreamQueryContext query, PerfQueryParser.StreamContext stream){
    return (joinQuerySignature(stream) +
            "  retTuple = Tuple();\n" +
            "  retTuple = tuple1.joinTuple(tuple2);\n" +
            "  retTuple.valid = tuple1.valid and tuple2.valid\n" +
            "  return ret;\n" + "\n");
  }

  /// Turn SQL GROUPBYs into Python function definitions
  private String groupbyDef(PerfQueryParser.StreamQueryContext query, PerfQueryParser.StreamContext stream) {
    ParserRuleContext groupbyList = query.groupby().columnList();
    ParserRuleContext aggFunc     = query.groupby().aggFunc();
    String streamName = textWithSpaces(stream);
    kvStores.add("stateDict" + streamName);
    return (spgQuerySignature(stream) +
            "  global stateDict" + streamName + " ;\n\n" +
            generateTuplePreamble(tupleFieldSet) + "\n" +
            "  keyForDict = tuple(" + textWithSpaces(groupbyList) + ");\n\n" +
            "  tupleState = stateDict" + streamName + "[keyForDict] if keyForDict in stateDict" + streamName + " else State();\n\n" +
            "  return " + textWithSpaces(aggFunc) + "(tupleState, tupleVar);" + "\n" + "\n");
  }

  @Override public void exitStreamStmt(PerfQueryParser.StreamStmtContext ctx) {
    PerfQueryParser.StreamContext stream = ctx.stream();
    PerfQueryParser.StreamQueryContext query = ctx.streamQuery();

    lastAssigned = textWithSpaces((ParserRuleContext)stream);
    OperationType operation = Utility.getOperationType(query);
    if (operation == OperationType.FILTER) {
      functionDefs  += filterDef(query, stream);
      functionCalls += generateSpgQueries(query, stream);
    } else if (operation == OperationType.PROJECT) {
      functionDefs  += projectDef(query, stream);
      functionCalls += generateSpgQueries(query, stream); 
    } else if (operation == OperationType.JOIN) {
      functionDefs  += joinDef(query, stream);
      functionCalls += generateJoinQueries(query, stream); 
    } else if (operation == OperationType.GROUPBY) {
      functionDefs  += groupbyDef(query, stream);
      functionCalls += generateSpgQueries(query, stream);
    } else {
      assert(false);
    }
  }

  /// Signature for Python function for select, project, and group by
  private String spgQuerySignature(PerfQueryParser.StreamContext stream) {
    String streamName = textWithSpaces(stream);
    return "def func" + streamName + "(tupleVar):\n";
  }

  /// Signature for Python functions for join
  private String joinQuerySignature(PerfQueryParser.StreamContext stream) {
    String streamName = textWithSpaces(stream);
    return "def func" + streamName + "(tuple1, tuple2):\n";
  }

  /// Generate Python function calls for select, project, and group by queries
  private String generateSpgQueries(PerfQueryParser.StreamQueryContext query, PerfQueryParser.StreamContext stream) {
    String streamName = textWithSpaces(stream);
    // TODO: This is a total hack and very brittle to use getChild(foo, etc.)
    String argName    = textWithSpaces((ParserRuleContext)(query.getChild(0).getChild(2)));
    return "  " + streamName + " = func" + streamName + "(" + argName + ")\n";
  }

  /// Generate Python function call for join queries
  private String generateJoinQueries(PerfQueryParser.StreamQueryContext query, PerfQueryParser.StreamContext stream) {
    String streamName = textWithSpaces(stream);
    String arg0   = textWithSpaces((ParserRuleContext)(query.zip().stream().get(0)));
    String arg1   = textWithSpaces((ParserRuleContext)(query.zip().stream().get(1)));
    return "  " + streamName + " = func" + streamName + "(" + arg0 + ", " + arg1 + ")\n";
  }

  private String textWithSpaces(ParserRuleContext production) {
    Token startToken = production.getStart();
    Token stopToken  = production.getStop();
    return parser.getTokenStream().getText(startToken, stopToken);
  }

  private String generateTupleClass() {
    String ret = "import random;\n";
    ret += "random.seed(1)\n";
    ret += "# tuple class\n";
    ret += "class Tuple: \n";
    ret += "  def __init__(self):\n";
    for (String key : tupleFieldSet) {
      ret = ret + "    " + "self." + key + " = None;\n";
    }
    ret += "    self.valid = True;\n"; // Everything is valid in the base stream

    ret += "  def printTuple(self):\n";
    for (String key : tupleFieldSet) {
      ret += "    print(\"" + key + "\"," + "self." + key + ")\n";
    }

    ret += "  def joinTuple(self, other):\n";
    ret += "    ret = Tuple();\n";
    for (String key : tupleFieldSet) {
      ret += "    ret." + key + " = fuse(self." + key + ", other." + key + ");\n";
    }
    ret += "    return ret;\n";

    return ret;
  }

  private String generateTuplePreamble(TreeSet<String> columns) {
    String ret = "  # tuple preamble\n";
    for (String key : columns) {
      ret = ret + "  " + key + " = tupleVar." + key + "\n";
    }
    return ret;
  }

  private String generateTuplePostamble(TreeSet<String> columns) {
    String ret = "  # tuple postamble\n";
    for (String key : columns) {
      ret = ret + "  " + "tupleVar." + key + " = " + key + "\n";
    }
    ret += "  return tupleVar;\n";
    return ret;
  }

  private String generateStateClass() {
    String ret = "# state class\n";
    ret += "class State: \n";
    ret += "  def __init__(self):\n";
    // For now, we are assuming all state is init. to 0
    for (String key : stateFieldSet) {
      ret = ret + "    " + "self." + key + " = 0;\n";
    }
    ret += "  def printState(self):\n";
    for (String key : stateFieldSet) {
      ret = ret + "    print(\"" + key + "\", self." + key +");\n";
    }

    return ret;
  }

  private String emitStub(TreeSet<String> states) {
    String ret = "";
    for (String key : states) {
      ret = ret + "tupleVar." + key + " = " + key + ";";
    }
    ret += "# emit stub";
    return ret;
  }

  private String generateStatePreamble(TreeSet<String> states) {
    String ret = "  # state preamble\n";
    for (String key : states) {
      ret = ret + "  " + key + " = state." + key + "\n";
    }
    return ret;
  }

  private String generateStatePostamble(TreeSet<String> states) {
    String ret = "  # state postamble\n";
    for (String key : states) {
      ret = ret + "  " + "state." + key + " = " + key + "\n";
    }
    return ret;
  }
}
