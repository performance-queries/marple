// import ANTLR's runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;
import java.util.HashSet;
import java.util.HashMap;

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

    System.out.println("Analyzing queries globally ...");
    GlobalAnalyzer globalAnalyzer = new GlobalAnalyzer(new SwitchSet().getSwitches(),
							exprTreeCreator.getSymTree(),
							exprTreeCreator.getLastAssignedId());
    LocatedExprTree queryTree = globalAnalyzer.visit(tree);
    System.err.println(queryTree.dotOutput());
  }
}
