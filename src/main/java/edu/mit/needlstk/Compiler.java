package edu.mit.needlstk;
// import ANTLR's runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;

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

    /// Produce code for aggregation functions
    System.out.println("Generating code for aggregation functions...");
    IfConvertor ifc = new IfConvertor();
    ifc.visit(tree);
    HashMap<String, ThreeOpCode> aggFunCode = ifc.getAggFunCode();

    /// Check for use-before-define errors in fold function code
    AggFunParamExtractor afpe = new AggFunParamExtractor();
    afpe.visit(tree);
    HashMap<String, List<String>> stateVars = afpe.getStateVars();
    HashMap<String, List<String>> fieldVars = afpe.getFieldVars();
    DefineBeforeUse codeChecker = new DefineBeforeUse(aggFunCode,
                                                      stateVars,
                                                      fieldVars);
    codeChecker.check();

    /// Produce code for all operators
    System.out.println("Generating code for all query operators...");
    CodeGen cg = new CodeGen(aggFunCode, stateVars, fieldVars);
    cg.visit(tree);

    /// Pipeline the stages
    PipeConstructor pc = new PipeConstructor(cg.getQueryToPipe(),
                                             exprTreeCreator.getDepTable(),
                                             exprTreeCreator.getLastAssignedId());
    System.out.println(pc.stitchPipe().toString());
  }
}
