package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.List;
import java.util.ArrayList;

public class ColumnExtractor {
  public static List<String> getColumns(PerfQueryParser.ColumnListContext col) {
    List<String> cols = new ArrayList<>();
    if(col instanceof PerfQueryParser.OneColsListContext) {
      PerfQueryParser.OneColsListContext newCol = (PerfQueryParser.OneColsListContext)col;
      cols.add(newCol.column().getText());
    } else if(col instanceof PerfQueryParser.MulColsListContext) {
      PerfQueryParser.MulColsListContext newCol = (PerfQueryParser.MulColsListContext)col;        
      cols.add(newCol.column().getText());
      for(PerfQueryParser.ColumnWithCommaContext cwcc: newCol.columnWithComma()) {
        cols.add(cwcc.column().getText());
      }
    } else if(col instanceof PerfQueryParser.NoColsListContext) {
      // do nothing.
    } else {
      assert(false); // Logic error. Expecting a new kind of columnList?
    }
    return cols;
  }
}
