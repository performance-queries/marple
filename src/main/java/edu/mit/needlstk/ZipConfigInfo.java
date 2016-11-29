package edu.mit.needlstk;
import org.antlr.v4.runtime.ParserRuleContext;
import java.util.List;
import java.util.ArrayList;

public class ZipConfigInfo implements PipeConfigInfo {
  private List<ThreeOpStmt> code;

  public ZipConfigInfo() {
    code = new ArrayList<>();
  }

  public String getP4() {
    String res = "";
    for (ThreeOpStmt stmt: code) {
      res += stmt.print();
      res += "\n";
    }
    return res;
  }

  public List<ThreeOpStmt> getCode() {
    return code;
  }
}
