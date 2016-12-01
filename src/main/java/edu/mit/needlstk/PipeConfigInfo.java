package edu.mit.needlstk;
import java.util.List;

public interface PipeConfigInfo {
  public String getP4();
  public List<ThreeOpStmt> getCode();
  public void addValidStmt(String q, String oq, boolean opT);
}
