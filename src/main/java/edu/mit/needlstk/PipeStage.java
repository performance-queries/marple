package edu.mit.needlstk;
import java.util.ArrayList;

public class PipeStage {
  /// Type of query operation, e.g., filter, map, etc.
  private OperationType op;

  /// Configuration info corresponding to the pipe type.
  private PipeConfigInfo configInfo;

  /// Name of the query corresponding to this stage from the original user program
  private String pipeName;

  /// List of fields in the result of this query operation
  private ArrayList<String> fields;

  public PipeStage(OperationType op, PipeConfigInfo configInfo) {
    this.op = op;
    this.configInfo = configInfo;
    this.fields = new ArrayList<String>();
  }

  @Override public String toString() {
    String res = this.pipeName + ": " + this.op.toString();
    res += "\n";
    res += configInfo.getP4();
    res += "\n";
    return res;
  }

  public PipeConfigInfo getConfigInfo() {
    return configInfo;
  }

  public void setPipeName(String name) {
    this.pipeName = name;
  }

  public void addFields(ArrayList<String> fields) {
    this.fields.addAll(fields);
  }

  public void addField(String field) {
    this.fields.add(field);
  }
}
