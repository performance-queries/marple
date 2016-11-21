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
    String res = "int" + String.valueOf(bitWidth) + " " + ident + ";";
  }
}
