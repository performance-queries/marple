package edu.mit.needlstk;
// import ANTLR's runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.io.PrintWriter;
import java.io.IOException;

public class Compiler {
  public static void main(String[] args) throws Exception {
    // create a CharStream that reads from standard input
    ANTLRInputStream input = new ANTLRInputStream(System.in);

    // create a lexer that feeds off of input CharStream
    PerfQueryLexer lexer = new PerfQueryLexer(input);

    // Add an error listener
    lexer.removeErrorListeners();
    lexer.addErrorListener(new ThrowingErrorListener());

    // create a buffer of tokens pulled from the lexer
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    // create a parser that feeds off the tokens buffer
    PerfQueryParser parser = new PerfQueryParser(tokens);

    // Add an error listener
    parser.removeErrorListeners();
    parser.addErrorListener(new ThrowingErrorListener());

    // begin parsing at the prog production
    ParseTree tree = parser.prog();

    // Create a walker for the parse tree
    ParseTreeWalker walker = new ParseTreeWalker();

    // Create symbol table
    System.out.println("Creating symbol table ...");
    SymbolTableCreator symbolTableCreator = new SymbolTableCreator();
    walker.walk(symbolTableCreator, tree);

    // Expression tree creator
    System.out.println("Creating expression tree ...");
    ExprTreeCreator exprTreeCreator = new ExprTreeCreator(PerfQueryParser.ID, symbolTableCreator.symbolTable());
    walker.walk(exprTreeCreator, tree);

    /// Global analysis to extract locations to install queries
    System.out.println("Analyzing queries globally ...");
    GlobalAnalyzer globalAnalyzer = new GlobalAnalyzer(new SwitchSet().getSwitches(),
                                                       exprTreeCreator.getSymTree(),
                                                       exprTreeCreator.getLastAssignedId(),
                                                       symbolTableCreator.getAggFunAssocMap());
    LocatedExprTree queryTree = globalAnalyzer.visit(tree);
    System.err.println(queryTree.dotOutput());

    /// Check for use-before-define errors in fold function code
    AggFunParamExtractor afpe = new AggFunParamExtractor();
    afpe.visit(tree);
    HashMap<String, List<String>> stateVars = afpe.getStateVars();
    HashMap<String, List<String>> fieldVars = afpe.getFieldVars();
    LexicalSymbolTable lst = new LexicalSymbolTable(stateVars, fieldVars);
    lst.visit(tree);
    HashMap<String, HashMap<String, AggFunVarType>> globalSymTab = lst.getGlobalSymTable();

    /// Detect bounded packet history
    HistoryDetector hd = new HistoryDetector(stateVars, fieldVars);
    hd.visit(tree);
    HashMap<String, HashMap<String, Integer>> hists = hd.getConvergedHistory();
    System.out.println(hd.reportHistory());

    /// Produce code for aggregation functions
    System.out.println("Generating code for aggregation functions...");
    IfConvertor ifc = new IfConvertor(globalSymTab);
    ifc.visit(tree);
    HashMap<String, ThreeOpCode> aggFunCode = ifc.getAggFunCode();

    /// Adjust state updates which are linear-in-state
    System.out.println("Performing linear-in-state transformations...");
    Linear linear = new Linear(aggFunCode, hists, stateVars, fieldVars, globalSymTab);
    linear.extractLinearUpdates();
    globalSymTab = linear.getGlobalSymTab();
    aggFunCode   = linear.getAggFunCode();

    System.out.println(aggFunCode);

    /// Produce code for all operators
    System.out.println("Generating code for all query operators...");
    ConfigGen cg = new ConfigGen(aggFunCode, globalSymTab, stateVars, fieldVars);
    cg.visit(tree);

    /// Pipeline the stages
    PipeConstructor pc = new PipeConstructor(cg.getQueryToPipe(),
                                             exprTreeCreator.getDepTable(),
                                             exprTreeCreator.getLastAssignedId());
    ArrayList<PipeStage> fullPipe = pc.stitchPipe();

    /// A small P4-specific pass to only allow divisions by (constant) powers of 2.
    DivisorChecker dc = new DivisorChecker(fullPipe);
    dc.checkDivisor();

    /// Print P4 fragments into a file
    boolean success = CodeFragmentPrinter.writeP4(pc, fullPipe);
    if (success) {
      System.out.println("P4 fragments output in output.p4");// TODO: unhardcode
    }

    boolean printed = CodeFragmentPrinter.writeDominoMonolithic(pc, fullPipe);
    if (printed) {
      System.out.println("Domino fragments output in domino-*.c");
    }
  }
}
