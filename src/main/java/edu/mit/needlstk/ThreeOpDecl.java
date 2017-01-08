package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;

public class ThreeOpDecl {
  public Integer bitWidth;
  public String ident;

  public ThreeOpDecl(Integer width, String id) {
    this.bitWidth = width;
    this.ident = id;
  }

  public String print() {
    return "int" + String.valueOf(bitWidth) + " " + ident + ";\n";
  }

  public String getP4() {
    if (bitWidth == P4Printer.BOOL_WIDTH) {
      return "bool " + ident + ";\n";
    } else {
      return "bit<" + String.valueOf(bitWidth) + "> " + ident + ";\n";
    }
  }

  public String getDomino() {
    return "  int " + ident + ";\n";
  }
}
