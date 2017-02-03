package edu.mit.needlstk;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.util.stream.Collectors;
import org.stringtemplate.v4.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.apache.commons.io.IOUtils;
import java.io.InputStream;

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
      res += "\n";
      res += "Registers used: ";
      res += ((FoldConfigInfo)this.configInfo).getStateArgs().toString();
      res += "\n--";
    }
    res += "\n";
    // res += configInfo.print();
    res += configInfo.getP4();
    res += "\n";
    return res;
  }

  public String getAction() {
    if (this.op != OperationType.GROUPBY) {
      return "action " + this.pipeName + "() {\n" + configInfo.getP4() + "\n}";
    } else {
     try {
      InputStream is = CodeFragmentPrinter.class.getClassLoader().getResourceAsStream("groupby.tmpl");
      ST groupby_template = new ST(IOUtils.toString(is, "UTF-8"), '$', '$');
      groupby_template.add("KeyFields", this.getQualifiedKeyFields());
      groupby_template.add("ValueFields", this.getValueFields());
      groupby_template.add("StageName", this.pipeName);
      groupby_template.add("TableSize", 1024); // TODO: Unhardcode this.
      groupby_template.add("UpdateCode", configInfo.getP4());
      return groupby_template.render();
     } catch (Exception e) {
      e.printStackTrace();
      System.exit(1);
      return "";
     }
    }
  }

  public String getAction_call() {
    if (this.op != OperationType.GROUPBY) {
      return this.pipeName + "();\n";
    } else {
      return  "Key_" + this.pipeName + "  evictedKey_" + this.pipeName + ";\n"
            + "Value_" + this.pipeName + " evictedValue_" + this.pipeName + ";\n"
            + this.pipeName + "(evictedKey_" + this.pipeName + "," +  "evictedValue_" + this.pipeName + ");\n";
    }
  }

  public List<String> getQualifiedKeyFields() {
    assert(this.op == OperationType.GROUPBY);
    FoldConfigInfo fci = (FoldConfigInfo)this.configInfo;
    return fci.getKeyFields().stream().
          map(var -> P4Printer.p4Ident(var, AggFunVarType.FIELD)).
          collect(Collectors.toList());
  }

  public List<String> getValueFields() {
    assert(this.op == OperationType.GROUPBY);
    FoldConfigInfo fci = (FoldConfigInfo)this.configInfo;
    return fci.getStateArgs().stream().collect(Collectors.toList());
  }

  public String getDominoFragment() {
    return configInfo.getDomino();
  }

  public PipeConfigInfo getConfigInfo() {
    return configInfo;
  }

  public String getPipeName() {
    return this.pipeName;
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

  public OperationType getOp() {
    return this.op;
  }
}
