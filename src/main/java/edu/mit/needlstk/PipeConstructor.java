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
  
  public PipeConstructor(HashMap<String, PipeStage> stages,
                         HashMap<String, Operation> depTable,
                         String lastAssignedId) {
    this.depTable = depTable;
    this.stages = stages;
    this.lastAssignedId = lastAssignedId;
    this.visited = new HashSet<>();
  }

  public ArrayList<PipeStage> stitchPipe() {
    this.pipe = getPipes(lastAssignedId);
    return this.pipe;
  }

  private ArrayList<PipeStage> getPipes(String queryId) {
    assert (depTable.containsKey(queryId)); // queryId should be in the depTable.
    assert (stages.containsKey(queryId));   // queryId should be one of the stages.
    Operation op = depTable.get(queryId);
    ArrayList<PipeStage> stageList = new ArrayList<>();
    for (String operand: op.operands) {
      if((! operand.equals(pktLogStr)) && (! visited.contains(operand))) {
        stageList.addAll(getPipes(operand));
      }
      addValidStmt(queryId, operand, op.opcode, stages.get(queryId));
    }
    stageList.add(stages.get(queryId));
    visited.add(queryId);
    return stageList;
  }

  private String transformQueryId(String queryId) {
    return "_" + queryId + "_valid";
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
        ps.getConfigInfo().addValidStmt(transformQueryId(queryId),
                                        transformQueryId(operandQueryId));
        break;
      default:
        assert(false);
        break;
    }
  }
}
