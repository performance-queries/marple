import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.Token;
import java.util.List;
import java.util.Iterator;

/// Check if the columns used in groupby operators contain a specified field.
public class ColumnChecker extends PerfQueryBaseVisitor<Boolean> {
    private String field_;

    public ColumnChecker (String field) {
	field_ = field;
    }

    @Override public Boolean visitColumn (PerfQueryParser.ColumnContext ctx) {
	return ctx.getText().equals(field_);
    }

    @Override public Boolean visitOneColsList (PerfQueryParser.OneColsListContext ctx) {
	return visit(ctx.column());
    }

    @Override public Boolean visitNoColsList (PerfQueryParser.NoColsListContext ctx) {
	return new Boolean("false");
    }

    @Override public Boolean visitMulColsList (PerfQueryParser.MulColsListContext ctx) {
	PerfQueryParser.ColumnContext first_col = ctx.column();
	Boolean found = visit(first_col);
	List<PerfQueryParser.ColumnWithCommaContext> other_cols = ctx.columnWithComma();
	for (PerfQueryParser.ColumnWithCommaContext new_col_with_comma : other_cols) {
	    found = found || visit(new_col_with_comma.column());
	}
	return found;
    }
}
