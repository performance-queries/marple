package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

public class ThreeOpDecl {
  public Integer bitWidth;
  public Integer valType;
  public String ident;

  public ThreeOpDecl(Integer width, Integer type, String id) {
    if (type != P4Printer.INT_TYPE && type != P4Printer.BOOL_TYPE) {
      throw new RuntimeException("Declaration must include either int or bool type.");
    }
    if (width != P4Printer.INT_WIDTH && width != P4Printer.BOOL_WIDTH) {
      throw new RuntimeException("Declaration must specify only int or bool data widths.");
    }
    this.bitWidth = width;
    this.valType = type;
    this.ident = id;
  }

  public String print() {
    return "int" + String.valueOf(bitWidth) + " " + ident + ";\n";
  }

  public String getP4() {
    if (valType == P4Printer.BOOL_TYPE) {
      return "bool " + ident + ";\n";
    } else {
      assert (valType == P4Printer.INT_TYPE);
      return "bit<" + String.valueOf(bitWidth) + "> " + ident + ";\n";
    }
  }

  public String getDomino() {
    return "  int " + ident + ";\n";
  }
}
