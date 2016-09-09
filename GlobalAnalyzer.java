import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.lang.RuntimeException;

/// Determine which queries run where, and (conceptually) which set of packets
/// each instance of the query processes. This results in an annotated query
/// expression tree.
public class GlobalAnalyzer extends perf_queryBaseVisitor<Void> {
    /// A reference to the symbol table created by the SymbolTableCreator pass
    private HashMap<String, IdentifierType> symbol_table_;

    /// Internal "value expression" detection instance
    private ValueExprDetector value_detect_;

    /// Constructor
    public GlobalAnalyzer(HashMap<String, IdentifierType> symbol_table) {
	symbol_table_ = symbol_table;
	value_detect_ = new ValueExprDetector();
    }

    /// Call visitor methods for the "value detection" class on the current
    /// expression.
    private Void valueDetect(perf_queryParser.ExprContext ctx) {
	System.out.println("Got an expression!");
	System.out.println(ctx.getText());
	System.out.println(value_detect_.visit(ctx).toString());
	return null;
    }

    /// Test methods to check out some basic functionalities
    @Override public Void visitFilter(perf_queryParser.FilterContext ctx) {
	System.out.println(ctx.predicate().getText());
	return null;
    }

    @Override public Void visitExprCol(perf_queryParser.ExprColContext ctx) {
    	return valueDetect(ctx);
    }

    @Override public Void visitExprInf(perf_queryParser.ExprInfContext ctx) {
    	return valueDetect(ctx);
    }

    @Override public Void visitExprVal(perf_queryParser.ExprValContext ctx) {
    	return valueDetect(ctx);
    }

    @Override public Void visitExprComb(perf_queryParser.ExprCombContext ctx) {
    	return valueDetect(ctx);
    }

}
