package edu.mit.needlstk;
import java.util.ArrayList;
import java.util.List;
import java.util.HashSet;
import java.io.PrintWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.stringtemplate.v4.*;
import org.apache.commons.io.IOUtils;
import java.io.InputStream;

/// Class with static members to help print code fragments of different kinds.
public class CodeFragmentPrinter {
  static class GroupbyStruct {
    public String struct_name;
    public List<String> fields;
    public GroupbyStruct(String t_struct_name, List<String> t_fields) {
      this.struct_name = t_struct_name;
      this.fields = t_fields;
    }
  }

  public static boolean writeP4(PipeConstructor pc, ArrayList<PipeStage> pipe) {
    try {
      PrintWriter writer = new PrintWriter("output.p4", "UTF-8"); // unhardcode
      InputStream is = CodeFragmentPrinter.class.getClassLoader().getResourceAsStream("p4.tmpl");
      ST p4_template = new ST(IOUtils.toString(is, "UTF-8"), '$', '$');
      p4_template.add("SwitchId", 666);         // Unhardcode this.

      // XXX: Restoring NG's old code using getPacketFieldDeclList
      // Need to find out how this differs from getAllPacketFields
      ArrayList<ThreeOpDecl> decls = pc.getPacketFieldDeclList(pipe);
      List<String> res = new ArrayList<String>();
      for (ThreeOpDecl decl: decls) {
        res.add(decl.getP4());
      }
      p4_template.add("QueryMetadata", res); // Query metadata

      List<GroupbyStruct> groupby_structs = new ArrayList<GroupbyStruct>();// groupby structs
      for (PipeStage stage: pipe) {
        if (stage.getOp() == OperationType.GROUPBY) {
          groupby_structs.add(new GroupbyStruct("Key_"+stage.getPipeName(), stage.getQualifiedKeyFields()));
          groupby_structs.add(new GroupbyStruct("Value_"+stage.getPipeName(), stage.getValueFields()));
        }
      }
      p4_template.add("GroupbyStructs", groupby_structs);
      p4_template.add("Stages", pipe);    // Add every stage's information
      writer.print(p4_template.render()); // render template
      writer.close();
      return true;
    } catch (IOException e) {
      System.err.println("Could not write into output.p4");
      e.printStackTrace();
      System.exit(1);
      return false;
    }
  }

  public static boolean writeDominoMonolithic(PipeConstructor pc, ArrayList<PipeStage> pipe) {
    String fPrefix = "domino-";
    String fSuffix = ".c";
    try {
      /// Print final output for inspection
      String decls = pc.getNonRegisterDeclsDomino(pipe);
      String regs  = pc.getAllRegisterDeclsDomino(pipe);
      String fileName = fPrefix + "full" + fSuffix;
      PrintWriter writer = new PrintWriter(fileName, "UTF-8");
      writer.print("// Packet fields\n");
      writer.print("struct Packet {\n");
      writer.print(decls);
      writer.print("};\n\n");
      writer.print("// State declarations\n");
      writer.print(regs);
      writer.print("\n");
      writer.print("// Fold function definition\n");
      writer.print("void func(struct Packet pkt) {\n");
      for (PipeStage stage: pipe) {
        writer.print(stage.getDominoFragment());
      }
      writer.print("}\n");
      writer.close();
      return true;
    } catch (IOException e) {
      System.err.println("Could not write into domino file!");
      return false;
    }
  }
}
