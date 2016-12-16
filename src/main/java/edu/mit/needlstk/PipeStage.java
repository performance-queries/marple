package edu.mit.needlstk;
import java.util.ArrayList;
import java.util.HashSet;

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
    String res = this.pipeName;
    res += "\nSchema ";
    res += this.fields.toString();
    res += "\n";
    res += this.op.toString();
    if (this.op == OperationType.GROUPBY) {
      res += " ";
      res += ((FoldConfigInfo)this.configInfo).getKeyFields().toString();
    }
    res += "\n";
    // res += configInfo.print();
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

  public HashSet<String> getSetFields() {
    return configInfo.getSetFields();
  }

  public HashSet<String> getUsedFields() {
    return configInfo.getUsedFields();
  }
}
