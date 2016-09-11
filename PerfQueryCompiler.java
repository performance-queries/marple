// import ANTLR's runtime libraries
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public class PerfQueryCompiler {
  public static void main(String[] args) throws Exception {
    // create a CharStream that reads from standard input
    ANTLRInputStream input = new ANTLRInputStream(System.in);

    // create a lexer that feeds off of input CharStream
    perf_queryLexer lexer = new perf_queryLexer(input);

    // Add an error listener
    lexer.removeErrorListeners();
    lexer.addErrorListener(new ThrowingErrorListener());

    // create a buffer of tokens pulled from the lexer
    CommonTokenStream tokens = new CommonTokenStream(lexer);

    // create a parser that feeds off the tokens buffer
    perf_queryParser parser = new perf_queryParser(tokens);

    // Add an error listener
    parser.removeErrorListeners();
    parser.addErrorListener(new ThrowingErrorListener());

    // begin parsing at the prog production
    ParseTree tree = parser.prog();

    // Create a walker for the parse tree
    ParseTreeWalker walker = new ParseTreeWalker();

    // Create symbol table
    System.out.println("Creating symbol table ...");
    SymbolTableCreator symbol_table_creator = new SymbolTableCreator();
    walker.walk(symbol_table_creator, tree);

    // Expression tree creator
    System.out.println("Creating expression tree ...");
    ExprTreeCreator expr_tree_creator = new ExprTreeCreator(perf_queryParser.ID, symbol_table_creator.symbol_table());
    walker.walk(expr_tree_creator, tree);

    System.out.println("Trying out visitor pattern ...");
    GlobalAnalyzer global_analyzer = new GlobalAnalyzer(
        symbol_table_creator.symbol_table());
    global_analyzer.visit(tree);
  }
}
