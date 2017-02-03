package edu.mit.needlstk;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.HashSet;

public class PipeConstructor {
  private HashMap<String, Operation> depTable;
  private HashMap<String, PipeStage> stages;
  private String lastAssignedId;
  private ArrayList<PipeStage> pipe;
  private String pktLogStr = "T"; /// Operand denoting the packet log
  private HashSet<String> visited;
  private HashMap<String, ArrayList<String>> schema;
  private HashSet<String> predPacketFields;
  
  public PipeConstructor(HashMap<String, PipeStage> stages,
                         HashMap<String, Operation> depTable,
                         String lastAssignedId) {
    this.depTable = depTable;
    this.stages = stages;
    this.lastAssignedId = lastAssignedId;
    this.visited = new HashSet<>();
    this.schema = new HashMap<String, ArrayList<String>>();
    this.schema.put(pktLogStr, Fields.fields);
    this.predPacketFields = new HashSet<String>();
    this.predPacketFields.add(PipeConfigInfo.fieldTransformQueryId(pktLogStr));
  }

  public ArrayList<PipeStage> stitchPipe() {
    this.pipe = getPipes(lastAssignedId);
    return this.pipe;
  }

  private boolean checkUseBeforeDefine(String queryId,
                                       ArrayList<String> operandQueryIds,
                                       HashSet<String> operandSet,
                                       HashSet<String> usedSet) {
    if (operandSet.containsAll(usedSet)) {
      return true;
    } else {
      HashSet<String> copyUsedSet = new HashSet<>(usedSet);
      copyUsedSet.removeAll(operandSet);
      throw new RuntimeException("Fields " + copyUsedSet.toString() + " used in query `" + queryId +
                                 "` not available from operands "
                                 + operandQueryIds.toString());
    }
  }

  private ArrayList<String> getSchemaFields(OperationType op,
                                            HashSet<String> inputFieldSet,
                                            HashSet<String> setFields) {
    switch (op) {
      case FILTER:
      case JOIN:
        return new ArrayList<String>(inputFieldSet);
      case PROJECT:
        HashSet<String> schemaFields = new HashSet<>(setFields);
        schemaFields.addAll(inputFieldSet);
        return new ArrayList<>(schemaFields);
      case GROUPBY:
        return new ArrayList<String>(setFields);
      case PKTLOG:
        return Fields.fields;
      default:
        assert(false); // Logic error. Operation must be one of the above types
        return null;
    }
  }

  private ArrayList<PipeStage> getPipes(String queryId) {
    assert (depTable.containsKey(queryId)); // queryId should be in the depTable.
    assert (stages.containsKey(queryId));   // queryId should be one of the stages.
    Operation op = depTable.get(queryId);
    PipeStage stage = stages.get(queryId);
    ArrayList<PipeStage> stageList = new ArrayList<>();
    HashSet<String> inputFieldSet = new HashSet<>();
    for (String operand: op.operands) {
      /// Get pipes for operand subqueries
      if((! operand.equals(pktLogStr)) && (! visited.contains(operand))) {
        stageList.addAll(getPipes(operand));
      }
      /// Accumulate set of available fields from all operands
      inputFieldSet.addAll(schema.get(operand));
      /// Determine validity of current query result in generated code
      addValidStmt(queryId, operand, op.opcode, stage);
    }
    /// Determine schema of output
    ArrayList<String> querySchema = getSchemaFields(
        op.opcode,
        inputFieldSet,
        stage.getSetFields());
    schema.put(queryId, querySchema);
    stage.addFields(querySchema);
    /// Check if all fields used in the query are defined prior
    checkUseBeforeDefine(queryId, op.operands, inputFieldSet, stage.getUsedFields());
    stageList.add(stage);
    visited.add(queryId);
    return stageList;
  }

  /// Given a stage of a specific type, add a "validity" statement at the end of it.
  private void addValidStmt(String queryId,
                            String operandQueryId,
                            OperationType opcode,
                            PipeStage ps) {
    switch(opcode) {
      case FILTER:
      case PROJECT:
      case JOIN:
      case GROUPBY:
        ps.getConfigInfo().addValidStmt(queryId,
                                        operandQueryId,
                                        operandQueryId.equals(pktLogStr));
        predPacketFields.add(PipeConfigInfo.fieldTransformQueryId(queryId));
        break;
      default:
        assert(false);
        break;
    }
  }

  public HashSet<String> getAllPacketFields(ArrayList<PipeStage> pipe) {
    HashSet<String> fields = new HashSet<>();
    for (PipeStage stage: pipe) {
      fields.addAll(stage.getConfigInfo().getPacketFields());
    }
    return fields;
  }

  public HashSet<String> getAllRegisters(ArrayList<PipeStage> pipe) {
    HashSet<String> registers = new HashSet<>();
    for (PipeStage stage: pipe) {
      registers.addAll(stage.getConfigInfo().getRegisters());
    }
    return registers;
  }

  public String getAllRegisterDeclsDomino(ArrayList<PipeStage> pipe) {
    HashSet<String> registers = getAllRegisters(pipe);
    String res = "";
    for (String reg: registers) {
      res += ("int " + reg + " = 0;\n");
    }
    return res;
  }

  public HashSet<String> getAllNonRegisters(ArrayList<PipeStage> pipe) {
    HashSet<String> nonRegisters = new HashSet<>();
    for (PipeStage stage: pipe) {
      nonRegisters.addAll(stage.getConfigInfo().getNonRegisters());
    }
    return nonRegisters;
  }

  public ArrayList<ThreeOpDecl> getPacketFieldDeclList(ArrayList<PipeStage> pipe) {
    ArrayList<ThreeOpDecl> decls = new ArrayList<>();
    HashSet<String> fieldsChecked = new HashSet<>();
    for (PipeStage stage: pipe) {
      /// Collect all fields that could potentially be packet metadata fields
      HashSet<String> fields = new HashSet<>(stage.getConfigInfo().getPacketFields());
      fields.addAll(stage.getConfigInfo().getRegisters());
      for (String field: fields) {
        /// Only look to add declaration if field hasn't been looked at so far.
        if (! fieldsChecked.contains(field)) {
          if (predPacketFields.contains(field)) { // predicate variable
            decls.add(new ThreeOpDecl(P4Printer.BOOL_WIDTH, P4Printer.INT_TYPE, field));
          } else if ((! Fields.fields.contains(field))) { // internal function variable
            decls.add(new ThreeOpDecl(P4Printer.INT_WIDTH, P4Printer.INT_TYPE, field));
          } // Otherwise: packet fields which are headers or the usual metadata
          fieldsChecked.add(field);
        }
      }
    }
    return decls;
  }

  private ArrayList<ThreeOpDecl> getNonRegisterDeclList(ArrayList<PipeStage> pipe) {
    ArrayList<ThreeOpDecl> decls = new ArrayList<>();
    HashSet<String> fieldsChecked = new HashSet<>();
    for (PipeStage stage: pipe) {
      /// Collect all fields that could potentially be packet metadata fields
      HashSet<String> fields = stage.getConfigInfo().getAllFields();
      for (String field: fields) {
        /// Only look to add declaration if field hasn't been looked at so far.
        if (! fieldsChecked.contains(field)) {
          decls.add(new ThreeOpDecl(P4Printer.INT_WIDTH, P4Printer.INT_TYPE,
                                    DominoPrinter.getDominoVarName(field)));
          fieldsChecked.add(field);
        }
      }
    }
    return decls;
  }

  public String getNonRegisterDeclsDomino(ArrayList<PipeStage> pipe) {
    ArrayList<ThreeOpDecl> decls = getNonRegisterDeclList(pipe);
    String res = "";
    for (ThreeOpDecl decl: decls) {
      res += decl.getDomino();
    }
    return res;
  }
}
