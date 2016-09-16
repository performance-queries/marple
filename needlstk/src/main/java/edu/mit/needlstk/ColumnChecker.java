package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.Token;
import java.util.List;
import java.util.Iterator;

/// Check if the columns used in groupby operators contain a specified field.
public class ColumnChecker extends PerfQueryBaseVisitor<Boolean> {
  private String field;
  
  public ColumnChecker (String tField) {
    field = tField;
  }
  
  @Override public Boolean visitColumn (PerfQueryParser.ColumnContext ctx) {
    return ctx.getText().equals(field);
  }
  
  @Override public Boolean visitOneColsList (PerfQueryParser.OneColsListContext ctx) {
    return visit(ctx.column());
  }
  
  @Override public Boolean visitNoColsList (PerfQueryParser.NoColsListContext ctx) {
    return new Boolean("false");
  }
  
  @Override public Boolean visitMulColsList (PerfQueryParser.MulColsListContext ctx) {
    PerfQueryParser.ColumnContext firstCol = ctx.column();
    Boolean found = visit(firstCol);
    List<PerfQueryParser.ColumnWithCommaContext> otherCols = ctx.columnWithComma();
    for (PerfQueryParser.ColumnWithCommaContext newColWithComma : otherCols) {
      found = found || visit(newColWithComma.column());
    }
    return found;
  }
}
