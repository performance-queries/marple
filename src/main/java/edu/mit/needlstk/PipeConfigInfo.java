package edu.mit.needlstk;
import java.util.List;
import java.util.HashSet;

public interface PipeConfigInfo {
  public String getP4();
  public List<ThreeOpStmt> getCode();
  public void addValidStmt(String q, String oq, boolean opT);
  public HashSet<String> getSetFields();
}
