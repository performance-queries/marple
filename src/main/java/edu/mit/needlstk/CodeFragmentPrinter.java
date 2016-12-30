package edu.mit.needlstk;
import java.util.ArrayList;
import java.util.HashSet;
import java.io.PrintWriter;
import java.io.IOException;

/// Class with static members to help print code fragments of different kinds.
public class CodeFragmentPrinter {
  public static String writeP4(PipeConstructor pc, ArrayList<PipeStage> pipe) {
    String fileName = "p4-frags.txt";
    try {
      /// Print final output for inspection
      PrintWriter writer = new PrintWriter(fileName, "UTF-8");
      String decls = pc.getPacketFieldDeclsP4(pipe);
      HashSet<String> registers = pc.getAllRegisters(pipe);
      writer.println("=================================");
      for (String pktLogField: Fields.pktLogMetadataFields) {
        writer.print(new ThreeOpDecl(P4Printer.INT_WIDTH,
                                     Fields.p4Map.get(pktLogField)).getP4());
      }
      writer.println("=================================");
      writer.print(decls);
      writer.println("=================================");
      writer.println(registers);
      writer.println("=================================");
      for (PipeStage stage: pipe) {
        writer.print(stage.getP4Fragment());
        writer.println("=================================");
      }
      writer.close();
      return fileName;
    } catch (IOException e) {
      System.err.println("Could not write into P4 fragments file! " + fileName);
      return null;
    }
  }

  public static boolean writeDomino(PipeConstructor pc, ArrayList<PipeStage> pipe) {
    String fPrefix = "domino-";
    String fSuffix = ".c";
    try {
      /// Print final output for inspection
      String decls = pc.getNonRegisterDeclsDomino(pipe);
      String regs  = pc.getAllRegisterDeclsDomino(pipe);
      for (PipeStage stage: pipe) {
        if (stage.getOp() == OperationType.GROUPBY) {
          String fileName = fPrefix + stage.getPipeName() + fSuffix;
          PrintWriter writer = new PrintWriter(fileName, "UTF-8");
          writer.print("struct Packet {\n");
          writer.print(decls);
          writer.print("}\n\n");
          writer.print(regs);
          writer.print("\n");
          writer.print("void func(struct Packet pkt) {\n");
          writer.print(stage.getDominoFragment());          
          writer.print("}\n\n");
          writer.close();
        }
      }
      return true;
    } catch (IOException e) {
      System.err.println("Could not write into domino fragments files!");
      return false;
    }
  }
}
