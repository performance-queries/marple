import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.Token;
import java.util.List;
import java.util.Iterator;

/// Check if the columns used in groupby operators contain a specified field.
public class ColumnChecker extends perf_queryBaseVisitor<Boolean> {
    private String field_;

    public ColumnChecker (String field) {
	field_ = field;
    }

    @Override public Boolean visitColumn (perf_queryParser.ColumnContext ctx) {
	return ctx.getText().equals(field_);
    }

    @Override public Boolean visitOneColsList (perf_queryParser.OneColsListContext ctx) {
	return visit(ctx.column());
    }

    @Override public Boolean visitNoColsList (perf_queryParser.NoColsListContext ctx) {
	return new Boolean("false");
    }

    @Override public Boolean visitMulColsList (perf_queryParser.MulColsListContext ctx) {
	perf_queryParser.ColumnContext first_col = ctx.column();
	Boolean found = visit(first_col);
	List<perf_queryParser.Column_with_commaContext> other_cols = ctx.column_with_comma();
	for (perf_queryParser.Column_with_commaContext new_col_with_comma : other_cols) {
	    found = found || visit(new_col_with_comma.column());
	}
	return found;
    }
}
